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
import moe.ouom.neriplayer.ui.tv.TvFocusGate
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvFocusGate
import moe.ouom.neriplayer.ui.tv.tvTrackFocusWithin

/**
 * 电视用顶部标签栏。
 *
 * 电视机没有触摸屏，底部标签栏会掉进滚动容器的焦点序列里，遥控器根本到不了，
 * 所以把它挪到屏幕上沿并固定，压成一条左对齐的窄带，不浪费垂直空间。
 *
 * 第一个格子是「正在播放」入口，用歌曲列表里那套频谱柱表示播放状态，
 * 播放时柱体动、暂停时静止。
 */
internal val TvTopBarHeight = 58.dp
private val TvTopBarItemHeight = 44.dp
// 主标签的选中底板是胶囊形（不是 12dp 圆角矩形）：高度 44dp → 半径取一半
private val TvTopBarShape = RoundedCornerShape(percent = 50)
/** 标签高度的一半，作为胶囊焦点环的半径 */
private val TvTopBarItemRadius = TvTopBarItemHeight / 2

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

    internal fun onTabFocusChanged(route: String, focused: Boolean) {
        val previous = focusedTabRoute
        if (focused) {
            focusedTabRoute = route
            val correct = currentTabRoute
            val movedByUser =
                android.os.SystemClock.uptimeMillis() - lastTabBlurAt < TAB_MOVE_WINDOW_MS
            // 光标被系统塞进了标签栏（我们事先并不在标签栏里），且落点不是本页标签
            // → 送回本页标签。用户自己在标签之间移动的情况不干预。
            if (!movedByUser && previous == null && correct != null && route != correct) {
                requestTab(correct)
            }
        } else if (previous == route) {
            focusedTabRoute = null
            lastTabBlurAt = android.os.SystemClock.uptimeMillis()
        }
        android.util.Log.e(
            "NERI_TV_FOCUS",
            "tab=$route focused=$focused prev=$previous " +
                "=> focused=$focusedTabRoute current=$currentTabRoute"
        )
    }

    /** 标签之间移动时，「旧标签失焦 → 新标签获得焦点」的最大间隔 */
    private const val TAB_MOVE_WINDOW_MS = 250L

    internal fun onCurrentTabResolved(route: String?) {
        currentTabRoute = route
        android.util.Log.e("NERI_TV_FOCUS", "currentTabRoute -> $route")
    }

    /** 把光标送到指定标签；节点尚未挂载时静默失败 */
    fun requestTab(route: String): Boolean {
        // 标签栏在横向按键期间是「关闸」状态（canFocus=false），程序化拉焦点必须先开闸
        TvFocusGate.open()
        return runCatching { requesterFor(route).requestFocus() }.getOrDefault(false)
    }

    /** 把光标送到当前页面所属的标签；算不出归属时退化成路由本身 */
    fun requestCurrentTab(fallbackRoute: String? = null): Boolean {
        val route = currentTabRoute ?: fallbackRoute ?: return false
        return requestTab(route)
    }
}

