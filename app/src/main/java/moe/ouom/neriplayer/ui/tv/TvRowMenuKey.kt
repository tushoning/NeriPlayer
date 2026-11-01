package moe.ouom.neriplayer.ui.tv

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.tv/TvRowMenuKey
 * Created: 2026/10/26
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * 遥控器菜单键触发「当前这一行」的更多操作。
 *
 * 列表里每行右侧的三点按钮在电视上刻意保持不可聚焦（否则方向键每行都要在
 * 「播放」和「三个点」之间多走一步），取而代之的是：光标停在某一行时按菜单键，
 * 直接弹出这一行的菜单。
 *
 * 行的容器和菜单组件通常不在同一个函数里，所以用一个 CompositionLocal 把
 * 「这一行被按了菜单键」的计数器传给行内的菜单组件。
 */
@Stable
class TvRowMenuKeyState {
    var tick by mutableIntStateOf(0)
        private set

    internal fun trigger() {
        tick++
    }
}

val LocalTvRowMenuKeyState = compositionLocalOf<TvRowMenuKeyState?> { null }

@Composable
fun rememberTvRowMenuKeyState(): TvRowMenuKeyState = remember { TvRowMenuKeyState() }

/**
 * 挂在行容器上：焦点在这一行时按菜单键（兼容外接键盘的右键菜单上下文键）触发展开。
 * 菜单键只消费按下，避免长按重复触发。
 */
fun Modifier.tvRowMenuKeyPress(state: TvRowMenuKeyState, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    return this.onPreviewKeyEvent { event ->
        val isMenuKey = event.key == Key.Menu
        if (!isMenuKey) return@onPreviewKeyEvent false
        if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent true
        state.trigger()
        true
    }
}

/**
 * 挂在行的菜单组件里：本行被按下菜单键时把菜单展开。
 * 不在 [LocalTvRowMenuKeyState] 作用域内时（非电视、或这行没接菜单键）什么都不做。
 *
 * 行容器和菜单组件通常跨了几层函数，可以用 [state] 显式传下来，也可以用
 * CompositionLocal 隐式传递。
 */
@Composable
fun TvRowMenuKeyEffect(
    state: TvRowMenuKeyState? = LocalTvRowMenuKeyState.current,
    enabled: Boolean = true,
    expand: () -> Unit
) {
    if (state == null) return
    if (!enabled) return
    val tick = state.tick
    LaunchedEffect(tick, state) {
        if (tick > 0) expand()
    }
}
