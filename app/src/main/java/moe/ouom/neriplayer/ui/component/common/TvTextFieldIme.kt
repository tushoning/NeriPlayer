package moe.ouom.neriplayer.ui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision

/**
 * 电视上「自动弹出软键盘」关掉时，输入框不弹输入法。
 *
 * 做法和原生给 EditText 设 `InputType.TYPE_NULL` 是一个意思：不再外面套一层可聚焦的壳
 * （套壳会多出一个焦点位，焦点框还会被壳抢走、和输入框自己的一起叠成两个框），
 * 而是把输入框本身切成只读——Compose 的只读输入框不会向系统申请输入法，键盘自然弹不出来，
 * 焦点仍然停在输入框本身，用的还是它原来的焦点框。
 *
 * 只读状态下按一次确认键切回可编辑，键盘才弹出来；焦点离开自动复位。
 *
 * 用法：
 * ```
 * val ime = rememberTvTextFieldIme()
 * OutlinedTextField(..., readOnly = ime.readOnly, modifier = base.then(ime.modifier))
 * ```
 * 手机，或开关打开时：[TvTextFieldIme.readOnly] 恒为 false，[TvTextFieldIme.modifier] 是空
 * Modifier，和原版完全一致。
 */
@Stable
class TvTextFieldIme internal constructor(
    /** 传给输入框的 readOnly */
    val readOnly: Boolean,
    /** 挂在输入框 Modifier 上：拦确认键、焦点离开时复位 */
    val modifier: Modifier
)

@Composable
fun rememberTvTextFieldIme(
    /**
     * 只读状态下**左键**是否留在原地（true = 吞掉，焦点不动）。
     * 贴在页面最左边的搜索框要吞，否则左键会穿出去落到相邻的横向页面上（分页器跟着翻页）。
     */
    swallowLeft: Boolean = true,
    /**
     * 只读状态下**右键**是否留在原地。
     * 右侧还有别的东西要过去的（探索页来源标签、歌手搜索框右边的三点按钮）必须放行。
     */
    swallowRight: Boolean = true,
    /**
     * 右侧第一个目标。给了就显式把焦点送过去，而不是交给方向搜索去猜。
     *
     * 探索页搜索框右边是上下两排（来源标签 / 歌曲·歌单·歌手），几何搜索会挑更近的那一排，
     * 结果经常落到下面那排小分类上。这里直接指定落到来源标签那一行。
     */
    rightFocusTarget: FocusRequester? = null
): TvTextFieldIme {
    val isTelevision = LocalIsTelevision.current
    val autoShowKeyboard by AppContainer.settingsRepo.autoShowKeyboardFlow
        .collectAsStateWithLifecycle(initialValue = true)
    // 只有电视 + 开关关闭时才阻断输入法，其他情况一律原样
    val blocked = isTelevision && !autoShowKeyboard

    val typingState = remember { mutableStateOf(false) }
    val typing = typingState.value
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(blocked) {
        if (!blocked) typingState.value = false
    }
    LaunchedEffect(typing) {
        if (!typing) return@LaunchedEffect
        // 只读切回可编辑是下一帧才生效，等状态落地再请求输入法
        withFrameNanos { }
        withFrameNanos { }
        runCatching { keyboardController?.show() }
    }

    val readOnly = blocked && !typing
    val modifier = remember(blocked, swallowLeft, swallowRight, rightFocusTarget) {
        if (!blocked) {
            Modifier
        } else {
            Modifier
                .onFocusChanged { state ->
                    // 焦点离开就退出输入状态，下次移回来还是「只选中、不弹键盘」
                    if (!state.isFocused) typingState.value = false
                }
                .onPreviewKeyEvent { event ->
                    if (typingState.value || event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            typingState.value = true
                            true
                        }
                        // 只读（还没进输入状态）时按调用方的要求决定左右键去留。
                        // 这些搜索框大多贴在页面最左边，左键穿出去会落到相邻的横向页面上，
                        // 分页器跟着一页页翻走（B站搜索框"一直往左跑"就是这个）。
                        // 上下键照常走，把焦点移走。
                        Key.DirectionLeft -> swallowLeft
                        // 给了明确目标就自己送过去：几何搜索在两排候选之间挑不准
                        Key.DirectionRight -> if (rightFocusTarget != null) {
                            runCatching { rightFocusTarget.requestFocus() }
                            true
                        } else {
                            swallowRight
                        }
                        else -> false
                    }
                }
        }
    }
    return remember(readOnly, modifier) { TvTextFieldIme(readOnly, modifier) }
}
