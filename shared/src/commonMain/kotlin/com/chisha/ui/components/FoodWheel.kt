package com.chisha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.data.Dish
import com.chisha.ui.theme.ChishaPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * 转盘：纯 Canvas 绘制，rotation 由调用方驱动。
 * 0 度指向正上方，扇区顺时针排布。
 */
@Composable
fun FoodWheel(
    items: List<Dish>,
    rotation: Float,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val p = com.chisha.ui.theme.LocalPalette.current
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f - 4.dp.toPx()).coerceAtLeast(1f)

        // 外圈光晕
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    p.accent.copy(alpha = if (dimmed) 0.10f else 0.20f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 1.12f,
            ),
            radius = radius * 1.12f,
            center = center,
        )

        val n = items.size
        if (n > 0) {
            val sweep = 360f / n
            items.forEachIndexed { i, dish ->
                val start = i * sweep + rotation - 90f
                val arcBrush = if (p.isDark) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (i % 2 == 0) 0.13f else 0.055f),
                            p.accent.copy(alpha = 0.02f),
                        ),
                        center = center,
                        radius = radius,
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            p.accent.copy(alpha = if (i % 2 == 0) 0.20f else 0.09f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    )
                }
                drawArc(
                    brush = arcBrush,
                    startAngle = start,
                    sweepAngle = sweep - 2.0f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                )
            }

            // 分格辐线：让轮盘的扇区边界清晰
            val edgeColor = p.accent.copy(
                alpha = if (dimmed) 0.06f else if (p.isDark) 0.16f else 0.24f,
            )
            items.forEachIndexed { i, _ ->
                val deg = i * sweep + rotation - 90f
                val rad = Math.toRadians(deg.toDouble())
                drawLine(
                    color = edgeColor,
                    start = center,
                    end = Offset(
                        center.x + radius * cos(rad).toFloat(),
                        center.y + radius * sin(rad).toFloat(),
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }

            // 扇区文字：按扇区实际宽窄自动缩字号 / 换行 / 省略，尽量完整显示菜名
            val halfSweep = Math.toRadians((sweep / 2f).toDouble())
            val ringR = radius * 0.62f
            val availW = (2f * ringR * sin(halfSweep).toFloat() - 8.dp.toPx())
                .coerceAtLeast(28.dp.toPx())
            val availH = radius * 0.36f
            val baseFs = (size.minDimension * 0.045f).coerceIn(9f, 16f)
            // 浅色皮肤上白字会糊成一片，跟着主题走：深色用暖白，浅色用正文色
            val textColor = if (p.isDark) {
                Color(0xFFFFF6E6).copy(alpha = if (dimmed) 0.55f else 0.95f)
            } else {
                p.text.copy(alpha = if (dimmed) 0.62f else 1f)
            }

            items.forEachIndexed { i, dish ->
                val midDeg = i * sweep + sweep / 2f + rotation - 90f
                val mid = Math.toRadians(midDeg.toDouble())

                fun layoutOf(text: String, fs: Float, lines: Int): TextLayoutResult =
                    measurer.measure(
                        text = text,
                        style = TextStyle(
                            color = textColor,
                            fontSize = fs.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        constraints = Constraints(maxWidth = availW.toInt()),
                        maxLines = lines,
                        overflow = TextOverflow.Ellipsis,
                    )

                // 1) 优先单行：字号往下压，能一行放下就一行
                var chosen: TextLayoutResult? = null
                run {
                    var fs = baseFs
                    while (fs >= 10f) {
                        val lay = layoutOf(dish.name, fs, 1)
                        if (!lay.hasVisualOverflow) {
                            chosen = lay
                            return@run
                        }
                        fs -= 0.5f
                    }
                }

                // 2) 单行放不下：按字数均分两行，避免出现「玛格丽特披 / 萨」这种断字
                if (chosen == null && dish.name.length >= 4) {
                    val brk = (dish.name.length + 1) / 2
                    val twoLine = dish.name.substring(0, brk) + "\n" + dish.name.substring(brk)
                    var fs = baseFs
                    while (fs >= 9f) {
                        val lay = layoutOf(twoLine, fs, 2)
                        if (!lay.hasVisualOverflow && lay.size.height <= availH) {
                            chosen = lay
                            break
                        }
                        fs -= 0.5f
                    }
                }

                val layout = chosen ?: layoutOf(dish.name, 9f, 2)

                val cx = center.x + radius * 0.66f * cos(mid).toFloat()
                val cy = center.y + radius * 0.66f * sin(mid).toFloat()
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
                )
            }
        }

        // 内圈
        drawCircle(color = p.bgTop.copy(alpha = 0.92f), radius = radius * 0.30f, center = center)
        drawCircle(
            color = p.accent.copy(alpha = 0.35f),
            radius = radius * 0.30f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx()),
        )

        // 外描边
        drawCircle(
            color = p.cardBorder,
            radius = radius,
            center = center,
            style = Stroke(width = 1.4.dp.toPx()),
        )
    }
}

