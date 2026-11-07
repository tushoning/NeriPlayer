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
 * File: moe.ouom.neriplayer.ui.tv/TvFocusNav
 * Created: 2026/10/26
 */

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 电视横条的滚动交给 Compose 自己：它在拿到焦点时会请求 bring-into-view，落点规范由
// TvMinimalBringIntoViewSpec 决定（不把卡片拽到视口中间）。这里曾经有一份手写的
// 「聚焦即滚动」，和规范重复、一次按键滚两格，已经整段删掉 —— 现在只剩 tvStripClamp
// 负责两端夹住。

/**
 * 电视：横条走到尽头就别再往那个方向走了。
 *
 * 不夹住的话，最后一张卡片再按右、Compose 会往上跑到根节点那一层继续找候选：
 * 分页器（HorizontalPager）会被翻到下一页，看起来就是「整个画面向左跑」，
 * 焦点最后落到最右边那个入口上。
 */
@Composable
fun Modifier.tvStripClamp(
    state: LazyListState,
    index: Int,
    lastIndex: Int,
    /**
     * 是否夹住左端。探索页的搜索类型行（歌曲/歌单/歌手…）左边就是搜索框，
     * 那里必须放行，否则光标走不到搜索框上。
     */
    clampStart: Boolean = true
): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> clampStart && index <= 0
            Key.DirectionRight -> index >= lastIndex && !state.canScrollForward
            else -> false
        }
    }
}

/**
 * 电视：一整行控件走到两端就别再往那个方向走了。
 *
 * 走到最左还按左，Compose 会往上跑到根节点那一层继续找候选，
 * 结果焦点落到完全不相干的一行上（页面内标签跳到主标签、主标签跳到顶部标签栏…）。
 */
@Composable
fun Modifier.tvClampRowEnds(index: Int, lastIndex: Int): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> index <= 0
            Key.DirectionRight -> index >= lastIndex
            else -> false
        }
    }
}

/**
 * 电视：网格里上下键「按同一列逐行移动」。
 *
 * Compose 的方向搜索只能看到**已经组合出来**的格子。视口下面那一行还没进组合，
 * 往下就搜不到候选，搜索会一路往上跑到根节点重新来一遍，最后落到树里第一个
 * 可聚焦元素上 —— 表现就是「前两行正常，从第三行往下按会跳回第一个卡片」。
 *
 * 这里自己算目标行：在同一行里按 x 距离挑最近的格子。
 *
 * **节奏跟首页对齐**（用户点名要的就是这个）：
 * - 目标行已经整行露在视口里 → 直接搬焦点（瞬时、不滚动）；
 * - 目标行只露出半截 / 还在视口外 → **列表先开始滚，滚动动起来两帧之内焦点就落过去**，
 *   环跟着目标卡片一起走进屏幕。
 *   注意不是「等目标行完全进入视口再落焦点」—— 那种「停住、等它整个进来、啪一下」的手感
 *   和首页完全不同，用户明确否掉过。
 *
 * 到边界（上下不能再滚、也没有目标行）时交还给 Compose，比如从第一行往上回到标签行。
 */
