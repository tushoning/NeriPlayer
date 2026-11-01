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
 * File: moe.ouom.neriplayer.ui.tv/TvTabConfirmBus
 * Created: 2026/10/31
 */

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 电视顶部标签栏「再按一次确定键」的请求总线。
 *
 * 标签栏在导航层，拿不到各页自己的 ViewModel 和列表状态，所以用一条按路由过滤的
 * 事件流把请求转达给真正持有数据的那一层：
 *  - 探索 / 媒体库：刷新当前内容 + 回到顶部；
 *  - 设置：只回顶部（没有可刷新的远端数据）；
 *  - 首页仍走 [moe.ouom.neriplayer.ui.screen.tab.home.HomeRefreshBus]。
 */
object TvTabConfirmBus {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val events: SharedFlow<String> = _events

    fun request(route: String) {
        _events.tryEmit(route)
    }
}
