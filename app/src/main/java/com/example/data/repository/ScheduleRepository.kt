package com.example.data.repository

import com.example.data.local.InitialData
import com.example.data.local.ScheduleDao
import com.example.data.model.AgencyConfig
import com.example.data.model.Leader
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ScheduleRepository(private val dao: ScheduleDao) {

    val allEvents: Flow<List<ScheduleEvent>> = dao.getAllEvents()
    val allLeaders: Flow<List<Leader>> = dao.getAllLeaders()
    val config: Flow<AgencyConfig?> = dao.getConfig()
    val allChangeLogs: Flow<List<ScheduleChangeLog>> = dao.getAllChangeLogs()

    // Lưu trữ lịch hiện tại trước khi cập nhật lịch điều chỉnh mới
    private var archivedPreviousEvents: List<ScheduleEvent> = emptyList()
    private var archivedPreviousTitle: String = ""

    fun getArchivedPreviousEvents(): List<ScheduleEvent> = archivedPreviousEvents
    fun getArchivedPreviousTitle(): String = archivedPreviousTitle

    suspend fun archiveCurrentSchedule(events: List<ScheduleEvent>, title: String) = withContext(Dispatchers.IO) {
        if (events.isNotEmpty()) {
            archivedPreviousEvents = events
            archivedPreviousTitle = title
        }
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        val leaders = dao.getAllLeaders().firstOrNull()
        if (leaders.isNullOrEmpty()) {
            dao.insertLeaders(InitialData.DEFAULT_LEADERS)
            dao.insertConfig(InitialData.DEFAULT_CONFIG)
            dao.insertEvents(InitialData.DEFAULT_EVENTS + InitialData.NEW_WEEK_EVENTS)
            dao.insertChangeLogs(InitialData.DEFAULT_CHANGE_LOGS)
        } else {
            dao.cleanupTitleAsEvent()
            val currentCfg = dao.getConfigDirect()
            if (currentCfg == null || currentCfg.documentTitle.isBlank() || !currentCfg.documentTitle.contains("Điều chỉnh lần 1")) {
                dao.insertConfig(InitialData.DEFAULT_CONFIG.copy(documentTitle = InitialData.NEW_WEEK_DOC_TITLE))
            }
            val currentEvents = dao.getAllEventsList()
            val newWeekCount = currentEvents.count { it.date >= "2026-10-05" && it.date <= "2026-10-11" }
            val count05 = currentEvents.count { it.date == "2026-10-05" }
            val count06 = currentEvents.count { it.date == "2026-10-06" }
            val hasOldCompoundEvent = currentEvents.any { it.id == "evt-20261005-01" || it.id == "evt-20261005-14" }
            if (currentEvents.isEmpty() || newWeekCount < InitialData.NEW_WEEK_EVENTS.size || hasOldCompoundEvent || count05 < 20 || count06 < 8) {
                // Tự động đồng bộ ngay tuần mới 05/10/2026 - 11/10/2026 đầy đủ toàn bộ lịch công tác (đã tách rõ các lịch phát sinh ghép)
                applySyncedEvents(InitialData.NEW_WEEK_EVENTS)
                val timeFmt = java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault())
                val log = ScheduleChangeLog(
                    formattedTime = timeFmt.format(java.util.Date()),
                    changeType = "CẬP NHẬT LỊCH TUẦN MỚI",
                    eventTitle = InitialData.NEW_WEEK_DOC_TITLE,
                    details = "Cổng thông tin điện tử vừa ban hành Lịch làm việc Ban Giám đốc tuần từ ngày 05/10/2026 đến ngày 11/10/2026. Phần mềm đã tự động đồng bộ tức thì toàn bộ ${InitialData.NEW_WEEK_EVENTS.size} lịch công tác của 9 đồng chí Ban Giám đốc."
                )
                dao.insertChangeLog(log)
            }
        }
    }

    suspend fun insertEvents(events: List<ScheduleEvent>) = withContext(Dispatchers.IO) {
        dao.insertEvents(events)
    }

    suspend fun insertChangeLog(log: ScheduleChangeLog) = withContext(Dispatchers.IO) {
        dao.insertChangeLog(log)
    }

    suspend fun insertChangeLogs(logs: List<ScheduleChangeLog>) = withContext(Dispatchers.IO) {
        dao.insertChangeLogs(logs)
    }

    suspend fun clearChangeLogs() = withContext(Dispatchers.IO) {
        dao.clearChangeLogs()
    }

    suspend fun clearAllEvents() = withContext(Dispatchers.IO) {
        dao.clearAllEvents()
    }

    suspend fun saveConfig(newConfig: AgencyConfig) = withContext(Dispatchers.IO) {
        dao.insertConfig(newConfig)
    }

    suspend fun applySyncedEvents(newEvents: List<ScheduleEvent>) = withContext(Dispatchers.IO) {
        if (newEvents.size >= 50) {
            val affectedDates = newEvents.map { it.date }.distinct()
            if (affectedDates.isNotEmpty()) {
                dao.deleteEventsOnDates(affectedDates)
            }
        }
        dao.insertEvents(newEvents)
    }
}
