package moe.ouom.neriplayer.ui.component.navigation

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
 * File: moe.ouom.neriplayer.ui.component.navigation/NeriTvTopBar
 * Created: 2026/10/26
 */

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import moe.ouom.neriplayer.ui.tv.TvPerf
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvTrackFocusWithin

/**
 * 电视导航栏（现在是左侧竖栏）的焦点调度器。
 *
 * 页面本体在 [TvNavigationSidebar]，这里只留焦点相关的东西：
 * 返回键、播放页关闭后的光标归还、`TvReturnFocus` 的顶栏锚点全都直接读
 * `barHasFocus` / `focusedTabRoute` / `requesterFor`，所以这部分不跟着搬。
 */

/**
 * 顶部标签栏的焦点调度中心。
 *
 * 电视上「光标该停在哪」必须由页面状态决定：每次进入一个页面（含返回上一级），
 * 光标都要落到这一页对应的标签上。Compose 默认在焦点节点被移除时会把焦点交给
 * 树里的第一个可聚焦元素——也就是「正在播放」，这正是不希望发生的事，
 * 所以这里给每个格子一个常驻的 [FocusRequester]，由路由变化主动拉焦点。
 */
object TvTopBarFocus {

    /** 「正在播放」格子的伪路由，用来和其他标签区分 */
    const val NOW_PLAYING_ROUTE: String = "__tv_now_playing__"

    private val lock = Any()
    private val requesters = HashMap<String, FocusRequester>()

    fun requesterFor(route: String): FocusRequester = synchronized(lock) {
        requesters.getOrPut(route) { FocusRequester() }
    }

    /**
     * 内容区入口：每个主标签一份。
     *
     * 按标签分开而不是共用一个，是为了在「切页那一拍两个场景同时在」的时候，
     * 落点一定在目标标签自己的场景里，不会钻进正在淡出的那一层。
     */
    private val contentEntryRequesters = HashMap<String, FocusRequester>()

    fun contentEntryRequesterFor(route: String): FocusRequester = synchronized(lock) {
        contentEntryRequesters.getOrPut(route) { FocusRequester() }
    }

    /**
     * 侧栏按右键：把光标送进内容区**第一个可聚焦元素**。
     *
     * 走这条路而不是交给 Compose 的几何方向搜索，是因为几何搜索按「纵向距离最近」选点，
     * 落点取决于每个标签格子恰好对着哪一行卡片，不稳定。
     *
     * 返回 false 表示内容区还没有可聚焦节点（页面还在加载），调用方应退回系统默认搜索，
     * 免得按键被吞掉、光标停在原地。
     */
    fun requestContentEntry(): Boolean {
        val route = currentTabRoute ?: return false
        return runCatching { contentEntryRequesterFor(route).requestFocus() }.getOrDefault(false)
    }

    /**
     * 当前焦点所在的格子路由，焦点离开顶部栏时为 null。
     *
     * 必须是普通 @Volatile 字段，**不能**用 mutableStateOf 包一层：
     * onFocusChanged 回调里要读它来判断"上一次焦点在哪"，而 Compose 的 State 在同一
     * 快照内读到的可能是旧值，会让每次移动焦点都被误判成"焦点补偿"，从而被强行拉回首页
     * （表现为标签栏只有首页能聚焦、按左键到不了「继续播放」）。
     */
    @Volatile
    var focusedTabRoute: String? = null
        private set

    /**
     * 当前页面归属的标签路由。二级页面没有自己的标签，由 [NeriTvTopBar] 按选中态算出来，
     * 用于「失焦后把光标送回本页」时选锚点。
     */
    @Volatile
    var currentTabRoute: String? = null
        private set

    /**
     * 最近一次解析出来的「本页属于哪个标签」，**不停留在 null**。
     *
     * 详情页（歌单 / 歌手 / 专辑）的 route 不属于任何主标签，[currentTabRoute] 会被置空。
     * 一旦置空，下面的焦点补偿就失去了锚点：焦点被系统塞进侧栏时没人把它挪走，
     * 光标就停在标签栏上了。用这个字段当兜底锚点，详情页期间也能把光标收回到
     * 「进入详情页之前那个标签」，和按返回键的语义一致。
     */
    @Volatile
    private var lastResolvedTabRoute: String? = null

    /**
     * 标签栏这一层里**现在**有没有焦点。
     *
     * 不能拿 [focusedTabRoute] 是否为 null 当「焦点在不在标签栏」用：层切换、节点移除时
     * 那个值会失同步，于是返回键会误判成「光标已经在标签栏里」，直接跳页。
     * 这里改读焦点树本身（[tvTrackFocusWithin]），值永远和屏幕上的焦点框一致。
     */
    @Volatile
    var barHasFocus: Boolean = false
        private set

