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
 * File: moe.ouom.neriplayer.ui.component.navigation/TvNavigationSidebar
 * Created: 2026/11/04
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvTrackFocusWithin
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 电视左侧导航栏。
 *
 * 原来是一条贴着屏幕上沿的横栏，现在改成竖栏：电视的宽高比是 16:9，横向那一行白白吃掉
 * 58dp 高度；竖过来后左右方向留给内容，上下方向用来放时钟和「播放」入口。
 *
 * 焦点调度仍然交给 [TvTopBarFocus] —— 返回键、播放页关闭后的光标归还、`TvReturnFocus`
 * 的顶栏锚点全都直接读它的 `barHasFocus` / `focusedTabRoute`，换名字会把这些一起打断。
 */
internal val TvSidebarWidth = 55.dp

/** 55 减去左右各 8dp：底板不贴边，图标 30dp 居中后两侧还剩 4.5dp */
private val TvSidebarItemWidth = 39.dp

/**
 * 单个标签的高度。
 *
 * 5 项 + 4 个间距 + 时钟 + 上下各 16dp 内边距 ≈ 502dp，默认 540dp 和 1.05 倍缩放都放得下；
 * 界面缩放调到 1.1 倍（可用高度≈约 490dp）时底部会被切掉一截。
 */
private val TvSidebarItemHeight = 78.dp
private val TvSidebarItemSpacing = 10.dp
private val TvSidebarIconSize = 30.dp

/** 选中底板：整块标签区域一块，39 宽 × 78 高，左右窄、上下长 */
private val TvSidebarItemShape = RoundedCornerShape(16.dp)

/** 选中底板底色：主色压到 22%，深色/浅色主题下都能透出图标轮廓 */
private const val TvSidebarSelectedAlpha = 0.22f

/** 焦点环圆角：必须跟底板圆角一致，否则简洁风格下会出现「内方外圆」 */
private val TvSidebarRingRadius = 16.dp

/** 标签文字：13sp 介于 labelMedium(12) 与 labelLarge(14) 之间，12sp 在 3 米外偏小 */
private val TvSidebarLabelSize = 13.sp

/** 时钟：`HH` 是 00–23，天生 24 小时制，不受 Locale 的 12/24 习惯影响 */
private val TvClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 时钟基准字号；实际绘制字号由 [TvSidebarClock] 按「正好占满底板宽度」实测放大 */
private val TvSidebarClockSize = 14.sp

@Composable
fun TvNavigationSidebar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    hasCurrentSong: Boolean,
    isPlaying: Boolean,
    onItemSelected: (Destinations) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTabConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 二级页面没有自己的标签，按选中态反推它挂在哪个主标签下，
    // 失焦兜底和返回键都用它当锚点
    val selectedTabRoute = items
        .firstOrNull { (dest, _) -> isNeriNavigationDestinationSelected(currentDestination, dest) }
        ?.first?.route
    SideEffect { TvTopBarFocus.onCurrentTabResolved(selectedTabRoute) }

    Column(
        modifier = modifier
            .width(TvSidebarWidth)
            .fillMaxHeight()
            .tvTrackFocusWithin(TvTopBarFocus::onBarFocusChanged)
            // 按右：直接送进内容区第一个可聚焦元素。
            // 不这么做就会被系统按「纵向距离最近」的几何搜索结果决定落点，
            // 落点取决于标签格子恰好对着哪一行卡片，很不稳定。
            // 内容区还没挂上可聚焦节点时返回 false，交给系统默认处理，按键不会被吞掉。
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionRight) {
                    return@onPreviewKeyEvent false
                }
                TvTopBarFocus.requestContentEntry()
            }
            .padding(vertical = 16.dp),
        // 标签在标签栏内居左（底板贴左边缘，右侧留白）
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TvSidebarItemSpacing)
    ) {
        TvSidebarClock(modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        // 时钟留在最上沿，标签组整体沉到底部，中间留空
        Spacer(Modifier.weight(1f))

        TvNowPlayingEntry(
            hasCurrentSong = hasCurrentSong,
            isPlaying = isPlaying,
            onClick = {
                context.performHapticFeedback()
                onOpenNowPlaying()
            }
        )

        items.forEach { (dest, icon) ->
            val selected = isNeriNavigationDestinationSelected(currentDestination, dest)
            // 播放页即将盖回来那一拍要压住底板，否则会看到「底板亮一下再被盖住」
            val showSelectedStyle = selected && !TvTopBarFocus.suppressSelectionPill
            TvSidebarEntry(
                route = dest.route,
                icon = icon,
                selectedIcon = cleanSelectedTabIcon(dest),
                label = stringResource(dest.labelResId),
                showSelectedStyle = showSelectedStyle,
                onClick = {
                    context.performHapticFeedback()
                    // 已经停在这个标签上时，再按一次确定键当成「刷新 + 回到顶部」
                    if (selected) {
                        onTabConfirm(dest.route)
                    } else {
                        onItemSelected(dest)
                    }
                }
            )
        }
    }
}

