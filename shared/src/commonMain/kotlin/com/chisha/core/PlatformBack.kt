package com.chisha.core

import androidx.compose.runtime.Composable

/**
 * 系统返回键（安卓手势/三键）拦截。
 *
 * 安卓上一打开全屏食谱，按返回键会直接把 App 退到桌面，而不是先关掉食谱层；
 * 桌面端没有返回键，空实现即可。Compose Multiplatform 自带的 BackHandler
 * 在这个版本里还没进 common，所以自己 expect/actual。
 */
@Composable
expect fun PlatformBack(enabled: Boolean, onBack: () -> Unit)
