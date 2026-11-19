package com.chisha.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** 相册原图动辄几 MB，缩到长边 1440 再存 —— 列表里几百张缩略图不至于把内存吃满 */
private const val MAX_SIDE = 1440

@Composable
actual fun rememberImagePicker(onPicked: (String?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val path = withContext(Dispatchers.IO) { copyInto(ctx, uri) }
            onPicked(path)
        }
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

/**
 * 系统给的 Uri 只是临时授权，重启 App 就读不到了，所以必须复制一份进私有目录。
 * 顺便压一下：长边超过 1440 的等比缩掉，JPEG 88 质量。
 */
private fun copyInto(ctx: Context, uri: Uri): String? = runCatching {
    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
    val target = File(dir, "self_" + System.currentTimeMillis() + ".jpg")

    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    val sample = if (longSide > MAX_SIDE) longSide / MAX_SIDE else 1
    val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null

    val out = if (maxOf(bitmap.width, bitmap.height) > MAX_SIDE) {
        val ratio = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        bitmap
    }
    FileOutputStream(target).use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    if (out !== bitmap) out.recycle()
    bitmap.recycle()

    "file://" + target.absolutePath
}.getOrNull()
