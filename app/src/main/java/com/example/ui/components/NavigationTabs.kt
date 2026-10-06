package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
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
import com.example.data.model.ViewMode

@Composable
fun NavigationTabs(
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    changeLogsCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Lịch Ban Giám Đốc (Theo Lãnh Đạo - Mặc định)
            TabItem(
                mode = ViewMode.BY_LEADER,
                icon = Icons.Default.Leaderboard,
                label = "Lịch Ban Giám Đốc",
                isSelected = currentView == ViewMode.BY_LEADER,
                onClick = { onViewChange(ViewMode.BY_LEADER) },
                modifier = Modifier.weight(1f)
            )

            // 2. Cổng TTĐT (Theo dõi Cổng TTĐT & Đối chiếu điều chỉnh)
            TabItem(
                mode = ViewMode.WEB_MONITOR,
                icon = Icons.Default.Language,
                label = "Cổng TTĐT",
                badge = if (changeLogsCount > 0) "$changeLogsCount" else null,
                isSelected = currentView == ViewMode.WEB_MONITOR || currentView == ViewMode.DIFF_COMPARISON,
                onClick = { onViewChange(ViewMode.WEB_MONITOR) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabItem(
    mode: ViewMode,
    icon: ImageVector,
    label: String? = null,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF991B1B) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 4.dp)
            .testTag("tab_${mode.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.title,
                tint = if (isSelected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label ?: mode.title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155),
                maxLines = 1
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isSelected) Color.White else Color(0xFF991B1B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF991B1B) else Color.White
                    )
                }
            }
        }
    }
}