@Stable
class TvGridRowNavigation internal constructor(
    private val state: androidx.compose.foundation.lazy.grid.LazyGridState,
    private val scope: CoroutineScope,
    /** 目标行滚进视口时额外留的边距（px），和 TvFocusScrollMargin 一致 */
    private val marginPx: Float = 0f
) {
    private val requesters = HashMap<Any, FocusRequester>()

    fun requesterFor(key: Any): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    private fun requestFocus(key: Any) {
        runCatching { requesterFor(key).requestFocus() }
    }

    /**
     * @return true 表示这次按键已经处理（焦点会由这里搬动），调用方不用再管
     */
    internal fun handleKey(key: Any, eventKey: Key): Boolean {
        val direction = when (eventKey) {
            Key.DirectionDown -> 1
            Key.DirectionUp -> -1
            else -> return false
        }
        val layout = state.layoutInfo
        val current = layout.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
        val neighbour = findRowNeighbour(current, layout.visibleItemsInfo, direction)
        // 目标行已经整行露在视口里：和首页在视口内的移动一样，直接搬焦点、不滚动
        if (neighbour != null && isFullyVisible(neighbour, layout, direction)) {
            requestFocus(neighbour.key)
            return true
        }
        val canScroll = if (direction > 0) state.canScrollForward else state.canScrollBackward
        if (!canScroll) {
            // 到边界（上下已经不能再滚）：有半露着的目标行就地搬过去，
            // 没有就交还 Compose（从第一行往上要能回到上面的标签行）
            if (neighbour == null) return false
            requestFocus(neighbour.key)
            return true
        }

        // 目标行不在视口里、或者只露出半截：节奏要跟「首页」一模一样 ——
        // 按下之后列表立刻开始滚，**只等滚动动起来一点点（两帧）焦点框就落过去**，
        // 卡片刚往屏幕里走了没多少，环就已经在它身上，剩下的路程一起走进去。
        // 绝不能等目标行完全进入视口再落焦点：那是「先停住、等它整个进来、啪一下跳过去」，
        // 跟首页是两种手感。
        val delta = neighbour?.let { scrollDistanceToReveal(it, layout, direction) }
            ?: (current.size.height * direction).toFloat()
        scope.launch {
            // 单独起协程滚动，不要 await：焦点必须在滚动进行中就落地
            launch { runCatching { state.animateScrollBy(delta) } }
            withFrameNanos { }
            withFrameNanos { }
            repeat(4) {
                val now = state.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == key } ?: return@repeat
                val hit = findRowNeighbour(now, state.layoutInfo.visibleItemsInfo, direction)
                val hitKey = hit?.key
                if (hitKey != null) {
                    requestFocus(hitKey)
                    return@launch
                }
                withFrameNanos { }
            }
        }
        return true
    }

    /**
     * 这一格是否**整格**落在视口内（留出 marginPx，避免焦点环贴着视口边缘被裁）。
     *
     * 用 `0..viewportSize.height` 而不是 `viewportStartOffset/viewportEndOffset`：
     * 后两个把 contentPadding 也算进去了（网格底下还有 mini player 那么高的 padding），
     * 结果「只露出半截、甚至整个在屏幕外」的格子也会被判成「完整可见」，
     * 于是一按方向键就直接搬焦点、滚动跟在后面 —— 又变回「环先动、卡片后滚」。
     */
    private fun isFullyVisible(
        item: androidx.compose.foundation.lazy.grid.LazyGridItemInfo,
        layout: androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo,
        direction: Int
    ): Boolean {
        val top = item.offset.y
        val bottom = top + item.size.height
        val end = layout.viewportSize.height - marginPx
        return if (direction > 0) {
            top >= marginPx && bottom <= end
        } else {
            // 向上：只要这一行整行都在视口里就直接搬焦点。
            // 这里不再要求留 marginPx —— 上面那排往往只是贴着视口顶边，
            // 为了补那一点边距多滚一段，用户看到的就是「往上走滚得比往下走多」。
            top >= 0f && bottom <= layout.viewportSize.height
        }
    }

    /** 把这一格完整推进视口还差多少滚动距离（正数 = 内容上移，负数 = 内容下移） */
    private fun scrollDistanceToReveal(
        item: androidx.compose.foundation.lazy.grid.LazyGridItemInfo,
        layout: androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo,
        direction: Int
    ): Float {
        val top = item.offset.y
        val bottom = top + item.size.height
        return if (direction > 0) {
            (bottom - (layout.viewportSize.height - marginPx)).coerceAtLeast(0f)
        } else {
            // 向上只滚到「这一行的顶边贴住视口顶边」为止，不多留余量，
            // 否则同样一次按键，往上走会比往下走滚得多。
            top.coerceAtMost(0).toFloat()
        }
    }

    /** 同一行判定用 y 偏移：同一行的格子 y 完全相同 */
    private fun findRowNeighbour(
        current: androidx.compose.foundation.lazy.grid.LazyGridItemInfo,
        items: List<androidx.compose.foundation.lazy.grid.LazyGridItemInfo>,
        direction: Int
    ): androidx.compose.foundation.lazy.grid.LazyGridItemInfo? {
        // 只比较同尺寸的格子：整行占满的标签行、加载/错误占位的尺寸都不同，要排除掉
        val cells = items.filter {
            it.size.height == current.size.height && it.size.width == current.size.width
        }
        val rowTops = cells.map { it.offset.y }.distinct().sorted()
        val currentRow = rowTops.indexOf(current.offset.y)
        if (currentRow < 0) return null
        val targetTop = rowTops.getOrNull(currentRow + direction) ?: return null
        return cells
            .filter { it.offset.y == targetTop }
            .minByOrNull { kotlin.math.abs(it.offset.x - current.offset.x) }
    }
}

