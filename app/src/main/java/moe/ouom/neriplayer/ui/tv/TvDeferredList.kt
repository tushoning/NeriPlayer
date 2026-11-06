package moe.ouom.neriplayer.ui.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * File: moe.ouom.neriplayer.ui.tv/TvDeferredList
 * Created: 2026/11/02
 */

/**
 * 进歌单/专辑这类页面时，LazyColumn 的第一帧要同时完成两件事：组合页头（封面、渐变、
 * 标题、操作条）和组合首屏那七八行。实测那一帧要 **99ms 重组 + 110ms 测量**，而 60fps
 * 的帧预算只有 16.7ms —— 观感就是「页面已经切过去了，但要卡一下才出内容」。
 *
 * 曾经还有一版「页头 / 列表分两帧」的实现（只拆一刀，单帧峰值减半），它已经被下面这个
 * 分批版本取代并删除：拆一刀仍然有 90ms 的单帧，还是肉眼可见的一顿。
 */

/**
 * 分批揭示：进页面时把首屏行**拆成两三批**填进去，而不是一次性全填。
 *
 * 这里返回「当前该显示多少行」：
 * 1. 首帧返回 0 —— 只组合页头，页面先出现；
 * 2. 约一帧后返回 [firstBatch] —— 先出最上面几行；
 * 3. 再过一帧返回 [Int.MAX_VALUE] —— 补满。
 *
 * 每批之间的间隔很小（默认 24ms，约 1.5 帧），肉眼看到的是「内容紧接着出现」，
 * 但单帧峰值被摊成三份。行在页头下方依次出现，不会造成布局跳动。
 *
 * 用 `delay` 而不是 `withFrameNanos`：如果页面恰好不出帧（完全静止），等帧会永远
 * 等下去、列表就永远不出现了；`delay` 按时间放行，最坏情况只是并回同一帧，不会卡死。
 *
 * 用法：`items = songs.take(state.value)`（带上限判断避免整表复制）。
 */
@Composable
fun rememberProgressiveListReveal(
    firstBatch: Int = 4,
    stepMs: Long = 24L
): State<Int> {
    val count = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(stepMs)
        count.value = firstBatch
        delay(stepMs)
        count.value = Int.MAX_VALUE
    }
    return count
}