@Composable
private fun TvSidebarEntry(
    route: String,
    icon: ImageVector,
    selectedIcon: ImageVector?,
    label: String,
    showSelectedStyle: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // 圆角必须跟底板一致，且不能让“简洁”风格再放大一档（默认 +8dp）——
    // 否则焦点环会比底板更圆，看着像「内方外圆」。
    val indicator = rememberTvFocusRingIndication(
        radius = TvSidebarRingRadius,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    // 选中态：整块底板 + 实心图标 + 主色 + 文字变亮加粗
    val accented = showSelectedStyle
    val iconTint = if (accented) colors.primary else colors.onSurfaceVariant
    val textTint = if (accented) colors.onSurface else colors.onSurfaceVariant
    Column(
        modifier = Modifier
            .width(TvSidebarItemWidth)
            .height(TvSidebarItemHeight)
            .focusRequester(TvTopBarFocus.requesterFor(route))
            .tvTabFocusTracker(route)
            // 底板盖住整块（图标 + 文字），形状是竖向的圆角矩形
            .background(
                color = if (accented) colors.primary.copy(alpha = TvSidebarSelectedAlpha)
                else Color.Transparent,
                shape = TvSidebarItemShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = indicator,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (accented) selectedIcon ?: icon else icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(TvSidebarIconSize)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = textTint,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = TvSidebarLabelSize,
                fontWeight = if (accented) FontWeight.Medium else FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

/**
 * 播放入口。三态：
 *  - 没有任何歌曲：「播放」
 *  - 有歌且在播：「正播」，频谱动
 *  - 有歌但暂停：「续播」，频谱静止
 *
 * 不跟路由走，所以不画选中底板。
 */
@Composable
private fun TvNowPlayingEntry(
    hasCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val active = hasCurrentSong && isPlaying
    val label = when {
        !hasCurrentSong -> "播放"
        isPlaying -> "正播"
        else -> "续播"
    }
    val indicator = rememberTvFocusRingIndication(
        radius = TvSidebarRingRadius,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    Column(
        modifier = Modifier
            .width(TvSidebarItemWidth)
            .height(TvSidebarItemHeight)
            .focusRequester(TvTopBarFocus.requesterFor(TvTopBarFocus.NOW_PLAYING_ROUTE))
            .tvTabFocusTracker(TvTopBarFocus.NOW_PLAYING_ROUTE)
            // 封顶：这是侧栏第一格，再往上只有不可聚焦的时钟。
            // 不拦的话方向键会按几何搜索一路找进内容区（探索页会跳到顶部搜索框）。
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = indicator,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 和上面几项图标同大（30×30 画布），5 项的文字基线才对得齐
        Box(Modifier.size(TvNowPlayingBarsSize), contentAlignment = Alignment.Center) {
            TvNowPlayingBars(
                color = if (active) colors.primary else colors.onSurfaceVariant,
                animate = active
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) colors.primary else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = TvSidebarLabelSize,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

/**
 * 侧栏顶部的 24 小时时钟。
 *
 * 每一轮都对齐到「下一分钟」再醒，不逐秒轮询：电视盒子长时间开机，
 * 每秒重组一次整条侧栏是白烧 CPU。`tnum` 让数字等宽，分钟跳动时不会左右抖。
 *
 * 字号不是写死的：14sp 只是基准，真正用的是「实测放大到正好占满底板宽度
 * （[TvSidebarItemWidth] 39dp）」的等比系数。字体、系统字号缩放、密度一变，
 * 手写死字号就会偏，实测才稳。
 */
@Composable
private fun TvSidebarClock(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(LocalTime.now().format(TvClockFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            text = LocalTime.now().format(TvClockFormatter)
            val now = LocalTime.now()
            val millisToNextMinute = 60_000L - (now.second * 1000L + now.nano / 1_000_000L)
            delay(millisToNextMinute.coerceIn(250L, 60_000L))
        }
    }

    val baseStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = TvSidebarClockSize,
        // 关掉字距：缩放字号时字距不跟着变，留 0.5sp 会让实测值线性度变差
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    // 样本固定用「00:00」，tnum 下每分钟宽度一样，量一次就能把整串放大到目标宽度
    val targetWidthPx = with(LocalDensity.current) { TvSidebarItemWidth.toPx() }
    val measurer = rememberTextMeasurer()
    val fittedStyle = remember(baseStyle, targetWidthPx, measurer) {
        val measured = measurer
            .measure(AnnotatedString("00:00"), baseStyle, maxLines = 1)
            .size.width
        if (measured <= 0) baseStyle
        else baseStyle.copy(fontSize = TvSidebarClockSize * (targetWidthPx / measured))
    }

    Text(
        text = text,
        // 宽度锁死成底板宽度、居中：等比放大后即使差零点几 dp 也不会越出底板
        modifier = modifier.width(TvSidebarItemWidth),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = fittedStyle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false
    )
}

/**
 * 三根频谱柱：播放时起伏，暂停时是一组静止的柱体。
 *
 * 形状是「两边高中间低」的对称方块：左右各 4dp、上下各 4dp 留白，墨迹 22×22。
 * 比 Material 图标满格（25dp）小一档：实心柱体的视觉重量本来就比描边图标大，
 * 按同样的满格尺寸会看着比旁边图标胖一圈。
 *
 * 静态↔动态共用一组 [Animatable]：切换时 `LaunchedEffect` 取消旧协程，新动画从
 * `Animatable.value` 的当前值起步，所以两个方向都连续，不会跳形状。
 */
@Composable
private fun TvNowPlayingBars(color: Color, animate: Boolean) {
    val bars = remember { List(3) { Animatable(StaticBarHeights[it]) } }
    LaunchedEffect(animate) {
        if (animate) {
            launch {
                bars[0].animateTo(
                    targetValue = 14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                bars[1].animateTo(
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                bars[2].animateTo(
                    targetValue = 14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, easing = FastOutSlowInEasing, delayMillis = 90),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            // 暂停：从当前起伏位置平滑落回静态形状，而不是瞬间换一套绘制
            bars.forEachIndexed { index, bar ->
                launch {
                    bar.animateTo(
                        targetValue = StaticBarHeights[index],
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }
    TvNowPlayingBarsRow(color = color) { floatArrayOf(bars[0].value, bars[1].value, bars[2].value) }
}

/**
 * 静止态的三根柱高：左 22、中 15、右 22。
 *
 * 22dp 在 30dp 画布里上下各留 4dp；中间比两边低 7dp，形成「两边高中间低」的对称缺口。
 */
private val StaticBarHeights = floatArrayOf(22f, 15f, 22f)

/** 画布与其它标签图标完全一致（[TvSidebarIconSize] = 30dp），切换时不会跳大小 */
private val TvNowPlayingBarsSize = TvSidebarIconSize
private val TvNowPlayingBarWidth = 4.dp
private val TvNowPlayingBarGap = 5.dp

/**
 * 三根柱子用一个 Canvas 画，而不是三个每帧改 `.height()` 的 Box：
 * 改子节点高度会让整条侧栏重新测量 + 重新布局，Canvas 只走绘制阶段。
 *
 * [heights] 传的是 lambda：只在绘制块里读 [Animatable.value]，动画每帧只让绘制失效，
 * 不触发重组。
 */
@Composable
private fun TvNowPlayingBarsRow(color: Color, heights: () -> FloatArray) {
    Canvas(modifier = Modifier.size(TvNowPlayingBarsSize)) {
        val barHeights = heights()
        val barWidth = TvNowPlayingBarWidth.toPx()
        val gap = TvNowPlayingBarGap.toPx()
        val cornerRadius = CornerRadius(1.dp.toPx())
        val centerY = size.height / 2f
        // 三根柱子的总宽比画布窄，必须自己居中；不从 0 开画的话整组会偏到左边
        val totalWidth = barHeights.size * barWidth + (barHeights.size - 1) * gap
        val startX = (size.width - totalWidth) / 2f
        barHeights.forEachIndexed { index, barHeightDp ->
            val barHeight = barHeightDp.dp.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(x = startX + index * (barWidth + gap), y = centerY - barHeight / 2f),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