@Composable
fun rememberTvGridRowNavigation(
    state: androidx.compose.foundation.lazy.grid.LazyGridState
): TvGridRowNavigation {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(state, scope, density) {
        TvGridRowNavigation(state, scope, with(density) { GridRowFocusScrollMargin.toPx() })
    }
}

/** 目标行滚进视口时多推进来的距离，和主题里的 TvFocusScrollMargin 保持同一个值 */
private val GridRowFocusScrollMargin = 12.dp

/**
 * 挂在网格里的每一张卡片上（[key] 就是 `items(key = ...)` 用的那个，必须和布局信息对得上）。
 */
@Composable
fun Modifier.tvGridRowNav(nav: TvGridRowNavigation, key: Any?): Modifier {
    if (!LocalIsTelevision.current) return this
    if (key == null) return this
    return this
        .focusRequester(nav.requesterFor(key))
        .onPreviewKeyEvent { event ->
            event.type == KeyEventType.KeyDown && nav.handleKey(key, event.key)
        }
}

/**
 * 电视：在「第一屏内容」上按上键时，先把列表滚回顶部，而不是直接把焦点跳到返回按钮。
 *
 * 详情页的头图（大封面）不是可聚焦节点，Compose 的方向搜索找不到它，
 * 于是从「播放全部」那一行按上会一路往上落到顶栏的返回按钮上 ——
 * 用户看到的就是「回不到大封面头图，直接飘到返回那一行」。
 * 这里接管上键：列表还能往回滚就先滚到顶（头图完整露出来），滚到顶了才放行。
 */
@Composable
fun Modifier.tvScrollToTopOnUp(listState: LazyListState): Modifier {
    if (!LocalIsTelevision.current) return this
    val scope = rememberCoroutineScope()
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionUp) {
            return@onPreviewKeyEvent false
        }
        if (!listState.canScrollBackward) return@onPreviewKeyEvent false
        scope.launch { runCatching { listState.animateScrollToItem(0) } }
        true
    }
}

/**
 * 电视：滚动内容「走到头」时的兜底 —— 由滚动容器自己把内容挪一段。
 *
 * 这一类页面的最后一段常常只有文字、没有按钮。光标走到最后一个可聚焦项就再也按不下去，
 * 滚动容器自然也不会再滚，下面那段内容永远看不到。
 *
 * 做法是挂在滚动容器自身上，且只在「方向搜索确实无路可走」时才动手：
 *  1. 先按默认行为移动焦点，移得动就照旧（焦点优先，和系统行为一致）；
 *  2. 移不动了、而内容还能滚 -> 容器自己滚一屏的一半，焦点原地不动；
 *  3. 已经到端头 -> 不消费，照常交回系统（按上就去表头/返回按钮，按下就什么都不做）。
 *
 * 相比「在内容首尾塞一个不可见可聚焦落点」：这里不产生任何额外的焦点节点，
 * 所以初始焦点、方向搜索、按到端头之后的走向都不会被带偏，也不存在「落到落点上再按就乱跳」。
 */
