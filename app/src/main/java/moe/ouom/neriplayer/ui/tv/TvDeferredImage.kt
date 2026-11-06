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
 * File: moe.ouom.neriplayer.ui.tv/TvDeferredImage
 * Created: 2026/11/03
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * [rememberTvDeferredImage] 的产物：交给 `AsyncImage` 的 model 和一个「已加载」回执。
 */
@Stable
class TvDeferredImage internal constructor(
    /** 传 null 时 AsyncImage 什么都不画（保留卡片底色），也就不会发起请求 */
    val model: Any?,
    private val markLoadedAction: () -> Unit
) {
    /** 在 `AsyncImage(onSuccess = { ... })` 里调一次 */
    fun markLoaded() = markLoadedAction()
}

/**
 * 电视端：连续按键期间**不发起新的图片加载**，手停下来再加载。
 *
 * 为什么需要：按住方向键时，每移一格都会有一批新卡片进入视口，如果它们在同一帧里
 * 去解码十几张封面，帧预算（16.7ms）直接爆掉，这一格就跳不过去 —— 表现就是
 * 页面「一停一停」。
 *
 * 语义是「只拦新加载」：
 * - 这张卡如果以前加载成功过（[TvDeferredImage.markLoaded] 被调过），滚动期间照常显示；
 * - 没加载出来的，滚动期间 model 传 null（不请求、不解码），
 *   `LocalTvInputBusy` 落回 false 之后再发请求。
 *
 * @param key 图片的身份（通常是 URL）。**必须是稳定值** —— 用它判断「换图了要重置已加载标记」；
 *   传 ImageRequest 这类每次重建的对象会导致标记被反复重置，等于关不掉加载。
 * @param model 只有在需要真正加载时才求值，滚动期间不会被调用。
 */
@Composable
fun rememberTvDeferredImage(key: Any?, model: () -> Any?): TvDeferredImage {
    // 读的是追踪器上的 busy：订阅范围只落在用这张图的卡片上，不会波及整棵树
    val busy = LocalTvInputTracker.current?.busy ?: false
    val loadedState = remember(key) { mutableStateOf(false) }
    val deferred = busy && !loadedState.value
    val markLoaded = remember(loadedState) { { loadedState.value = true } }
    return remember(key, deferred, markLoaded) {
        TvDeferredImage(
            model = if (deferred) null else model(),
            markLoadedAction = markLoaded
        )
    }
}
