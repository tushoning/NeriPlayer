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
 * File: moe.ouom.neriplayer.ui.tv/TvReturnFocus
 * Created: 2026/11/03
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import moe.ouom.neriplayer.ui.component.navigation.TvTopBarFocus

/**
 * 电视「返回来源」：从二级页退出时，把光标送回当初打开它的那张卡片。
 *
 * 设置页那套 `SettingsEntryFocus` 只解决「一层 + 短列表 + 枚举 key」，这里补上它没有的三件事：
 *
 *  1. **按层记**。歌单可以进专辑、再从专辑进歌手，只有一份「最后聚焦的」会被上层覆盖，
 *     返回时落到错的地方。所以每个宿主按 [TvReturnAnchor] 存一份「每层从哪进来的」。
 *  2. **按稳定 key 记**。首页会把刚点开的卡片提到最前（`recordOpen`），按 index 记必然错位。
 *  3. **先滚回视口再逐帧重试**。卡片在 lazy 列表里没组合时，`requestFocus` 会直接抛异常。
 *
 * 还有一个关键点：锚点的 scope 是「宿主 + 层级」自动拼出来的（例如 `home.0`、`home.1`），
 * 由 [LocalTvReturnScope] 和 [LocalTvReturnDepth] 下发。卡片自己只传 key。
 * 这样详情页里的行不会污染一级列表的锚点 —— 不用赌「depth 变化的那一刻谁先跑」。
 */
sealed interface TvReturnAnchor {
    /** 列表里某张卡：scope 形如 `home.0`、`library.1` */
    data class Item(val scope: String, val key: String) : TvReturnAnchor

    /** 顶栏某个标签（含「正在播放」伪标签） */
    data class TopBar(val route: String) : TvReturnAnchor
}

/** 当前卡片属于哪个宿主（`home` / `library` / `explore` / `nav`）。null 表示不参与返回来源。 */
val LocalTvReturnScope = staticCompositionLocalOf<String?> { null }

/** 当前列表处于该宿主的第几层：一级列表 0，详情页里的列表 1，依次递增。 */
val LocalTvReturnDepth = staticCompositionLocalOf { 0 }

object TvReturnFocus {

    private val lock = Any()

    /** `"<scope>\u0000<key>"` -> 请求器 */
    private val requesters = HashMap<String, FocusRequester>()

    /** 每个宿主每一层是「从哪张卡进来的」 */
    private val layers = HashMap<String, HashMap<Int, TvReturnAnchor>>()

    /** 每个 scope 最近聚焦 / 最近失焦的卡片，用来算「来源」 */
    private val lastFocusedByScope = HashMap<String, TvReturnAnchor.Item>()
    private val lastBlurredByScope = HashMap<String, TvReturnAnchor.Item>()

    /** 全局最近聚焦的卡片，播放页打开时用它当来源 */
    @Volatile
    private var lastFocusedItem: TvReturnAnchor.Item? = null

    /** 播放页打开前的来源锚点（由 `AppNowPlayingOverlay` 在 clearFocus 之前抓下来） */
    @Volatile
    var nowPlayingSource: TvReturnAnchor? = null
        private set

    /**
     * 用户按键计数。逐帧重试期间一变就放弃 —— 否则返回后马上按方向键，
     * 光标会被重试逻辑拽回原来的卡片。
     */
    @Volatile
    private var inputEpoch = 0

    fun onUserInput() {
        inputEpoch++
    }

    fun requesterFor(scope: String, key: String): FocusRequester = synchronized(lock) {
        requesters.getOrPut("$scope\u0000$key") { FocusRequester() }
    }

    fun onItemFocusChanged(scope: String, key: String, focused: Boolean) {
        val item = TvReturnAnchor.Item(scope, key)
        synchronized(lock) {
            if (focused) {
                lastFocusedByScope[scope] = item
                lastBlurredByScope.remove(scope)
            } else {
                lastBlurredByScope[scope] = item
            }
        }
        if (focused) lastFocusedItem = item
    }

