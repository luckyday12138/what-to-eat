package com.chisha.core

import androidx.compose.runtime.Composable

/**
 * 打开系统图库（Android）或文件选择器（桌面）挑一张图，回来的是能直接喂给 Coil 的本地地址
 * （`file://…` 形式，已复制进应用私有目录，重启后还在）。取消或失败时回 null。
 */
@Composable
expect fun rememberImagePicker(onPicked: (String?) -> Unit): () -> Unit
