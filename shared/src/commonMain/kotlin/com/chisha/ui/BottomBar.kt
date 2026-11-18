package com.chisha.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.ui.theme.LocalPalette

private val tabs = listOf("今天", "发现", "菜库", "我的")

@Composable
fun BottomBar(current: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(p.cardStrong)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val sel = i == current
            val bg by animateColorAsState(if (sel) Color.Transparent else Color.Transparent)
            val labelColor by animateColorAsState(if (sel) Color(0xFF231708) else p.sub)
            val dot by animateDpAsState(if (sel) 7.dp else 4.dp, spring(dampingRatio = 0.55f, stiffness = 500f))

            Box(
                modifier = Modifier
                    .testTag("tab_$i")
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (sel) Modifier.background(Brush.horizontalGradient(listOf(p.accent, p.accent2)))
                        else Modifier.background(bg)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(i)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(dot)
                            .clip(CircleShape)
                            .background(if (sel) Color(0xFF231708).copy(alpha = 0.55f) else p.faint)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        label,
                        color = labelColor,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
