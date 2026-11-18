package com.chisha.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.data.Dish
import com.chisha.ui.components.Chip
import com.chisha.ui.components.DishArt
import com.chisha.ui.theme.LocalPalette

/**
 * 菜谱详情：大图 + 食材清单（带用量）+ 分步做法（带火候时间）+ 小贴士。
 */
@Composable
fun RecipeOverlay(dish: Dish?, onClose: () -> Unit) {
    val p = LocalPalette.current

    AnimatedVisibility(
        visible = dish != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(280)) { it / 6 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(220)) { it / 8 },
    ) {
        val d = dish ?: return@AnimatedVisibility
        val recipe = d.recipe

        Box(Modifier.fillMaxSize()) {
            // 遮罩：点空白收起
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable { onClose() }
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 44.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(p.bgBottom)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                // 顶部操作条
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(p.card)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("×", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("菜谱", color = p.faint, fontSize = 12.sp, letterSpacing = 2.sp)
                }

                Spacer(Modifier.height(14.dp))

                DishArt(
                    d,
                    Modifier.fillMaxWidth().height(196.dp),
                    corner = 24.dp,
                    showName = false,
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        d.name,
                        color = p.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                    )
                    // 「平价 · 自制约 15 元」：只留能看懂的档位白话，不再堆 ¥ 符号
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.priceLabel, color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("·", color = p.faint, fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(d.priceHint, color = p.sub, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(d.cuisine)
                    if (d.spicy > 0) Chip("辣度 ${d.spicy}")
                    d.tags.distinct().take(2).forEach { Chip(it) }
                }

                if (d.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(d.note, color = p.faint, fontSize = 12.sp)
                }

                // ---------- 食材 ----------
                if (recipe.ingredients.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    SectionTitle("食材", recipe.ingredients.size, p.accent, p.text, p.faint)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(p.card)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        recipe.ingredients.forEach { line ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(p.accent.copy(alpha = 0.7f))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(line, color = p.sub, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }

                // ---------- 做法 ----------
                Spacer(Modifier.height(22.dp))
                SectionTitle("做法", recipe.steps.size, p.accent, p.text, p.faint)
                Spacer(Modifier.height(10.dp))

                recipe.steps.forEachIndexed { i, step ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(p.card)
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(p.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${i + 1}",
                                color = p.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(
                            step,
                            color = p.sub,
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }

                // ---------- 小贴士 ----------
                if (recipe.tip.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(p.accent.copy(alpha = 0.10f))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("小贴士", color = p.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text(recipe.tip, color = p.sub, fontSize = 12.5.sp, lineHeight = 19.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(99.dp))
                        .background(p.cardStrong)
                        .clickable { onClose() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("收起", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    count: Int,
    accent: Color,
    textColor: Color,
    faintColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text("$count 项", color = faintColor, fontSize = 11.sp)
    }
}
