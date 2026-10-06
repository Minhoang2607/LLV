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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConflictInfo
import com.example.data.model.Leader
import com.example.data.model.ScheduleEvent
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EventCard(
    event: ScheduleEvent,
    leaders: List<Leader>,
    conflicts: List<ConflictInfo> = emptyList(),
    showDate: Boolean = true,
    onViewDetail: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val leaderMap = leaders.associateBy { it.id }
    val primaryLeader = leaderMap[event.primaryLeaderId]
    val isBgdMeeting = (event.title.contains("Ban Giám đốc", ignoreCase = true) ||
            event.attendees.contains("Ban Giám đốc", ignoreCase = true)) &&
            !event.title.contains("làm việc với Ban Giám đốc", ignoreCase = true)
    val isDo = event.primaryLeaderId == "tran-hoang-do" ||
            (event.coAttendees.isNotBlank() && event.coAttendees.split(",").map { it.trim() }.contains("tran-hoang-do")) ||
            isBgdMeeting
    val hasConflicts = conflicts.isNotEmpty()

    val dayName = when (event.dayOfWeek) {
        1 -> "T2"
        2 -> "T3"
        3 -> "T4"
        4 -> "T5"
        5 -> "T6"
        6 -> "T7"
        else -> "CN"
    }

    val formattedDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d = parser.parse(event.date)
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(d!!)
    } catch (e: Exception) {
        event.date
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_event_${event.id}")
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isDo) 1.5.dp else if (hasConflicts) 1.5.dp else 1.dp,
                    color = if (hasConflicts) Color(0xFFF97316) else if (isDo) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // Conflict Alert Banner if any
            if (hasConflicts) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF7ED))
                        .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TRÙNG LỊCH THỜI GIAN THỰC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                    }
                    conflicts.forEach { c ->
                        Text(
                            text = "• ${c.message}",
                            fontSize = 9.sp,
                            color = Color(0xFF9A3412),
                            modifier = Modifier.padding(start = 16.dp, top = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Top Row: Time, Date info & Presiding Leader Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time & Day label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (showDate) {
                        Text(
                            text = "$dayName ($formattedDate)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF991B1B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = event.time,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (event.session == "sang") Color(0xFFFEF3C7) else Color(0xFFDBEAFE))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (event.session == "sang") "Sáng" else "Chiều",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (event.session == "sang") Color(0xFF92400E) else Color(0xFF1E40AF)
                        )
                    }
                }

                // Presiding Leader Badge & BGĐ indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBgdMeeting) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "Toàn thể BGĐ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    LeaderBadge(leader = primaryLeader)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Title (Fully visible)
            Text(
                text = event.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 17.sp
            )

            // Co-Attendees or BGĐ attendance list
            val hasCo = event.coAttendees.isNotBlank()
            if (hasCo || isBgdMeeting) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier
                            .size(12.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val coNames = if (hasCo) {
                        event.coAttendees.split(",")
                            .mapNotNull { leaderMap[it.trim()]?.shortName }
                            .joinToString(", ")
                    } else {
                        "Toàn thể Ban Giám đốc CATP (bao gồm Đ/c Độ và các Đ/c PGĐ)"
                    }
                    Text(
                        text = "Thành phần BGĐ: $coNames",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }

            // Compact, unified details: Địa điểm, Chuẩn bị, Mời dự (Gần nhau, liền mạch)
            val hasLocation = event.location.isNotBlank()
            val hasPrep = event.preparation.isNotBlank()
            val hasAtt = event.attendees.isNotBlank() && !event.attendees.trim().equals("theo giấy mời", true)

            if (hasLocation || hasPrep || hasAtt) {
                Spacer(modifier = Modifier.height(3.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Địa điểm
                    if (hasLocation) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "📍 Địa điểm: ",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = event.location,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 2. Chuẩn bị
                    if (hasPrep) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "📝 Chuẩn bị: ",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = event.preparation,
                                fontSize = 10.5.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 3. Mời dự
                    if (hasAtt) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "👥 Mời dự: ",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                            Text(
                                text = event.attendees,
                                fontSize = 10.5.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
