package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Leader
import com.example.data.model.ScheduleEvent
import com.example.ui.components.LeaderBadge

@Composable
fun EventDetailDialog(
    event: ScheduleEvent?,
    leaders: List<Leader>,
    onDismiss: () -> Unit
) {
    if (event == null) return

    val leaderMap = leaders.associateBy { it.id }
    val primaryLeader = leaderMap[event.primaryLeaderId]
    val isDo = event.primaryLeaderId == "tran-hoang-do"

    val dayName = when (event.dayOfWeek) {
        1 -> "Thứ Hai"
        2 -> "Thứ Ba"
        3 -> "Thứ Tư"
        4 -> "Thứ Năm"
        5 -> "Thứ Sáu"
        6 -> "Thứ Bảy"
        else -> "Chủ Nhật"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_event_detail"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar (Crimson Red with Shield Accent)
                Surface(
                    color = Color(0xFF991B1B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CHI TIẾT LỊCH CÔNG TÁC",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$dayName, ngày ${event.date}",
                                color = Color(0xFFFECACA),
                                fontSize = 11.5.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Time and Session Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thời gian: ${event.time}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (event.session == "sang") Color(0xFFFEF3C7) else Color(0xFFDBEAFE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (event.session == "sang") "Buổi Sáng" else "Buổi Chiều",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (event.session == "sang") Color(0xFF92400E) else Color(0xFF1E40AF)
                            )
                        }
                    }

                    // Chủ trì
                    DetailRow(
                        icon = Icons.Default.Person,
                        iconTint = Color(0xFFDC2626),
                        label = "Chủ trì (Lãnh đạo BGD)"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            LeaderBadge(leader = primaryLeader)
                            if (isDo) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "★ Đồng chí Trần Hoàng Độ",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }

                    // Cùng dự
                    if (event.coAttendees.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Group,
                            iconTint = Color(0xFF2563EB),
                            label = "Cùng dự"
                        ) {
                            val coNames = event.coAttendees.split(",")
                                .mapNotNull { leaderMap[it.trim()]?.shortName }
                                .joinToString(", ")
                            Text(
                                text = coNames,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }

                    // Nội dung
                    DetailRow(
                        icon = Icons.Default.Assignment,
                        iconTint = Color(0xFF0F172A),
                        label = "Nội dung cuộc họp / công tác"
                    ) {
                        Text(
                            text = event.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            lineHeight = 18.sp
                        )
                    }

                    // Địa điểm
                    DetailRow(
                        icon = Icons.Default.Place,
                        iconTint = Color(0xFFEA580C),
                        label = "Địa điểm"
                    ) {
                        Text(
                            text = event.location.ifBlank { "Chưa xác định địa điểm" },
                            fontSize = 12.sp,
                            color = if (event.location.isNotBlank()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Chuẩn bị nội dung
                    if (event.preparation.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFF059669),
                            label = "Chuẩn bị nội dung"
                        ) {
                            Text(
                                text = event.preparation,
                                fontSize = 12.sp,
                                color = Color(0xFF065F46)
                            )
                        }
                    }

                    // Đơn vị liên quan / Mời dự
                    if (event.attendees.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Group,
                            iconTint = Color(0xFF7C3AED),
                            label = "Đơn vị liên quan / Mời dự"
                        ) {
                            Text(
                                text = event.attendees,
                                fontSize = 12.sp,
                                color = Color(0xFF5B21B6)
                            )
                        }
                    }

                    // Ghi chú nếu có
                    if (event.notes.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFF64748B),
                            label = "Ghi chú"
                        ) {
                            Text(
                                text = event.notes,
                                fontSize = 11.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Close Action Button (Clean read-only)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                    ) {
                        Text(
                            text = "Đóng",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}
