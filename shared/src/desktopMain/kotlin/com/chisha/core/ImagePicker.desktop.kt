package com.chisha.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.SwingUtilities

private val IMAGE_EXT = Regex("(?i).*\\.(jpg|jpeg|png|webp|bmp|heic)$")

@Composable
actual fun rememberImagePicker(onPicked: (String?) -> Unit): () -> Unit = remember {
    {
        // 原生对话框会阻塞，丢到单独线程里，选完再用 EDT 回调 Compose 的 state
        Thread {
            val picked = runCatching { pickOnce() }.getOrNull()
            SwingUtilities.invokeLater { onPicked(picked) }
        }.apply { isDaemon = true }.start()
    }
}

private fun pickOnce(): String? {
    val dialog = FileDialog(null as Frame?, "选一张菜品图", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> IMAGE_EXT.matches(name) }
    dialog.isVisible = true
    val fileName = dialog.file ?: return null
    val dir = dialog.directory ?: return null

    val src = File(dir, fileName)
    if (!src.exists()) return null

    val outDir = File(System.getProperty("user.home"), ".chisha/photos").apply { mkdirs() }
    val ext = src.extension.ifBlank { "jpg" }
    val target = File(outDir, "self_" + System.currentTimeMillis() + "." + ext)
    src.copyTo(target, overwrite = true)
    return "file://" + target.absolutePath.replace('\\', '/')
}
