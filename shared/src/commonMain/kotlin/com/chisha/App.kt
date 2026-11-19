package com.chisha

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.chisha.core.PlatformBack
import com.chisha.core.PlatformStore
import com.chisha.core.slotOfHour
import com.chisha.data.ChishaRepo
import com.chisha.data.DishPhoto
import com.chisha.ui.BottomBar
import com.chisha.ui.DiscoverScreen
import com.chisha.ui.LibraryScreen
import com.chisha.ui.MineScreen
import com.chisha.ui.RecipeOverlay
import com.chisha.ui.TodayScreen
import com.chisha.ui.theme.ChishaBackground
import com.chisha.ui.theme.ChishaTheme
import com.chisha.ui.theme.LocalPalette
import com.chisha.ui.theme.ThemeStyle

@Composable
fun App(
    initialTab: Int = 0,
    initialSection: Int = 0,
    demoRecipe: Boolean = false,
    demoResult: Boolean = false,
    demoEditor: Boolean = false,
    demoEditorName: String = "",
    warmPhotos: Boolean = true,
) {
    SetupNetworkImages()
    val repo = remember { ChishaRepo.instance.also { it.load() } }
    var tab by remember { mutableIntStateOf(initialTab) }
    var recipe by remember {
        mutableStateOf(if (demoRecipe) repo.dishes.firstOrNull { it.enabled } else null)
    }

    val themeStyle = ThemeStyle.of(repo.prefs.theme)
    // 餐段放在这里持有：首页切换餐段时，背景光晕和配色要一起跟着变
    var slot by remember { mutableStateOf(slotOfHour(PlatformStore.hourOfDay())) }

    // 后台把本地菜库的照片逐张补上（拿到就替换品类图，拿不到保持品类图）
    LaunchedEffect(Unit) { if (warmPhotos) DishPhoto.backfill() }

    ChishaTheme(themeStyle, slot) {
        ChishaBackground(themeStyle, slot) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                val p = LocalPalette.current

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }) togetherWith
                            (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 12 })
                    },
                ) { index ->
                    when (index) {
                        0 -> TodayScreen(
                            onGoDiscover = { tab = 1 },
                            onGoLibrary = { tab = 2 },
                            onOpenRecipe = { recipe = it },
                            initialResult = if (demoResult) repo.dishes.firstOrNull { it.enabled } else null,
                            slot = slot,
                            onSlotChange = { slot = it },
                        )
                        1 -> DiscoverScreen(
                            initialSection = initialSection,
                            onOpenRecipe = { recipe = it },
                        )
                        2 -> LibraryScreen(
                            onOpenRecipe = { recipe = it },
                            startCreating = demoEditor,
                            startName = demoEditorName,
                        )
                        3 -> MineScreen()
                        else -> TodayScreen(
                            onOpenRecipe = { recipe = it },
                            onGoLibrary = { tab = 2 },
                            slot = slot,
                            onSlotChange = { slot = it },
                        )
                    }
                }

                // 底部导航：一整条渐变底 + 拦截点击，避免点穿到下层内容
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(124.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.28f to p.bgBottom.copy(alpha = 0.92f),
                                1f to p.bgBottom,
                            )
                        )
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BottomBar(
                        current = tab,
                        onSelect = { tab = it },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 22.dp),
                    )
                }

                // 安卓返回键 / 手势先关食谱层，别直接退出 App
                PlatformBack(enabled = recipe != null) { recipe = null }
                RecipeOverlay(dish = recipe, onClose = { recipe = null })
            }
        }
    }
}

@Composable
private fun SetupNetworkImages() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
    }
}

@Composable
internal fun PlaceholderScreen(title: String) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = p.text)
            Text("建设中", color = p.faint)
        }
    }
}