@Composable
fun Modifier.tvEdgeScroll(
    scrollState: ScrollState,
    /**
     * 端头夹边：滚到底再按下键时光标停在最后一项不动。
     *
     * 默认关闭 —— YouTube 播放源那个对话框要靠「下键放行」交给系统兜底，
     * 才能从列表走到下面的「关闭」按钮。
     */
    clampDownAtEnd: Boolean = false
): Modifier {
    if (!LocalIsTelevision.current) return this
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { TV_EDGE_SCROLL_STEP.toPx() }
    return this.onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val direction = when (event.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            else -> return@onKeyEvent false
        }
        val forward = direction == FocusDirection.Down
        if (focusManager.moveFocus(direction)) return@onKeyEvent true
        val canScroll = if (forward) scrollState.canScrollForward else scrollState.canScrollBackward
        if (!canScroll) {
            // 已经在端头（moveFocus 上面已经试过并失败，说明这个方向确实没有可聚焦项了）。
            // clampDownAtEnd 时才把下键吃掉：光标停在最后一项不动；
            // 其余用法保持原语义，放行给系统兜底。
            return@onKeyEvent if (clampDownAtEnd) forward else !forward
        }
        val delta = if (forward) stepPx else -stepPx
        scope.launch { runCatching { scrollState.animateScrollBy(delta) } }
        true
    }
}

/** 见 [tvEdgeScroll] 的 ScrollState 版说明；按一下挪一屏的一半。 */
private val TV_EDGE_SCROLL_STEP = 300.dp

/**
 * 电视：同样的道理用 [androidx.compose.foundation.ScrollState]（普通 Column 滚动）。
 * 表头是固定的、内容在下面滚，光标回到表头按钮后按上，先把内容滚回顶部，
 * 把滚上去的那段文字露出来；已经到顶了才放行。
 */
/**
 * 电视：可滚动的文字面板 —— 一个修饰符管完三件事。
 *
 *  1. 内容比一屏长时，光标走到最后一个可聚焦项还能继续把剩下的内容挪出来（[tvEdgeScroll]）；
 *  2. 滚到底再按「下」，Compose 的方向搜索会回绕到页面顶部的返回键上。这里把这个出口掐掉，
 *     光标就停在底部不动；
 *  3. 整个面板当成一个焦点组，光标不会从中间某一行直接跳到组外的控件上。
 *
 * 说明：`focusProperties.onExit` 在当前版本拿不到方向，所以先用面板自己的按键监听
 * 把「这一下按的是不是下键」记在 [lastDown] 里，再在出口判断时用它。
 */
@Composable
fun Modifier.tvScrollPane(scrollState: ScrollState): Modifier {
    if (!LocalIsTelevision.current) return this
    val lastDown = remember { mutableStateOf(false) }
    return this
        .onPreviewKeyEvent { event ->
            when (event.type) {
                KeyEventType.KeyDown -> lastDown.value = event.key == Key.DirectionDown
                // 标记「用完就清」：onExit 拦的是所有离开本节点的焦点变更，包括返回键
                // 把光标收回侧栏、程序化 requestFocus。只在上下键 down 时置位不够 ——
                // 按过下键再按返回，收回侧栏的请求会被一起取消。
                KeyEventType.KeyUp -> lastDown.value = false
                else -> Unit
            }
            false
        }
        // 必须同时是焦点组：容器成为组边界后，组内搜不到候选时才会走 onExit；
        // 只挂 focusTarget 的话，二维搜索会直接跨组命中页面顶部的返回键，onExit 根本不触发。
        .focusGroup()
        .focusProperties {
            canFocus = false
            onExit = {
                // 按下键一律不许把焦点送出面板。
                //
                // 原先是「到底了才拦」（`&& !scrollState.canScrollForward`），但焦点按到
                // 最后一个可聚焦项时内容通常还剩一截没滚 —— 此时 canScrollForward 仍为 true，
                // 于是放行，二维搜索跨组命中页面顶部的返回键，光标直接飞上去；
                // 下面 tvEdgeScroll 的滚动兜底压根轮不到（焦点已经离开了原节点）。
                // 现在改成无条件取消：焦点留在原地，能滚就交给 tvEdgeScroll 滚，到头就停住。
                if (lastDown.value) cancelFocusChange()
                lastDown.value = false
            }
        }
        .focusTarget()
        // 文字面板都要「到底就停」：不夹边的话系统兜底搜索会回绕到页面顶部的返回键。
        .tvEdgeScroll(scrollState, clampDownAtEnd = true)
}