    private fun captureInScope(scope: String): TvReturnAnchor.Item? = synchronized(lock) {
        lastFocusedByScope[scope] ?: lastBlurredByScope[scope]
    }

    private fun scopeOf(owner: String, depth: Int): String = "$owner.$depth"

    /** 进入更深一层之前调用：把当前这层的来源记下来（同层覆盖） */
    fun stash(owner: String, depth: Int) {
        val anchor = captureInScope(scopeOf(owner, depth))
        if (anchor == null) return
        synchronized(lock) {
            layers.getOrPut(owner) { HashMap() }[depth] = anchor
        }
    }

    fun anchorFor(owner: String, depth: Int): TvReturnAnchor? = synchronized(lock) {
        layers[owner]?.get(depth)
    }

    /**
     * 播放页打开时调用，**必须在 `clearFocus` 之前**。
     *
     * 顶栏格子不挂 [tvReturnFocus]，所以它只能从 [TvTopBarFocus.focusedTabRoute] 取；
     * 一旦 clearFocus 执行，这个字段就变 null 了。
     */
    fun captureNowPlayingSource() {
        val anchor = TvTopBarFocus.focusedTabRoute?.let { TvReturnAnchor.TopBar(it) }
            ?: lastFocusedItem
        if (anchor != null) nowPlayingSource = anchor
    }

    /** 取出并清空播放页来源。播放页可能被反复开关，取过一次就作废。 */
    fun consumeNowPlayingSource(): TvReturnAnchor? {
        val anchor = nowPlayingSource
        nowPlayingSource = null
        return anchor
    }

    /** 正在把光标交还来源：这一刻浮层的焦点边界要放开（见 AppNowPlayingOverlay 的 onExit） */
    @Volatile
    var isRestoringSource: Boolean = false
        private set

    /** 这次关闭是「跳去别的页面」：锚点作废，别再把光标还回列表。 */
    fun discardNowPlayingSource() {
        nowPlayingSource = null
    }

    /**
     * 切标签时清掉「从哪张卡进来的」记忆。
     *
     * 只清锚点，**不动** [requesters]（清了会让已组合的卡片与新的请求器对不上，
     * 顶栏和列表再也聚焦不上去），也**不动** [nowPlayingSource]（播放页关闭时还要归还）。
     */
    fun clear() {
        synchronized(lock) {
            layers.clear()
            lastFocusedByScope.clear()
            lastBlurredByScope.clear()
        }
        lastFocusedItem = null
        inputEpoch++
    }

    private fun requestAnchorFocus(anchor: TvReturnAnchor): Boolean = runCatching {
        when (anchor) {
            is TvReturnAnchor.Item -> requesterFor(anchor.scope, anchor.key)
            is TvReturnAnchor.TopBar -> TvTopBarFocus.requesterFor(anchor.route)
        }.requestFocus()
    }.getOrDefault(false)

    /**
     * 同步把光标送回播放页来源，成功才算数（成功即消费锚点）。
     *
     * 必须在 `showNowPlaying = false` **之前、同一个事件里**调用：此刻浮层还挂着、
     * 下层节点仍可聚焦，程序化请求直接改 activeFocusTarget —— 被移除的播放按钮本来就
     * 不持有焦点，帧末「交给树里第一个可聚焦元素」的兜底不会发生，那一帧画出来的
     * 就是目标行，而不是左上角返回键 / 标签栏。
     */
    fun restoreNowPlayingSourceNow(): Boolean {
        val anchor = nowPlayingSource ?: return false
        isRestoringSource = true
        val ok = try {
            requestAnchorFocus(anchor)
        } finally {
            // 漏复位会让浮层方向键漏到被遮住的下层
            isRestoringSource = false
        }
        if (ok) nowPlayingSource = null
        return ok
    }

