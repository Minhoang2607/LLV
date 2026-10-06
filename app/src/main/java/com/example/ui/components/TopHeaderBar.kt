package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgencyConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale

@Composable
fun TopHeaderBar(
    config: AgencyConfig,
    weekStart: Date,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onTitleClick: (() -> Unit)? = null,
    onOpenAdjustDialog: (() -> Unit)? = null,
    onOpenComparison: (() -> Unit)? = null,
    scanStatusMessage: String? = null,
    onScanStatusClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    cal.time = weekStart
    val weekEndCal = Calendar.getInstance()
    weekEndCal.time = weekStart
    weekEndCal.add(Calendar.DAY_OF_YEAR, 6)

    val dateFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
    val weekTitle = "Tuần: ${dateFmt.format(weekStart)} - ${dateFmt.format(weekEndCal.time)}/${yearFmt.format(weekEndCal.time)}"

    val editionNum = remember(config.documentTitle) {
        val extracted = com.example.service.ScheduleParser.extractEditionNumber(config.documentTitle)
        if (extracted > 0) extracted
        else if (config.documentTitle.contains("05/10/2026") || config.documentTitle.contains("điều chỉnh", ignoreCase = true)) 1
        else 0
    }

    val hasAdjustment = editionNum > 0 || config.documentTitle.contains("điều chỉnh", ignoreCase = true)
    val editionBadgeText = remember(editionNum, hasAdjustment) {
        when {
            editionNum > 0 -> "$editionNum"
            hasAdjustment -> "ĐC"
            else -> "Gốc"
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF991B1B),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF881337), Color(0xFF991B1B), Color(0xFF7F1D1D))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Row 1: Brand crest, Agency Title, Title "LỊCH BAN GIÁM ĐỐC", and Edition Badge at top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThamMuuLogo(
                    size = 38.dp,
                    modifier = Modifier.testTag("logo_tham_muu")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Tiêu đề Lịch Ban Giám Đốc
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onTitleClick?.invoke() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("btn_header_title_full_schedule")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LỊCH BAN GIÁM ĐỐC",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasAdjustment) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (editionNum > 0) "Lần $editionNum" else "Điều chỉnh",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Góc trên bên phải: Huy hiệu số lần điều chỉnh
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                        .clickable { onOpenComparison?.invoke() ?: onOpenAdjustDialog?.invoke() }
                        .padding(horizontal = 8.dp)
                        .testTag("badge_adjustment_edition"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (editionNum > 0) "Lần $editionNum" else "Bản gốc",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF991B1B)
                    )
                }
            }

            // Dải trạng thái quét thời gian thực: Quét lúc mấy giờ mấy phút có / không thay đổi
            if (!scanStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val isChanged = scanStatusMessage.contains("Có thay đổi", ignoreCase = true) || scanStatusMessage.contains("Điều chỉnh", ignoreCase = true)
                val isBlocked = scanStatusMessage.contains("chặn", ignoreCase = true) || scanStatusMessage.contains("403")
                val isError = isBlocked || scanStatusMessage.contains("Không truy cập", ignoreCase = true) ||
                        scanStatusMessage.contains("Không thể", ignoreCase = true) ||
                        scanStatusMessage.contains("Lỗi", ignoreCase = true) ||
                        scanStatusMessage.contains("thất bại", ignoreCase = true)
                val statusDotColor = when {
                    isBlocked -> Color(0xFFEF4444)
                    isError -> Color(0xFFF97316)
                    isChanged -> Color(0xFFFBBF24)
                    else -> Color(0xFF34D399)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.65f))
                        .clickable { onScanStatusClick?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(statusDotColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scanStatusMessage,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFEF3C7),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Week Navigator & Search Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Week Navigator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onPrevWeek,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tuần trước",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = weekTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier
                            .clickable { onCurrentWeek() }
                            .padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Tuần sau",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm kiếm...",
                                    color = Color(0xFFFCA5A5).copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_search_schedule")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onSearchChange("") }
                            )
                        }
                    }
                }
            }
        }
    }
}
