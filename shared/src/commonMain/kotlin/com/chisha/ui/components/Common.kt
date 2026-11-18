package com.chisha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.ui.theme.LocalPalette

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(1f, spring())

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(99.dp))
            .then(
                if (primary) Modifier.background(Brush.horizontalGradient(listOf(p.accent, p.accent2)))
                else Modifier.background(p.cardStrong).border(1.dp, p.cardBorder, RoundedCornerShape(99.dp))
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            leading?.invoke()
            Text(
                text,
                color = if (primary) Color(0xFF221709) else p.text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (selected) p.accent.copy(alpha = 0.22f) else p.card)
    val fg by animateColorAsState(if (selected) p.accent else p.sub)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .then(if (selected) Modifier.border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(99.dp)) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 随机 / 推荐 双选胶囊 */
@Composable
fun ModeSwitch(
    smart: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(p.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false, true).forEach { isSmart ->
            val selected = isSmart == smart
            val bg by animateColorAsState(
                if (selected) p.accent.copy(alpha = 0.9f) else Color.Transparent,
            )
            val fg by animateColorAsState(if (selected) Color(0xFF211608) else p.sub)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(bg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(isSmart)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    // 说清来源差异：随机只抽本机菜库，推荐才走网络
                    if (isSmart) "网络推荐" else "本地随机",
                    color = fg,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.text)
        if (subtitle != null) {
            Text(subtitle, color = p.faint, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun GhostIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(p.card)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}
