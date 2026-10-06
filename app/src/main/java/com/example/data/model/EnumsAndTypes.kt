package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ViewMode(val title: String, val shortTitle: String) {
    BY_LEADER("Lịch Ban Giám Đốc", "Lãnh Đạo"),
    DIFF_COMPARISON("Đối Chiếu Điều Chỉnh", "Đối Chiếu"),
    WEB_MONITOR("Cổng TTĐT", "Cổng TTĐT")
}

enum class ConflictType {
    LEADER_CONFLICT,
    ROOM_CONFLICT
}

data class ConflictInfo(
    val type: ConflictType,
    val message: String,
    val conflictingEventTitle: String,
    val conflictingEventId: String
)

data class WeekDayInfo(
    val dateStr: String,
    val dayOfWeek: Int,
    val label: String
)

@Entity(tableName = "schedule_change_logs")
data class ScheduleChangeLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val changeType: String, // "THÊM MỚI", "CẬP NHẬT", "HỦY BỎ", "ĐỒNG BỘ"
    val eventTitle: String,
    val details: String,
    val sourceUrl: String = "Cổng thông tin điện tử CATP"
)

data class MonitorStatus(
    val targetUrl: String = "https://congan.cantho.gov.vn:8888/",
    val username: String = "anbd",
    val password: String = "An@CanTho?2025",
    val intervalMinutes: Int = 10,
    val lastCheckedTime: String = "Chưa kiểm tra",
    val nextCheckCountdown: String = "10:00",
    val isChecking: Boolean = false,
    val isOnline: Boolean = true,
    val statusMessage: String = "Đang theo dõi cập nhật từ Cổng thông tin điện tử",
    val totalChangesDetected: Int = 0,
    val lastArticleUrl: String = "",
    val lastArticleTitle: String = "",
    val lastArchivedContent: String = "",
    val isBlocked: Boolean = false,
    val isUnreachable: Boolean = false,
    val isAuthRequired: Boolean = false
)
