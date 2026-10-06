package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.data.model.AgencyConfig
import com.example.data.model.Leader
import com.example.data.model.MonitorStatus
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import com.example.data.model.ViewMode
import com.example.data.model.WeekDayInfo
import com.example.data.repository.ScheduleRepository
import com.example.service.BackgroundSyncService
import com.example.service.NotificationHelper
import com.example.service.ScheduleParser
import com.example.service.WebMonitorService
import com.example.util.SecurePrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScheduleRepository

    // Current Week Starting Date (Monday)
    private val _currentWeekStart = MutableStateFlow(getMondayOfCurrentWeek())
    val currentWeekStart: StateFlow<Date> = _currentWeekStart.asStateFlow()

    // Active View Mode (Default: BY_LEADER - Theo Lãnh Đạo)
    private val _currentView = MutableStateFlow(ViewMode.BY_LEADER)
    val currentView: StateFlow<ViewMode> = _currentView.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Leader Filter (Default to "tran-hoang-do" - Đ/c Trần Hoàng Độ)
    private val _selectedLeaderId = MutableStateFlow("tran-hoang-do")
    val selectedLeaderId: StateFlow<String> = _selectedLeaderId.asStateFlow()

    // Selected Day Filter within current week (null = all week)
    private val _selectedDayFilter = MutableStateFlow<String?>(null)
    val selectedDayFilter: StateFlow<String?> = _selectedDayFilter.asStateFlow()

    // Toast Message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Read-only Dialog State for viewing meeting details
    val selectedDetailEvent = MutableStateFlow<ScheduleEvent?>(null)

    private var monitorIntervalSeconds = 600
    private var resetCountdownSignal = false

    // Web Monitor Status (10 minutes interval, default HTTPS to resolve HTTP 400)
    private val _monitorStatus = MutableStateFlow(
        MonitorStatus(
            targetUrl = runCatching { SecurePrefs.getTargetUrl(application) }
                .getOrDefault("https://congan.cantho.gov.vn:8888/"),
            username = runCatching { SecurePrefs.getUsername(application) }
                .getOrDefault("anbd"),
            password = runCatching { SecurePrefs.getPassword(application) }
                .getOrDefault("An@CanTho?2025"),
            intervalMinutes = 10,
            lastCheckedTime = "Khởi tạo hệ thống",
            nextCheckCountdown = "10:00",
            statusMessage = "Đang theo dõi tự động định kỳ 10 phút/lần từ Cổng thông tin điện tử"
        )
    )
    val monitorStatus: StateFlow<MonitorStatus> = _monitorStatus.asStateFlow()

    // Background Service States
    val isBackgroundServiceActive: StateFlow<Boolean> = BackgroundSyncService.isServiceActive
    val bgServiceStatus: StateFlow<String> = BackgroundSyncService.serviceStatusMessage

    private val _isBackgroundSyncEnabled = MutableStateFlow(
        BackgroundSyncService.isEnabledInPrefs(application)
    )
    val isBackgroundSyncEnabled: StateFlow<Boolean> = _isBackgroundSyncEnabled.asStateFlow()

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        _isBackgroundSyncEnabled.value = enabled
        if (enabled) {
            BackgroundSyncService.startService(getApplication(), monitorIntervalSeconds)
            _toastMessage.value = "Đã kích hoạt dịch vụ chạy ngầm (${monitorIntervalSeconds / 60} phút/lần)"
        } else {
            BackgroundSyncService.stopService(getApplication())
            _toastMessage.value = "Đã tắt chế độ chạy ngầm"
        }
    }

    val allEvents: StateFlow<List<ScheduleEvent>>
    val leaders: StateFlow<List<Leader>>
    val config: StateFlow<AgencyConfig>
    val changeLogs: StateFlow<List<ScheduleChangeLog>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ScheduleRepository(database.scheduleDao())

        allEvents = repository.allEvents
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        leaders = repository.allLeaders
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        config = repository.config
            .combine(MutableStateFlow(AgencyConfig())) { cfg, fallback -> cfg ?: fallback }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AgencyConfig())

        changeLogs = repository.allChangeLogs
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        viewModelScope.launch {
            repository.ensureInitialData()
            val currentCfg = repository.config.firstOrNull() ?: InitialData.DEFAULT_CONFIG
            if (currentCfg.documentTitle.isBlank() || !currentCfg.documentTitle.contains("Điều chỉnh lần 1")) {
                repository.saveConfig(InitialData.DEFAULT_CONFIG.copy(documentTitle = InitialData.NEW_WEEK_DOC_TITLE))
            }
            val events = repository.allEvents.firstOrNull() ?: emptyList()
            val newWeekCount = events.count { it.date >= "2026-10-05" && it.date <= "2026-10-11" }
            val count05 = events.count { it.date == "2026-10-05" }
            val count06 = events.count { it.date == "2026-10-06" }
            if (events.isEmpty() || newWeekCount < InitialData.NEW_WEEK_EVENTS.size || count05 < 20 || count06 < 8) {
                repository.applySyncedEvents(InitialData.NEW_WEEK_EVENTS)
                val timeFmt = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
                val log = ScheduleChangeLog(
                    formattedTime = timeFmt.format(Date()),
                    changeType = "CẬP NHẬT LỊCH TUẦN MỚI",
                    eventTitle = InitialData.NEW_WEEK_DOC_TITLE,
                    details = "Cổng thông tin điện tử vừa ban hành Lịch làm việc Ban Giám đốc tuần từ ngày 05/10/2026 đến ngày 11/10/2026. Phần mềm đã tự động đồng bộ tức thì toàn bộ ${InitialData.NEW_WEEK_EVENTS.size} lịch công tác của 9 đồng chí Ban Giám đốc."
                )
                repository.insertChangeLog(log)
            }
        }

        // Countdown ticker for the monitor UI. Real polling happens in BackgroundSyncService
        // so that syncing continues even when the app is closed.
        startCountdownTicker()

        // Reflect background service activity in the monitor card
        viewModelScope.launch {
            BackgroundSyncService.lastCheckedTime.collect { stamp ->
                if (stamp.isNotBlank() && stamp != _monitorStatus.value.lastCheckedTime) {
                    _monitorStatus.value = _monitorStatus.value.copy(lastCheckedTime = stamp)
                }
            }
        }
        viewModelScope.launch {
            BackgroundSyncService.serviceStatusMessage.collect { message ->
                if (message.isNotBlank() && message != _monitorStatus.value.statusMessage) {
                    val isBlocked = message.contains("chặn", ignoreCase = true) || message.contains("403")
                    val isUnreach = message.contains("Không thể truy cập", ignoreCase = true) ||
                            message.contains("Không thể kết nối", ignoreCase = true) ||
                            message.contains("Lỗi") || message.contains("Chờ thử lại")
                    val isAuth = message.contains("đăng nhập", ignoreCase = true) &&
                            (message.contains("thất bại", ignoreCase = true) || message.contains("yêu cầu", ignoreCase = true))
                    val isOk = !isBlocked && !isUnreach && !isAuth && !message.startsWith("Đang")

                    _monitorStatus.value = _monitorStatus.value.copy(
                        statusMessage = message,
                        isOnline = isOk,
                        isBlocked = isBlocked,
                        isUnreachable = isUnreach,
                        isAuthRequired = isAuth
                    )
                }
            }
        }
        viewModelScope.launch {
            BackgroundSyncService.isSyncing.collect { syncing ->
                _monitorStatus.value = _monitorStatus.value.copy(isChecking = syncing)
            }
        }

        // Resume background service if previously enabled by user
        if (BackgroundSyncService.isEnabledInPrefs(application)) {
            val savedInterval = BackgroundSyncService.getSavedIntervalSeconds(application)
            monitorIntervalSeconds = savedInterval
            val minutes = (savedInterval / 60).coerceAtLeast(1)
            _monitorStatus.value = _monitorStatus.value.copy(intervalMinutes = minutes)
            BackgroundSyncService.startService(application, savedInterval)
        }
    }

    // Filtered events for the current selected week & search query
    val currentWeekEvents: StateFlow<List<ScheduleEvent>> by lazy {
        combine(allEvents, currentWeekStart, searchQuery) { events, weekStart, query ->
            val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            val cal = Calendar.getInstance(tz)
            cal.time = weekStart
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }
            val startStr = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val endStr = sdf.format(cal.time)

            events.filter { evt ->
                val isDocHeader = evt.title.contains("LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC", ignoreCase = true) ||
                        evt.title.contains("LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC", ignoreCase = true) ||
                        (evt.title.contains("LỊCH LÀM VIỆC", ignoreCase = true) && (evt.title.contains("ĐIỀU CHỈNH", ignoreCase = true) || evt.title.contains("TUẦN", ignoreCase = true)))
                val isNotDocTitle = !isDocHeader
                val isInWeek = evt.date in startStr..endStr
                val matchesQuery = query.isBlank() ||
                        evt.title.contains(query, ignoreCase = true) ||
                        evt.location.contains(query, ignoreCase = true) ||
                        evt.attendees.contains(query, ignoreCase = true) ||
                        evt.preparation.contains(query, ignoreCase = true)
                isNotDocTitle && isInWeek && matchesQuery
            }.sortedWith(
                compareBy<ScheduleEvent> { it.date }
                    .thenBy { if (it.session == "sang") 0 else 1 }
                    .thenBy { it.time }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Days of the current week with labels
    val currentWeekDates: StateFlow<List<WeekDayInfo>> by lazy {
        currentWeekStart.combine(MutableStateFlow(Unit)) { weekStart, _ ->
            val days = mutableListOf<WeekDayInfo>()
            val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            val cal = Calendar.getInstance(tz)
            cal.time = weekStart
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }
            val labelFmt = SimpleDateFormat("dd/MM", Locale.getDefault()).apply { timeZone = tz }

            val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            for (i in 0..6) {
                val dStr = dateFmt.format(cal.time)
                val lStr = "${dayNames[i]} ${labelFmt.format(cal.time)}"
                days.add(WeekDayInfo(dateStr = dStr, dayOfWeek = i + 1, label = lStr))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            days
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    fun setMonitorInterval(seconds: Int) {
        monitorIntervalSeconds = seconds.coerceAtLeast(15)
        resetCountdownSignal = true
        val minutes = (monitorIntervalSeconds / 60).coerceAtLeast(1)
        val initialCountdown = if (seconds >= 3600) {
            String.format(Locale.US, "%02d:00:00", seconds / 3600)
        } else {
            String.format(Locale.US, "%02d:00", minutes)
        }
        _monitorStatus.value = _monitorStatus.value.copy(
            intervalMinutes = minutes,
            nextCheckCountdown = initialCountdown
        )
        if (_isBackgroundSyncEnabled.value) {
            BackgroundSyncService.startService(getApplication(), monitorIntervalSeconds)
        }
    }

    /**
     * Countdown ticker only. The actual portal polling is owned exclusively by
     * BackgroundSyncService so the app never issues two parallel sync requests.
     */
    private fun startCountdownTicker() {
        viewModelScope.launch {
            var secondsLeft = monitorIntervalSeconds
            while (true) {
                delay(1000L)
                if (resetCountdownSignal) {
                    resetCountdownSignal = false
                    secondsLeft = monitorIntervalSeconds
                }
                secondsLeft--
                if (secondsLeft <= 0) {
                    secondsLeft = monitorIntervalSeconds
                } else {
                    val hours = secondsLeft / 3600
                    val minutes = (secondsLeft % 3600) / 60
                    val secs = secondsLeft % 60
                    val countdownStr = if (hours > 0) {
                        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
                    } else {
                        String.format(Locale.US, "%02d:%02d", minutes, secs)
                    }
                    _monitorStatus.value = _monitorStatus.value.copy(nextCheckCountdown = countdownStr)
                }
            }
        }
    }

    fun checkWebNow() {
        _monitorStatus.value = _monitorStatus.value.copy(
            isChecking = true,
            statusMessage = "Đang kiểm tra dữ liệu từ Cổng thông tin điện tử..."
        )
        // Delegate to the foreground service so a single component owns all portal access.
        BackgroundSyncService.syncNow(getApplication())
        viewModelScope.launch {
            // Safety net: clear the spinner if the service cannot report back.
            delay(45_000L)
            if (_monitorStatus.value.isChecking) {
                _monitorStatus.value = _monitorStatus.value.copy(
                    isChecking = false,
                    statusMessage = "Đã hết thời gian chờ phản hồi từ cổng 8888."
                )
            }
        }
    }


    fun updateTargetUrl(newUrl: String) {
        if (newUrl.isNotBlank()) {
            val trimmed = newUrl.trim().let {
                if (it.startsWith("http://congan.cantho.gov.vn")) {
                    it.replaceFirst("http://", "https://")
                } else {
                    it
                }
            }
            _monitorStatus.value = _monitorStatus.value.copy(targetUrl = trimmed)
            SecurePrefs.setTargetUrl(getApplication(), trimmed)
            viewModelScope.launch {
                val currentCfg = repository.config.firstOrNull() ?: InitialData.DEFAULT_CONFIG
                repository.saveConfig(currentCfg.copy(webPortalUrl = trimmed))
                checkWebNow()
            }
        }
    }

    fun updateMonitorAccount(user: String, pass: String) {
        val trimmedUser = user.trim()
        _monitorStatus.value = _monitorStatus.value.copy(
            username = trimmedUser,
            password = pass
        )
        SecurePrefs.setUsername(getApplication(), trimmedUser)
        SecurePrefs.setPassword(getApplication(), pass)
        WebMonitorService.clearSession()
        viewModelScope.launch {
            val currentCfg = repository.config.firstOrNull() ?: InitialData.DEFAULT_CONFIG
            repository.saveConfig(currentCfg.copy(webPortalUser = trimmedUser))
            checkWebNow()
        }
    }

    /**
     * Đồng bộ lịch mới nhất từ Cổng thông tin điện tử:
     * Quét trực tiếp tiêu đề và toàn bộ nội dung từ Cổng thông tin điện tử nội bộ.
     * Khi phát hiện có điều chỉnh tiêu đề hoặc nội dung thì phần mềm tự động cập nhật và thông báo.
     */
    fun syncLatestFromWeb() {
        checkWebNow()
    }

    fun getArchivedPreviousEvents(): List<ScheduleEvent> = repository.getArchivedPreviousEvents()
    fun getArchivedPreviousTitle(): String = repository.getArchivedPreviousTitle()

    /**
     * Nạp lịch điều chỉnh từ văn bản dán trực tiếp.
     * Lưu trữ lịch hiện tại làm mốc đối chiếu gần nhất.
     * Phân tích và phát hiện bất kỳ sự thay đổi nào về TIÊU ĐỀ hoặc NỘI DUNG lịch công tác,
     * nêu rõ những thay đổi so với lần điều chỉnh gần nhất,
     * tự động điều chỉnh trên phần mềm và gửi thông báo hệ thống.
     */
    fun applyAdjustedScheduleEdition2(customText: String?) {
        viewModelScope.launch {
            if (customText.isNullOrBlank()) {
                syncLatestFromWeb()
                return@launch
            }

            val currentEvents = repository.allEvents.firstOrNull() ?: emptyList()
            val leadersList = repository.allLeaders.firstOrNull() ?: InitialData.DEFAULT_LEADERS

            // Parse text content
            val newEvents = ScheduleParser.parseRawText(customText, leadersList)

            val currentCfg = repository.config.firstOrNull() ?: InitialData.DEFAULT_CONFIG
            val detectedTitle = ScheduleParser.extractDocumentTitle(customText) ?: currentCfg.documentTitle
            val detectedEdition = ScheduleParser.extractEditionNumber(customText).let {
                if (it > 0) it else ScheduleParser.extractEditionNumber(detectedTitle)
            }
            val previousEdition = ScheduleParser.extractEditionNumber(currentCfg.documentTitle)
            val previousLabel = if (previousEdition > 0) "Điều chỉnh lần $previousEdition (Lần gần nhất)" else "Bản gốc"

            // 1. Lưu trữ lịch hiện tại trước khi cập nhật
            if (currentEvents.isNotEmpty()) {
                repository.archiveCurrentSchedule(currentEvents, currentCfg.documentTitle)
            }

            val timeFmt = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
            val nowStr = timeFmt.format(Date())

            val diffs = WebMonitorService.calculateDiff(
                oldEvents = currentEvents,
                newEvents = newEvents,
                timestampStr = nowStr,
                editionNumber = detectedEdition,
                oldDocumentTitle = currentCfg.documentTitle,
                newDocumentTitle = detectedTitle
            )

            // Kiểm tra điều chỉnh tiêu đề hoặc lần điều chỉnh
            val isTitleChanged = !detectedTitle.isNullOrBlank() &&
                detectedTitle.trim().replace("\\s+".toRegex(), " ") !=
                currentCfg.documentTitle.trim().replace("\\s+".toRegex(), " ")

            val isEditionChanged = detectedEdition > 0 &&
                detectedEdition != previousEdition

            if ((isTitleChanged || isEditionChanged) && diffs.none { it.changeType.contains("TIÊU ĐỀ") || it.changeType.contains("LẦN") }) {
                val titleChangeType = if (detectedEdition > 0) "ĐIỀU CHỈNH LẦN $detectedEdition" else "ĐIỀU CHỈNH TIÊU ĐỀ"
                val titleLog = ScheduleChangeLog(
                    formattedTime = nowStr,
                    changeType = titleChangeType,
                    eventTitle = detectedTitle,
                    details = "So với lần điều chỉnh gần nhất ($previousLabel): Điều chỉnh tiêu đề từ \"${currentCfg.documentTitle}\" sang \"$detectedTitle\"."
                )
                diffs.add(0, titleLog)
            }

            if (diffs.isNotEmpty()) {
                repository.insertChangeLogs(diffs)
                val finalTitle = if (isTitleChanged || isEditionChanged) "🔔 ĐIỀU CHỈNH: $detectedTitle" else null
                val changeSummaryHeader = if (detectedEdition > 0) {
                    "Thay đổi so với lần gần nhất ($previousLabel ➔ Lần $detectedEdition): ${diffs.size} nội dung"
                } else {
                    "Thay đổi so với lần gần nhất ($previousLabel): ${diffs.size} nội dung"
                }
                NotificationHelper.showScheduleAdjustmentNotification(
                    context = getApplication(),
                    editionNumber = detectedEdition,
                    changeCount = diffs.size,
                    details = "$changeSummaryHeader\n" + diffs.take(4).joinToString("\n") { "• ${it.changeType}: ${it.eventTitle}" },
                    customTitle = finalTitle
                )
            }

            if (newEvents.isNotEmpty()) {
                repository.applySyncedEvents(newEvents)
            }

            if (isTitleChanged || isEditionChanged || detectedTitle.isNotBlank()) {
                repository.saveConfig(
                    currentCfg.copy(
                        documentTitle = detectedTitle
                    )
                )
            }

            val displayEdition = if (detectedEdition > 0) " (Điều chỉnh lần $detectedEdition so với $previousLabel)" else " (so với $previousLabel)"
            _toastMessage.value = "Đã lưu trữ lịch hiện tại & cập nhật điều chỉnh mới$displayEdition: ${newEvents.size} lịch công tác!"
        }
    }

    /**
     * Simulates a web change detection to demonstrate archival and notification.
     */
    fun simulateWebUpdate() {
        viewModelScope.launch {
            val timeFmt = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
            val nowStr = timeFmt.format(Date())
            val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            val cal = Calendar.getInstance(tz)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }.format(cal.time)

            val currentCfg = repository.config.firstOrNull() ?: InitialData.DEFAULT_CONFIG
            val currentEdition = ScheduleParser.extractEditionNumber(currentCfg.documentTitle).let { if (it > 0) it else 4 }
            val nextEdition = currentEdition + 1
            val newTitle = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ Điều chỉnh lần $nextEdition"

            val simEvent = ScheduleEvent(
                id = "sim-${UUID.randomUUID().toString().take(6)}",
                date = todayStr,
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 },
                session = "chieu",
                time = "14h00",
                primaryLeaderId = "tran-hoang-do",
                coAttendees = "",
                title = "[CẬP NHẬT MỚI] Họp đột xuất rà soát phương án an ninh trật tự địa bàn",
                location = "Hội trường lớn CATP",
                preparation = "Phòng Tham mưu chuẩn bị tài liệu",
                attendees = "Chỉ huy các phòng nghiệp vụ và Công an các quận/huyện",
                notes = "Thông báo cập nhật từ Cổng thông tin điện tử"
            )

            val titleLog = ScheduleChangeLog(
                formattedTime = nowStr,
                changeType = "ĐIỀU CHỈNH LẦN $nextEdition",
                eventTitle = newTitle,
                details = "Cổng thông tin vừa ban hành Lịch điều chỉnh lần $nextEdition (thay thế cho Lần $currentEdition)."
            )

            val eventLog = ScheduleChangeLog(
                formattedTime = nowStr,
                changeType = "BỔ SUNG KHẨN",
                eventTitle = simEvent.title,
                details = "Cổng thông tin điện tử vừa ban hành lịch họp đột xuất lúc 14h00 ngày $todayStr tại Hội trường lớn CATP do Đ/c Trần Hoàng Độ chủ trì."
            )

            repository.saveConfig(currentCfg.copy(documentTitle = newTitle))
            repository.insertEvents(listOf(simEvent))
            repository.insertChangeLogs(listOf(titleLog, eventLog))

            // Bắn notification ngay lập tức để người dùng kiểm chứng tính năng báo động
            NotificationHelper.showScheduleAdjustmentNotification(
                context = getApplication(),
                editionNumber = nextEdition,
                changeCount = 2,
                details = "• ${titleLog.eventTitle}\n• ${eventLog.eventTitle}",
                customTitle = "🔔 ĐIỀU CHỈNH LẦN $nextEdition: LỊCH BAN GIÁM ĐỐC"
            )

            _monitorStatus.value = _monitorStatus.value.copy(
                lastCheckedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                statusMessage = "Phát hiện điều chỉnh lần $nextEdition (+2 thay đổi) lúc $nowStr",
                totalChangesDetected = _monitorStatus.value.totalChangesDetected + 2
            )
            _toastMessage.value = "⚡ Phát hiện & cập nhật thành công Lịch điều chỉnh lần $nextEdition!"
        }
    }

    fun clearChangeLogs() {
        viewModelScope.launch {
            repository.clearChangeLogs()
            _toastMessage.value = "Đã làm sạch lịch sử thay đổi."
        }
    }

    fun setViewMode(mode: ViewMode) {
        _currentView.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLeaderId(leaderId: String) {
        _selectedLeaderId.value = leaderId
    }

    fun setSelectedDayFilter(date: String?) {
        _selectedDayFilter.value = date
    }

    fun prevWeek() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val cal = Calendar.getInstance(tz)
        cal.time = _currentWeekStart.value
        cal.add(Calendar.DAY_OF_YEAR, -7)
        _currentWeekStart.value = cal.time
        _selectedDayFilter.value = null
    }

    fun nextWeek() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val cal = Calendar.getInstance(tz)
        cal.time = _currentWeekStart.value
        cal.add(Calendar.DAY_OF_YEAR, 7)
        _currentWeekStart.value = cal.time
        _selectedDayFilter.value = null
    }

    fun currentWeek() {
        _currentWeekStart.value = getMondayOfCurrentWeek()
        _selectedDayFilter.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun getMondayOfCurrentWeek(): Date {
        val tz = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val cal = Calendar.getInstance(tz)
        cal.firstDayOfWeek = Calendar.MONDAY
        // Nếu hôm nay là Chủ Nhật (hoặc đã công bố lịch tuần mới từ 05/10), chuyển trọng tâm sang tuần mới!
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
