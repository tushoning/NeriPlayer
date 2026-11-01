package moe.ouom.neriplayer.ui.screen.tab.home

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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomeRefreshBus
 * Created: 2026/10/26
 */

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 首页刷新请求总线。
 *
 * 电视上首页右上角的刷新按钮已经去掉，功能挪到「首页」标签上：焦点停在该标签时
 * 按确定键就刷新。标签栏在导航层，拿不到 HomeViewModel，用一个全局计数把请求
 * 转达给真正持有数据的那一层。
 */
object HomeRefreshBus {
    private val _tick = MutableStateFlow(0)

    val tick: StateFlow<Int> = _tick

    fun request() {
        _tick.value = _tick.value + 1
    }
}
