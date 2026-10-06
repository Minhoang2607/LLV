package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Leader

@Composable
fun LeaderBadge(
    leader: Leader?,
    modifier: Modifier = Modifier,
    showStar: Boolean = true
) {
    if (leader == null) {
        Text(
            text = "Lãnh đạo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val badgeColor = try {
        Color(android.graphics.Color.parseColor(leader.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isDo = leader.id == "tran-hoang-do"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDo) Color(0xFFECFEFF) else Color(0xFFF1F5F9))
            .border(
                width = 1.dp,
                color = if (isDo) Color(0xFF06B6D4) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = leader.shortName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDo) Color(0xFF0E7490) else Color(0xFF1E293B)
        )
        if (showStar && leader.isPriority) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "★",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFD97706)
            )
        }
    }
}