    /**
     * 正在归还的那个锚点。
     *
     * 目标卡一落到布局里就由 [claimPendingRestore] 抢回焦点 —— 这样焦点在**同一帧的绘制之前**
     * 就已经在目标卡上，用户看不到「先亮在上一行第一张卡、再跳回来」。
     * 只靠逐帧重试做不到这一点：重试的第一拍通常早于卡片 attach，会失败一次，
     * 那一帧画出来的就是 Compose 的兜底焦点（树里第一个可聚焦节点）。
     */
    @Volatile
    private var pendingAnchor: TvReturnAnchor? = null

    /** 卡片在布局阶段调用：是不是该由我接住这次归还？ */
    fun claimPendingRestore(scope: String, key: String): Boolean {
        val pending = pendingAnchor as? TvReturnAnchor.Item ?: return false
        return pending.scope == scope && pending.key == key
    }

    /** 逐帧重试把光标送回某个锚点；用户中途按键则让给他。 */
    suspend fun restoreAnchor(anchor: TvReturnAnchor?, maxFrames: Int = 24): Boolean {
        if (anchor == null) return false
        val startEpoch = inputEpoch
        // 先登记，让目标卡在 placed 的那一刻就能同步抢回焦点（见 claimPendingRestore）
        pendingAnchor = anchor
        try {
            repeat(maxFrames) {
                if (requestAnchorFocus(anchor)) return true
                if (inputEpoch != startEpoch) return false
                withFrameNanos { }
            }
            return false
        } finally {
            // 必须无条件清：留着会让之后任何一次布局回调都误抢焦点
            pendingAnchor = null
        }
    }

    /** 退出到第 [depth] 层后调用 */
    suspend fun restore(owner: String, depth: Int, maxFrames: Int = 24): Boolean =
        restoreAnchor(anchorFor(owner, depth), maxFrames)
}

/**
 * 挂在卡片上：绑请求器 + 登记焦点变化。scope / 层级自动从 CompositionLocal 取，
 * 所以同一个行组件在「一级列表」和「详情页列表」里都能正确区分。
 */
fun Modifier.tvReturnFocus(key: String): Modifier = composed {
    val owner = LocalTvReturnScope.current
    val depth = LocalTvReturnDepth.current
    if (owner == null) {
        this
    } else {
        val scope = "$owner.$depth"
        val requester = remember(scope, key) { TvReturnFocus.requesterFor(scope, key) }
        this
            .focusRequester(requester)
            .onFocusChanged { TvReturnFocus.onItemFocusChanged(scope, key, it.isFocused) }
            // 归还焦点期间，本卡片一被摆进布局就立刻把焦点抢过来。
            //
            // `onGloballyPositioned` 在 layout 阶段末尾、draw 之前回调，所以抢到之后
            // 这一帧画出来的就是本卡片 —— 不会先亮一下兜底焦点（上一行第一张卡）再跳回来。
            // 逐帧重试那条路仍然保留：卡片晚几帧才出现时（比如要等分页器回到某一页），
            // 它也会在出现的那一帧走这里，同样不闪。
            .onGloballyPositioned {
                if (TvReturnFocus.claimPendingRestore(scope, key)) {
                    runCatching { requester.requestFocus() }
                }
            }
    }
}

/**
 * 挂在「有二级页的宿主」根上：层数变深就记来源，变浅就把光标送回去。
 *
 * 调用方必须把这一行放在**恢复滚动位置**的 effect 之后，否则卡片还没滚回视口，
 * `requestFocus` 会白白重试 24 帧。
 */
@Composable
fun TvReturnFocusPageBridge(owner: String, depth: Int) {
    var previousDepth by remember(owner) { mutableStateOf(depth) }
    LaunchedEffect(owner, depth) {
        val previous = previousDepth
        previousDepth = depth
        when {
            depth > previous -> for (level in previous until depth) {
                TvReturnFocus.stash(owner, level)
            }

            depth < previous -> TvReturnFocus.restore(owner, depth)
        }
    }
}
