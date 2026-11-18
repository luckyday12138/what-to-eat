package com.chisha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.chisha.data.Dish
import com.chisha.data.ImageKey
import com.chisha.resources.Res
import com.chisha.resources.cat_bao
import com.chisha.resources.cat_bbq
import com.chisha.resources.cat_burger
import com.chisha.resources.cat_dessert
import com.chisha.resources.cat_drink
import com.chisha.resources.cat_dumpling
import com.chisha.resources.cat_hotpot
import com.chisha.resources.cat_korean
import com.chisha.resources.cat_noodle
import com.chisha.resources.cat_pizza
import com.chisha.resources.cat_porridge
import com.chisha.resources.cat_rice
import com.chisha.resources.cat_salad
import com.chisha.resources.cat_snack
import com.chisha.resources.cat_soup
import com.chisha.resources.cat_stirfry
import com.chisha.resources.cat_sushi
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.absoluteValue

/** 内置图包：17 个类别的统一风格美食摄影 */
fun builtinRes(key: String?): DrawableResource? = when (key) {
    ImageKey.NOODLE -> Res.drawable.cat_noodle
    ImageKey.RICE -> Res.drawable.cat_rice
    ImageKey.HOTPOT -> Res.drawable.cat_hotpot
    ImageKey.BBQ -> Res.drawable.cat_bbq
    ImageKey.SOUP -> Res.drawable.cat_soup
    ImageKey.DUMPLING -> Res.drawable.cat_dumpling
    ImageKey.PORRIDGE -> Res.drawable.cat_porridge
    ImageKey.BAO -> Res.drawable.cat_bao
    ImageKey.SALAD -> Res.drawable.cat_salad
    ImageKey.BURGER -> Res.drawable.cat_burger
    ImageKey.PIZZA -> Res.drawable.cat_pizza
    ImageKey.SUSHI -> Res.drawable.cat_sushi
    ImageKey.KOREAN -> Res.drawable.cat_korean
    ImageKey.SNACK -> Res.drawable.cat_snack
    ImageKey.DESSERT -> Res.drawable.cat_dessert
    ImageKey.DRINK -> Res.drawable.cat_drink
    ImageKey.STIRFRY -> Res.drawable.cat_stirfry
    else -> null
}

/** 每个图片类别一套配色，用于无图兜底 */
fun artPalette(key: String?): List<Color> = when (key) {
    ImageKey.NOODLE -> listOf(Color(0xFF6E4B23), Color(0xFFC98A3E))
    ImageKey.RICE -> listOf(Color(0xFF5A4321), Color(0xFFD9A648))
    ImageKey.HOTPOT -> listOf(Color(0xFF7A1F14), Color(0xFFD9512F))
    ImageKey.BBQ -> listOf(Color(0xFF4A1C12), Color(0xFFB4482A))
    ImageKey.SOUP -> listOf(Color(0xFF384A2B), Color(0xFF8FA45C))
    ImageKey.DUMPLING -> listOf(Color(0xFF4E3A2A), Color(0xFFD7B98A))
    ImageKey.PORRIDGE -> listOf(Color(0xFF3E3A2C), Color(0xFFBFAE83))
    ImageKey.BAO -> listOf(Color(0xFF4A3423), Color(0xFFE0BE8E))
    ImageKey.SALAD -> listOf(Color(0xFF22402C), Color(0xFF7CB86A))
    ImageKey.BURGER -> listOf(Color(0xFF4A2A16), Color(0xFFE09B4F))
    ImageKey.PIZZA -> listOf(Color(0xFF58210F), Color(0xFFE8834B))
    ImageKey.SUSHI -> listOf(Color(0xFF23303E), Color(0xFF6E93B5))
    ImageKey.KOREAN -> listOf(Color(0xFF4A1A24), Color(0xFFC4556A))
    ImageKey.SNACK -> listOf(Color(0xFF3D2418), Color(0xFFB7763F))
    ImageKey.DESSERT -> listOf(Color(0xFF4A2440), Color(0xFFC87AA8))
    ImageKey.DRINK -> listOf(Color(0xFF1E3346), Color(0xFF5C9EA8))
    ImageKey.STIRFRY -> listOf(Color(0xFF3A2A18), Color(0xFFC08A4A))
    else -> listOf(Color(0xFF2A2438), Color(0xFF7C6AA8))
}

/**
 * 菜品视觉块：优先内置图包，缺图时用程序化渐变 + 光斑兜底。
 * showName 打开时在底部压一层渐变压暗并叠菜名，保证任何底色都可读。
 */
@Composable
fun DishArt(
    dish: Dish,
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    showName: Boolean = true,
    scrim: Boolean = true,
) {
    val res = builtinRes(dish.imageKey)
    val remote = dish.imageUri?.takeIf { it.startsWith("http", ignoreCase = true) }
    val measurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(corner)

    Box(modifier = modifier.clip(shape)) {
        if (remote != null) {
            val holder = res?.let { painterResource(it) }
            AsyncImage(
                model = remote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // 竖版实拍图居中裁会把成菜裁掉（只剩背景和道具），往下压一点
                alignment = BiasAlignment(0.5f, 0.58f),
                placeholder = holder,
                error = holder,
            )
        } else if (res != null) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.5f, 0.58f),
            )
        } else {
            val (c1, c2) = artPalette(dish.imageKey).let { it[0] to it[1] }
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(listOf(c1, c2), Offset(0f, 0f), Offset(size.width, size.height)))
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.78f, size.height * 0.18f),
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width * 0.15f, size.height * 0.9f),
                )
            }
        }

        // 底部渐变压暗 + 菜名
        if (scrim) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.86f)),
                        startY = size.height * 0.20f,
                    )
                )
                if (showName) drawDishLabel(dish.name, measurer, this)
            }
        }
    }
}

private fun drawDishLabel(name: String, measurer: TextMeasurer, scope: DrawScope) {
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.97f),
        fontSize = (scope.size.minDimension * 0.085f).sp,
        fontWeight = FontWeight.Black,
    )
    val layout = measurer.measure(name, style)
    val x = scope.size.width * 0.07f
    val y = scope.size.height - layout.size.height - scope.size.height * 0.08f
    scope.drawText(layout, topLeft = Offset(x, y))
}

/** 列表用的小缩略图 */
@Composable
fun DishGlyph(dish: Dish, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    val res = builtinRes(dish.imageKey)
    val remote = dish.imageUri?.takeIf { it.startsWith("http", ignoreCase = true) }
    val shape = RoundedCornerShape(corner)
    Box(modifier = modifier.clip(shape)) {
        if (remote != null) {
            AsyncImage(
                model = remote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.5f, 0.58f),
            )
        } else if (res != null) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            val (c1, c2) = artPalette(dish.imageKey).let { it[0] to it[1] }
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2))))
        }
    }
}

/** 按文字生成稳定的小数，用于布局上的随机感 */
fun stableSeed(text: String): Float =
    (text.hashCode().absoluteValue % 1000) / 1000f
