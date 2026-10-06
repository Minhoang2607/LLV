package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_events")
data class ScheduleEvent(
    @PrimaryKey
    val id: String,
    val date: String,            // YYYY-MM-DD e.g. "2026-09-29"
    val dayOfWeek: Int,          // 1 = Thứ Hai, ..., 7 = Chủ Nhật
    val session: String,         // "sang", "chieu"
    val time: String,            // "07h30", "08h00", "14h00"
    val primaryLeaderId: String, // e.g. "huynh-viet-hoa", "tran-hoang-do"
    val coAttendees: String = "", // Comma-separated leader IDs e.g. "tran-hoang-do,le-duc-bay"
    val title: String,           // Nội dung công tác
    val location: String = "",   // Địa điểm
    val preparation: String = "",// Chuẩn bị nội dung
    val attendees: String = "",  // Thành phần mời dự / Đơn vị liên quan
    val notes: String = "",      // Ghi chú (trang phục, xe...)
    val updatedAt: Long = System.currentTimeMillis()
)
