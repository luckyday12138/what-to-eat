package com.chisha.core

import androidx.compose.runtime.Composable

/** 桌面端没有系统返回键，什么都不做 */
@Composable
actual fun PlatformBack(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
