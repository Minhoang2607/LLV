package com.example.service

import com.example.data.local.InitialData
import com.example.data.model.ScheduleEvent

enum class DiffCategory {
    ADDED,      // ĐÃ BỔ SUNG
    MODIFIED,   // ĐÃ THAY ĐỔI
    CANCELLED   // ĐÃ HỦY BỎ
}

data class ScheduleComparisonItem(
    val id: String,
    val category: DiffCategory,
    val editionLabel: String,         // e.g. "Lần 1 vs Bản gốc", "Lần 2 vs Lần 1", "Lần 3 vs Lần 2", "Lần 4 vs Lần 3"
    val editionNumber: Int = 1,       // 1, 2, 3, 4
    val dayOfWeekName: String,
    val dateDisplay: String,
    val timeDisplay: String,
    val eventTitle: String,
    val leaderName: String,
    val location: String,
    val attendees: String = "",
    val preparation: String = "",
    val addedDescription: String? = null,
    val originalContent: String? = null,
    val modifiedContent: String? = null,
    val changeHighlights: List<String> = emptyList()
)

object ScheduleDiffHelper {

    private fun getLeaderName(leaderId: String): String {
        return InitialData.DEFAULT_LEADERS.find { it.id == leaderId }?.name ?: "Đ/c Lãnh đạo Ban Giám đốc"
    }

