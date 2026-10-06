package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AgencyConfig
import com.example.data.model.Leader
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    // Schedule Events Queries
    @Query("SELECT * FROM schedule_events WHERE UPPER(title) NOT LIKE '%LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC%' AND UPPER(title) NOT LIKE '%LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC%' ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<ScheduleEvent>>

    @Query("SELECT * FROM schedule_events WHERE UPPER(title) NOT LIKE '%LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC%' AND UPPER(title) NOT LIKE '%LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC%' ORDER BY date ASC, time ASC")
    suspend fun getAllEventsList(): List<ScheduleEvent>

    @Query("DELETE FROM schedule_events WHERE UPPER(title) LIKE '%LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC%' OR UPPER(title) LIKE '%LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC%'")
    suspend fun cleanupTitleAsEvent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ScheduleEvent>)

    @Query("DELETE FROM schedule_events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM schedule_events WHERE date IN (:dates)")
    suspend fun deleteEventsOnDates(dates: List<String>)

    @Query("DELETE FROM schedule_events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<String>)

    // Leaders Queries
    @Query("SELECT * FROM leaders ORDER BY orderIndex ASC")
    fun getAllLeaders(): Flow<List<Leader>>

    @Query("SELECT * FROM leaders ORDER BY orderIndex ASC")
    suspend fun getAllLeadersList(): List<Leader>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaders(leaders: List<Leader>)

    @Query("DELETE FROM leaders")
    suspend fun clearLeaders()

    // Config Queries
    @Query("SELECT * FROM agency_config WHERE id = 1")
    fun getConfig(): Flow<AgencyConfig?>

    @Query("SELECT * FROM agency_config WHERE id = 1")
    suspend fun getConfigDirect(): AgencyConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AgencyConfig)

    // Schedule Change Logs (Archive)
    @Query("SELECT * FROM schedule_change_logs ORDER BY timestamp DESC")
    fun getAllChangeLogs(): Flow<List<ScheduleChangeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeLog(log: ScheduleChangeLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeLogs(logs: List<ScheduleChangeLog>)

    @Query("DELETE FROM schedule_change_logs")
    suspend fun clearChangeLogs()
}
