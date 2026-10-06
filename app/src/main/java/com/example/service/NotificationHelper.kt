package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "catp_schedule_adjustments"
    const val CHANNEL_NAME = "Lịch công tác Ban Giám đốc CATP"
    const val CHANNEL_SERVICE_ID = "catp_background_service"
    const val CHANNEL_SERVICE_NAME = "Dịch vụ giám sát ngầm Cổng CATP"
    private const val NOTIFICATION_ID = 1001
    const val SERVICE_NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // High priority channel for instant alerts
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Thông báo tức thì khi Cổng thông tin điều chỉnh lịch Ban Giám đốc CATP"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)

            // Completely silent, non-intrusive channel for ongoing background service
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                CHANNEL_SERVICE_NAME,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Dịch vụ đồng bộ dữ liệu ngầm"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Builds ongoing foreground notification for BackgroundSyncService.
     */
    fun buildServiceNotification(
        context: Context,
        intervalMinutes: Int,
        lastCheckedTime: String,
        lastRealChangeTimeAgo: String = "10 phút trước",
        statusSummary: String = "Dữ liệu đang khớp 100% với Cổng thông tin"
    ): android.app.Notification {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "WEB_MONITOR")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "Lần thay đổi gần nhất: $lastRealChangeTimeAgo • Quét $intervalMinutes phút/lần"
        val bigText = "• Lần thay đổi gần nhất: $lastRealChangeTimeAgo\n• Lần kiểm tra gần nhất: $lastCheckedTime\n• Trạng thái: $statusSummary"

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Lịch Ban Giám đốc CATP")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun cancelServiceNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(SERVICE_NOTIFICATION_ID)
    }

    /**
     * Clears all notifications from the status bar immediately.
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        notificationManager?.cancel(SERVICE_NOTIFICATION_ID)
        notificationManager?.cancelAll()
    }

    /**
     * Posts an instant high-priority system notification when schedule changes/adjustments are detected.
     */
    fun showScheduleAdjustmentNotification(
        context: Context,
        editionNumber: Int,
        changeCount: Int,
        details: String,
        customTitle: String? = null
    ) {
        // Thông báo ngay khi có lịch điều chỉnh mới hoặc bổ sung

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        // Intent to open MainActivity directly to the WebMonitor screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "WEB_MONITOR")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            !customTitle.isNullOrBlank() -> customTitle
            editionNumber > 0 -> "🔔 LỊCH BAN GIÁM ĐỐC: ĐIỀU CHỈNH LẦN THỨ $editionNumber"
            else -> "🔔 CẬP NHẬT LỊCH CÔNG TÁC BAN GIÁM ĐỐC"
        }

        val shortMessage = if (editionNumber > 0) {
            "Phát hiện Lịch điều chỉnh lần thứ $editionNumber ($changeCount nội dung thay đổi/bổ sung)."
        } else {
            "Phát hiện $changeCount nội dung thay đổi/bổ sung mới từ Cổng thông tin điện tử."
        }

        val bigText = if (details.isNotBlank()) {
            "$shortMessage\n\n$details"
        } else {
            shortMessage
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
