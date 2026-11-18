package com.chisha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chisha.data.Slot

enum class ThemeStyle(val key: String, val label: String) {
    DARK_GOLD("DARK_GOLD", "暗夜金"),
    MIDNIGHT("MIDNIGHT", "午夜蓝"),
    CREAM("CREAM", "奶油白");

    companion object {
        fun of(key: String): ThemeStyle = entries.firstOrNull { it.key == key } ?: DARK_GOLD
    }
}

@Immutable
data class ChishaPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val glow: Color,
    val glow2: Color,
    val accent: Color,
    val accentDeep: Color,
    val accent2: Color,
    val card: Color,
    val cardStrong: Color,
    val cardBorder: Color,
    val text: Color,
    val sub: Color,
    val faint: Color,
    val isDark: Boolean,
    val danger: Color,
)

/** 主题 × 时段 = 背景情绪 */
fun paletteOf(style: ThemeStyle, slot: Slot): ChishaPalette {
    val (glow, glow2) = when (slot) {
        Slot.BREAKFAST -> Color(0xFFFF9A3D) to Color(0xFFE8562A)
        Slot.LUNCH -> Color(0xFFFFC65C) to Color(0xFFF08A2C)
        Slot.DINNER -> Color(0xFFC4587A) to Color(0xFF7A3BC9)
        Slot.MIDNIGHT -> Color(0xFF4C5FD5) to Color(0xFF1F2A6B)
    }
    return when (style) {
        ThemeStyle.DARK_GOLD -> ChishaPalette(
            bgTop = Color(0xFF0C0A08),
            bgBottom = Color(0xFF171008),
            glow = glow,
            glow2 = glow2,
            accent = Color(0xFFE9B44C),
            accentDeep = Color(0xFF9E7420),
            accent2 = Color(0xFFF2733F),
            card = Color(0x14FFFFFF),
            cardStrong = Color(0x1FFFFFFF),
            cardBorder = Color(0x24FFFFFF),
            text = Color(0xFFFFF6E6),
            sub = Color(0xFFB6A78E),
            faint = Color(0xFF8E8069),
            isDark = true,
            danger = Color(0xFFE2544B),
        )
        ThemeStyle.MIDNIGHT -> ChishaPalette(
            bgTop = Color(0xFF070A15),
            bgBottom = Color(0xFF101427),
            glow = glow,
            glow2 = glow2,
            accent = Color(0xFF8A9BFF),
            accentDeep = Color(0xFF4453C4),
            accent2 = Color(0xFF5AE4D4),
            card = Color(0x12FFFFFF),
            cardStrong = Color(0x1CFFFFFF),
            cardBorder = Color(0x22FFFFFF),
            text = Color(0xFFEDF0FF),
            sub = Color(0xFF9AA3C7),
            faint = Color(0xFF7E88AE),
            isDark = true,
            danger = Color(0xFFFF6B6B),
        )
        ThemeStyle.CREAM -> ChishaPalette(
            bgTop = Color(0xFFFBF6EE),
            bgBottom = Color(0xFFF0E4D3),
            glow = glow.copy(alpha = 0.55f),
            glow2 = glow2.copy(alpha = 0.55f),
            accent = Color(0xFFC2673A),
            accentDeep = Color(0xFF8A4225),
            accent2 = Color(0xFF5F7A4C),
            card = Color(0xFFFFFFFF).copy(alpha = 0.72f),
            cardStrong = Color(0xFFFFFFFF).copy(alpha = 0.9f),
            cardBorder = Color(0x1A000000),
            text = Color(0xFF2A2118),
            // 米色底上原来的 #9C8C7C 对比只有 2:1 出头，小字基本糊掉
            sub = Color(0xFF5A4B3C),
            faint = Color(0xFF7A6A58),
            isDark = false,
            danger = Color(0xFFB23A2E),
        )
    }
}

val LocalPalette = staticCompositionLocalOf { paletteOf(ThemeStyle.DARK_GOLD, Slot.DINNER) }

private val ChishaTypography = Typography().let { base ->
    base.copy(
        displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 46.sp, letterSpacing = (-1.2).sp),
        headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.6).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.2).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
    )
}

@Composable
fun ChishaTheme(style: ThemeStyle, slot: Slot, content: @Composable () -> Unit) {
    val p = paletteOf(style, slot)
    val dark = p.isDark || isSystemInDarkTheme() && style != ThemeStyle.CREAM
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent,
            onPrimary = Color(0xFF20160A),
            primaryContainer = p.accentDeep,
            onPrimaryContainer = p.text,
            secondary = p.accent2,
            onSecondary = Color(0xFF101010),
            background = p.bgTop,
            onBackground = p.text,
            surface = p.bgBottom,
            onSurface = p.text,
            surfaceVariant = p.cardStrong,
            onSurfaceVariant = p.sub,
            outline = p.cardBorder,
            error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent,
            onPrimary = Color.White,
            secondary = p.accent2,
            background = p.bgTop,
            onBackground = p.text,
            surface = p.bgBottom,
            onSurface = p.text,
            surfaceVariant = p.cardStrong,
            onSurfaceVariant = p.sub,
            outline = p.cardBorder,
            error = p.danger,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = ChishaTypography, content = content)
    }
}

/** 全屏背景：纵向渐变 + 两团时段光晕 */
@Composable
fun ChishaBackground(style: ThemeStyle, slot: Slot, content: @Composable BoxScope.() -> Unit) {
    val p = paletteOf(style, slot)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(p.bgTop, p.bgBottom, p.bgTop),
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(p.glow.copy(alpha = if (p.isDark) 0.30f else 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.02f),
                        radius = size.width * 1.05f,
                    ),
                    radius = size.width * 1.05f,
                    center = Offset(size.width * 0.86f, size.height * 0.02f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(p.glow2.copy(alpha = if (p.isDark) 0.22f else 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.06f, size.height * 0.72f),
                        radius = size.width * 0.95f,
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width * 0.06f, size.height * 0.72f),
                )
            },
        content = content,
    )
}
