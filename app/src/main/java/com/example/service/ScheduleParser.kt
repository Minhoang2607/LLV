package com.example.service

import com.example.data.model.Leader
import com.example.data.model.ScheduleEvent
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduleParser {

    /** All nine members of the Ban Giám đốc, in display order. */
    val ALL_BGD_IDS = listOf(
        "huynh-viet-hoa",
        "tran-hoang-do",
        "nguyen-thanh-trang",
        "le-duc-bay",
        "bui-duc-an",
        "dinh-tung-an",
        "nguyen-van-thang",
        "huynh-hoai-han",
        "nguyen-thanh-binh"
    )

    data class ArticleLink(
        val url: String,
        val title: String,
        val edition: Int
    )

    /**
     * Quét trang chủ web tìm link bài đăng lịch làm việc của Ban Giám đốc:
     * Tiêu đề: "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (DỰ THẢO, điều chỉnh lần 1, 2, 3,....) (Từ ngày ... đến ngày ...)"
     */
    fun findFirstScheduleArticleLink(html: String, baseUrl: String): ArticleLink? {
        val doc = Jsoup.parse(html)
        val anchors = doc.select("a[href]")

        for (anchor in anchors) {
            val href = anchor.attr("href").trim()
            val text = anchor.text().trim()
            val upper = text.uppercase()

            if ((upper.contains("LỊCH LÀM VIỆC") || upper.contains("LỊCH CÔNG TÁC") || upper.contains("ĐIỀU CHỈNH")) &&
                (upper.contains("BAN GIÁM ĐỐC") || upper.contains("BGD") || upper.contains("GIÁM ĐỐC"))
            ) {
                val resolvedUrl = when {
                    href.startsWith("http://") || href.startsWith("https://") -> href
                    href.startsWith("/") -> {
                        val baseWithoutTrailing = baseUrl.trimEnd('/')
                        val hostPart = if (baseWithoutTrailing.contains("://")) {
                            val scheme = baseWithoutTrailing.substringBefore("://")
                            val domain = baseWithoutTrailing.substringAfter("://").substringBefore("/")
                            "$scheme://$domain"
                        } else baseWithoutTrailing
                        "$hostPart$href"
                    }
                    else -> "${baseUrl.trimEnd('/')}/$href"
                }

                val edition = extractEditionNumber(text)
                return ArticleLink(
                    url = resolvedUrl,
                    title = text,
                    edition = edition
                )
            }
        }
        return null
    }

    /**
     * Trích xuất tiêu đề chính thức của lịch làm việc:
     * "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (DỰ THẢO, điều chỉnh lần 1, 2, 3,....) (Từ ngày ... đến ngày ...)"
     * Chỉ lấy đúng nguyên văn, không tự ý suy diễn hoặc thêm nội dung khác.
     */
    fun extractDocumentTitle(rawTextOrHtml: String): String? {
        val sanitized = extractTopScheduleSection(rawTextOrHtml)
        val firstLines = sanitized.lines().take(30).map { it.trim() }.filter { it.isNotBlank() }

        // Tìm dòng chứa tiêu đề theo mẫu chuẩn
        for (line in firstLines) {
            val upper = line.uppercase()
            if ((upper.contains("LỊCH LÀM VIỆC") || upper.contains("LỊCH CÔNG TÁC")) &&
                (upper.contains("BAN GIÁM ĐỐC") || upper.contains("CÔNG AN THÀNH PHỐ") || upper.contains("BGD"))
            ) {
                // Kiểm tra xem dòng có chứa chi tiết điều chỉnh / tuần ngày không
                if (line.length in 15..200) {
                    return line
                }
            }
        }

        // Tìm kiếm toàn văn mẫu tiêu đề hoàn chỉnh
        val fullTitleRegex = Regex(
            """(LỊCH\s+(?:LÀM\s+VIỆC|CÔNG\s+TÁC)\s+CỦA\s+BAN\s+GIÁM\s+ĐỐC\s+CÔNG\s+AN\s+THÀNH\s+PHỐ[^\n<]{0,120})""",
            RegexOption.IGNORE_CASE
        )
        val match = fullTitleRegex.find(rawTextOrHtml)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        return null
    }

    /**
     * Trích xuất số lần điều chỉnh:
     * - "DỰ THẢO" -> 0
     * - "Điều chỉnh lần 1" -> 1
     * - "Điều chỉnh lần 2" -> 2
     * - "Điều chỉnh lần 3" -> 3
     * - v.v...
     */
    fun extractEditionNumber(rawTextOrHtml: String): Int {
        val upper = rawTextOrHtml.uppercase()
        if (upper.contains("DỰ THẢO") && !upper.contains("ĐIỀU CHỈNH LẦN")) {
            return 0
        }
        val match = Regex(
            """(?:ĐIỀU\s*CHỈNH\s*LẦN\s*(\d+)|LẦN\s*THỨ\s*(\d+)|LẦN\s*(\d+)|ĐC\s*LẦN\s*(\d+)|ĐCL\s*(\d+))""",
            RegexOption.IGNORE_CASE
        ).find(rawTextOrHtml)

        return match?.let {
            val num = it.groupValues.drop(1).firstOrNull { g -> g.isNotBlank() }
            num?.toIntOrNull() ?: 0
        } ?: 0
    }

    /**
     * Trích xuất phần nội dung lịch mới nhất từ HTML/văn bản.
     */
    fun extractTopScheduleSection(rawTextOrHtml: String): String {
        var content = rawTextOrHtml

        if (content.contains("<article", ignoreCase = true) || content.contains("<table", ignoreCase = true)) {
            val doc = Jsoup.parse(content)
            val article = doc.select("article").firstOrNull()
            if (article != null) {
                content = article.html()
            } else {
                val table = doc.select("table").firstOrNull()
                if (table != null) {
                    content = table.outerHtml()
                }
            }

            content = content
                .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("""</(p|div|tr|li|h[1-6])>""", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("""</td>""", RegexOption.IGNORE_CASE), " | ")
                .replace(Regex("""<[^>]+>"""), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#8211;", "-")
                .replace("&#8212;", "-")
        }

        return content
    }

    /**
     * Phân tích văn bản/HTML lấy về từ https://congan.cantho.gov.vn:8888/
     * Trích xuất các trường:
     * - Nội dung (title)
     * - Thời gian (date, dayOfWeek, session, time)
     * - Địa điểm (location)
     * - Chủ trì (primaryLeaderId)
     * - Người được dự / cùng dự (coAttendees, attendees)
     * - Đơn vị chuẩn bị nội dung (preparation)
     *
     * NGUYÊN TẮC QUAN TRỌNG:
     * Dữ liệu nào không có thì BỎ TRỐNG ("").
     * Tuyệt đối không tự ý suy diễn hoặc thêm nội dung khác!
     */
    fun parseRawText(
        text: String,
        leaders: List<Leader>,
        referenceDate: Date = Date()
    ): List<ScheduleEvent> {
        // 1. Thử phân tích dạng Bảng HTML (nếu có table)
        if (text.contains("<table", ignoreCase = true)) {
            val tableEvents = parseHtmlTable(text, leaders, referenceDate)
            if (tableEvents.isNotEmpty()) {
                return tableEvents
            }
        }

        // 2. Phân tích theo từng dòng văn bản
        return parseLineByLine(text, leaders, referenceDate)
    }

    /**
     * Phân tích bảng dữ liệu HTML với các cột chuẩn hành chính của Công an thành phố
     */
    private fun parseHtmlTable(
        html: String,
        leaders: List<Leader>,
        referenceDate: Date
    ): List<ScheduleEvent> {
        val events = mutableListOf<ScheduleEvent>()
        try {
            val doc = Jsoup.parse(html)
            val table = doc.select("table").firstOrNull() ?: return emptyList()
            val rows = table.select("tr")
            if (rows.isEmpty()) return emptyList()

            var dateCol = -1
            var timeCol = -1
            var contentCol = -1
            var leaderCol = -1
            var attendeeCol = -1
            var locCol = -1
            var prepCol = -1

            var startRow = 0
            for (i in 0 until minOf(rows.size, 3)) {
                val thCells = rows[i].select("th, td")
                for (j in thCells.indices) {
                    val txt = thCells[j].text().lowercase()
                    if (txt.contains("thứ") || txt.contains("ngày")) dateCol = j
                    if (txt.contains("buổi") || txt.contains("thời gian") || txt.contains("giờ")) timeCol = j
                    if (txt.contains("nội dung") || txt.contains("công tác") || txt.contains("chương trình")) contentCol = j
                    if (txt.contains("chủ trì") || txt.contains("lãnh đạo")) leaderCol = j
                    if (txt.contains("cùng dự") || txt.contains("người dự") || txt.contains("mời dự")) attendeeCol = j
                    if (txt.contains("địa điểm") || txt.contains("điểm")) locCol = j
                    if (txt.contains("chuẩn bị")) prepCol = j
                }
                if (contentCol != -1) {
                    startRow = i + 1
                    break
                }
            }

            if (contentCol == -1) {
                // Nếu không có header rõ ràng, thử cột 0..n
                return emptyList()
            }

            var activeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(referenceDate)
            var activeDayOfWeek = 1

            for (r in startRow until rows.size) {
                val cells = rows[r].select("td")
                if (cells.isEmpty()) continue

                // Cập nhật ngày nếu cột ngày có giá trị
                if (dateCol != -1 && dateCol < cells.size) {
                    val dText = cells[dateCol].text().trim()
                    if (dText.isNotBlank()) {
                        val parsedDate = parseDateHeader(dText)
                        if (parsedDate != null) {
                            activeDate = parsedDate.first
                            activeDayOfWeek = parsedDate.second
                        }
                    }
                }

                val contentText = if (contentCol < cells.size) cells[contentCol].text().trim() else ""
                if (contentText.isBlank() || isHeaderOrFooterText(contentText)) continue

                val timeText = if (timeCol != -1 && timeCol < cells.size) cells[timeCol].text().trim() else ""
                val (formattedTime, session) = parseTimeAndSession(timeText, contentText)

                val leaderText = if (leaderCol != -1 && leaderCol < cells.size) cells[leaderCol].text().trim() else ""
                val attendeeCellText = if (attendeeCol != -1 && attendeeCol < cells.size) cells[attendeeCol].text().trim() else ""
                val locText = if (locCol != -1 && locCol < cells.size) cells[locCol].text().trim() else ""
                val prepText = if (prepCol != -1 && prepCol < cells.size) cells[prepCol].text().trim() else ""

                // Trích xuất lãnh đạo: ưu tiên từ cột chủ trì, nếu trống thì tìm trong nội dung
                val (primaryLeader, coLeaders) = if (leaderText.isNotBlank()) {
                    extractLeaders(leaderText, leaders)
                } else {
                    extractLeaders(contentText, leaders)
                }

                // Địa điểm: nếu cột địa điểm có thì lấy đúng cột, không có thì trích từ nội dung, không có nữa thì BỎ TRỐNG
                val finalLocation = if (locText.isNotBlank()) {
                    locText
                } else {
                    extractLocation(contentText)
                }

                // Đơn vị chuẩn bị: nếu cột có thì lấy đúng, nếu không trích từ nội dung, không có nữa thì BỎ TRỐNG
                val finalPrep = if (prepText.isNotBlank()) {
                    prepText
                } else {
                    extractPreparation(contentText)
                }

                // Thành phần dự / người được dự
                val finalAttendees = if (attendeeCellText.isNotBlank()) {
                    attendeeCellText
                } else {
                    extractAttendees(contentText)
                }

                val cleanTitleText = cleanTitle(contentText)
                val hash = cleanTitleText.hashCode().toString(16).removePrefix("-").take(6)
                val leaderTag = primaryLeader.ifBlank { "bgd" }
                val eventId = "synced-$activeDate-$formattedTime-$leaderTag-$hash"

                events.add(
                    ScheduleEvent(
                        id = eventId,
                        date = activeDate,
                        dayOfWeek = activeDayOfWeek,
                        session = session,
                        time = formattedTime,
                        primaryLeaderId = primaryLeader, // Không có để trống ""
                        coAttendees = coLeaders.joinToString(","),
                        title = cleanTitleText,
                        location = finalLocation,       // Không có để trống ""
                        preparation = finalPrep,         // Không có để trống ""
                        attendees = finalAttendees,     // Không có để trống ""
                        notes = ""
                    )
                )
            }
        } catch (_: Exception) {}
        return events
    }

    /**
     * Phân tích văn bản dòng theo dòng
     */
    private fun parseLineByLine(
        text: String,
        leaders: List<Leader>,
        referenceDate: Date
    ): List<ScheduleEvent> {
        val sanitizedText = extractTopScheduleSection(text)
        val lines = sanitizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val events = mutableListOf<ScheduleEvent>()

        var currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(referenceDate)
        var currentDayOfWeek = 1
        var currentTimeStr = "08h00"
        var currentSession = "sang"

        val timeRegex = Regex("""(\d{1,2})\s*(?:giờ|[h:])\s*(\d{2})""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            if (isHeaderOrFooterText(line)) continue

            // 1. Kiểm tra dòng tiêu đề Ngày (Ví dụ: "Thứ Hai, ngày 05/10/2026" hoặc "Thứ Ba | 06/10/2026")
            val dateHeader = parseDateHeader(line)
            if (dateHeader != null && !line.contains("Đ/c", ignoreCase = true) && !line.contains("Hội nghị", ignoreCase = true)) {
                currentDateStr = dateHeader.first
                currentDayOfWeek = dateHeader.second
                continue
            }

            // 2. Kiểm tra giờ bắt đầu trên dòng (Ví dụ: 07h30, 08:00, 14 giờ 00)
            val timeMatch = timeRegex.find(line)
            var contentLine = line
            if (timeMatch != null && (timeMatch.range.first < 35 || line.contains("|"))) {
                val hour = timeMatch.groupValues[1].toIntOrNull() ?: 8
                val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
                currentTimeStr = String.format(Locale.US, "%02dh%02d", hour, minute)
                currentSession = if (hour < 12) "sang" else "chieu"
                contentLine = line.substring(timeMatch.range.last + 1).trimStart(':', ' ', '-', '|')
            } else {
                contentLine = line.trimStart('-', ' ', '|')
            }

            // 3. Nhận diện sự kiện làm việc
            val lowerLine = contentLine.lowercase()
            val hasLeaderMention = lowerLine.contains("đ/c") ||
                    lowerLine.contains("ban giám đốc") ||
                    lowerLine.contains("giám đốc") ||
                    lowerLine.contains("bgd") ||
                    leaders.any { leader ->
                        lowerLine.contains(leader.name.lowercase()) ||
                        lowerLine.contains(leader.shortName.lowercase().substringBefore("(").trim())
                    } ||
                    (timeMatch != null && (lowerLine.contains("họp") || lowerLine.contains("làm việc") || lowerLine.contains("kiểm tra") || lowerLine.contains("hội ý") || lowerLine.contains("trực ban") || lowerLine.contains("giao ban") || lowerLine.contains("dự ")))

            if (hasLeaderMention) {
                // Kiểm tra lịch phát sinh ghép nhiều mốc giờ trong 1 đoạn
                val compoundRegex = Regex("""(?:[.;,]|\b)\s*(?:Đến|Sau\s*đó,?\s*lúc|Tiếp\s*theo,?\s*lúc)\s+(\d{1,2})\s*(?:giờ|[h:])\s*(\d{2})?\s*(?:phút)?[:\-–]?\s*(.*)""", RegexOption.IGNORE_CASE)
                val compoundMatch = compoundRegex.find(contentLine)

                if (compoundMatch != null) {
                    val part1 = contentLine.substring(0, compoundMatch.range.first).trim()
                    val (primaryLeaderId1, coAttendees1) = extractLeaders(part1.ifBlank { contentLine }, leaders)
                    val location1 = extractLocation(part1)
                    val preparation1 = extractPreparation(part1)
                    val attendees1 = extractAttendees(part1)
                    val leaderTag1 = primaryLeaderId1.ifBlank { "bgd" }
                    val titleClean1 = cleanTitle(part1)
                    val hash1 = titleClean1.hashCode().toString(16).removePrefix("-").take(6)

                    events.add(
                        ScheduleEvent(
                            id = "synced-$currentDateStr-$currentTimeStr-$leaderTag1-$hash1",
                            date = currentDateStr,
                            dayOfWeek = currentDayOfWeek,
                            session = currentSession,
                            time = currentTimeStr,
                            primaryLeaderId = primaryLeaderId1, // Dữ liệu không có thì bỏ trống ""
                            coAttendees = coAttendees1.joinToString(","),
                            title = titleClean1,
                            location = location1,               // Dữ liệu không có thì bỏ trống ""
                            preparation = preparation1,         // Dữ liệu không có thì bỏ trống ""
                            attendees = attendees1,             // Dữ liệu không có thì bỏ trống ""
                            notes = ""
                        )
                    )

                    val h2 = compoundMatch.groupValues[1].toIntOrNull() ?: 8
                    val m2 = compoundMatch.groupValues[2].toIntOrNull() ?: 0
                    val timeStr2 = String.format(Locale.US, "%02dh%02d", h2, m2)
                    val session2 = if (h2 < 12) "sang" else "chieu"
                    val rawPart2 = compoundMatch.groupValues[3].trim()

                    val (leaderId2, coAttendees2) = if (rawPart2.contains("đ/c", ignoreCase = true) || leaders.any { rawPart2.contains(it.name, ignoreCase = true) }) {
                        extractLeaders(rawPart2, leaders)
                    } else {
                        Pair(primaryLeaderId1, emptyList())
                    }

                    val titleClean2 = cleanTitle(rawPart2)
                    val location2 = extractLocation(rawPart2)
                    val preparation2 = extractPreparation(rawPart2)
                    val attendees2 = extractAttendees(rawPart2)
                    val leaderTag2 = leaderId2.ifBlank { "bgd" }
                    val hash2 = titleClean2.hashCode().toString(16).removePrefix("-").take(6)

                    events.add(
                        ScheduleEvent(
                            id = "synced-$currentDateStr-$timeStr2-$leaderTag2-$hash2",
                            date = currentDateStr,
                            dayOfWeek = currentDayOfWeek,
                            session = session2,
                            time = timeStr2,
                            primaryLeaderId = leaderId2,
                            coAttendees = coAttendees2.joinToString(","),
                            title = titleClean2,
                            location = location2,
                            preparation = preparation2,
                            attendees = attendees2,
                            notes = ""
                        )
                    )
                } else {
                    val (primaryLeaderId, coAttendees) = extractLeaders(contentLine, leaders)
                    val location = extractLocation(contentLine)
                    val preparation = extractPreparation(contentLine)
                    val attendees = extractAttendees(contentLine)

                    val leaderTag = primaryLeaderId.ifBlank { "bgd" }
                    val titleClean = cleanTitle(contentLine)
                    val titleHash = titleClean.hashCode().toString(16).removePrefix("-").take(6)
                    val eventId = "synced-$currentDateStr-$currentTimeStr-$leaderTag-$titleHash"

                    val event = ScheduleEvent(
                        id = eventId,
                        date = currentDateStr,
                        dayOfWeek = currentDayOfWeek,
                        session = currentSession,
                        time = currentTimeStr,
                        primaryLeaderId = primaryLeaderId, // Bỏ trống nếu không có
                        coAttendees = coAttendees.joinToString(","),
                        title = titleClean,
                        location = location,               // Bỏ trống nếu không có
                        preparation = preparation,         // Bỏ trống nếu không có
                        attendees = attendees,             // Bỏ trống nếu không có
                        notes = ""
                    )
                    events.add(event)
                }
            } else if (events.isNotEmpty() && !contentLine.contains("nghỉ", ignoreCase = true)) {
                // Dòng nối tiếp của sự kiện trước đó
                val last = events.last()
                val updated = last.copy(title = "${last.title} $contentLine")
                events[events.lastIndex] = updated
            }
        }

        return events
    }

    private fun parseDateHeader(line: String): Pair<String, Int>? {
        val dateHeaderRegex = Regex(
            """(?:Thứ\s*(Hai|Ba|Tư|Năm|Sáu|Bảy)|Chủ\s*Nhật)[,\s|/]+(?:ngày\s*)?(\d{1,2})[/-](\d{1,2})(?:[/-](\d{4}))?""",
            RegexOption.IGNORE_CASE
        )
        val dateMatch = dateHeaderRegex.find(line) ?: return null

        val dayStr = dateMatch.groupValues[1]
        val d = dateMatch.groupValues[2].toIntOrNull() ?: 1
        val m = dateMatch.groupValues[3].toIntOrNull() ?: 1
        val rawYear = dateMatch.groupValues[4]
        val y = rawYear.toIntOrNull() ?: 2026

        val dayOfWeek = when (dayStr.lowercase()) {
            "hai" -> 1
            "ba" -> 2
            "tư" -> 3
            "năm" -> 4
            "sáu" -> 5
            "bảy" -> 6
            else -> 7
        }
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
        return Pair(dateStr, dayOfWeek)
    }

    private fun parseTimeAndSession(timeColText: String, contentText: String): Pair<String, String> {
        val combined = "$timeColText $contentText"
        val timeRegex = Regex("""(\d{1,2})\s*(?:giờ|[h:])\s*(\d{2})""", RegexOption.IGNORE_CASE)
        val m = timeRegex.find(combined)
        return if (m != null) {
            val h = m.groupValues[1].toIntOrNull() ?: 8
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val tStr = String.format(Locale.US, "%02dh%02d", h, min)
            val session = if (h < 12) "sang" else "chieu"
            Pair(tStr, session)
        } else {
            val lower = timeColText.lowercase()
            if (lower.contains("chiều") || lower.contains("14h")) {
                Pair("14h00", "chieu")
            } else {
                Pair("08h00", "sang")
            }
        }
    }

    private fun isHeaderOrFooterText(line: String): Boolean {
        val upper = line.uppercase()
        return upper.contains("LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC") ||
                upper.contains("LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC") ||
                (upper.contains("LỊCH LÀM VIỆC") && (upper.contains("ĐIỀU CHỈNH") || upper.contains("TUẦN") || upper.contains("TỪ NGÀY"))) ||
                (upper.contains("CÔNG AN THÀNH PHỐ") && upper.length < 40 && !upper.contains("Đ/C") && !upper.contains("HỌP") && !upper.contains("DỰ")) ||
                (upper.contains("PHÒNG THAM MƯU") && upper.length < 35 && !upper.contains("Đ/C") && !upper.contains("HỌP") && !upper.contains("DỰ")) ||
                upper.contains("DỰ KIẾN LỊCH TRỰC TUẦN") ||
                line.startsWith("Author", ignoreCase = true) ||
                line.startsWith("Posted on", ignoreCase = true) ||
                line.startsWith("+ Lãnh đạo")
    }

    /**
     * Nhận diện chính xác Lãnh đạo chủ trì và thành phần cùng dự.
     * Quy tắc: Nếu văn bản không nêu đích danh đồng chí nào chủ trì thì trả về rỗng (""), KHÔNG tự ý gán!
     */
    private fun extractLeaders(text: String, leaders: List<Leader>): Pair<String, List<String>> {
        val lower = text.lowercase()
        val foundLeaders = mutableListOf<String>()

        val aliasGroups: List<Pair<String, List<String>>> = listOf(
            "huynh-viet-hoa" to listOf("huỳnh việt hòa", "việt hòa"),
            "nguyen-thanh-trang" to listOf("nguyễn thanh tràng", "thanh tràng", "đ/c tràng"),
            "le-duc-bay" to listOf("lê đức bảy", "đức bảy", "đ/c bảy"),
            "tran-hoang-do" to listOf("trần hoàng độ", "hoàng độ", "đ/c độ"),
            "nguyen-van-thang" to listOf("nguyễn văn thắng", "văn thắng", "đ/c thắng"),
            "huynh-hoai-han" to listOf("huỳnh hoài hận", "hoài hận", "đ/c hận"),
            "nguyen-thanh-binh" to listOf("nguyễn thanh bình", "thanh bình", "đ/c bình"),
            "bui-duc-an" to listOf("bùi đức an", "bùi đ.an", "đ/c bùi đức an"),
            "dinh-tung-an" to listOf("đinh tùng an", "đinh t.an", "đ/c đinh tùng an")
        )

        for ((id, aliases) in aliasGroups) {
            if (aliases.any { alias -> alias in lower }) {
                foundLeaders.add(id)
            }
        }

        // Kiểm tra từ khóa Giám đốc (chỉ khi không có tên cụ thể khác)
        val mentionsDeputyOffice = "phó giám đốc" in lower || "phó gd" in lower
        if ("huynh-viet-hoa" !in foundLeaders &&
            !mentionsDeputyOffice &&
            ("giám đốc" in lower || "đ/c hòa" in lower) &&
            !aliasGroups.drop(1).any { (_, aliases) -> aliases.any { it in lower } }
        ) {
            foundLeaders.add(0, "huynh-viet-hoa")
        }

        // Toàn thể Ban Giám đốc
        if ("ban giám đốc" in lower || "bgd" in lower) {
            for (id in ALL_BGD_IDS) {
                if (id !in foundLeaders) {
                    foundLeaders.add(id)
                }
            }
        }

        // NGUYÊN TẮC: Dữ liệu nào không có BỎ TRỐNG (""), không tự ý gán người mặc định!
        val primary = foundLeaders.firstOrNull() ?: ""
        val coAttendees = foundLeaders.drop(1)
        return Pair(primary, coAttendees)
    }

    /**
     * Trích xuất Địa điểm:
     * Dữ liệu không có thì BỎ TRỐNG (""), tuyệt đối không tự thêm "Trụ sở CATP".
     */
    private fun extractLocation(text: String): String {
        val regex = Regex("""(?:Điểm tại|Địa điểm:|Địa điểm|Điểm:)\s+([^.;]+?)(?:\.|$|;|Thành phần|Chuẩn bị|Đến\s+\d)""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            val loc = match.groupValues[1].trim()
            if (loc.isNotBlank()) return loc
        }
        val fallbackRegex = Regex("""(?:\btại\b)\s+([^.;]+?)(?:\.|$|;|Thành phần|Chuẩn bị)""", RegexOption.IGNORE_CASE)
        val fallbackMatch = fallbackRegex.find(text)
        val fallbackLoc = fallbackMatch?.groupValues?.get(1)?.trim()
        return if (!fallbackLoc.isNullOrBlank()) fallbackLoc else ""
    }

    /**
     * Trích xuất Đơn vị chuẩn bị nội dung:
     * Dữ liệu không có thì BỎ TRỐNG ("").
     */
    private fun extractPreparation(text: String): String {
        val regex = Regex("""(?:Chuẩn bị nội dung|chuẩn bị[:\s]+)([^.]+?)(?:\.|$|Thành phần|Điểm)""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    /**
     * Trích xuất Thành phần dự / Mời dự / Cùng dự:
     * Dữ liệu không có thì BỎ TRỐNG ("").
     */
    private fun extractAttendees(text: String): String {
        val regex = Regex("""(?:Thành phần dự|Thành phần|Mời dự|Cùng dự[:\s]+)([^.]+?)(?:\.|$|Điểm|Chuẩn bị)""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun cleanTitle(text: String): String {
        val trimmed = text.trim().trimEnd(';', ',')
        return if (trimmed.endsWith(".")) trimmed else "$trimmed."
    }
}
