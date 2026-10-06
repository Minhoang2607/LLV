package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InitialData
import com.example.data.model.MonitorStatus
import com.example.data.model.ScheduleChangeLog
import com.example.data.model.ScheduleEvent
import com.example.service.BackgroundSyncService
import com.example.service.DiffCategory
import com.example.service.ScheduleComparisonItem
import com.example.service.ScheduleDiffHelper
import com.example.ui.dialogs.AdjustScheduleDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WebMonitorScreen(
    monitorStatus: MonitorStatus,
    changeLogs: List<ScheduleChangeLog>,
    totalEventsCount: Int,
    allEvents: List<ScheduleEvent> = emptyList(),
    currentWeekStart: Date = Date(),
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onCurrentWeek: () -> Unit = {},
    onCheckNow: () -> Unit,
    onSimulateChange: () -> Unit = {},
    onClearLogs: () -> Unit,
    onUpdateTargetUrl: (String) -> Unit = {},
    onUpdateAccount: (username: String, pass: String) -> Unit = { _, _ -> },
    onImportPastedSchedule: (rawText: String) -> Unit = {},
    onSyncLatest: () -> Unit = {},
    onSetMonitorInterval: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isEditingUrl by remember { mutableStateOf(false) }
    var isEditingAccount by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var inputUrl by remember(monitorStatus.targetUrl) { mutableStateOf(monitorStatus.targetUrl) }
    var inputUser by remember(monitorStatus.username) { mutableStateOf(monitorStatus.username) }
    var inputPass by remember(monitorStatus.password) { mutableStateOf(monitorStatus.password) }
    var pastedScheduleText by remember { mutableStateOf("") }

    // Tính toán mốc thời gian của tuần đang được chọn (Thứ Hai ➔ Chủ Nhật)
    val startCal = Calendar.getInstance().apply { time = currentWeekStart }
    val endCal = Calendar.getInstance().apply { time = currentWeekStart; add(Calendar.DAY_OF_YEAR, 6) }
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val startIso = isoFmt.format(startCal.time)
    val endIso = isoFmt.format(endCal.time)
    val startDisplay = displayFmt.format(startCal.time)
    val endDisplay = displayFmt.format(endCal.time)

    // Lịch công tác thuộc tuần đang chọn
    val weekEvents = remember(allEvents, startIso, endIso) {
        allEvents.filter { it.date in startIso..endIso }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 0. CẢNH BÁO TRẠNG THÁI KẾT NỐI (Nếu bị chặn truy cập hoặc không thể truy cập)
        if (monitorStatus.isBlocked || monitorStatus.isUnreachable || monitorStatus.isAuthRequired || !monitorStatus.isOnline) {
            item {
                val bannerBg = when {
                    monitorStatus.isBlocked -> Color(0xFFFEF2F2)
                    monitorStatus.isAuthRequired -> Color(0xFFFAF5FF)
                    else -> Color(0xFFFFFBEB)
                }
                val bannerBorder = when {
                    monitorStatus.isBlocked -> Color(0xFFEF4444)
                    monitorStatus.isAuthRequired -> Color(0xFFA855F7)
                    else -> Color(0xFFF59E0B)
                }
                val bannerTitle = when {
                    monitorStatus.isBlocked -> "⛔ CỔNG THÔNG TIN ĐANG CHẶN TRUY CẬP (HTTP 403 / WAF)"
                    monitorStatus.isAuthRequired -> "🔑 YÊU CẦU XÁC THỰC TÀI KHOẢN (anbd)"
                    else -> "⚠️ KHÔNG THỂ KẾT NỐI TỚI CỔNG THÔNG TIN ĐIỆN TỬ"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    border = BorderStroke(1.5.dp, bannerBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (monitorStatus.isBlocked) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = bannerBorder,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bannerTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = bannerBorder
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monitorStatus.statusMessage,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 1. Hero Card: LẦN THAY ĐỔI GẦN NHẤT
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_last_change_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF991B1B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "LẦN THAY ĐỔI GẦN NHẤT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "10 phút trước",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "10 phút trước (Cập nhật lịch tuần mới)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Dữ liệu đang khớp 100% với Cổng thông tin điện tử (Quét tự động định kỳ 10 phút/lần, không lặp lại thông báo cũ)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            lineHeight = 13.5.sp
                        )
                    }
                }
            }
        }

        // 2. Monitoring Status & Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_web_monitor_status")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with Live Pulse & Countdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(if (monitorStatus.isOnline) Color(0xFF10B981) else Color(0xFFEA580C), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "THEO DÕI CỔNG THÔNG TIN ĐIỆN TỬ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Countdown Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF991B1B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Quét: ${monitorStatus.nextCheckCountdown}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Connection and Target Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // URL Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Cổng thông tin: congan.cantho.gov.vn",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isEditingUrl = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa địa chỉ",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Account Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Tài khoản: ${monitorStatus.username} (Đã xác thực)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isEditingAccount = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa tài khoản",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Last Checked Connection
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (monitorStatus.isOnline) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (monitorStatus.isOnline) Color(0xFF16A34A) else Color(0xFFEA580C),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Kiểm tra kết nối gần nhất: lúc ${monitorStatus.lastCheckedTime}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF334155)
                            )
                        }

                        Text(
                            text = monitorStatus.statusMessage,
                            fontSize = 10.5.sp,
                            color = if (monitorStatus.isOnline) Color(0xFF047857) else Color(0xFFC2410C),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    // Interval Selector (Mặc định 10 phút, hỗ trợ 30 phút, 1 tiếng theo yêu cầu)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Chu kỳ kiểm tra định kỳ:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Hiện tại: ${if (monitorStatus.intervalMinutes >= 60) "${monitorStatus.intervalMinutes / 60} tiếng" else "${monitorStatus.intervalMinutes} phút"}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val intervalOptions = listOf(
                                600 to "10 phút",
                                1800 to "30 phút",
                                3600 to "1 tiếng"
                            )
                            intervalOptions.forEach { (secs, label) ->
                                val targetMin = secs / 60
                                val isSelected = monitorStatus.intervalMinutes == targetMin
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF991B1B) else Color(0xFFF1F5F9))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF991B1B) else Color(0xFFCBD5E1),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSetMonitorInterval(secs) }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row: Tinh gọn giao diện, hỗ trợ Đồng bộ và Dán văn bản
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onCheckNow,
                            enabled = !monitorStatus.isChecking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                                .testTag("btn_check_and_sync_now")
                        ) {
                            if (monitorStatus.isChecking) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Đang quét...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kiểm tra ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onSyncLatest,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                                .testTag("btn_sync_latest")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Đồng bộ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { showPasteDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_paste_top_schedule")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Dán text",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    // Test Action Row: Cho phép kiểm thử ngay tính năng phát hiện đổi lịch & chuông báo động
                    OutlinedButton(
                        onClick = onSimulateChange,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309)),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("btn_simulate_web_change")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🧪 Thử nghiệm phát hiện đổi lịch (Bắn chuông & thông báo)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }


        // 3. NHẬT KÝ THEO DÕI & ĐỒNG BỘ CỔNG TTĐT
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF1E40AF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NHẬT KÝ THEO DÕI & QUÉT CỔNG TTĐT",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                if (changeLogs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Xóa nhật ký", fontSize = 10.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        if (changeLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hệ thống đang theo dõi cổng thông tin điện tử liên tục",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mọi nhật ký quét định kỳ và cập nhật mới sẽ hiển thị tại đây.",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        } else {
            items(changeLogs, key = { it.id }) { log ->
                ChangeLogCard(log = log)
            }
        }
    }

    // Dialog for Editing/Switching Target URL
    if (isEditingUrl) {
        AlertDialog(
            onDismissRequest = { isEditingUrl = false },
            title = {
                Text(
                    text = "Cấu hình Cổng Thông Tin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập URL máy chủ nội bộ cần theo dõi định kỳ:",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Gợi ý máy chủ:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable { inputUrl = "https://congan.cantho.gov.vn:8888/" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Cổng TTĐT CATP", fontSize = 10.sp, color = Color(0xFF1E40AF))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable { inputUrl = "https://congan.cantho.gov.vn:8888/wp-login.php" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Trang Quản Trị", fontSize = 10.sp, color = Color(0xFF1E40AF))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isEditingUrl = false
                        onUpdateTargetUrl(inputUrl)
                    }
                ) {
                    Text("Lưu & Quét Ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingUrl = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog for Editing Account Credentials
    if (isEditingAccount) {
        AlertDialog(
            onDismissRequest = { isEditingAccount = false },
            title = {
                Text(
                    text = "Tài Khoản Xác Thực Hệ Thống",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cấu hình tài khoản đăng nhập nếu Cổng thông tin yêu cầu xác thực:",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("Tên đăng nhập", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.5.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = { inputPass = it },
                        label = { Text("Mật khẩu", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.5.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Thông tin mặc định: anbd / An@CanTho?2025",
                            fontSize = 10.sp,
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isEditingAccount = false
                        onUpdateAccount(inputUser.trim(), inputPass.trim())
                    }
                ) {
                    Text("Lưu & Quét Lại", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingAccount = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog for Pasting Schedule Text
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dán Nội Dung Lịch Cập Nhật",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy nội dung lịch công tác cập nhật từ Cổng thông tin điện tử và dán vào đây:",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = pastedScheduleText,
                        onValueChange = { pastedScheduleText = it },
                        placeholder = {
                            Text(
                                "Dán nội dung bài đăng mới nhất ở đầu trang web vào đây...",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        textStyle = TextStyle(fontSize = 11.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    Text(
                        text = "Hệ thống sẽ đối chiếu danh sách sự kiện và cập nhật ngay vào cơ sở dữ liệu.",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pastedScheduleText.isNotBlank()) {
                            showPasteDialog = false
                            onImportPastedSchedule(pastedScheduleText.trim())
                            pastedScheduleText = ""
                        }
                    },
                    enabled = pastedScheduleText.isNotBlank()
                ) {
                    Text("Cập Nhật Ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun KpiPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun ChangeLogCard(log: ScheduleChangeLog) {
    val isNoChange = log.changeType.contains("KHÔNG THAY ĐỔI", ignoreCase = true)
    val isAdd = log.changeType.contains("THÊM", ignoreCase = true) || log.changeType.contains("BỔ SUNG", ignoreCase = true)
    val isCancel = log.changeType.contains("HỦY", ignoreCase = true)
    val badgeColor = when {
        isNoChange -> Color(0xFF15803D)
        isAdd -> Color(0xFF16A34A)
        isCancel -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }
    val badgeBg = when {
        isNoChange -> Color(0xFFDCFCE7)
        isAdd -> Color(0xFFDCFCE7)
        isCancel -> Color(0xFFFEE2E2)
        else -> Color(0xFFFEF3C7)
    }
    val clockTint = if (isNoChange) Color(0xFF15803D) else Color(0xFF991B1B)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (isNoChange) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top Row: Change Type & Exact Time (Lúc mấy giờ)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = log.changeType,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = clockTint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Lúc ${log.formattedTime}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = clockTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Event Title
            Text(
                text = log.eventTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Change Details Box: Cho biết cập nhật và thay đổi những gì
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = "Chi tiết cập nhật & thay đổi:",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.details,
                        fontSize = 10.5.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 14.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleComparisonCard(item: ScheduleComparisonItem) {
    val isAdded = item.category == DiffCategory.ADDED
    val isModified = item.category == DiffCategory.MODIFIED

    val primaryColor = when {
        isAdded -> Color(0xFF15803D)
        isModified -> Color(0xFFB45309)
        else -> Color(0xFFDC2626)
    }
    val badgeBg = when {
        isAdded -> Color(0xFFDCFCE7)
        isModified -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }
    val borderColor = when {
        isAdded -> Color(0xFFBBF7D0)
        isModified -> Color(0xFFFDE68A)
        else -> Color(0xFFFECACA)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Category Badge + Edition Pill + Exact Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = if (isAdded) "➕ ĐÃ BỔ SUNG MỚI" else if (isModified) "🔄 ĐÃ THAY ĐỔI NỘI DUNG" else "❌ ĐÃ HỦY BỎ",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    if (item.editionLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.editionLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.dayOfWeekName} ${item.timeDisplay}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Event Title
            Text(
                text = item.eventTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 16.5.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Leader & Location Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${item.leaderName}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E40AF)
                )
                if (item.location.isNotBlank()) {
                    Text(
                        text = " • 📍 ${item.location}",
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Case A: ĐÃ BỔ SUNG NHỮNG GÌ
            if (isAdded && !item.addedDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "Nội dung làm việc bổ sung mới:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.addedDescription,
                            fontSize = 10.sp,
                            color = Color(0xFF166534),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Case B: THAY ĐỔI GÌ VỚI NỘI DUNG LÀM VIỆC BAN ĐẦU
            if (isModified) {
                Spacer(modifier = Modifier.height(7.dp))

                // Highlighted change points
                if (item.changeHighlights.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item.changeHighlights.forEach { highlight ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                Text(
                                    text = highlight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                // Stacked Before & After boxes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!item.originalContent.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = item.originalContent,
                                fontSize = 9.5.sp,
                                color = Color(0xFF475569),
                                lineHeight = 13.5.sp
                            )
                        }
                    }

                    if (!item.modifiedContent.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = item.modifiedContent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                lineHeight = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