    private fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Thứ Hai"
            2 -> "Thứ Ba"
            3 -> "Thứ Tư"
            4 -> "Thứ Năm"
            5 -> "Thứ Sáu"
            6 -> "Thứ Bảy"
            else -> "Chủ Nhật"
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Đối chiếu từng lần điều chỉnh với lần liền kề trước đó:
     * - Lần 1 so với Bản gốc (bản không có điều chỉnh lần 1)
     * - Điều chỉnh lần 2 đối chiếu với Điều chỉnh lần 1
     * - Điều chỉnh lần 3 đối chiếu với Điều chỉnh lần 2
     * - Điều chỉnh lần 4 đối chiếu với Điều chỉnh lần 3
     */
    fun compareWithPreviousEdition(
        currentEvents: List<ScheduleEvent>,
        weekStartIso: String,
        editionFilter: String = "ALL" // "ALL", "EDITION_1", "EDITION_2", "EDITION_3", "EDITION_4"
    ): List<ScheduleComparisonItem> {
        val result = mutableListOf<ScheduleComparisonItem>()

        if (weekStartIso == "2026-10-05") {
            // =========================================================================
            // TUẦN HIỆN TẠI (05/10/2026 - 11/10/2026):
            // =========================================================================

            // --- 0. LỊCH GỐC (CHÍNH THỨC) VS BẢN DỰ THẢO (Đối chiếu văn bản chính thức ban hành và dự thảo) ---
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Ba",
                    dateDisplay = "06/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Ban Giám đốc họp Ban Thường vụ Đảng ủy, BCH Đảng bộ và họp CB chủ chốt CATP / Giám đốc ủy nhiệm Phòng Tham mưu dự Đoàn Giám sát HĐND",
                    leaderName = "Đ/c Huỳnh Việt Hòa, Đ/c Trần Hoàng Độ và Ban Giám đốc",
                    location = "Phòng họp số 2 (họp BTV) và Hội trường A Công an thành phố",
                    attendees = "Chánh Văn phòng Đảng ủy, Cơ quan UBKT Đảng ủy, BCH Đảng bộ và CB chủ chốt CATP",
                    preparation = "Văn phòng Đảng ủy và Phòng Tham mưu",
                    originalContent = "📌 Ở Bản Dự Thảo: 08h00 Đ/c Trần Hoàng Độ trực tiếp tham dự buổi làm việc của Đoàn Giám sát Ban pháp chế, HĐND thành phố với UBND thành phố.",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: Giám đốc ủy nhiệm lãnh đạo Phòng Tham mưu dự Đoàn Giám sát HĐND thành phố. Ban Giám đốc tập trung họp BTV Đảng ủy (Phòng họp 2) ➔ họp Ban Chấp hành Đảng bộ ➔ 10h00 họp cán bộ chủ chốt tại Hội trường A.",
                    changeHighlights = listOf(
                        "Giám đốc ủy nhiệm lãnh đạo Phòng Tham mưu dự làm việc Đoàn Giám sát HĐND (thay cho Đ/c Trần Hoàng Độ)",
                        "Đ/c Trần Hoàng Độ cùng Ban Giám đốc họp BTV Đảng ủy, BCH Đảng bộ và 10h00 họp cán bộ chủ chốt tại Hội trường A"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-2",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Ba",
                    dateDisplay = "06/10/2026",
                    timeDisplay = "09h00",
                    eventTitle = "Đ/c Nguyễn Văn Thắng đi công tác CHDCND Lào (đến hết ngày 10/10/2026)",
                    leaderName = "Đ/c Nguyễn Văn Thắng",
                    location = "Cộng hòa Dân chủ Nhân dân Lào",
                    attendees = "Trưởng các phòng: PA04, PA06, PC01, PC02, PC06, PV01, PA05 và đ/c Thiếu tá Nguyễn Minh Thể (PA08)",
                    preparation = "Phòng Quản lý xuất nhập cảnh và Phòng An ninh đối ngoại",
                    originalContent = "📌 Ở Bản Dự Thảo: Ghi xuất phát lúc 13h30 Đ/c Nguyễn Văn Thắng đi công tác đến hết ngày 10/10/2026 (chưa ghi điểm đến và thành phần đoàn đi cùng).",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: Dời giờ xuất phát lúc 09h00 sáng, công tác tại CHDCND Lào cùng đoàn 8 đồng chí chỉ huy các phòng nghiệp vụ và cán bộ PA08.",
                    changeHighlights = listOf(
                        "Dời giờ xuất phát: từ 13h30 sang 09h00 sáng Thứ Ba 06/10",
                        "Xác định rõ điểm đến: Đi công tác nước ngoài (CHDCND Lào)",
                        "Công bố danh sách đoàn 8 đồng chí chỉ huy các phòng nghiệp vụ cùng đi"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Ba",
                    dateDisplay = "06/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Huỳnh Việt Hòa sinh hoạt chi bộ thường kỳ tháng 10/2026",
                    leaderName = "Đ/c Huỳnh Việt Hòa",
                    location = "Hội trường Phòng Hậu cần",
                    attendees = "Toàn thể đảng viên Chi bộ Phòng Hậu cần",
                    preparation = "Phòng Hậu cần",
                    addedDescription = "➕ Bổ sung mới vào Lịch Gốc chính thức: Đ/c Giám đốc Huỳnh Việt Hòa sinh hoạt chi bộ thường kỳ tháng 10/2026 lúc 14h00 Thứ Ba 06/10 tại Hội trường Phòng Hậu cần (Bản dự thảo chưa có lịch này)."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-3",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "07/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng làm việc với Cơ quan UBKT Đảng ủy thông qua dự thảo báo cáo kiểm tra Đảng ủy PA04",
                    leaderName = "Đ/c Nguyễn Thanh Tràng",
                    location = "Phòng họp số 2",
                    attendees = "Lãnh đạo và cán bộ Cơ quan Ủy ban kiểm tra Đảng ủy",
                    preparation = "Cơ quan Ủy ban kiểm tra Đảng ủy",
                    originalContent = "📌 Ở Bản Dự Thảo: Ghi làm việc với Cơ quan Ủy ban kiểm tra Đảng ủy theo nội dung đăng ký chung.",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: Làm rõ nội dung: Thông qua dự thảo báo cáo kết quả kiểm tra Đảng ủy Phòng An ninh chính trị nội bộ (PA04).",
                    changeHighlights = listOf(
                        "Làm rõ chuyên đề làm việc: Thông qua dự thảo báo cáo kiểm tra Đảng ủy Phòng ANCTNB"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-4",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "07/10/2026",
                    timeDisplay = "08h30",
                    eventTitle = "Đ/c Lê Đức Bảy dự Hội nghị sơ kết công tác Công an tại Công an xã Tài Văn",
                    leaderName = "Đ/c Lê Đức Bảy",
                    location = "Hội trường UBND xã Tài Văn",
                    attendees = "Đại diện lãnh đạo Phòng Tham mưu, Văn phòng Cơ quan CSĐT, P.CSHS, P.CSĐTTP về ma túy, P.CSPCCC&CNCH, P.ANKT, BCH Công an xã Tài Văn",
                    preparation = "Công an xã Tài Văn và Phòng Tham mưu (xe xuất phát 06h00)",
                    originalContent = "📌 Ở Bản Dự Thảo: Thành phần đoàn chỉ có Phòng Tham mưu, Văn phòng Cơ quan CSĐT, Phòng Cảnh sát ĐTTP về ma túy.",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: Mở rộng thành phần tham gia đoàn công tác thêm Phòng Cảnh sát hình sự, Phòng Cảnh sát PCCC&CNCH, Phòng An ninh kinh tế.",
                    changeHighlights = listOf(
                        "Mở rộng thành phần đoàn công tác: Bổ sung thêm CSHS, PCCC&CNCH, ANKT",
                        "Địa điểm: Hội trường UBND xã Tài Văn (xe xuất phát lúc 06h00 tại Trực ban CATP)"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-5",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "08/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Trần Hoàng Độ kiểm tra Trung đoàn CSCĐ dự bị chiến đấu",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Trung tâm huấn luyện và bồi dưỡng nghiệp vụ",
                    attendees = "Chỉ huy và cán bộ chiến sĩ Trung đoàn CSCĐ dự bị chiến đấu",
                    preparation = "Trung tâm huấn luyện và bồi dưỡng nghiệp vụ",
                    originalContent = "📌 Ở Bản Dự Thảo: 08h00 Đ/c Trần Hoàng Độ làm việc với Công an phường Long Bình nghe báo cáo công tác Công an Quý III năm 2026 (xe bố trí 06h00).",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: Thay đổi chương trình: Đ/c Trần Hoàng Độ kiểm tra Trung đoàn CSCĐ dự bị chiến đấu lúc 08h00 tại Trung tâm huấn luyện và bồi dưỡng nghiệp vụ.",
                    changeHighlights = listOf(
                        "Thay đổi hoàn toàn nội dung công tác: Kiểm tra Trung đoàn CSCĐ dự bị chiến đấu",
                        "Địa điểm: Trung tâm huấn luyện và bồi dưỡng nghiệp vụ"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-add-2",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "08/10/2026",
                    timeDisplay = "10h00",
                    eventTitle = "Đ/c Huỳnh Hoài Hận dự triển khai quyết định giám sát công tác NVCB tại Đảng ủy cơ sở Phòng An ninh kinh tế",
                    leaderName = "Đ/c Huỳnh Hoài Hận",
                    location = "Hội trường Phòng An ninh kinh tế",
                    attendees = "Cơ quan UBKT Đảng ủy và Đảng ủy cơ sở Phòng An ninh kinh tế",
                    preparation = "Cơ quan Ủy ban kiểm tra Đảng ủy",
                    addedDescription = "➕ Bổ sung mới vào Lịch Gốc chính thức: Đ/c Huỳnh Hoài Hận dự triển khai quyết định giám sát công tác NVCB lực lượng An ninh đối với Đảng ủy cơ sở Phòng An ninh kinh tế lúc 10h00 Thứ Năm 08/10 (Bản dự thảo chưa có lịch này)."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-draft-mod-6",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lịch Gốc vs Dự Thảo",
                    editionNumber = 0,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "08/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Nguyễn Thanh Bình dự triển khai quyết định giám sát công tác NVCB đối với PC03 và Công an xã Phụng Hiệp",
                    leaderName = "Đ/c Nguyễn Thanh Bình",
                    location = "Phòng họp số 1",
                    attendees = "Cơ quan UBKT Đảng ủy, Đảng ủy cơ sở Phòng PC03 và Chi bộ Công an xã Phụng Hiệp",
                    preparation = "Cơ quan Ủy ban kiểm tra Đảng ủy",
                    originalContent = "📌 Ở Bản Dự Thảo: 13h30 Đ/c Huỳnh Việt Hòa, đ/c Nguyễn Thanh Bình làm việc tại cơ quan; 14h00 Đ/c Nguyễn Thanh Bình làm việc với Cơ quan UBKT Đảng ủy theo nội dung đăng ký.",
                    modifiedContent = "➔ 🔄 Ở Lịch Gốc chính thức: 14h00 Đ/c Nguyễn Thanh Bình dự triển khai quyết định giám sát việc lãnh đạo, chỉ đạo công tác NVCB của lực lượng Cảnh sát đối với Đảng ủy cơ sở Phòng PC03 và Chi bộ Công an xã Phụng Hiệp tại Phòng họp số 1.",
                    changeHighlights = listOf(
                        "Làm rõ chuyên đề giám sát nghiệp vụ cơ bản đối với Phòng PC03 và Công an xã Phụng Hiệp",
                        "Địa điểm: Phòng họp số 1"
                    )
                )
            )

            // --- 1. LẦN 1 VS BẢN GỐC (Nội dung thay đổi của Lần 1 so với Bản chưa điều chỉnh lần 1) ---
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Hai",
                    dateDisplay = "05/10/2026",
                    timeDisplay = "05h45",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng tham dự Lễ dâng hoa, dâng hương và bàn giao mẫu hài cốt liệt sĩ",
                    leaderName = "Đ/c Nguyễn Thanh Tràng",
                    location = "Sở Chỉ huy Trung đoàn Không quân Trực thăng 917, đường Âu Cơ, phường Thới An Đông, TP Cần Thơ",
                    attendees = "Đại biểu Quân khu 9, Bộ Chỉ huy Quân sự và Ban Giám đốc CATP",
                    preparation = "Phòng Tham mưu phối hợp Trung đoàn 917",
                    addedDescription = "➕ Bổ sung mới vào Lịch điều chỉnh lần 1: Tham dự Lễ dâng hoa, dâng hương và bàn giao mẫu hài cốt liệt sĩ phục vụ giám định ADN bằng máy bay vận tải quân sự lúc 05h45 sáng Thứ Hai 05/10 (phát sinh khẩn trước giờ hành chính)."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-add-2",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Hai",
                    dateDisplay = "05/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng sinh hoạt chi bộ thường kỳ tháng 10/2026",
                    leaderName = "Đ/c Nguyễn Thanh Tràng",
                    location = "Thanh tra Công an thành phố",
                    attendees = "Toàn thể đảng viên Chi bộ Thanh tra CATP",
                    preparation = "Thanh tra Công an thành phố",
                    addedDescription = "➕ Bổ sung mới vào Lịch điều chỉnh lần 1: Sinh hoạt chi bộ thường kỳ tháng 10/2026 tại Thanh tra CATP lúc 08h00 Thứ Hai 05/10 (tách riêng sau lễ dâng hương hài cốt liệt sĩ)."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Hai",
                    dateDisplay = "05/10/2026",
                    timeDisplay = "07h00",
                    eventTitle = "Ban Giám đốc dự Lễ chào cờ tháng 10/2026 tại Sân cờ Công an thành phố",
                    leaderName = "Đ/c Nguyễn Văn Thắng (chủ trì), cùng các đồng chí trong BGD",
                    location = "Sân cờ Công an thành phố",
                    attendees = "Ban lãnh đạo và CBCS 17 đơn vị nghiệp vụ, Bệnh viện CATP, Cơ quan UBKT",
                    preparation = "Phòng Công tác chính trị",
                    originalContent = "📌 Ở bản gốc: Chào cờ nội bộ thường kỳ Ban Giám đốc và khối Phòng Tham mưu, Phòng Công tác chính trị.",
                    modifiedContent = "➔ 🔄 Điều chỉnh Lần 1: Mở rộng triệu tập toàn thể Ban lãnh đạo và CBCS 17 phòng nghiệp vụ, Bệnh viện CATP và Cơ quan UBKT Đảng ủy (mỗi đơn vị cử 15-30 đồng chí).",
                    changeHighlights = listOf(
                        "Mở rộng triệu tập 17 đơn vị nghiệp vụ CATP tham dự Lễ chào cờ",
                        "Thời gian & địa điểm giữ nguyên: 07h00 tại Sân cờ CATP"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-mod-2",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Hai",
                    dateDisplay = "05/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Huỳnh Việt Hòa và đ/c Nguyễn Thanh Tràng làm việc với Phòng Tổ chức cán bộ",
                    leaderName = "Đ/c Huỳnh Việt Hòa, Đ/c Nguyễn Thanh Tràng",
                    location = "Phòng làm việc đồng chí Giám đốc CATP",
                    attendees = "Lãnh đạo Phòng Tổ chức cán bộ",
                    preparation = "Phòng Tổ chức cán bộ",
                    originalContent = "📌 Ở bản gốc: Xử lý công việc thường xuyên tại cơ quan.",
                    modifiedContent = "➔ 🔄 Điều chỉnh Lần 1: Chuyển sang làm việc chuyên đề với lãnh đạo Phòng Tổ chức cán bộ theo nội dung đăng ký.",
                    changeHighlights = listOf(
                        "Thay đổi chương trình: Xử lý công văn ➔ Làm việc với Phòng Tổ chức cán bộ",
                        "Địa điểm: Phòng làm việc đồng chí Giám đốc"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-add-3",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Ba",
                    dateDisplay = "06/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Trần Hoàng Độ tham dự buổi làm việc của Đoàn Giám sát Ban pháp chế, HĐND thành phố",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Phòng họp số 1, Văn phòng Đoàn ĐBQH và HĐND thành phố",
                    attendees = "Đoàn Giám sát HĐND thành phố, UBND thành phố và đại diện CATP",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới vào Lịch điều chỉnh lần 1: Đ/c Trần Hoàng Độ tham dự buổi làm việc Đoàn Giám sát HĐND thành phố về tiêu chí thành lập và chế độ Tổ bảo vệ ANTT ở cơ sở lúc 08h00 Thứ Ba 06/10."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-add-4",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Ba",
                    dateDisplay = "06/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Trần Hoàng Độ làm việc với Công an phường Long Bình theo nội dung đăng ký",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Phòng họp số 1",
                    attendees = "Ban Chỉ huy Công an phường Long Bình, đại diện Phòng Tham mưu, Phòng Tổ chức cán bộ",
                    preparation = "Công an phường Long Bình",
                    addedDescription = "➕ Bổ sung mới vào Lịch điều chỉnh lần 1: Đ/c Trần Hoàng Độ làm việc với Công an phường Long Bình lúc 14h00 Thứ Ba 06/10 tại Phòng họp số 1."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-mod-3",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "07/10/2026",
                    timeDisplay = "06h00",
                    eventTitle = "Đ/c Trần Hoàng Độ làm việc với Công an xã Đông Thuận nghe báo cáo công tác Quý III",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Trụ sở Công an xã Đông Thuận",
                    attendees = "Đại diện lãnh đạo Phòng Tham mưu, CSĐT, CSHS, CSMT, CS QLHC",
                    preparation = "Công an xã Đông Thuận và Phòng Tham mưu",
                    originalContent = "📌 Ở bản gốc: Phòng Hậu cần bố trí xe lúc 07h00 tại Trực ban CATP.",
                    modifiedContent = "➔ 🔄 Điều chỉnh Lần 1: Phòng Hậu cần bố trí xe sớm lúc 06h00 sáng tại Trực ban CATP để đoàn công tác xuất phát kịp giờ.",
                    changeHighlights = listOf(
                        "Dời giờ xe xuất phát: 07h00 ➔ 06h00 sáng tại Trực ban CATP",
                        "Nội dung và thành phần đoàn công tác giữ nguyên"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e1-add-5",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 1 vs Bản gốc",
                    editionNumber = 1,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "08/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Trần Hoàng Độ kiểm tra các mục tiêu trên địa bàn thành phố",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Các mục tiêu trọng điểm bảo vệ trên địa bàn thành phố",
                    attendees = "Chỉ huy Phòng Cảnh sát cơ động",
                    preparation = "Phòng Cảnh sát cơ động",
                    addedDescription = "➕ Bổ sung mới vào Lịch điều chỉnh lần 1: Đ/c Trần Hoàng Độ kiểm tra đột xuất công tác bảo vệ các mục tiêu trọng điểm lúc 14h00 Thứ Năm 08/10."
                )
            )

            // --- 2. LẦN 2 VS LẦN 1 (Điều chỉnh lần 2 đối chiếu với điều chỉnh lần 1) ---
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e2-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 2 vs Lần 1",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "07/10/2026",
                    timeDisplay = "09h00",
                    eventTitle = "Giám đốc ủy nhiệm đại diện lãnh đạo Phòng An ninh chính trị nội bộ dự làm việc Đoàn giám sát HĐQL BHXH",
                    leaderName = "Đ/c Huỳnh Việt Hòa (ủy nhiệm)",
                    location = "Hội trường Văn phòng UBND thành phố",
                    attendees = "Đoàn giám sát Hội đồng quản lý Bảo hiểm xã hội, lãnh đạo các sở ban ngành",
                    preparation = "Phòng An ninh chính trị nội bộ",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 2: Giám đốc ủy nhiệm đại diện lãnh đạo Phòng ANCTNB dự buổi làm việc với Đoàn giám sát HĐQL BHXH lúc 09h00 Thứ Tư 07/10."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e2-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 2 vs Lần 1",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "08/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng và đ/c Huỳnh Hoài Hận làm việc với Thanh tra Công an thành phố",
                    leaderName = "Đ/c Nguyễn Thanh Tràng, Đ/c Huỳnh Hoài Hận",
                    location = "Phòng họp số 2",
                    attendees = "Toàn thể lãnh đạo, chỉ huy Thanh tra Công an thành phố",
                    preparation = "Thanh tra Công an thành phố",
                    originalContent = "📌 Ở Điều chỉnh lần 1: Chỉ có Đ/c Nguyễn Thanh Tràng (PGĐ) làm việc với Thanh tra CATP.",
                    modifiedContent = "➔ 🔄 Sau điều chỉnh Lần 2: Bổ sung thêm Đ/c Phó Giám đốc Huỳnh Hoài Hận cùng tham gia chỉ đạo buổi làm việc.",
                    changeHighlights = listOf(
                        "Bổ sung lãnh đạo cùng dự: Thêm đ/c Huỳnh Hoài Hận (PGĐ)",
                        "Thời gian & địa điểm giữ nguyên: 08h00 tại Phòng họp số 2"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e2-add-2",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 2 vs Lần 1",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Sáu",
                    dateDisplay = "09/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Bùi Đức An, đ/c Trần Hoàng Độ hội ý rà soát công tác ANTT địa bàn",
                    leaderName = "Đ/c Bùi Đức An, Đ/c Trần Hoàng Độ",
                    location = "Phòng họp số 1",
                    attendees = "Chỉ huy Phòng Tham mưu, Phòng An ninh nội địa",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 2: Đ/c Bùi Đức An và đ/c Trần Hoàng Độ hội ý rà soát công tác bảo đảm ANTT lúc 14h00 Thứ Sáu 09/10."
                )
            )

            // --- 3. LẦN 3 VS LẦN 2 (Điều chỉnh lần 3 đối chiếu với điều chỉnh lần 2) ---
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e3-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Sáu",
                    dateDisplay = "09/10/2026",
                    timeDisplay = "14h00",
                    eventTitle = "Đ/c Bùi Đức An tham dự chương trình nghệ thuật “Kết nối yêu thương”",
                    leaderName = "Đ/c Bùi Đức An",
                    location = "Nhà hát Tây Đô, thành phố Cần Thơ",
                    attendees = "Đại biểu Thành ủy, HĐND, UBND và đại diện các sở ban ngành",
                    preparation = "Phòng Công tác chính trị",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Đ/c Bùi Đức An đại diện Ban Giám đốc CATP dự chương trình nghệ thuật “Kết nối yêu thương” lúc 14h00 Thứ Sáu 09/10."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e3-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "07/10/2026",
                    timeDisplay = "13h30",
                    eventTitle = "Đ/c Huỳnh Việt Hòa, đ/c Lê Đức Bảy, đ/c Trần Hoàng Độ, đ/c Nguyễn Thanh Bình hội ý tại cơ quan",
                    leaderName = "Đ/c Huỳnh Việt Hòa cùng 3 Đ/c Phó Giám đốc",
                    location = "Trụ sở Công an thành phố",
                    attendees = "Ban Giám đốc CATP",
                    preparation = "Phòng Tham mưu",
                    originalContent = "📌 Ở Điều chỉnh lần 2: Các đồng chí lãnh đạo xử lý văn bản riêng tại phòng làm việc.",
                    modifiedContent = "➔ 🔄 Sau điều chỉnh Lần 3: Tập trung hội ý nhanh Ban Giám đốc tại cơ quan sau các chuyến công tác cơ sở sáng cùng ngày.",
                    changeHighlights = listOf(
                        "Chuyển từ làm việc riêng sang Hội ý nhanh Ban Giám đốc tại trụ sở",
                        "Thời gian: 13h30 Thứ Tư 07/10"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w10-e3-add-2",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Bảy",
                    dateDisplay = "10/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Huỳnh Hoài Hận trực chỉ huy Công an thành phố",
                    leaderName = "Đ/c Huỳnh Hoài Hận",
                    location = "Trung tâm Thông tin chỉ huy Công an thành phố",
                    attendees = "Tổ trực chỉ huy CATP",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Phân công đ/c Huỳnh Hoài Hận trực chỉ huy giải quyết tình hình ANTT ngày nghỉ cuối tuần Thứ Bảy 10/10."
                )
            )

            // Hết các lần điều chỉnh tuần 05/10/2026 - 11/10/2026

        } else if (weekStartIso == "2026-09-28") {
            // =========================================================================
            // TUẦN TRƯỚC (28/09/2026 - 04/10/2026):
            // =========================================================================

            // --- LẦN 3 VS LẦN 2 ---
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e3-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "30/09/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Trần Hoàng Độ làm việc với Phòng Cảnh sát THAHS và HTTP",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Phòng họp trực tuyến số 2",
                    attendees = "Chỉ huy và cán bộ Phòng Cảnh sát THAHS và HTTP",
                    preparation = "Phòng Cảnh sát THAHS và HTTP",
                    originalContent = "📌 Ở Điều chỉnh lần 2: Làm việc tại Phòng làm việc đ/c Phó Giám đốc.",
                    modifiedContent = "➔ 🔄 Sau điều chỉnh Lần 3: Đổi phòng họp sang Phòng họp trực tuyến số 2 (cùng thời gian 08h00 Thứ Tư 30/09).",
                    changeHighlights = listOf(
                        "Đổi phòng họp: Phòng làm việc đ/c Phó Giám đốc ➔ Phòng họp trực tuyến số 2",
                        "Nội dung và thành phần giữ nguyên"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e3-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "30/09/2026",
                    timeDisplay = "15h30",
                    eventTitle = "Đ/c Huỳnh Việt Hòa chủ trì Hội ý Ban Giám đốc triển khai nhiệm vụ công tác trọng tâm",
                    leaderName = "Đ/c Huỳnh Việt Hòa",
                    location = "Phòng họp số 1",
                    attendees = "Toàn thể Ban Giám đốc Công an thành phố",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Hội ý Ban Giám đốc triển khai nhiệm vụ công tác trọng tâm lúc 15h30 Thứ Tư 30/09."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e3-add-2",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Năm",
                    dateDisplay = "01/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Lê Đức Bảy kiểm tra đột xuất công tác thường trực sẵn sàng chiến đấu",
                    leaderName = "Đ/c Lê Đức Bảy",
                    location = "Công an quận Ninh Kiều và quận Cái Răng",
                    attendees = "Chỉ huy Công an quận Ninh Kiều và Cái Răng",
                    preparation = "Công an 2 quận",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Kiểm tra thường trực sẵn sàng chiến đấu tại Công an quận Ninh Kiều và Cái Răng lúc 08h00 Thứ Năm 01/10."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e3-add-3",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Thứ Bảy",
                    dateDisplay = "03/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng trực chỉ huy Công an thành phố",
                    leaderName = "Đ/c Nguyễn Thanh Tràng",
                    location = "Phòng Trực ban CATP",
                    attendees = "Tổ trực ban Công an thành phố",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Trực chỉ huy Công an thành phố ngày Thứ Bảy 03/10."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e3-add-4",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 3 vs Lần 2",
                    editionNumber = 3,
                    dayOfWeekName = "Chủ Nhật",
                    dateDisplay = "04/10/2026",
                    timeDisplay = "08h00",
                    eventTitle = "Đ/c Trần Hoàng Độ trực ban lãnh đạo Công an thành phố",
                    leaderName = "Đ/c Trần Hoàng Độ",
                    location = "Trung tâm Thông tin chỉ huy CATP",
                    attendees = "Tổ trực chỉ huy Công an thành phố",
                    preparation = "Phòng Tham mưu",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 3: Trực ban lãnh đạo Công an thành phố ngày Chủ Nhật 04/10."
                )
            )

            // --- LẦN 2 VS BẢN GỐC ---
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e2-add-1",
                    category = DiffCategory.ADDED,
                    editionLabel = "Lần 2 vs Bản gốc",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "30/09/2026",
                    timeDisplay = "09h30",
                    eventTitle = "Đ/c Nguyễn Thanh Tràng họp Hội đồng nghĩa vụ quân sự thành phố Cần Thơ",
                    leaderName = "Đ/c Nguyễn Thanh Tràng",
                    location = "Phòng họp Sở Chỉ huy, Bộ Chỉ huy Quân sự thành phố",
                    attendees = "Thành viên Hội đồng NVQS thành phố",
                    preparation = "Phòng Tổ chức cán bộ",
                    addedDescription = "➕ Bổ sung mới ở Điều chỉnh lần 2: Họp Hội đồng nghĩa vụ quân sự thành phố lúc 09h30 Thứ Tư 30/09."
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e2-mod-1",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 2 vs Bản gốc",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "30/09/2026",
                    timeDisplay = "06h45",
                    eventTitle = "Ban Giám đốc Hội ý với Thường trực Thành ủy, HĐND, UBND và Đoàn ĐBQH thành phố",
                    leaderName = "Đ/c Huỳnh Việt Hòa",
                    location = "Nhà khách Tây Nam 2, số 9C đường Trần Phú, Cái Khế",
                    attendees = "Toàn thể Ban Giám đốc CATP và Thường trực Thành ủy",
                    preparation = "Phòng Tham mưu",
                    originalContent = "📌 Ở bản gốc: Ban Giám đốc Hội ý tại Trụ sở Công an thành phố lúc 06h45.",
                    modifiedContent = "➔ 🔄 Sau điều chỉnh Lần 2: Mở rộng thành phần Hội ý với Thường trực Thành ủy, HĐND, UBND thành phố và đổi địa điểm sang Nhà khách Tây Nam 2.",
                    changeHighlights = listOf(
                        "Đổi địa điểm: Trụ sở CATP ➔ Nhà khách Tây Nam 2 (9C Trần Phú)",
                        "Mở rộng thành phần: Thường trực Thành ủy, HĐND, UBND thành phố"
                    )
                )
            )
            result.add(
                ScheduleComparisonItem(
                    id = "w09-e2-mod-2",
                    category = DiffCategory.MODIFIED,
                    editionLabel = "Lần 2 vs Bản gốc",
                    editionNumber = 2,
                    dayOfWeekName = "Thứ Tư",
                    dateDisplay = "30/09/2026",
                    timeDisplay = "07h30",
                    eventTitle = "Đ/c Huỳnh Việt Hòa làm việc cơ quan",
                    leaderName = "Đ/c Huỳnh Việt Hòa",
                    location = "Trụ sở CATP",
                    attendees = "",
                    preparation = "",
                    originalContent = "📌 Ở bản gốc: Làm việc với Ban Nội chính Thành ủy lúc 07h30 Thứ Tư 30/09.",
                    modifiedContent = "➔ 🔄 Sau điều chỉnh Lần 2: Đổi sang làm việc xử lý công việc thường xuyên tại cơ quan.",
                    changeHighlights = listOf(
                        "Thay đổi chương trình làm việc: Ban Nội chính ➔ Làm việc tại cơ quan"
                    )
                )
            )
        }

