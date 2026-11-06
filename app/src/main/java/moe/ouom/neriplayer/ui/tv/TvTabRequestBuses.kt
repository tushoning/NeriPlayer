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
 * File: moe.ouom.neriplayer.ui.tv/TvTabRequestBuses
 * Created: 2026/11/06
 */

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 「按路由下发一条请求」的通用总线。
 *
 * 侧栏在导航层，拿不到各页自己的 ViewModel 和列表状态，只能靠一条按 route 过滤的事件流
 * 把请求转达给真正持有数据的那一层。两个子类型只是语义不同，机制完全一样：
 *  - [TvTabRootBus]    「再按一次当前标签」，由标签宿主办判断当前在哪一层；
 *  - [TvTabConfirmBus] 宿主把它翻译成「刷新 + 回顶」，交给页面自己消费。
 */
open class TvRouteRequestBus {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val events: SharedFlow<String> = _events

    fun request(route: String) {
        _events.tryEmit(route)
    }
}

/**
 * 电视侧栏「再按一次当前标签」的请求总线。
 *
 * 侧栏只知道 NavHost 的 route：首页 / 探索 / 归集各自把详情页藏在宿主内部的 `selected` 里，
 * route 一直停在标签根上。所以「二级界面点本页标签」和「一级界面点本页标签」发出的信号
 * 必须一模一样，由宿主自己判断当前处在哪一层：
 *  - 详情态 → 退回根页面 + 列表回顶（不刷新网络数据）；
 *  - 根态   → 转成原来的刷新逻辑（首页走 HomeRefreshBus，其余走 [TvTabConfirmBus]）。
 *
 * 设置页没有详情态分层，直接自己消费这一条流。
 */
object TvTabRootBus : TvRouteRequestBus()

/**
 * 标签宿主翻译后的「刷新当前内容 + 回到顶部」请求。
 *
 * 页面对应关系：探索 / 媒体库 → 刷新 + 回顶；设置 → 只回顶（没有可刷新的远端数据）；
 * 首页不走这条，它有自己的 [moe.ouom.neriplayer.ui.screen.tab.home.HomeRefreshBus]。
 */
object TvTabConfirmBus : TvRouteRequestBus()