private fun Modifier.tvTabFocusTracker(route: String): Modifier = onFocusChanged { state ->
    TvTopBarFocus.onTabFocusChanged(route, state.isFocused)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NeriTvTopBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    hasCurrentSong: Boolean,
    isPlaying: Boolean,
    onItemSelected: (Destinations) -> Unit,
    onOpenNowPlaying: () -> Unit,
    /** 已经停在某个标签上再按一次确定键：按路由分发给各页（刷新 / 回到顶部） */
    onTabConfirm: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val isTelevision = LocalIsTelevision.current
    // 标签字号与页面里的分类标题（私人雷达、雷达猜歌那些）对齐
    val labelStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold
    )
    // 二级页面没有自己的标签，按选中态反推它挂在哪个主标签下，
    // 失焦兜底时用它当锚点
    val selectedTabRoute = items
        .firstOrNull { (dest, _) -> isNeriNavigationDestinationSelected(currentDestination, dest) }
        ?.first?.route
    SideEffect { TvTopBarFocus.onCurrentTabResolved(selectedTabRoute) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TvTopBarHeight)
            // 内容区按左/右键时不许跳到标签栏上来（只能按上键进标签栏）
            .tvFocusGate()
            .tvTrackFocusWithin(TvTopBarFocus::onBarFocusChanged)
            .padding(start = pageSidePaddingOr(20.dp), end = pageSidePaddingOr(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TvNowPlayingTab(
            hasCurrentSong = hasCurrentSong,
            isPlaying = isPlaying,
            labelStyle = labelStyle,
            onClick = {
                context.performHapticFeedback()
                onOpenNowPlaying()
            },
            modifier = Modifier
                .then(
                    if (isTelevision) {
                        Modifier
                            .focusRequester(TvTopBarFocus.requesterFor(TvTopBarFocus.NOW_PLAYING_ROUTE))
                            .tvTabFocusTracker(TvTopBarFocus.NOW_PLAYING_ROUTE)
                    } else {
                        Modifier
                    }
                )
        )

        items.forEach { (dest, icon) ->
            val selected = isNeriNavigationDestinationSelected(currentDestination, dest)
            val label = stringResource(dest.labelResId)
            val contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant
            // 焦点环按胶囊画：半径 = 节点短边一半（44dp 高 → 22dp 精确半圆）。
            // 用默认指示器是 16dp 圆角矩形，两端看着「平」。
            val tabRing = rememberTvFocusRingIndication(radius = TvTopBarItemRadius)
                ?: LocalIndication.current
            Row(
                modifier = Modifier
                    .height(TvTopBarItemHeight)
                    .then(
                        if (isTelevision) {
                            Modifier
                                .focusRequester(TvTopBarFocus.requesterFor(dest.route))
                                .tvTabFocusTracker(dest.route)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        color = if (selected) colors.primary.copy(alpha = 0.22f) else Color.Transparent,
                        shape = TvTopBarShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = tabRing
                    ) {
                        context.performHapticFeedback()
                        // 已经停在这个标签上时，再按一次确定键当成「刷新 + 回到顶部」
                        // （电视上没有位置再放一个悬浮刷新按钮，挂在同名标签上是唯一不占地方的做法）
                        if (selected) {
                            onTabConfirm(dest.route)
                        } else {
                            onItemSelected(dest)
                        }
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) colors.primary else contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    color = contentColor,
                    style = labelStyle,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 「正在播放」格子不跟着页面走，所以不画选中底板（继续播放 / 播放界面两个状态本来也没有），
 * 只用频谱柱的颜色和文字颜色表示「有歌在播」。
 */
@Composable
private fun TvNowPlayingTab(
    hasCurrentSong: Boolean,
    isPlaying: Boolean,
    labelStyle: androidx.compose.ui.text.TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val active = hasCurrentSong && isPlaying
    val label = when {
        !hasCurrentSong -> "播放界面"
        isPlaying -> "正在播放"
        else -> "继续播放"
    }
    val tabRing = rememberTvFocusRingIndication(radius = TvTopBarItemRadius)
        ?: LocalIndication.current
    Row(
        modifier = modifier
            .height(TvTopBarItemHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tabRing,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TvNowPlayingBars(
            color = if (active) colors.primary else colors.onSurfaceVariant,
            animate = active
        )
        Text(
            text = label,
            color = if (active) colors.onSurface else colors.onSurfaceVariant,
            style = labelStyle,
            maxLines = 1
        )
    }
}

/**
 * 三根频谱柱：播放时起伏，暂停时是一组静止的柱体。
 *
 * 静止状态下不能只是把动画值「冻住」——只要还读着无限动画的 state，整个标签栏
 * 就会每帧重组一次，电视那种弱 SoC 上白烧 CPU。所以暂停时直接换成一份静态的、
 * 完全不读动画的绘制。
 */
@Composable
private fun TvNowPlayingBars(color: Color, animate: Boolean) {
    // 电视：顶栏是常驻的，频谱柱一直动就等于一直重绘，直接静态化
    if (!animate || TvPerf.reduceMotion) {
        TvNowPlayingBarsRow(color = color, heights = StaticBarHeights)
        return
    }
    val transition = rememberInfiniteTransition(label = "tv_now_playing")
    val wave1 = transition.animateFloat(
        initialValue = 7f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val wave2 = transition.animateFloat(
        initialValue = 11f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val wave3 = transition.animateFloat(
        initialValue = 9f,
        targetValue = 17f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing, delayMillis = 90),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    TvNowPlayingBarsRow(color = color, heights = floatArrayOf(wave1.value, wave2.value, wave3.value))
}

private val StaticBarHeights = floatArrayOf(9f, 13f, 10f)

@Composable
private fun TvNowPlayingBarsRow(color: Color, heights: FloatArray) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(22.dp)
    ) {
        heights.forEachIndexed { index, barHeight ->
            if (index > 0) Spacer(Modifier.width(3.dp))
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}