    internal fun onBarFocusChanged(hasFocus: Boolean) {
        barHasFocus = hasFocus
    }

    /**
     * 标签栏里最近一次「某个标签失焦」的时刻。
     *
     * 用来区分两种完全不同的焦点来源：
     *  • 用户按左右键在标签之间移动 → 必然紧跟着一次「旧标签失焦」，间隔只有几毫秒；
     *  • 系统焦点补偿（进新页面、内容里的焦点目标被移除）→ 原焦点在内容区，
     *    标签栏这边没有失焦事件，这个时间戳会很旧。
     *
     * 只有后者才需要把光标挪到本页标签上。
     */
    @Volatile
    private var lastTabBlurAt = 0L

    /**
     * 最近一次「我们主动把光标送到哪个标签」+ 时刻。
     *
     * 用来区分「系统把光标塞进标签栏」和「我们自己指定的落点」：
     * 后者（比如从播放页返回后要求落到「正在播放」）不能被下面的补偿规则拽回本页标签。
     */
    @Volatile
    private var explicitTarget: String? = null
    @Volatile
    private var explicitAt = 0L

    /**
     * 暂时不画「选中底板」。
     *
     * 只在「从歌单/歌手详情返回、播放页即将盖回来」这一小段窗口里置位：
     * 详情页的 route 不属于任何主标签，所以详情页期间顶栏没有底板；按返回时
     * 路由先变回首页、底板立刻亮起，而播放页要晚一拍才盖回来 —— 那一拍就是
     * 用户看到的那道「阴影闪一下」。压住这一拍即可，其它时机一律不动。
     *
     * 必须是 Compose 状态（标签栏要跟着重组），不能像上面几个字段那样用 @Volatile。
     */
    private val selectionPillSuppressed = androidx.compose.runtime.mutableStateOf(false)

    var suppressSelectionPill: Boolean
        get() = selectionPillSuppressed.value
        set(value) {
            selectionPillSuppressed.value = value
        }

    internal fun onTabFocusChanged(route: String, focused: Boolean) {
        val previous = focusedTabRoute
        if (focused) {
            focusedTabRoute = route
            // 详情页 currentTabRoute 是 null，这时退回到「进入详情页之前那个标签」
            val correct = currentTabRoute ?: lastResolvedTabRoute
            val movedByUser =
                android.os.SystemClock.uptimeMillis() - lastTabBlurAt < TAB_MOVE_WINDOW_MS
            // 是不是我们刚主动指定的落点（从播放页返回后落到「正在播放」就是这种）
            val explicit = route == explicitTarget &&
                android.os.SystemClock.uptimeMillis() - explicitAt < TAB_MOVE_WINDOW_MS
            // 光标被系统塞进了标签栏（我们事先并不在标签栏里），且落点不是本页标签
            // → 送回本页标签。用户自己在标签之间移动、或我们主动指定的落点，都不干预。
            if (!movedByUser && !explicit && previous == null && correct != null && route != correct) {
                requestTab(correct)
            }
        } else if (previous == route) {
            focusedTabRoute = null
            lastTabBlurAt = android.os.SystemClock.uptimeMillis()
        }
    }

    /** 标签之间移动时，「旧标签失焦 → 新标签获得焦点」的最大间隔 */
    private const val TAB_MOVE_WINDOW_MS = 250L

    internal fun onCurrentTabResolved(route: String?) {
        currentTabRoute = route
        if (route != null) lastResolvedTabRoute = route
    }

    /** 把光标送到指定标签；节点尚未挂载时静默失败 */
    fun requestTab(route: String): Boolean {
        // 记下这是我们主动定的落点，别让 onTabFocusChanged 的补偿规则再拽回本页标签
        explicitTarget = route
        explicitAt = android.os.SystemClock.uptimeMillis()
        return runCatching { requesterFor(route).requestFocus() }.getOrDefault(false)
    }

    /** 把光标送到当前页面所属的标签；算不出归属时退化成路由本身 */
    fun requestCurrentTab(fallbackRoute: String? = null): Boolean {
        val route = currentTabRoute ?: fallbackRoute ?: return false
        return requestTab(route)
    }
}

internal fun Modifier.tvTabFocusTracker(route: String): Modifier = onFocusChanged { state ->
    TvTopBarFocus.onTabFocusChanged(route, state.isFocused)
}
