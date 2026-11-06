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
 * File: moe.ouom.neriplayer.ui.tv/TvFocusSupport
 * Created: 2026/10/26
 */

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import moe.ouom.neriplayer.ui.theme.NeriDesign
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 是否为电视/遥控器运行环境。电视上没有触摸屏，全部交互都靠方向键 + 确认键，
 * 因此必须有可见的焦点指示，否则用户不知道当前选中的是哪一项。
 */
val LocalIsTelevision = staticCompositionLocalOf { false }

fun Context.isTelevisionDevice(): Boolean {
    val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val uiModeType =
        resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return hasLeanback || uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

@Composable
fun rememberIsTelevision(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isTelevisionDevice() }
}

/**
 * 电视：新开一层窗口（对话框、二级面板）时，系统不会自动把焦点交给里面的控件，
 * 表现就是「点进去之后遥控器按什么都没反应」。
 *
 * 用法：`val initial = rememberTvInitialFocus()`，把它挂到这一层里第一个可操作项，
 * 或者挂到包住整层的容器上（焦点请求会往下找到第一个可聚焦的后代）。
 * 非电视环境返回 null，什么都不做，手机逻辑完全不受影响。
 */
@Composable
fun rememberTvInitialFocus(
    delayFrames: Int = 0,
    /**
     * 页面自己已经有别的抢焦点入口时传 false。
     *
     * 典型场景：详情页的搜索框（`showSearch` 是持久化的），重新进入时搜索框会自己 requestFocus。
     * 这时如果初始焦点也在跑 60 帧重试，两边会互相把光标拽来拽去。
     */
    enabled: Boolean = true
): FocusRequester? {
    if (!LocalIsTelevision.current) return null
    val requester = remember { FocusRequester() }
    val currentEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(requester) {
        if (!currentEnabled) return@LaunchedEffect
        repeat(delayFrames) { withFrameNanos { } }
        // 窗口/布局要过几帧才真正挂上节点，失败就下一帧再试。
        //
        // 原来只试 20 帧（约 330ms）。歌单 / 歌手页首帧可能整个还是加载态，一帧可聚焦节点
        // 都没有，20 帧一过这里就放弃了 —— 而焦点已经在那之前被系统塞给了树里第一个
        // 可聚焦元素（左侧导航栏的「播放」），表现就是「偶尔一进歌单光标就跑到侧栏上」。
        // 拉到 60 帧（约 1 秒）把这个窗口盖住；每帧只有一次 requestFocus，开销可以忽略。
        repeat(60) {
            if (runCatching { requester.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }
    return requester
}

/**
 * 电视：[visible] 从 true 变回 false（对话框 / 子面板关掉）之后，把焦点交还给
 * [requester] 指向的那个按钮。
 *
 * 这些浮层都是独立窗口，关掉之后原来那个窗口里的焦点已经没了，
 * 不主动还给入口按钮的话，遥控器就悬在半空，按什么都没反应。
 */
@Composable
fun TvRestoreFocusWhenClosed(visible: Boolean, requester: FocusRequester?) {
    if (!LocalIsTelevision.current || requester == null) return
    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        val previous = wasVisible
        wasVisible = visible
        if (!previous || visible) return@LaunchedEffect
        // 立刻归还；只有节点确实还没挂上时才等一帧再试。
        // 以前固定等两个 VSync —— 用户在这期间按方向键，刚移开的光标会被稍后那次
        // requestFocus 又拽回原按钮上（「添加到歌单」退出后按左被拉回来就是这个）。
        if (!runCatching { requester.requestFocus() }.getOrDefault(false)) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }
}

/** 挂一个可能为 null 的焦点请求器：null（非电视）时原样返回，不改变布局行为 */
fun Modifier.tvInitialFocus(requester: FocusRequester?): Modifier =
    if (requester != null) {
        this.focusRequester(requester)
    } else {
        this
    }

/**
 * 播放页（全屏浮层）的焦点入口。
 *
 * 注意这里不能靠「给浮层根节点一个 FocusRequester 再 requestFocus」：
 * 根节点必须挂 `canFocus = false` 才能既不当空焦点位、又拦住焦点外逃，
 * 而 canFocus=false 的节点本身不接受焦点请求，requestFocus 会直接失败——
 * 这就是之前播放页方向键完全没反应的原因。
 * 所以把请求器绑到浮层里真实存在的第一个按钮（播放/暂停）上。
 */
object TvNowPlayingFocus {
    val playPause = androidx.compose.ui.focus.FocusRequester()

    /** 右上角「更多」按钮：面板关掉后光标要还回这里 */
    val moreOptions = androidx.compose.ui.focus.FocusRequester()

    /** 底部工具栏的歌词按钮：歌词页/相关面板关掉后光标要还回这里 */
    val lyrics = androidx.compose.ui.focus.FocusRequester()

    /** 右上角「评论」按钮：评论面板关掉后光标要还回这里 */
    val comment = androidx.compose.ui.focus.FocusRequester()

    /** 底部工具栏「播放列表」按钮：队列面板关掉后光标要还回这里 */
    val queue = androidx.compose.ui.focus.FocusRequester()

    /** 底部工具栏「添加到歌单」按钮：加歌单面板关掉后光标要还回这里 */
    val addToPlaylist = androidx.compose.ui.focus.FocusRequester()

    /**
     * 右侧那排播放控制最左边的「随机播放」。
     *
     * 左排最右的「添加到歌单」按右键原本会落到进度条上（进度条横跨整个右栏，
     * 方向搜索里它的位置更「近」），所以把这一对显式连起来。
     */
    val shuffle = androidx.compose.ui.focus.FocusRequester()

    /** 右上角「红心」（收藏）：进度条按上键落到这里，而不是跳到封面上 */
    val favorite = androidx.compose.ui.focus.FocusRequester()

    /** 请求失败（节点没挂载）时静默返回 false，调用方可以退回到播放键 */
    fun request(requester: androidx.compose.ui.focus.FocusRequester): Boolean =
        runCatching { requester.requestFocus() }.getOrDefault(false)
}

/**
 * 观察「这一层（含它的所有子孙）里现在有没有焦点」。
 *
 * 用 [FocusState.hasFocus] 而不是各子项自己上报的 route：子项上报的值在层切换、
 * 节点移除时会失同步（「首页标签背景亮了但焦点框没上去，再按两下就退出应用」
 * 就是拿着一个过期的 route 做判断）。这里只读焦点树，不改任何焦点属性，安全。
 */
@Composable
fun Modifier.tvTrackFocusWithin(onChanged: (Boolean) -> Unit): Modifier {
    val callback by rememberUpdatedState(onChanged)
    return this then TvFocusWithinElement(callback)
}

private data class TvFocusWithinElement(
    val onChanged: (Boolean) -> Unit
) : ModifierNodeElement<TvFocusWithinNode>() {
    override fun create(): TvFocusWithinNode = TvFocusWithinNode(onChanged)
    override fun update(node: TvFocusWithinNode) {
        node.onChanged = onChanged
    }
}

private class TvFocusWithinNode(
    var onChanged: (Boolean) -> Unit
) : Modifier.Node(), FocusEventModifierNode {

    private var last: Boolean? = null

    override fun onFocusEvent(focusState: FocusState) {
        val now = focusState.hasFocus
        if (now == last) return
        last = now
        onChanged(now)
    }
}

/**
 * 取一个「圆角跟组件自己一致」的焦点环 indication。
 *
 * 全局焦点环圆角固定在 12dp，而卡片大多是 8dp —— 差的这 4dp 落在四个角上，
 * 看起来就是「焦点框和卡片边缘对不齐」。组件里这样用：
 *
 * ```
 * val ring = rememberTvFocusRingIndication(8.dp) ?: LocalIndication.current
 * Modifier.combinedClickable(
 *     interactionSource = remember { MutableInteractionSource() },
 *     indication = ring,
 *     onClick = ..., onLongClick = ...
 * )
 * ```
 *
 * @return 电视环境下返回圆角已覆盖的 indication；非电视环境返回 null（调用方退回默认值）
 */
@Composable
fun rememberTvFocusRingIndication(
    radius: Dp,
    /**
     * 环向外扩出节点的距离。
     *
     * 传 0（默认）= 环画在节点边界内侧，这时节点必须比可见内容大一圈
     * （`padding` 放在 `clickable` 之后），否则环会压住内容。
     *
     * 传 >0 = 环画在节点边界**外侧**，节点尺寸一点不用改、内容也不会被压缩，
     * 但要求父容器在四个方向留出至少同样的空间（`contentPadding` / `padding`），
     * 否则超出父级边界的部分会被裁掉。
     */
    outset: Dp = 0.dp,
    /** 环左右再往里收一点（只改环，不改布局） */
    insetHorizontal: Dp = 0.dp,
    /** 环上下再往里收一点 */
    insetVertical: Dp = 0.dp,
    /**
     * 「简洁」风格下这个环额外放大的量。
     *
     * 默认 +8dp。卡片类传 0：卡片的焦点环必须和卡片自身的圆角一致，
     * 否则环在小卡片上会显得又圆又扁（继续播放 / 雷达歌单 / 探索页卡片就是这个毛病）。
     */
    cleanRadiusBoost: Dp = CleanRingRadiusBoost
): Indication? {
    if (!LocalIsTelevision.current) return null
    val current = LocalIndication.current
    if (current !is TvFocusIndication) return null
    // 「简洁」风格是苹果那种大圆角：所有焦点环的圆角统一放大一档，
    // 这样设置页/媒体库/探索/首页各处不用分别配置。
    val forceCleanRadius = LocalTvFocusRingForceCleanRadius.current
    val effectiveRadius = if (NeriDesign.isClean || forceCleanRadius) {
        radius + cleanRadiusBoost
    } else {
        radius
    }
    return remember(current, effectiveRadius, outset, insetHorizontal, insetVertical) {
        current.withRing(
            radius = effectiveRadius,
            outset = outset,
            insetHorizontal = insetHorizontal,
            insetVertical = insetVertical
        )
    }
}

private const val TvRingAlpha = 0.95f

/** 简洁风格下焦点环圆角额外加大多少 */
private val CleanRingRadiusBoost = 8.dp

/** 电视焦点环的默认圆角：简洁风格走苹果那种大圆角，另一主题跟随卡片圆角 */
internal val CleanTvRingRadius = 20.dp
internal val DefaultTvRingRadius = 12.dp

/** 焦点环内缩量：描边居中绘制，不留出内缩就会被父级 clip 切掉四个角 */
private val TvRingInset = 1.dp

/**
 * 电视：这一层里所有焦点环是否强制按「简洁风格」的大圆角来画。
 *
 * 播放页的浮层是一层套一层的自绘面板（一级播放页 / 二级更多面板 / 三级子页），
 * 每个面板各自传 radius，切到另一个主题风格后圆角就会一层一个样。用这个开关把它统一掉。
 */
val LocalTvFocusRingForceCleanRadius = compositionLocalOf { false }

/**
 * 电视：把 [content] 里所有焦点环的圆角统一成「简洁风格」那一档。
 *
 * 只在电视 + 非简洁风格下生效；简洁风格本来就带 boost，不用再叠。
 */
@Composable
fun TvCleanFocusRingRadius(content: @Composable () -> Unit) {
    if (!LocalIsTelevision.current || NeriDesign.isClean) {
        content()
        return
    }
    // 直接把「简洁风格」的环参数搬过来：默认圆角 20dp（另一主题是 12dp）。
    // 不依赖当前 LocalIndication 是什么 —— 这里就是要把整套环换成简洁风格的那一套。
    // 显式传 radius 的地方（rememberTvFocusRingIndication / tvFocusRing）由下面的开关补同样的量。
    val ringColor = rememberUpdatedState(MaterialTheme.colorScheme.primary)
    val cleanRing = remember(ringColor) {
        TvFocusIndication(ringColor, cornerRadius = CleanTvRingRadius)
    }
    CompositionLocalProvider(
        LocalIndication provides cleanRing,
        LocalTvFocusRingForceCleanRadius provides true
    ) {
        content()
    }
}

/**
 * 电视焦点指示：焦点环 + 按下轻微收缩。
 * 通过 [androidx.compose.foundation.LocalIndication] 全局提供给所有 clickable，
 * 这样列表项、卡片、普通按钮都能自动获得可见的焦点。
 */
internal class TvFocusIndication(
    private val ringColor: State<Color>,
    private val cornerRadius: Dp = 16.dp,
    private val strokeWidth: Dp = 2.dp,
    /** 环外扩距离：>0 时环画在节点外侧，节点尺寸不变、内容不被压缩 */
    private val outset: Dp = 0.dp,
    /** 额外左右内收：只让环变窄，布局完全不动 */
    private val insetHorizontal: Dp = 0.dp,
    /** 额外上下内收 */
    private val insetVertical: Dp = 0.dp
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TvFocusIndicationNode(
            interactionSource, ringColor, cornerRadius, strokeWidth,
            outset, insetHorizontal, insetVertical
        )

    /** 复制一份、改圆角 / 外扩 / 内收 */
    internal fun withRing(
        radius: Dp,
        outset: Dp = 0.dp,
        insetHorizontal: Dp = 0.dp,
        insetVertical: Dp = 0.dp
    ): TvFocusIndication =
        TvFocusIndication(
            ringColor = ringColor,
            cornerRadius = radius,
            strokeWidth = strokeWidth,
            outset = outset,
            insetHorizontal = insetHorizontal,
            insetVertical = insetVertical
        )

    /**
     * 只改内收距离、圆角沿用当前值。
     *
     * 给「默认环不能动圆角、只想往里缩一点」的场景用（比如设置行）。
     */
    internal fun withInsets(horizontal: Dp, vertical: Dp): TvFocusIndication =
        TvFocusIndication(
            ringColor = ringColor,
            cornerRadius = cornerRadius,
            strokeWidth = strokeWidth,
            outset = outset,
            insetHorizontal = horizontal,
            insetVertical = vertical
        )

    override fun equals(other: Any?): Boolean =
        other is TvFocusIndication &&
            other.ringColor == ringColor &&
            other.cornerRadius == cornerRadius &&
            other.strokeWidth == strokeWidth &&
            other.outset == outset &&
            other.insetHorizontal == insetHorizontal &&
            other.insetVertical == insetVertical

    override fun hashCode(): Int {
        var result = ringColor.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + strokeWidth.hashCode()
        result = 31 * result + outset.hashCode()
        result = 31 * result + insetHorizontal.hashCode()
        result = 31 * result + insetVertical.hashCode()
        return result
    }
}

private class TvFocusIndicationNode(
    private val interactionSource: InteractionSource,
    private val ringColor: State<Color>,
    private val cornerRadius: Dp,
    private val strokeWidth: Dp,
    private val outset: Dp = 0.dp,
    private val insetHorizontal: Dp = 0.dp,
    private val insetVertical: Dp = 0.dp
) : Modifier.Node(), DrawModifierNode {

    private var focused = false
    private val pressProgress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var activePresses = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> {
                        focused = true
                        invalidateDraw()
                        return@collect
                    }

                    is FocusInteraction.Unfocus -> {
                        focused = false
                        invalidateDraw()
                        return@collect
                    }

                    is PressInteraction.Press -> activePresses++

                    is PressInteraction.Release,
                    is PressInteraction.Cancel ->
                        activePresses = (activePresses - 1).coerceAtLeast(0)

                    else -> return@collect
                }
                val target = if (activePresses > 0) 1f else 0f
                launch {
                    pressProgress.animateTo(
                        targetValue = target,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f)
                    ) { invalidateDraw() }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        drawTvFocusRing(
            cornerRadiusPx = cornerRadius.toPx(),
            color = ringColor.value.copy(alpha = TvRingAlpha),
            strokeWidthPx = strokeWidth.toPx(),
            outsetPx = outset.toPx(),
            insetHorizontalPx = insetHorizontal.toPx(),
            insetVerticalPx = insetVertical.toPx()
        )
    }
}

