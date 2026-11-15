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
 * File: moe.ouom.neriplayer.ui/tv/TvPagerPageMemory
 * Created: 2026/11/15
 */

/**
 * 电视：分页横条「上次停在第几页」的记忆（按横条自己的稳定 key）。
 *
 * 为什么不能只靠 `rememberPagerState` 自带的 saveable 恢复：
 * 首页的横条装在一个 LazyGrid 的 item 里，进二级页时整个网格被移出组合，
 * 回来时 item 内的 saveable 不保证能还原；更关键的是 [moe.ouom.neriplayer.ui.screen.tab.home.PagedCardStrip]
 * 首帧有「分帧限流」（只渲染前两页的卡），`pageCount` 那一帧是一两页，
 * 保存的页码会被 `PagerState` 直接夹到 0，等于永远回不到第 2 页往后。
 *
 * 页码一丢，`TvReturnFocus` 要归还焦点的那张卡就不在「已组合的页」里，
 * `requestFocus` 连试 24 帧都失败，焦点只能落到树里第一个可聚焦节点
 * （表现：从第 2 页的卡片进详情，返回后光标跑到上一分类的第一张卡上）。
 *
 * 纯内存、进程内有效：冷启动没有记忆 = 从第 0 页开始，符合预期。
 */
object TvPagerPageMemory {

    private val lock = Any()
    private val pages = HashMap<String, Int>()

    fun remember(stripKey: String, page: Int) {
        if (stripKey.isEmpty()) return
        synchronized(lock) { pages[stripKey] = page }
    }

    fun page(stripKey: String): Int? = synchronized(lock) { pages[stripKey] }

    /** 点标签「回根 / 回顶」时清掉，避免下次进来还停在中途那一页 */
    fun clear() = synchronized(lock) { pages.clear() }
}
