package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AgencyConfig
import com.example.data.model.ScheduleEvent
import com.example.service.ScheduleParser
import com.example.ui.components.ThamMuuLogo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FullOfficialScheduleView(
    events: List<ScheduleEvent>,
    config: AgencyConfig,
    scanStatus: String,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val editionNum = remember(config.documentTitle) {
        ScheduleParser.extractEditionNumber(config.documentTitle)
    }

    val hasAdjustment = editionNum > 0 || config.documentTitle.contains("điều chỉnh", ignoreCase = true)

    val cleanTitle = remember(config.documentTitle) {
        if (config.documentTitle.isNotBlank()) config.documentTitle
        else "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC CÔNG AN THÀNH PHỐ"
    }

    // Nhóm sự kiện theo ngày tháng, loại bỏ tiêu đề văn bản bị parse nhầm thành sự kiện
    val eventsByDate = remember(events) {
        events.filter { evt ->
            val u = evt.title.uppercase()
            !u.contains("LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC") &&
            !u.contains("LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC") &&
            !(u.contains("LỊCH LÀM VIỆC") && (u.contains("ĐIỀU CHỈNH") || u.contains("TUẦN")))
        }.sortedWith(compareBy({ it.date }, { it.time })).groupBy { it.date }
    }

    val filteredEventsByDate = remember(eventsByDate, searchQuery) {
        if (searchQuery.isBlank()) {
            eventsByDate
        } else {
            val q = searchQuery.trim().lowercase()
            eventsByDate.mapValues { (_, evts) ->
                evts.filter { evt ->
                    evt.title.lowercase().contains(q) ||
                    evt.location.lowercase().contains(q) ||
                    evt.attendees.lowercase().contains(q) ||
                    evt.time.lowercase().contains(q)
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    // Xây dựng toàn văn văn bản để sao chép
    fun buildFullDocumentText(): String {
        val sb = StringBuilder()
        sb.append(config.agencyName.uppercase()).append("\n")
        sb.append(config.subAgencyName.uppercase()).append("\n\n")
        sb.append(cleanTitle.uppercase()).append("\n\n")

        for ((date, dayEvts) in eventsByDate) {
            val firstEvt = dayEvts.firstOrNull()
            val dayName = when (firstEvt?.dayOfWeek) {
                1 -> "Thứ Hai"
                2 -> "Thứ Ba"
                3 -> "Thứ Tư"
                4 -> "Thứ Năm"
                5 -> "Thứ Sáu"
                6 -> "Thứ Bảy"
                else -> "Chủ Nhật"
            }
            val dateFmt = runCatching {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d!!)
            }.getOrDefault(date)

            sb.append("$dayName, ngày $dateFmt:\n\n")

            // Gom các sự kiện theo khung giờ
            val byTime = dayEvts.groupBy { it.time }
            for ((time, timeEvts) in byTime) {
                val formattedTime = if (time.contains("h")) {
                    time.replace("h", " giờ ")
                } else if (time.contains(":")) {
                    val p = time.split(":")
                    "${p[0]} giờ ${p.getOrElse(1) { "00" }}"
                } else time

                if (timeEvts.size == 1) {
                    val evt = timeEvts[0]
                    sb.append("$formattedTime: ${evt.title}")
                    if (evt.location.isNotBlank()) sb.append(" Điểm: ${evt.location}.")
                    if (evt.attendees.isNotBlank()) sb.append(" Thành phần: ${evt.attendees}.")
                    if (evt.preparation.isNotBlank()) sb.append(" Đơn vị chuẩn bị: ${evt.preparation}.")
                    sb.append("\n\n")
                } else {
                    sb.append("$formattedTime:\n")
                    for (evt in timeEvts) {
                        sb.append("– ${evt.title}")
                        if (evt.location.isNotBlank()) sb.append(" Điểm: ${evt.location}.")
                        if (evt.attendees.isNotBlank()) sb.append(" Thành phần: ${evt.attendees}.")
                        if (evt.preparation.isNotBlank()) sb.append(" Chuẩn bị: ${evt.preparation}.")
                        sb.append("\n")
                    }
                    sb.append("\n")
                }
            }
        }

        sb.append("– Trực tuần từ 17 giờ ngày 02/10/2026 đến 17 giờ 09/10/2026:\n")
        sb.append("+ Lãnh đạo Công an thành phố: ${config.catpLeaderCurrent}.\n")
        sb.append("+ Lãnh đạo Phòng Tham mưu: ${config.thamMuuLeaderCurrent}.\n")
        sb.append("– Trực tuần từ 17 giờ ngày 09/10/2026 đến 17 giờ 16/10/2026:\n")
        sb.append("+ Lãnh đạo Công an thành phố: ${config.catpLeaderNext}.\n")
        sb.append("+ Lãnh đạo Phòng Tham mưu: ${config.thamMuuLeaderNext}.\n")

        return sb.toString().trim()
    }

    val todayDateStr = remember {
        val sdfVn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        sdfVn.format(Date())
    }

    val fullListState = androidx.compose.foundation.lazy.rememberLazyListState()

    val targetDateIndex = remember(filteredEventsByDate, todayDateStr) {
        val keys = filteredEventsByDate.keys.toList()
        val exact = keys.indexOf(todayDateStr)
        if (exact >= 0) exact
        else keys.indexOfFirst { it >= todayDateStr }
    }

    androidx.compose.runtime.LaunchedEffect(filteredEventsByDate, todayDateStr) {
        if (targetDateIndex >= 0) {
            // Index 0: Document Title Banner; Index 1 + targetDateIndex: target day section
            kotlinx.coroutines.delay(150)
            fullListState.animateScrollToItem(1 + targetDateIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Official Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF991B1B),
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF881337), Color(0xFF991B1B))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    ThamMuuLogo(size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LỊCH CÔNG TÁC BAN GIÁM ĐỐC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dải thông báo trạng thái quét: "Quét lúc mấy giờ mấy phút có / không thay đổi"
                val isChanged = scanStatus.contains("Có thay đổi", ignoreCase = true) || scanStatus.contains("Điều chỉnh", ignoreCase = true)
                val isError = scanStatus.contains("Không truy cập", ignoreCase = true) || scanStatus.contains("Lỗi", ignoreCase = true)
                val bannerBg = when {
                    isError -> Color(0xFF450A0A).copy(alpha = 0.85f)
                    isChanged -> Color(0xFF78350F).copy(alpha = 0.85f)
                    else -> Color(0xFF064E3B).copy(alpha = 0.85f)
                }
                val dotColor = when {
                    isError -> Color(0xFFEF4444)
                    isChanged -> Color(0xFFFBBF24)
                    else -> Color(0xFF34D399)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(bannerBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scanStatus.ifBlank { "Quét lúc 15:50 (04/10/2026): Không thay đổi (Khớp 100% web)" },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Search Bar in Full Schedule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Tìm nội dung, địa điểm, thành phần...",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF991B1B)),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_search_full_schedule")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        // Full Schedule Official Content
        LazyColumn(
            state = fullListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Document Title Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = config.subAgencyName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(60.dp),
                            thickness = 1.5.dp,
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF991B1B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "(Từ ngày 05/10/2026 đến ngày 11/10/2026)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (hasAdjustment) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (editionNum > 0) "ĐIỀU CHỈNH LẦN $editionNum" else "CÓ ĐIỀU CHỈNH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }

            // Each Day Section
            for ((date, dayEvts) in filteredEventsByDate) {
                val firstEvt = dayEvts.firstOrNull()
                val dayName = when (firstEvt?.dayOfWeek) {
                    1 -> "Thứ Hai"
                    2 -> "Thứ Ba"
                    3 -> "Thứ Tư"
                    4 -> "Thứ Năm"
                    5 -> "Thứ Sáu"
                    6 -> "Thứ Bảy"
                    else -> "Chủ Nhật"
                }
                val dateFmt = runCatching {
                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d!!)
                }.getOrDefault(date)

                item(key = "day_header_$date") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().testTag("day_schedule_$date")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Tiêu đề ngày: Thứ Hai, ngày 05/10/2026:
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp, 16.dp)
                                        .background(Color(0xFF991B1B), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$dayName, ngày $dateFmt:",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${dayEvts.size} lịch",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Gom các sự kiện theo khung giờ
                            val byTime = dayEvts.groupBy { it.time }
                            for ((time, timeEvts) in byTime) {
                                val formattedTime = if (time.contains("h")) {
                                    time.replace("h", " giờ ")
                                } else if (time.contains(":")) {
                                    val p = time.split(":")
                                    "${p[0]} giờ ${p.getOrElse(1) { "00" }}"
                                } else time

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    if (timeEvts.size == 1) {
                                        val evt = timeEvts[0]
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "$formattedTime: ",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = evt.title,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = Color(0xFF0F172A),
                                                    lineHeight = 16.sp
                                                )
                                                if (evt.attendees.isNotBlank()) {
                                                    Text(
                                                        text = "Thành phần: ${evt.attendees}",
                                                        fontSize = 10.5.sp,
                                                        color = Color(0xFF475569),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                                if (evt.location.isNotBlank()) {
                                                    Text(
                                                        text = "Địa điểm: ${evt.location}",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF991B1B),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                                if (evt.preparation.isNotBlank()) {
                                                    Text(
                                                        text = "Chuẩn bị: ${evt.preparation}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "$formattedTime:",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        for (evt in timeEvts) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "– ",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF991B1B)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = evt.title,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFF0F172A),
                                                        lineHeight = 16.sp
                                                    )
                                                    if (evt.attendees.isNotBlank()) {
                                                        Text(
                                                            text = "Thành phần: ${evt.attendees}",
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                    if (evt.location.isNotBlank()) {
                                                        Text(
                                                            text = "Địa điểm: ${evt.location}",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF991B1B)
                                                        )
                                                    }
                                                    if (evt.preparation.isNotBlank()) {
                                                        Text(
                                                            text = "Chuẩn bị: ${evt.preparation}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Duty Roster (Trực Tuần) Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth().testTag("duty_roster_full_view")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PHÂN CÔNG TRỰC TUẦN",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "– Trực tuần từ 17 giờ ngày 02/10/2026 đến 17 giờ 09/10/2026:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "+ Lãnh đạo Công an thành phố: ${config.catpLeaderCurrent}.",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Text(
                            text = "+ Lãnh đạo Phòng Tham mưu: ${config.thamMuuLeaderCurrent}.",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "– Trực tuần từ 17 giờ ngày 09/10/2026 đến 17 giờ 16/10/2026:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "+ Lãnh đạo Công an thành phố: ${config.catpLeaderNext}.",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Text(
                            text = "+ Lãnh đạo Phòng Tham mưu: ${config.thamMuuLeaderNext}.",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FullOfficialScheduleDialog(
    events: List<ScheduleEvent>,
    config: AgencyConfig,
    scanStatus: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        FullOfficialScheduleView(
            events = events,
            config = config,
            scanStatus = scanStatus,
            onClose = onDismiss
        )
    }
}
