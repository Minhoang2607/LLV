package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agency_config")
data class AgencyConfig(
    @PrimaryKey
    val id: Int = 1,
    val agencyName: String = "CÔNG AN THÀNH PHỐ CẦN THƠ",
    val subAgencyName: String = "PHÒNG THAM MƯU",
    val documentTitle: String = "LỊCH CÔNG TÁC TUẦN CỦA BAN GIÁM ĐỐC",
    val signatoryTitle: String = "CHÁNH VĂN PHÒNG",
    val signatoryName: String = "Thượng tá Nguyễn Trọng Đoàn",
    val webPortalUrl: String = "https://congan.cantho.gov.vn:8888/",
    val webPortalUser: String = "anbd",
    // Weekly Duty Officers
    val catpLeaderCurrent: String = "Đ/c Đại tá Bùi Đức An (PGĐ)",
    val thamMuuLeaderCurrent: String = "Đ/c Thượng tá Nguyễn Trọng Đoàn",
    val catpLeaderNext: String = "Đ/c Đại tá Nguyễn Văn Thắng (PGĐ)",
    val thamMuuLeaderNext: String = "Đ/c Thượng tá Trần Nguyễn Hòa Thơ"
)