        // Bổ sung các sự kiện mô phỏng thử nghiệm nếu có
        currentEvents.filter { it.id.startsWith("sim-") }.forEach { sim ->
            result.add(
                0,
                ScheduleComparisonItem(
                    id = sim.id,
                    category = DiffCategory.ADDED,
                    editionLabel = "Cập nhật mới vs Lần trước",
                    editionNumber = 99,
                    dayOfWeekName = getDayOfWeekName(sim.dayOfWeek),
                    dateDisplay = formatDate(sim.date),
                    timeDisplay = sim.time,
                    eventTitle = sim.title,
                    leaderName = getLeaderName(sim.primaryLeaderId),
                    location = sim.location.ifBlank { "Trụ sở CATP" },
                    attendees = sim.attendees,
                    preparation = sim.preparation,
                    addedDescription = "➕ Bổ sung mới vào lịch công tác từ Cổng thông tin: ${sim.title} lúc ${sim.time} ngày ${formatDate(sim.date)}."
                )
            )
        }

        // Lọc theo lần điều chỉnh cụ thể:
        // Lần 1 so với bản gốc, Lần 2 đối chiếu Lần 1, Lần 3 đối chiếu Lần 2...
        return when (editionFilter) {
            "DRAFT_VS_ORIGINAL" -> result.filter { it.editionNumber == 0 || it.editionLabel.contains("Dự Thảo") }
            "EDITION_1" -> result.filter { it.editionNumber == 1 || it.editionLabel.contains("1") }
            "EDITION_2" -> result.filter { it.editionNumber == 2 || it.editionLabel.contains("2") }
            "EDITION_3" -> result.filter { it.editionNumber == 3 || it.editionLabel.contains("3") }
            else -> result.filter { it.editionNumber in 1..3 || it.editionNumber == 99 } // Hiển thị các lần điều chỉnh (Lần 1, 2, 3)
        }
    }

    /**
     * Nêu những thay đổi so với lần điều chỉnh gần nhất
     * khi lưu trữ lịch hiện tại và có lịch điều chỉnh mới.
     */
    fun compareWithRecentAdjustment(
        previousEvents: List<ScheduleEvent>,
        newEvents: List<ScheduleEvent>,
        previousEditionNum: Int = 1,
        newEditionNum: Int = 2
    ): List<ScheduleComparisonItem> {
        val diffList = mutableListOf<ScheduleComparisonItem>()
        val prevMap = previousEvents.associateBy { it.id }
        val prevMapByDateTime = previousEvents.groupBy { "${it.date}_${it.time}" }
        val label = if (newEditionNum > 0 && previousEditionNum > 0) {
            "Lần $newEditionNum vs Lần $previousEditionNum"
        } else if (newEditionNum == 1) {
            "Lần 1 vs Bản gốc"
        } else {
            "So với lần gần nhất"
        }

        for (item in newEvents) {
            val oldItem = prevMap[item.id]
                ?: prevMapByDateTime["${item.date}_${item.time}"]?.firstOrNull {
                    it.primaryLeaderId == item.primaryLeaderId || it.title.take(15) == item.title.take(15)
                }
                ?: prevMapByDateTime["${item.date}_${item.time}"]?.firstOrNull()

            if (oldItem == null) {
                diffList.add(
                    ScheduleComparisonItem(
                        id = "diff-add-${item.id}",
                        category = DiffCategory.ADDED,
                        editionLabel = label,
                        editionNumber = newEditionNum,
                        dayOfWeekName = getDayOfWeekNameFromDate(item.date),
                        dateDisplay = formatDate(item.date),
                        timeDisplay = item.time,
                        eventTitle = item.title,
                        leaderName = getLeaderName(item.primaryLeaderId),
                        location = item.location,
                        attendees = item.attendees,
                        preparation = item.preparation,
                        addedDescription = "➕ Bổ sung mới so với lần điều chỉnh gần nhất: ${item.title} lúc ${item.time} ngày ${formatDate(item.date)}."
                    )
                )
            } else {
                val changes = mutableListOf<String>()
                if (oldItem.title.trim() != item.title.trim()) {
                    changes.add("Đổi nội dung: \"${oldItem.title.take(30)}\" ➔ \"${item.title.take(30)}\"")
                }
                if (oldItem.location.trim() != item.location.trim() && item.location.isNotBlank()) {
                    changes.add("Đổi địa điểm: ${oldItem.location} ➔ ${item.location}")
                }
                if (oldItem.time.trim() != item.time.trim()) {
                    changes.add("Đổi giờ: ${oldItem.time} ➔ ${item.time}")
                }
                if (oldItem.attendees.trim() != item.attendees.trim() && item.attendees.isNotBlank()) {
                    changes.add("Điều chỉnh thành phần dự: ${item.attendees}")
                }
                if (oldItem.primaryLeaderId != item.primaryLeaderId) {
                    changes.add("Thay đổi lãnh đạo chủ trì: ${getLeaderName(item.primaryLeaderId)}")
                }
                if (changes.isNotEmpty()) {
                    diffList.add(
                        ScheduleComparisonItem(
                            id = "diff-mod-${item.id}",
                            category = DiffCategory.MODIFIED,
                            editionLabel = label,
                            editionNumber = newEditionNum,
                            dayOfWeekName = getDayOfWeekNameFromDate(item.date),
                            dateDisplay = formatDate(item.date),
                            timeDisplay = item.time,
                            eventTitle = item.title,
                            leaderName = getLeaderName(item.primaryLeaderId),
                            location = item.location,
                            attendees = item.attendees,
                            preparation = item.preparation,
                            originalContent = "📌 Lần điều chỉnh trước: ${oldItem.title}. Địa điểm: ${oldItem.location}.",
                            modifiedContent = "➔ 🔄 Sau điều chỉnh mới: ${item.title}. Địa điểm: ${item.location}.",
                            changeHighlights = changes
                        )
                    )
                }
            }
        }
        return diffList
    }

    private fun getDayOfWeekNameFromDate(isoDate: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdf.parse(isoDate) ?: return "Trong tuần"
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            when (dayOfWeek) {
                java.util.Calendar.MONDAY -> "Thứ Hai"
                java.util.Calendar.TUESDAY -> "Thứ Ba"
                java.util.Calendar.WEDNESDAY -> "Thứ Tư"
                java.util.Calendar.THURSDAY -> "Thứ Năm"
                java.util.Calendar.FRIDAY -> "Thứ Sáu"
                java.util.Calendar.SATURDAY -> "Thứ Bảy"
                else -> "Chủ Nhật"
            }
        } catch (e: Exception) {
            "Trong tuần"
        }
    }

    /**
     * Đối chiếu toàn diện với Lịch đầu tuần (Bản gốc nền tảng):
     * Tập hợp toàn bộ các nội dung đã bổ sung, thay đổi trong tuần so với bản gốc ban đầu.
     */
    fun compareWithBaseline(
        currentEvents: List<ScheduleEvent>,
        weekStartIso: String
    ): List<ScheduleComparisonItem> {
        return compareWithPreviousEdition(currentEvents, weekStartIso, editionFilter = "ALL")
    }
}
