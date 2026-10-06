package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.data.model.AgencyConfig
import com.example.data.model.Leader
import com.example.data.model.ScheduleEvent
import com.example.data.model.WeekDayInfo
import com.example.ui.components.DutyRosterCard
import com.example.ui.components.EventCard

import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import com.example.ui.screens.FullOfficialScheduleView
import com.example.ui.screens.ScheduleComparisonScreen

@Composable
fun ExecutiveMatrixScreen(
    events: List<ScheduleEvent>,
    leaders: List<Leader>,
    config: AgencyConfig,
    weekDays: List<WeekDayInfo>,
    selectedLeaderId: String,
    onSelectLeader: (String) -> Unit,
    selectedDayFilter: String?,
    onSelectDayFilter: (String?) -> Unit,
    onViewDetail: (ScheduleEvent) -> Unit,
    onOpenAdjustDialog: (() -> Unit)? = null,
    onOpenComparison: (() -> Unit)? = null,
    scanStatus: String = "",
    displayMode: String = "LEADERS",
    onToggleDisplayMode: ((String) -> Unit)? = null,
    allEvents: List<ScheduleEvent> = events,
    currentWeekStart: Date = Date(),
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onCurrentWeek: () -> Unit = {},
    archivedPreviousEvents: List<ScheduleEvent> = emptyList(),
    archivedPreviousTitle: String = "",
    modifier: Modifier = Modifier
) {
    var internalDisplayMode by rememberSaveable { mutableStateOf(displayMode) }
    val effectiveMode = onToggleDisplayMode?.let { displayMode } ?: internalDisplayMode
    val setMode: (String) -> Unit = { mode ->
        internalDisplayMode = mode
        onToggleDisplayMode?.invoke(mode)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Bộ chuyển chế độ: "Lịch Lãnh đạo" vs "Toàn bộ tuần" vs "Đối chiếu điều chỉnh"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (effectiveMode == "LEADERS") Color(0xFF991B1B) else Color.Transparent)
                    .clickable { setMode("LEADERS") }
                    .padding(vertical = 6.dp)
                    .testTag("tab_mode_leaders_matrix"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (effectiveMode == "LEADERS") Color.White else Color(0xFF334155),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Lịch Lãnh đạo",
                        fontSize = 10.5.sp,
                        fontWeight = if (effectiveMode == "LEADERS") FontWeight.Bold else FontWeight.Medium,
                        color = if (effectiveMode == "LEADERS") Color.White else Color(0xFF334155)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (effectiveMode == "FULL") Color(0xFF991B1B) else Color.Transparent)
                    .clickable { setMode("FULL") }
                    .padding(vertical = 6.dp)
                    .testTag("tab_mode_full_schedule"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (effectiveMode == "FULL") Color.White else Color(0xFF334155),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Toàn bộ tuần",
                        fontSize = 10.5.sp,
                        fontWeight = if (effectiveMode == "FULL") FontWeight.Bold else FontWeight.Medium,
                        color = if (effectiveMode == "FULL") Color.White else Color(0xFF334155)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (effectiveMode == "COMPARISON") Color(0xFF991B1B) else Color.Transparent)
                    .clickable { setMode("COMPARISON") }
                    .padding(vertical = 6.dp)
                    .testTag("tab_mode_comparison"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = if (effectiveMode == "COMPARISON") Color.White else Color(0xFF334155),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Đối chiếu điều chỉnh",
                        fontSize = 10.5.sp,
                        fontWeight = if (effectiveMode == "COMPARISON") FontWeight.Bold else FontWeight.Medium,
                        color = if (effectiveMode == "COMPARISON") Color.White else Color(0xFF334155)
                    )
                }
            }
        }

        if (effectiveMode == "COMPARISON") {
            ScheduleComparisonScreen(
                allEvents = allEvents,
                currentWeekStart = currentWeekStart,
                onPrevWeek = onPrevWeek,
                onNextWeek = onNextWeek,
                onCurrentWeek = onCurrentWeek,
                archivedPreviousEvents = archivedPreviousEvents,
                archivedPreviousTitle = archivedPreviousTitle,
                modifier = Modifier.weight(1f)
            )
        } else if (effectiveMode == "FULL") {
            FullOfficialScheduleView(
                events = events,
                config = config,
                scanStatus = scanStatus,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Chế độ xem lọc theo lãnh đạo
            val leaderMap = leaders.associateBy { it.id }
            val isAllLeaders = selectedLeaderId == "all" || selectedLeaderId.isBlank()
            val activeLeader = if (isAllLeaders) null else leaderMap[selectedLeaderId] ?: leaders.firstOrNull()

            val rawLeaderEvents = if (isAllLeaders) {
                events
            } else {
                events.filter { evt ->
                    val isLeaderInCo = evt.coAttendees.isNotBlank() && evt.coAttendees.split(",").map { it.trim() }.contains(selectedLeaderId)
                    val isBanGiamDocEvent = (evt.title.contains("Ban Giám đốc", ignoreCase = true) ||
                            evt.attendees.contains("Ban Giám đốc", ignoreCase = true)) &&
                            !evt.title.contains("làm việc với Ban Giám đốc", ignoreCase = true)
                    evt.primaryLeaderId == selectedLeaderId || isLeaderInCo || isBanGiamDocEvent
                }
            }

            // Tuyệt đối loại bỏ tiêu đề tài liệu khỏi danh sách nội dung lịch công tác
            val leaderEvents = rawLeaderEvents.filter { evt ->
                val upper = evt.title.uppercase()
                !upper.contains("LỊCH LÀM VIỆC CỦA BAN GIÁM ĐỐC") &&
                !upper.contains("LỊCH CÔNG TÁC CỦA BAN GIÁM ĐỐC") &&
                !(upper.contains("LỊCH LÀM VIỆC") && (upper.contains("ĐIỀU CHỈNH") || upper.contains("TUẦN")))
            }

            val displayEvents = if (selectedDayFilter == null) {
                leaderEvents
            } else {
                leaderEvents.filter { it.date == selectedDayFilter }
            }

            val coroutineScope = rememberCoroutineScope()
            val todayDateStr = remember {
                val sdfVn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                }
                sdfVn.format(Date())
            }

            val listState = rememberLazyListState()
            val dayRowState = rememberLazyListState()

            // Tìm vị trí sự kiện của ngày hiện tại
            val targetEventIndex = remember(displayEvents, todayDateStr) {
                val exact = displayEvents.indexOfFirst { it.date == todayDateStr }
                if (exact >= 0) exact
                else displayEvents.indexOfFirst { it.date >= todayDateStr }
            }

            // Khi người dùng bấm chọn 1 ngày cụ thể: cuộn ngay về đầu danh sách sự kiện ngày đó
            LaunchedEffect(selectedDayFilter) {
                if (selectedDayFilter != null) {
                    kotlinx.coroutines.delay(100)
                    listState.animateScrollToItem(2)
                }
            }

            // Lúc mở ứng dụng hoặc đổi lãnh đạo ở chế độ Cả tuần: tự động cuộn đến ngày hiện tại
            LaunchedEffect(selectedLeaderId, todayDateStr) {
                if (selectedDayFilter == null && displayEvents.isNotEmpty()) {
                    val targetItemIndex = if (targetEventIndex >= 0) {
                        // Vị trí trong LazyColumn:
                        // Item 0: Thanh chọn lãnh đạo
                        // Item 1: Banner lãnh đạo
                        // Item 2: Dải chọn ngày (Thứ 2..CN)
                        // Item 3 + targetEventIndex: Thẻ sự kiện ngày hiện tại
                        (3 + targetEventIndex).coerceAtMost(3 + displayEvents.lastIndex)
                    } else {
                        0 // Về đầu danh sách sự kiện, không cuộn xuống chân trang
                    }

                    // Chờ LazyColumn hoàn tất layout pass để cuộn chuẩn xác
                    kotlinx.coroutines.delay(150)
                    listState.animateScrollToItem(targetItemIndex)

                    // Đồng thời cuộn thanh ngày ngang đến ngày hiện tại
                    val dayIdx = weekDays.indexOfFirst { it.dateStr == todayDateStr }
                    if (dayIdx >= 0) {
                        dayRowState.animateScrollToItem((dayIdx + 1).coerceAtLeast(0))
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
        // 1. Horizontal Leader Selector Bar (Nút "Tất cả" + 9 đồng chí)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CHỌN ĐỒNG CHÍ BAN GIÁM ĐỐC",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Text(
                            text = if (isAllLeaders) "Toàn bộ 9 đồng chí" else "Đang xem 1 đồng chí",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAllLeaders) Color(0xFF991B1B) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // NÚT TẤT CẢ (9 ĐỒNG CHÍ BGD)
                        item {
                            val isSelected = isAllLeaders
                            val totalCount = events.size

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF991B1B) else Color(0xFFF1F5F9))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF991B1B) else Color(0xFFCBD5E1),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectLeader("all") }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("leader_tab_all")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tất cả (9 Đ/c)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                    if (totalCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) Color(0xFFFEF3C7) else Color(0xFFE2E8F0))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$totalCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF92400E) else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // DANH SÁCH 9 ĐỒNG CHÍ BAN GIÁM ĐỐC
                        items(leaders, key = { it.id }) { leader ->
                            val isSelected = !isAllLeaders && leader.id == selectedLeaderId
                            val count = events.count { evt ->
                                val isLeaderInCo = evt.coAttendees.isNotBlank() && evt.coAttendees.split(",").map { it.trim() }.contains(leader.id)
                                val isBanGiamDocEvent = (evt.title.contains("Ban Giám đốc", ignoreCase = true) ||
                                        evt.attendees.contains("Ban Giám đốc", ignoreCase = true)) &&
                                        !evt.title.contains("làm việc với Ban Giám đốc", ignoreCase = true)
                                evt.primaryLeaderId == leader.id || isLeaderInCo || isBanGiamDocEvent
                            }

                            val dotColor = try {
                                Color(android.graphics.Color.parseColor(leader.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF991B1B)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF991B1B) else Color(0xFFF1F5F9))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF991B1B) else Color(0xFFCBD5E1),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectLeader(leader.id) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("leader_tab_${leader.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = leader.shortName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                    if (leader.isPriority) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "★",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color(0xFFFDE68A) else Color(0xFFD97706)
                                        )
                                    }
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) Color(0xFFFEF3C7) else Color(0xFFE2E8F0))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF92400E) else Color(0xFF475569)
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

        // 2. Info Header Card (Banner hiển thị Tất Cả hoặc Lãnh Đạo đang chọn)
        item {
            if (isAllLeaders) {
                // Banner khi chọn Tất Cả 9 Đồng chí
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF881337), Color(0xFF991B1B), Color(0xFF7F1D1D))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BAN GIÁM ĐỐC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Lịch toàn bộ 9 đồng chí lãnh đạo",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${leaderEvents.size} lịch công tác",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else if (activeLeader != null) {
                // Banner cho 1 lãnh đạo cụ thể
                val bannerColor = try {
                    Color(android.graphics.Color.parseColor(activeLeader.colorHex))
                } catch (e: Exception) {
                    Color(0xFF991B1B)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerColor),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeLeader.title.uppercase(),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeLeader.name,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (activeLeader.isPriority) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFBBF24))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Ưu tiên theo dõi",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF78350F)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${leaderEvents.size} lịch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Day Filter Ribbon (Tối ưu giãn cách hàng ngang)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                LazyRow(
                    state = dayRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        val isAll = selectedDayFilter == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAll) Color(0xFF991B1B) else Color(0xFFF1F5F9))
                                .clickable { onSelectDayFilter(null) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isAll) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cả tuần",
                                    fontSize = 10.sp,
                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAll) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    items(weekDays, key = { it.dateStr }) { day ->
                        val isSelected = selectedDayFilter == day.dateStr
                        val isToday = day.dateStr == todayDateStr
                        val count = leaderEvents.count { it.date == day.dateStr }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Color(0xFF991B1B)
                                    else if (isToday) Color(0xFFFEF3C7)
                                    else Color(0xFFF1F5F9)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF991B1B)
                                    else if (isToday) Color(0xFFF59E0B)
                                    else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectDayFilter(day.dateStr) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${day.label}${if (count > 0) " ($count)" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White
                                    else if (isToday) Color(0xFF92400E)
                                    else if (count > 0) Color(0xFF0F172A)
                                    else Color(0xFF94A3B8)
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFD97706))
                                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    ) {
                                        Text(
                                            text = "Hôm nay",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Events Display List
        if (displayEvents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAllLeaders) "Không có lịch công tác nào của Ban Giám đốc trong thời gian này."
                               else "Không có lịch công tác nào của đồng chí trong phạm vi này.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            items(displayEvents, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    leaders = leaders,
                    onViewDetail = { onViewDetail(event) }
                )
            }
        }

        // 5. Duty Roster Footer
        item {
            DutyRosterCard(config = config)
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
    }
}
}
