package com.chisha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chisha.App
import com.chisha.core.AndroidPlatform
import com.chisha.core.PlatformStore
import com.chisha.core.chLog
import com.chisha.core.slotOfHour
import com.chisha.data.OnlineSource
import kotlinx.coroutines.runBlocking
import kotlin.time.TimeSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatform.init(this)
        enableEdgeToEdge()

        // 真机排障入口：am start ... --es chisha_probe batch
        // 把取菜链路跑三遍，耗时打到 logcat（tag CHISHA），不打开界面
        if (intent?.getStringExtra("chisha_probe") == "batch") {
            Thread {
                runBlocking {
                    val slot = slotOfHour(PlatformStore.hourOfDay())
                    repeat(3) { round ->
                        val t0 = TimeSource.Monotonic.markNow()
                        val meals = OnlineSource.batch(8, slot)
                        chLog(
                            "probe#${round + 1} 出=${meals.size} 耗时=${t0.elapsedNow().inWholeMilliseconds}ms | " +
                                meals.joinToString("、") { it.name } +
                                " | 有图=${meals.count { it.photo.isNotBlank() }}"
                        )
                    }
                }
                finish()
            }.start()
            return
        }

        setContent { App() }
    }
}
