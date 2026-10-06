package com.example

import com.example.data.local.InitialData
import com.example.data.model.ScheduleEvent
import com.example.service.ConflictDetector
import com.example.service.ScheduleParser
import com.example.service.WebMonitorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLeaderDistinction_BuiDucAn_vs_DinhTungAn() {
        val leaders = InitialData.DEFAULT_LEADERS

        val rawTextBuiAn = "Thứ Ba, 29/09/2026\n08h00: Đ/c Bùi Đức An làm việc với Văn phòng Cơ quan CSĐT. Điểm tại Phòng làm việc đ/c Phó Giám đốc."
        val eventsBuiAn = ScheduleParser.parseRawText(rawTextBuiAn, leaders)
        assertEquals(1, eventsBuiAn.size)
        assertEquals("bui-duc-an", eventsBuiAn[0].primaryLeaderId)

        val rawTextDinhAn = "Thứ Ba, 29/09/2026\n08h00: Đ/c Đinh Tùng An làm việc với các đơn vị nghiệp vụ. Điểm tại Phòng làm việc."
        val eventsDinhAn = ScheduleParser.parseRawText(rawTextDinhAn, leaders)
        assertEquals(1, eventsDinhAn.size)
        assertEquals("dinh-tung-an", eventsDinhAn[0].primaryLeaderId)
    }

    @Test
    fun testConflictDetector_LeaderOverlap() {
        val leaders = InitialData.DEFAULT_LEADERS
        val event1 = ScheduleEvent(
            id = "evt-1",
            date = "2026-09-29",
            dayOfWeek = 2,
            session = "sang",
            time = "08h00",
            primaryLeaderId = "tran-hoang-do",
            title = "Cuộc họp 1",
            location = "Phòng họp số 1"
        )
        val event2 = ScheduleEvent(
            id = "evt-2",
            date = "2026-09-29",
            dayOfWeek = 2,
            session = "sang",
            time = "08h00",
            primaryLeaderId = "tran-hoang-do",
            title = "Cuộc họp 2",
            location = "Phòng họp số 2"
        )

        val conflicts = ConflictDetector.checkConflicts(
            targetEvent = event2,
            allEvents = listOf(event1),
            leaders = leaders
        )

        assertTrue(conflicts.isNotEmpty())
        assertTrue(conflicts[0].message.contains("Trùng lịch"))
    }

    @Test
    fun testWebMonitorService_DiffDetection() {
        val event1 = ScheduleEvent(
            id = "evt-1",
            date = "2026-09-29",
            dayOfWeek = 2,
            session = "sang",
            time = "08h00",
            primaryLeaderId = "tran-hoang-do",
            title = "Hội nghị giao ban đầu tuần",
            location = "Hội trường 1"
        )
        val event2New = ScheduleEvent(
            id = "evt-new",
            date = "2026-09-29",
            dayOfWeek = 2,
            session = "chieu",
            time = "14h00",
            primaryLeaderId = "huynh-viet-hoa",
            title = "Họp đột xuất Ban Giám đốc",
            location = "Hội trường lớn"
        )

        val diffs = WebMonitorService.calculateDiff(
            oldEvents = listOf(event1),
            newEvents = listOf(event1, event2New),
            timestampStr = "12:00:00 29/09/2026"
        )

        assertEquals(1, diffs.size)
        assertEquals("BỔ SUNG MỚI", diffs[0].changeType)
        assertEquals("Họp đột xuất Ban Giám đốc", diffs[0].eventTitle)
    }

    @Test
    fun testScheduleParser_StrictParsing_EmptyFieldsWhenMissing() {
        val leaders = InitialData.DEFAULT_LEADERS
        // Text without location or preparation: must leave them empty!
        val rawText = "Thứ Hai, ngày 05/10/2026\n07h30: Đ/c Huỳnh Việt Hòa chào cờ đầu tháng."
        val events = ScheduleParser.parseRawText(rawText, leaders)

        assertEquals(1, events.size)
        assertEquals("huynh-viet-hoa", events[0].primaryLeaderId)
        assertEquals("2026-10-05", events[0].date)
        assertEquals("07h30", events[0].time)
        // Data not present: left empty
        assertEquals("", events[0].location)
        assertEquals("", events[0].preparation)
        assertEquals("", events[0].attendees)
    }

    @Test
    fun testScheduleParser_DocumentTitleAndEditions() {
        val titleDraft = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (DỰ THẢO) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
        val extractedDraft = ScheduleParser.extractDocumentTitle(titleDraft)
        assertEquals(titleDraft, extractedDraft)
        assertEquals(0, ScheduleParser.extractEditionNumber(titleDraft))

        val titleAdj1 = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Điều chỉnh lần 1) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
        val extractedAdj1 = ScheduleParser.extractDocumentTitle(titleAdj1)
        assertEquals(titleAdj1, extractedAdj1)
        assertEquals(1, ScheduleParser.extractEditionNumber(titleAdj1))

        val titleAdj2 = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Điều chỉnh lần 2) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
        val extractedAdj2 = ScheduleParser.extractDocumentTitle(titleAdj2)
        assertEquals(titleAdj2, extractedAdj2)
        assertEquals(2, ScheduleParser.extractEditionNumber(titleAdj2))
    }

    @Test
    fun testWebMonitorService_TitleAdjustmentDetection() {
        val event1 = ScheduleEvent(
            id = "evt-1",
            date = "2026-10-05",
            dayOfWeek = 1,
            session = "sang",
            time = "08h00",
            primaryLeaderId = "tran-hoang-do",
            title = "Hội nghị giao ban đầu tuần",
            location = "Hội trường 1"
        )

        val diffs = WebMonitorService.calculateDiff(
            oldEvents = listOf(event1),
            newEvents = listOf(event1),
            timestampStr = "10:00:00 05/10/2026",
            editionNumber = 2,
            oldDocumentTitle = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Điều chỉnh lần 1) (Từ ngày 05/10/2026 đến ngày 11/10/2026)",
            newDocumentTitle = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ (Điều chỉnh lần 2) (Từ ngày 05/10/2026 đến ngày 11/10/2026)"
        )

        assertEquals(1, diffs.size)
        assertEquals("ĐIỀU CHỈNH LẦN 2", diffs[0].changeType)
        assertTrue(diffs[0].details.contains("Điều chỉnh lần 1"))
        assertTrue(diffs[0].details.contains("Điều chỉnh lần 2"))
    }
}
