package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleEvent
import com.example.service.DiffCategory
import com.example.service.ScheduleComparisonItem
import com.example.service.ScheduleDiffHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleComparisonScreen(
    allEvents: List<ScheduleEvent>,
    currentWeekStart: Date,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    archivedPreviousEvents: List<ScheduleEvent> = emptyList(),
    archivedPreviousTitle: String = "",
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("ALL") } // "ALL", "ADDED", "MODIFIED"

    val startCal = Calendar.getInstance().apply { time = currentWeekStart }
    val endCal = Calendar.getInstance().apply { time = currentWeekStart; add(Calendar.DAY_OF_YEAR, 6) }
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val startIso = isoFmt.format(startCal.time)
    val endIso = isoFmt.format(endCal.time)
    val startDisplay = displayFmt.format(startCal.time)
    val endDisplay = displayFmt.format(endCal.time)

    val todayDateStr = remember {
        val sdfVn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        sdfVn.format(Date())
    }

    val weekEvents = remember(allEvents, startIso, endIso) {
        allEvents.filter { it.date in startIso..endIso }
    }

    val allComparisonItems = remember(weekEvents, startIso, archivedPreviousEvents) {
        val baseItems = ScheduleDiffHelper.compareWithPreviousEdition(
            currentEvents = weekEvents,
            weekStartIso = startIso,
            editionFilter = "ALL"
        ).toMutableList()

        if (archivedPreviousEvents.isNotEmpty()) {
            val weekArchived = archivedPreviousEvents.filter { it.date in startIso..endIso }
            if (weekArchived.isNotEmpty()) {
                val dynamicDiffs = ScheduleDiffHelper.compareWithRecentAdjustment(
                    previousEvents = weekArchived,
                    newEvents = weekEvents
                )
                // Đưa các thay đổi so với lần điều chỉnh gần nhất vừa lưu trữ lên đầu
                baseItems.addAll(0, dynamicDiffs)
            }
        }
        baseItems
    }

    val filteredItems = remember(allComparisonItems, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "ADDED" -> allComparisonItems.filter { it.category == DiffCategory.ADDED }
            "MODIFIED" -> allComparisonItems.filter { it.category == DiffCategory.MODIFIED }
            else -> allComparisonItems
        }
    }

    val totalAdded = remember(allComparisonItems) { allComparisonItems.count { it.category == DiffCategory.ADDED } }
    val totalModified = remember(allComparisonItems) { allComparisonItems.count { it.category == DiffCategory.MODIFIED } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. THANH ĐIỀU HƯỚNG TUẦN & TIÊU ĐỀ ĐỐI CHIẾU
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    tint = Color(0xFF991B1B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "ĐỐI CHIẾU LỊCH ĐIỀU CHỈNH",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Tuần từ $startDisplay đến $endDisplay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Các nút chuyển tuần
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPrevWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Tuần trước",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .clickable { onCurrentWeek() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        tint = Color(0xFF991B1B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Tuần này",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onNextWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Tuần sau",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. THẺ TỔNG HỢP NỘI DUNG BIẾN ĐỘNG QUA CÁC LẦN ĐIỀU CHỈNH
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF991B1B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "THAY ĐỔI SO VỚI LẦN ĐIỀU CHỈNH GẦN NHẤT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Text(
                            text = "${filteredItems.size} nội dung",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Lưu trữ lịch hiện tại và nêu chi tiết các nội dung bổ sung mới, thay đổi so với lần điều chỉnh gần nhất (Lần 1 vs Bản gốc, Lần 2 vs Lần 1, Lần 3 vs Lần 2...).",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 13.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lọc theo loại biến động: Tất cả, Bổ sung mới, Đã thay đổi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryPill(
                            label = "Tất cả",
                            count = allComparisonItems.size,
                            isSelected = selectedCategoryFilter == "ALL",
                            onClick = { selectedCategoryFilter = "ALL" },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryPill(
                            label = "➕ Bổ sung mới",
                            count = totalAdded,
                            isSelected = selectedCategoryFilter == "ADDED",
                            color = Color(0xFF047857),
                            onClick = { selectedCategoryFilter = "ADDED" },
                            modifier = Modifier.weight(1.1f)
                        )
                        CategoryPill(
                            label = "🔄 Đã điều chỉnh",
                            count = totalModified,
                            isSelected = selectedCategoryFilter == "MODIFIED",
                            color = Color(0xFFB45309),
                            onClick = { selectedCategoryFilter = "MODIFIED" },
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }

        // 4. DANH SÁCH CÁC THẺ ĐỐI CHIẾU CHI TIẾT
        if (filteredItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có nội dung điều chỉnh nào thỏa mãn bộ lọc đã chọn.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                ComparisonItemCard(
                    item = item,
                    todayDateStr = todayDateStr
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color = Color(0xFF991B1B),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color else Color.White)
            .border(1.dp, if (isSelected) color else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label ($count)",
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF334155),
            maxLines = 1
        )
    }
}

@Composable
private fun ComparisonItemCard(
    item: ScheduleComparisonItem,
    todayDateStr: String
) {
    val isAdded = item.category == DiffCategory.ADDED
    val statusColor = if (isAdded) Color(0xFF047857) else Color(0xFFB45309)
    val statusBg = if (isAdded) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
    val statusBorder = if (isAdded) Color(0xFFA7F3D0) else Color(0xFFFDE68A)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Edition Pair Badge, Category Badge, Time Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cặp đối chiếu (Ví dụ: "Lần 1 vs Bản gốc", "Lần 2 vs Lần 1",...)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF881337))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.editionLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Loại thay đổi: BỔ SUNG MỚI hoặc ĐÃ ĐIỀU CHỈNH
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBg)
                            .border(1.dp, statusBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAdded) "➕ BỔ SUNG MỚI" else "🔄 ĐÃ THAY ĐỔI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Thời gian: Thứ, ngày, giờ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF991B1B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.timeDisplay} • ${item.dayOfWeekName} (${item.dateDisplay})",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Tên Lãnh đạo phụ trách
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF991B1B),
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = item.leaderName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF991B1B)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tiêu đề sự kiện
            Text(
                text = item.eventTitle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 16.5.sp
            )

            // Địa điểm
            if (item.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp
                    )
                }
            }

            // Thành phần dự
            if (item.attendees.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thành phần: ${item.attendees}",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 13.5.sp
                    )
                }
            }

            // Chuẩn bị
            if (item.preparation.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chuẩn bị: ${item.preparation}",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 13.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CHI TIẾT ĐỐI CHIẾU
            if (isAdded && item.addedDescription != null) {
                // Hộp hiển thị nội dung bổ sung mới
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.addedDescription,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF065F46),
                        lineHeight = 14.5.sp
                    )
                }
            } else if (!isAdded) {
                // Hai hộp đối chiếu trước & sau điều chỉnh
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.originalContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(7.dp)
                        ) {
                            Text(
                                text = item.originalContent,
                                fontSize = 9.5.sp,
                                color = Color(0xFF475569),
                                lineHeight = 13.5.sp
                            )
                        }
                    }

                    if (item.modifiedContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                .padding(7.dp)
                        ) {
                            Text(
                                text = item.modifiedContent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E40AF),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    if (item.changeHighlights.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            item.changeHighlights.forEach { highlight ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier
                                            .size(11.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = highlight,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A),
                                        lineHeight = 13.sp
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