/**
 * 电视方向键「最近一次按的是哪边」。
 *
 * 只用来区分两种「焦点落到顶部返回键」：用户自己按上键走过去，和焦点被方向搜索意外跳上去。
 * 前者应该把正文滚回顶部，后者不能 —— 否则看起来就是「按到底又回到了开头」。
 */
internal object TvInputTrace {
    @Volatile
    private var lastDownAt: Long = 0L

    @Volatile
    private var lastUpAt: Long = 0L

    fun onKeyDown(key: Key) {
        val now = android.os.SystemClock.uptimeMillis()
        when (key) {
            Key.DirectionDown -> lastDownAt = now
            Key.DirectionUp -> lastUpAt = now
            else -> Unit
        }
    }

    fun movedUpRecently(windowMs: Long = 400L): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        return lastUpAt > lastDownAt && now - lastUpAt <= windowMs
    }
}

/**
 * 电视：网格停在「第一屏内、但已经把顶部表头滚掉一截」时，按上先把网格拉回最顶端。
 *
 * 首页第 0 项是纯文字的「继续播放」表头（不可聚焦），首个可聚焦项是第 1 项卡片行。
 * 电视的最小滚动策略（`TvMinimalBringIntoViewSpec`，只滚到「刚好露出 + 12dp」）把卡片
 * 滚到视口顶之后，表头只剩几像素 —— 而上面再没有可聚焦节点能把这几像素拉出来，
 * 于是那段标题就永远看不见了。只有这里接管上键才能回去。
 */
@Composable
fun Modifier.tvBringGridToTopOnUp(gridState: LazyGridState): Modifier {
    if (!LocalIsTelevision.current) return this
    val scope = rememberCoroutineScope()
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionUp) {
            return@onPreviewKeyEvent false
        }
        if (!gridState.canScrollBackward) return@onPreviewKeyEvent false
        // 首页第一屏的 item 布局：0 = 「继续播放」表头（不可聚焦）、1 = 继续播放卡片行。
        // 电视的最小滚动余量 12dp 叠加卡片内边距后，表头会被整块推出视口 —— 此时
        // firstVisibleItemIndex 已经是 1、scrollOffset 是 0，旧的两个 gate
        // （index > 0 / offset <= 0）会同时命中，函数退化成空操作。所以判到 1 为止。
        if (gridState.firstVisibleItemIndex > 1) return@onPreviewKeyEvent false
        scope.launch { runCatching { gridState.animateScrollToItem(0) } }
        true
    }
}

@Composable
fun Modifier.tvScrollToTopOnUp(scrollState: androidx.compose.foundation.ScrollState): Modifier {
    if (!LocalIsTelevision.current) return this
    val scope = rememberCoroutineScope()
    // 两段都挂：preview 负责「祖先先拦」，keyEvent 负责「焦点节点自身兜底」。
    // 只挂 preview 时，如果控件自己先消费或祖先链没覆盖到，上键就会漏过去（先跳去表头）。
    fun consumeUp(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionUp) return false
        if (scrollState.value <= 0) return false
        scope.launch { runCatching { scrollState.animateScrollTo(0) } }
        return true
    }
    return this
        .onPreviewKeyEvent { consumeUp(it) }
        .onKeyEvent { consumeUp(it) }
}