/**
 * 描边是居中绘制的，直接画在节点边界上会被父级 clip 切掉四个角，
 * 所以整体内缩 [TvRingInset]，圆角半径也按 app 自己的卡片圆角来，跟底板严丝合缝。
 */
private fun ContentDrawScope.drawTvFocusRing(
    cornerRadiusPx: Float,
    color: Color,
    strokeWidthPx: Float,
    outsetPx: Float = 0f,
    /** 额外向内收（左右），用来让环离行首/行尾远一点，不动任何布局 */
    insetHorizontalPx: Float = 0f,
    /** 额外向内收（上下） */
    insetVerticalPx: Float = 0f
) {
    // 正数向内缩（避免被父级 clip 切角），外扩时变成负数，环就画到节点外面去
    val baseInset = TvRingInset.toPx() - outsetPx
    val left = baseInset + insetHorizontalPx
    val top = baseInset + insetVerticalPx
    val width = size.width - left - (baseInset + insetHorizontalPx)
    val height = size.height - top - (baseInset + insetVerticalPx)
    if (width <= 0f || height <= 0f) return
    // 半径只跟基础内缩走：额外的左右/上下内收只把环往里挪，不把圆角磨成直角。
    // （之前用 maxOf(left, top)，左右内收 40dp 时半径被压成 0，四个角就变方了。）
    val radius = (cornerRadiusPx - baseInset).coerceIn(0f, minOf(width, height) / 2f)
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = strokeWidthPx)
    )
}

