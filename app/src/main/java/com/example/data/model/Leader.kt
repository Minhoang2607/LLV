package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaders")
data class Leader(
    @PrimaryKey
    val id: String,
    val name: String,
    val title: String,
    val shortName: String,
    val colorHex: String,
    val role: String, // "director" or "deputy"
    val isPriority: Boolean = false, // True for Đ/c Trần Hoàng Độ
    val orderIndex: Int = 0
)