/**
 * 菜单类浮层打开后，把光标送到菜单的第一个按钮上。
 *
 * 弹层（DropdownMenu / 自绘面板）拿到窗口焦点后，Compose 不会主动把光标交给第一项，
 * 遥控器上就表现为「菜单弹出来了，但方向键不知道在哪」。把返回的 [FocusRequester]
 * 挂到菜单第一项上即可；菜单没打开时什么都不做。
 */
@Composable
fun rememberTvMenuFirstItemFocus(expanded: Boolean, enabled: Boolean = true): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(expanded, enabled) {
        if (!expanded || !enabled) return@LaunchedEffect
        // 弹层刚挂载时焦点节点还没就绪，多试几帧
        repeat(8) {
            if (requester.requestFocus()) return@LaunchedEffect
            withFrameNanos { }
        }
    }
    return requester
}

/**
 * 焦点落到这一项时，主动请滚动容器把它滚进可视区。
 *
 * 大部分列表靠 Compose 自带的焦点滚动静默工作；但设置、媒体库、搜索结果这几页
 * 会出现在「光标已经走到屏幕外了，页面却不动」的情况。这里兜一层：拿到焦点就
 * 发一次 bringIntoView，已经可见时是空操作，不会干扰原本正常的滚动。
 */
@Composable
fun Modifier.tvBringIntoViewOnFocus(): Modifier {
    if (!LocalIsTelevision.current) return this
    // 可用 adb 关掉做 A/B：settings put global neri_tv_biv 0（见 TvTuning）
    if (!TvTuning.bringIntoViewOnFocus) return this
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                scope.launch {
                    // 焦点事件到达时这一项的布局还没跑完，立刻 bringIntoView 找不到可滚动
                    // 的锚点，等于什么都没做。等一帧、布局落定之后再请求。
                    withFrameNanos { }
                    runCatching { requester.bringIntoView() }
                }
            }
        }
}

/**
 * 电视：这一项不允许用左键离开（等价于「这一行最左边」）。
 *
 * 用在整行占满宽度的列表项上：它们左边没有兄弟节点，Compose 的方向搜索会一路
 * 往上找到页面顶部的标签栏，于是「按左就跳到最近播放/首页标签」。
 * 只吞左键，右键不吞 —— 行内右侧往往还有菜单按钮要走。
 */
@Composable
fun Modifier.tvClampLeft(): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft
    }
}

/**
 * 电视：网格里左右两端夹住。
 *
 * [key] 是这个格子自己的 key（`items(key = ...)` 用的那个）。列号在运行时从
 * [state] 的布局信息里查，所以不需要调用方自己算 span/列数。
 *
 * 为什么需要：卡片最右一列再按右，Compose 会往上找候选，常常落到分页器或别的行上，
 * 表现就是「焦点跑出屏幕、整页跟着翻」。
 */
@Composable
fun Modifier.tvGridClampHorizontal(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    key: Any?
): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        if (event.key != Key.DirectionLeft && event.key != Key.DirectionRight) {
            return@onPreviewKeyEvent false
        }
        val layout = state.layoutInfo
        val info = layout.visibleItemsInfo.firstOrNull { it.key == key }
            ?: return@onPreviewKeyEvent false
        if (event.key == Key.DirectionLeft) {
            info.column <= 0
        } else {
            // 最后一列：右边没有别的格子了
            info.column >= layout.visibleItemsInfo.maxOfOrNull { it.column.orZero() }.orZero()
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

/**
 * 电视：这一项不允许用右键离开（等价于「这一行/这一列的最右边」）。
 *
 * 用在行内最右侧的那个按钮上（比如搜索结果里的「更多」）：它是这一行最后一个
 * 可聚焦控件，再按右就会跳出容器，落到分页器上，表现为「焦点出屏、整页翻动」。
 */
@Composable
fun Modifier.tvClampRight(): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight
    }
}