/**
 * 给显式传了 `indication = null` 的组件（Material3 的 NavigationBarItem、
 * clean 风格 tab 等），以及内部硬写 ripple、读不到 LocalIndication 的组件
 * （Material3 的 IconButton/Button 一族）单独加焦点环。
 */
@Composable
fun Modifier.tvFocusRing(
    color: Color,
    /** 传 `Dp.Unspecified`（默认）= 按节点自身形状贴边：半径取短边的一半，圆形/胶囊都严丝合缝 */
    cornerRadius: Dp = Dp.Unspecified,
    strokeWidth: Dp = 2.dp,
    /** 环左右再往里收（只改环，不改布局） */
    insetHorizontal: Dp = 0.dp,
    /** 环上下再往里收 */
    insetVertical: Dp = 0.dp
): Modifier {
    // 圆角矩形类焦点框：播放页统一按「简洁风格」那一档画（见 TvCleanFocusRingRadius）。
    // 默认值 Dp.Unspecified 表示「贴节点自身形状」（圆形 / 胶囊），不受这里影响。
    val forceCleanRadius = LocalTvFocusRingForceCleanRadius.current
    val effectiveRadius = if (forceCleanRadius && cornerRadius != Dp.Unspecified) {
        cornerRadius + CleanRingRadiusBoost
    } else {
        cornerRadius
    }
    return this then TvFocusRingElement(
        color, effectiveRadius, strokeWidth, insetHorizontal, insetVertical
    )
}

