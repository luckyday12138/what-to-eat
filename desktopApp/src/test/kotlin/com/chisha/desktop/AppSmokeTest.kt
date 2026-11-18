package com.chisha.desktop

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chisha.App
import com.chisha.data.ChishaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * 内存渲染的 UI 冒烟测试：不弹窗口、不触碰鼠标键盘。
 * 直接派发 onClick 语义动作，规避桌面端鼠标消息依赖。
 */
@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {

    // 菜品图走 Coil 网络加载，需要一个 Main 派发器，测试环境默认没有
    @BeforeTest
    fun installMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    // 转盘/抽签类用例固定在随机模式（本地菜库），避免用例依赖网络时序
    @BeforeTest
    fun useLocalMode() {
        ChishaRepo.instance.updatePrefs { it.copy(mode = "PURE") }
    }

    @Test
    fun todayScreenRendersWheelAndSlots() = runComposeUiTest {
        setContent { App(warmPhotos = false) }

        onNodeWithText("今天吃点什么").assertIsDisplayed()
        onNodeWithText("早餐").assertIsDisplayed()
        onNodeWithText("午餐").assertIsDisplayed()
        onNodeWithText("晚餐").assertIsDisplayed()
        onNodeWithText("夜宵").assertIsDisplayed()
        onNodeWithText("转").assertIsDisplayed()
    }

    @Test
    fun spinProducesResultAndAcceptButton() = runComposeUiTest {
        setContent { App(warmPhotos = false) }

        onNodeWithTag("spin").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        mainClock.advanceTimeBy(3400)
        waitForIdle()

        onNodeWithText("就它了").assertExists()
        onNodeWithText("换一个").assertExists()
        onNodeWithText("其他候选").assertExists()
    }

    @Test
    fun acceptReturnsToWheel() = runComposeUiTest {
        setContent { App(warmPhotos = false) }

        onNodeWithTag("spin").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        mainClock.advanceTimeBy(3400)
        waitForIdle()

        onNodeWithText("就它了").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        mainClock.advanceTimeBy(800)
        waitForIdle()

        onNodeWithText("转").assertIsDisplayed()
    }

    // 首页两个模式的数据来源要分开：随机标「本地」，推荐标「网络」
    @Test
    fun sourceLabelFollowsMode() = runComposeUiTest {
        setContent { App(warmPhotos = false) }
        waitForIdle()

        // 已在 BeforeTest 固定为随机模式，此时来源标注应为「本地」
        onNodeWithText("本地").assertExists()
        onNodeWithText("推荐").assertExists()
    }

    // 我的页要有在线源选择入口
    @Test
    fun mineScreenExposesSourcePicker() = runComposeUiTest {
        setContent { App(warmPhotos = false) }

        onNodeWithTag("tab_3").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()

        onNodeWithText("在线源（首页推荐 / 在线灵感用哪个）").assertExists()
        onNodeWithText("自动").assertExists()
        onNodeWithText("选中的源连不上时会自动换下一个，恢复后再回来").assertExists()
    }
}
