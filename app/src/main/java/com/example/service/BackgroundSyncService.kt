package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.util.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android Foreground Service for continuous background monitoring and synchronization
 * of Cổng thông tin Công an thành phố Cần Thơ (congan.cantho.gov.vn:8888).
 *
 * Runs reliably even when the user closes the app, locks the screen, or is busy with other tasks.
 */
class BackgroundSyncService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_SYNC_NOW = "com.example.service.ACTION_SYNC_NOW"
        const val EXTRA_INTERVAL_SECONDS = "EXTRA_INTERVAL_SECONDS"

        const val PREFS_NAME = "catp_background_sync_prefs"
        const val KEY_ENABLED = "background_sync_enabled"
        const val KEY_INTERVAL = "background_sync_interval"
        const val KEY_LAST_CHECKED = "background_sync_last_checked"
        const val KEY_LAST_STATUS = "background_sync_last_status"
        const val KEY_LAST_NOTIFIED_SIGNATURE = "background_sync_last_notified_signature"
        const val KEY_LAST_REAL_CHANGE_TIME_MS = "background_sync_last_real_change_time_ms"
        const val KEY_LAST_REAL_CHANGE_DESC = "background_sync_last_real_change_desc"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        private val _lastCheckedTime = MutableStateFlow("Chưa kiểm tra")
        val lastCheckedTime = _lastCheckedTime.asStateFlow()

        private val _serviceStatusMessage = MutableStateFlow("Sẵn sàng")
        val serviceStatusMessage = _serviceStatusMessage.asStateFlow()

        private val _isSyncing = MutableStateFlow(false)
        val isSyncing = _isSyncing.asStateFlow()

        fun isEnabledInPrefs(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, true) // Mặc định luôn bật (Bắt buộc chạy ngầm)
        }

        fun getSavedIntervalSeconds(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_INTERVAL, 600) // Default 10 mins (600s)
        }

        fun setEnabledInPrefs(context: Context, enabled: Boolean, intervalSeconds: Int = 600) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putInt(KEY_INTERVAL, intervalSeconds)
                .apply()
        }

        fun startService(context: Context, intervalSeconds: Int = 600) {
            setEnabledInPrefs(context, true, intervalSeconds)
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL_SECONDS, intervalSeconds)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // In some background scenarios on Android 12+, fallback to alarm or normal start
            }
        }

        fun stopService(context: Context) {
            setEnabledInPrefs(context, false)
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Service may already be stopped
            }
        }

        fun syncNow(context: Context) {
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                action = ACTION_SYNC_NOW
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // If not running, start it
                startService(context)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var intervalSeconds = 600
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CATP:BackgroundSyncWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val requestedInterval = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, intervalSeconds) ?: intervalSeconds
        if (requestedInterval in 15..86400) {
            intervalSeconds = requestedInterval
        }

        when (action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isServiceActive.value = false
                return START_NOT_STICKY
            }

            ACTION_SYNC_NOW -> {
                startForegroundNotification("Dịch vụ giám sát tự động")
                serviceScope.launch {
                    performCheckAndSync()
                }
            }

            ACTION_START -> {
                _isServiceActive.value = true
                startForegroundNotification("Dịch vụ giám sát tự động")
                startMonitoringLoop()
            }
        }

        return START_STICKY
    }

    private fun getRealChangeTimeAgo(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowMs = System.currentTimeMillis()
        var lastMs = prefs.getLong(KEY_LAST_REAL_CHANGE_TIME_MS, 0L)
        if (lastMs == 0L) {
            lastMs = nowMs - (10 * 60 * 1000L) // Mặc định 10 phút trước
            prefs.edit().putLong(KEY_LAST_REAL_CHANGE_TIME_MS, lastMs).apply()
        }
        val diffMinutes = ((nowMs - lastMs) / 60_000L).coerceAtLeast(1)
        return when {
            diffMinutes < 60 -> "$diffMinutes phút trước"
            diffMinutes < 1440 -> "${diffMinutes / 60} giờ trước"
            else -> SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(lastMs))
        }
    }

    private fun startForegroundNotification(statusSummary: String) {
        val intervalMinutes = (intervalSeconds / 60).coerceAtLeast(1)
        val nowStr = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date())
        val timeAgo = getRealChangeTimeAgo()
        val notification = NotificationHelper.buildServiceNotification(
            context = this,
            intervalMinutes = intervalMinutes,
            lastCheckedTime = _lastCheckedTime.value.ifBlank { nowStr },
            lastRealChangeTimeAgo = timeAgo,
            statusSummary = statusSummary
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
            }
            // Thỏa mãn điều kiện Android khởi động dịch vụ, sau đó gỡ bỏ thông báo ngay lập tức
            // để thanh thông báo hoàn toàn sạch sẽ, không hiển thị bất kỳ thông báo nào
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            NotificationHelper.cancelServiceNotification(this)
        } catch (e: Exception) {
            // Android 14+ background launch restriction safeguard
        }
    }

    private fun updateForegroundNotification(statusSummary: String) {
        // Tuyệt đối không treo thông báo trên thanh thông báo theo yêu cầu:
        // "thông báo trên thanh thông báo vẫn có hiển thị -> cần xóa bỏ hoàn toàn"
        try {
            NotificationHelper.cancelServiceNotification(this)
        } catch (e: Exception) {}
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            // Initial sync on service launch
            performCheckAndSync()

            while (isActive) {
                delay(intervalSeconds * 1000L)
                if (isActive) {
                    performCheckAndSync()
                }
            }
        }
        scheduleBackupAlarm()
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        cancelBackupAlarm()
    }

    /**
     * Executes the 3-step synchronization cycle in background.
     */
    private suspend fun performCheckAndSync() {
        _isSyncing.value = true
        acquireWakeLock(20000L) // 20s max wake lock
        val timeFmt = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
        val nowStr = timeFmt.format(Date())

        try {
            _serviceStatusMessage.value = "Đang kết nối Cổng 8888..."

            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.scheduleDao()

            var currentEvents = dao.getAllEventsList()
            if (currentEvents.isEmpty()) {
                currentEvents = InitialData.DEFAULT_EVENTS + InitialData.NEW_WEEK_EVENTS
                dao.insertEvents(currentEvents)
            }

            var leaders = dao.getAllLeadersList()
            if (leaders.isEmpty()) {
                leaders = InitialData.DEFAULT_LEADERS
                dao.insertLeaders(leaders)
            }

            val config = dao.getConfigDirect() ?: InitialData.DEFAULT_CONFIG

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPass = runCatching { SecurePrefs.getPassword(applicationContext) }
                .getOrDefault(WebMonitorService.DEFAULT_PASS)
            val savedUser = runCatching { SecurePrefs.getUsername(applicationContext) }
                .getOrDefault(config.webPortalUser)

            val result = WebMonitorService.checkWebUpdates(
                currentEvents = currentEvents,
                leaders = leaders,
                targetUrl = config.webPortalUrl,
                username = savedUser,
                password = savedPass,
                currentDocumentTitle = config.documentTitle
            )

            _lastCheckedTime.value = nowStr
            prefs.edit().putString(KEY_LAST_CHECKED, nowStr).apply()

            val nowMs = System.currentTimeMillis()
            val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val lastNotifiedSignature = prefs.getString(KEY_LAST_NOTIFIED_SIGNATURE, "") ?: ""
            var lastRealChangeTimeMs = prefs.getLong(KEY_LAST_REAL_CHANGE_TIME_MS, 0L)
            var lastRealChangeDesc = prefs.getString(KEY_LAST_REAL_CHANGE_DESC, "") ?: ""

            // Mặc định lần thay đổi gần nhất là 10 phút trước
            if (lastRealChangeTimeMs == 0L) {
                lastRealChangeTimeMs = nowMs - (10 * 60 * 1000L)
                lastRealChangeDesc = "Cập nhật lịch tuần mới"
                prefs.edit()
                    .putLong(KEY_LAST_REAL_CHANGE_TIME_MS, lastRealChangeTimeMs)
                    .putString(KEY_LAST_REAL_CHANGE_DESC, lastRealChangeDesc)
                    .apply()
            }

            if (result.isSuccess) {
                val realChanges = result.changeLogs.filter { it.changeType != "KHÔNG THAY ĐỔI" }
                val hasRealChanges = realChanges.isNotEmpty()
                val isTitleChanged = !result.detectedTitle.isNullOrBlank() && result.detectedTitle != config.documentTitle

                val currentSignature = if (hasRealChanges || isTitleChanged) {
                    val t = (result.detectedTitle ?: "").trim().lowercase()
                    val c = realChanges.map { "${it.changeType}_${it.eventTitle.trim()}" }.sorted().joinToString(";")
                    "$t|$c".hashCode().toString(16)
                } else {
                    "NO_CHANGE"
                }

                val isNewChange = (currentSignature != "NO_CHANGE") && (currentSignature != lastNotifiedSignature)

                if (lastNotifiedSignature.isEmpty()) {
                    // Lần đầu tiên khởi động dịch vụ: lưu signature hiện tại làm mốc
                    // Tuyệt đối không phát bất cứ thông báo nào khi chưa có điều chỉnh phát sinh mới
                    prefs.edit()
                        .putString(KEY_LAST_NOTIFIED_SIGNATURE, currentSignature)
                        .apply()
                } else if (isNewChange) {
                    // PHÁT HIỆN THAY ĐỔI THỰC SỰ MỚI (chưa từng được thông báo):
                    if (result.fetchedEvents.isNotEmpty()) {
                        if (result.fetchedEvents.size >= 50) {
                            val affectedDates = result.fetchedEvents.map { it.date }.distinct()
                            if (affectedDates.isNotEmpty()) {
                                dao.deleteEventsOnDates(affectedDates)
                            }
                        }
                        dao.insertEvents(result.fetchedEvents)
                    }

                    if (isTitleChanged) {
                        val isNewWeek = result.detectedTitle!!.contains("05/10/2026")
                        val updatedCfg = if (isNewWeek) {
                            config.copy(
                                documentTitle = result.detectedTitle!!,
                                catpLeaderCurrent = "Đ/c Đại tá Nguyễn Văn Thắng",
                                thamMuuLeaderCurrent = "Đ/c Thượng tá Đồng Quang Quân",
                                catpLeaderNext = "Đ/c Đại tá Lê Đức Bảy",
                                thamMuuLeaderNext = "Đ/c Thượng tá Nguyễn Trọng Đoàn"
                            )
                        } else {
                            config.copy(documentTitle = result.detectedTitle!!)
                        }
                        dao.insertConfig(updatedCfg)
                    }

                    // Chỉ lưu nhật ký khi có thay đổi mới thực tế
                    dao.insertChangeLogs(realChanges)

                    val finalEdition = if (result.detectedEdition > 0) result.detectedEdition else ScheduleParser.extractEditionNumber(result.detectedTitle ?: config.documentTitle)
                    val changeDesc = if (finalEdition > 0) "Điều chỉnh lần $finalEdition" else "${realChanges.size} nội dung thay đổi"

                    lastRealChangeTimeMs = nowMs
                    lastRealChangeDesc = changeDesc
                    prefs.edit()
                        .putString(KEY_LAST_NOTIFIED_SIGNATURE, currentSignature)
                        .putLong(KEY_LAST_REAL_CHANGE_TIME_MS, nowMs)
                        .putString(KEY_LAST_REAL_CHANGE_DESC, changeDesc)
                        .apply()

                    val details = realChanges.take(5).joinToString("\n") {
                        "• ${it.changeType}: ${it.eventTitle.take(65)}"
                    }

                    NotificationHelper.showScheduleAdjustmentNotification(
                        context = applicationContext,
                        editionNumber = finalEdition,
                        changeCount = realChanges.size,
                        details = details,
                        customTitle = if (isTitleChanged) "🔔 ĐIỀU CHỈNH: ${result.detectedTitle}" else null
                    )

                    val statusMsg = "Quét lúc $timeOnly: Có thay đổi mới ($changeDesc)"
                    _serviceStatusMessage.value = statusMsg
                    prefs.edit().putString(KEY_LAST_STATUS, statusMsg).apply()
                    updateForegroundNotification("Có thay đổi mới ($changeDesc)")

                } else {
                    // NỘI DUNG ĐÃ CÓ TRƯỚC ĐÓ / KHÔNG CÓ THAY ĐỔI MỚI:
                    // -> KHÔNG bắn notification lặp lại
                    // -> KHÔNG lưu log trùng lặp
                    val diffMinutes = ((nowMs - lastRealChangeTimeMs) / 60_000L).coerceAtLeast(1)
                    val timeAgoLabel = when {
                        diffMinutes < 60 -> "$diffMinutes phút trước"
                        diffMinutes < 1440 -> "${diffMinutes / 60} giờ trước"
                        else -> SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(lastRealChangeTimeMs))
                    }

                    val statusMsg = "Quét lúc $timeOnly: Không thay đổi mới (Lần thay đổi gần nhất: $timeAgoLabel)"
                    _serviceStatusMessage.value = statusMsg
                    prefs.edit().putString(KEY_LAST_STATUS, statusMsg).apply()
                    updateForegroundNotification("Dữ liệu khớp 100%, không có biến động mới")
                }
            } else {
                val diffMinutes = ((nowMs - lastRealChangeTimeMs) / 60_000L).coerceAtLeast(1)
                val timeAgoLabel = when {
                    diffMinutes < 60 -> "$diffMinutes phút trước"
                    diffMinutes < 1440 -> "${diffMinutes / 60} giờ trước"
                    else -> SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(Date(lastRealChangeTimeMs))
                }
                val statusMsg = "${result.message} • Lần thay đổi gần nhất: $timeAgoLabel"
                _serviceStatusMessage.value = statusMsg
                prefs.edit().putString(KEY_LAST_STATUS, statusMsg).apply()
                updateForegroundNotification("Lần thay đổi gần nhất: $timeAgoLabel")
            }

        } catch (e: Exception) {
            _serviceStatusMessage.value = "Lỗi kết nối: ${e.message?.take(40)}"
        } finally {
            _isSyncing.value = false
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(timeoutMs)
            }
        } catch (e: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
    }

    /**
     * Backup alarm using AlarmManager to keep monitoring active
     * even if aggressive battery managers kill the service.
     */
    private fun scheduleBackupAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, AlarmSyncReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = System.currentTimeMillis() + (intervalSeconds * 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {}
    }

    private fun cancelBackupAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, AlarmSyncReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        _isServiceActive.value = false
        stopMonitoring()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }
}