private data class TvFocusRingElement(
    val color: Color,
    val cornerRadius: Dp,
    val strokeWidth: Dp,
    val insetHorizontal: Dp,
    val insetVertical: Dp
) : ModifierNodeElement<TvFocusRingNode>() {

    override fun create(): TvFocusRingNode =
        TvFocusRingNode(color, cornerRadius, strokeWidth, insetHorizontal, insetVertical)

    override fun update(node: TvFocusRingNode) {
        node.ringColor = color
        node.ringCornerRadius = cornerRadius
        node.strokeWidth = strokeWidth
        node.ringInsetHorizontal = insetHorizontal
        node.ringInsetVertical = insetVertical
    }
}

private class TvFocusRingNode(
    var ringColor: Color,
    var ringCornerRadius: Dp,
    var strokeWidth: Dp,
    var ringInsetHorizontal: Dp = 0.dp,
    var ringInsetVertical: Dp = 0.dp
) : Modifier.Node(), DrawModifierNode, FocusEventModifierNode {

    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        val nowFocused = focusState.isFocused
        if (nowFocused == focused) return
        focused = nowFocused
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        // 没指定圆角时按节点自己算：半径 = 短边一半 → 圆按钮、胶囊按钮的环正好贴着边框内侧
        val radiusPx = if (ringCornerRadius.isSpecified) {
            ringCornerRadius.toPx()
        } else {
            minOf(size.width, size.height) / 2f
        }
        drawTvFocusRing(
            cornerRadiusPx = radiusPx,
            color = ringColor.copy(alpha = TvRingAlpha),
            strokeWidthPx = strokeWidth.toPx(),
            insetHorizontalPx = ringInsetHorizontal.toPx(),
            insetVerticalPx = ringInsetVertical.toPx()
        )
    }
}
