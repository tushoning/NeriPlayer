package moe.ouom.neriplayer.ui.screen.nowplaying

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.data.model.settings.lyrics.LyricFontScalePage
import moe.ouom.neriplayer.data.model.settings.playback.PlaybackControlLayoutPreferences
import moe.ouom.neriplayer.data.model.settings.playback.PlaybackControlSize
import moe.ouom.neriplayer.ui.screen.nowplaying.cover.NowPlayingLeadingControls
import moe.ouom.neriplayer.ui.screen.nowplaying.cover.NowPlayingLeadingProgress
import moe.ouom.neriplayer.ui.screen.nowplaying.cover.NowPlayingTrailingControls
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP
import androidx.compose.foundation.layout.requiredWidth

private val NowPlayingCompactLandscapeHeight = 520.dp
/** 电视把横屏底栏整体上抬这么多，焦点环才不会贴着内容区下边缘被切 */
/**
 * 电视上「左侧 5 个辅助按钮」和「右侧 5 个播放控制」共用的行高。
 *
 * 两排按钮的触摸目标都是 48dp，但左边那排外层还有工具栏自己的 padding、
 * 右边那排上面压着进度条，之前两排的中心并不在同一条水平线上。
 * 统一套一个 48dp 的行容器、内容垂直居中，就能保证左右两排严格齐平。
 */
private val NowPlayingTvBottomRowHeight = 48.dp
private val NowPlayingExpandedPlayerPaneMaxWidth = 500.dp
private val NowPlayingCompactPlayerPaneMaxWidth = 340.dp
private val NowPlayingPhoneLandscapeMinimumContentHeight = 240.dp
private val NowPlayingTabletPortraitFixedContentReserve = 480.dp
private val NowPlayingTabletPortraitCoverMaxSize = 336.dp
private val NowPlayingExpandedCoverIdentitySpacing = 20.dp

/**
 * 电视版的左栏宽度。
 *
 * 由下面那排工具按钮反推：6 颗 48dp 的触摸目标 + 5 个间距，
 * 左右再各留 8dp 呼吸。宽度**不再按屏幕比例算** —— 按比例算的时候
 * 按钮会被拉开成一排散点，看着和上方 5 颗播控对不齐。
 */
private val NowPlayingTvToolbarButtonSize = 48.dp
private const val NowPlayingTvToolbarItemCount = 6
internal val NowPlayingTvControlSpacing = 12.dp
internal val NowPlayingTvControlButtonSize = 48.dp
internal val NowPlayingTvPrimaryButtonSize = 56.dp
internal val NowPlayingTvControlIconSize = 26.dp
private val NowPlayingTvPlayerPaneWidth = NowPlayingTvToolbarButtonSize * NowPlayingTvToolbarItemCount +
    NowPlayingTvControlSpacing * (NowPlayingTvToolbarItemCount - 1) + 8.dp * 2
/** 左栏封面的目标边长；实际还会被可用宽度和高度各压一手 */
private val NowPlayingTvCoverSide = 240.dp
private val NowPlayingTvPaneSpacing = 32.dp
private val NowPlayingTvCoverIdentitySpacing = 18.dp
private val NowPlayingTvLyricVerticalInset = 28.dp
private const val NOW_PLAYING_EXPANDED_CONTROLS_SPACER_WEIGHT = 0.12f
private const val NOW_PLAYING_TABLET_LANDSCAPE_COVER_WIDTH_FRACTION = 0.72f
private const val NOW_PLAYING_EXPANDED_TABLET_COVER_WIDTH_FRACTION = 0.78f

internal fun isNowPlayingPhoneLandscape(
    isLandscape: Boolean,
    smallestScreenWidthDp: Int
): Boolean = isLandscape && smallestScreenWidthDp < PHONE_SMALLEST_SCREEN_WIDTH_DP

internal fun isNowPlayingTabletPortrait(
    isLandscape: Boolean,
    smallestScreenWidthDp: Int
): Boolean = !isLandscape && smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP

internal fun shouldUseStandaloneNowPlayingLyricsPage(
    showLyricsScreen: Boolean,
    wideLandscape: Boolean
): Boolean = showLyricsScreen && !wideLandscape

internal fun resolveNowPlayingLyricFontPage(showLyricsScreen: Boolean): LyricFontScalePage =
    if (showLyricsScreen) LyricFontScalePage.LYRICS else LyricFontScalePage.COVER

internal fun resolveNowPlayingPageControlSize(
    preferences: PlaybackControlLayoutPreferences,
    showLyricsScreen: Boolean
): PlaybackControlSize = if (showLyricsScreen) preferences.lyricsSize else preferences.nowPlayingSize

internal data class NowPlayingControlBaseSizes(
    val secondaryButtonSize: Dp,
    val primaryButtonSize: Dp,
    val spacing: Dp,
    val iconSize: Dp
)

internal fun resolveNowPlayingControlBaseSizes(
    phoneLandscape: Boolean,
    wideLandscape: Boolean,
    compactWideLayout: Boolean,
    compactPortrait: Boolean,
    isTelevision: Boolean = false
): NowPlayingControlBaseSizes = when {
    // 电视：遥控器要隔着两米看清，手机横屏那套 36/40dp 太小了。
    // 间距也一起放大，和下面那排 6 个工具按钮用同一个值（NowPlayingTvControlSpacing）。
    isTelevision -> NowPlayingControlBaseSizes(
        secondaryButtonSize = NowPlayingTvControlButtonSize,
        primaryButtonSize = NowPlayingTvPrimaryButtonSize,
        spacing = NowPlayingTvControlSpacing,
        iconSize = NowPlayingTvControlIconSize
    )
    phoneLandscape -> NowPlayingControlBaseSizes(36.dp, 40.dp, 12.dp, 20.dp)
    wideLandscape && compactWideLayout -> NowPlayingControlBaseSizes(42.dp, 46.dp, 18.dp, 24.dp)
    wideLandscape -> NowPlayingControlBaseSizes(46.dp, 50.dp, 22.dp, 24.dp)
    compactPortrait -> NowPlayingControlBaseSizes(42.dp, 42.dp, 12.dp, 24.dp)
    else -> NowPlayingControlBaseSizes(42.dp, 42.dp, 20.dp, 24.dp)
}

internal data class NowPlayingWideLayoutSpec(
    val compactHeight: Boolean,
    val contentHeight: Dp,
    val scrollEnabled: Boolean,
    val playerPaneWidth: Dp,
    val paneSpacing: Dp,
    val sectionSpacing: Dp,
    val coverVerticalPadding: Dp,
    val progressVerticalOffset: Dp,
    val controlAreaVerticalOffset: Dp
)

internal fun resolveNowPlayingWideLayoutSpec(
    availableWidth: Dp,
    availableHeight: Dp,
    phoneLandscape: Boolean = false
): NowPlayingWideLayoutSpec {
    val compactHeight = phoneLandscape || availableHeight < NowPlayingCompactLandscapeHeight
    val contentHeight = nowPlayingWideContentHeight(availableHeight, phoneLandscape)
    val controlAreaOffset = nowPlayingWideControlAreaOffset(phoneLandscape, compactHeight)
    return NowPlayingWideLayoutSpec(
        compactHeight = compactHeight,
        contentHeight = contentHeight,
        scrollEnabled = contentHeight > availableHeight,
        playerPaneWidth = if (compactHeight) {
            minOf(availableWidth * 0.40f, NowPlayingCompactPlayerPaneMaxWidth)
        } else {
            minOf(availableWidth * 0.44f, NowPlayingExpandedPlayerPaneMaxWidth)
        },
        paneSpacing = if (compactHeight) 16.dp else 40.dp,
        sectionSpacing = if (compactHeight) 4.dp else 16.dp,
        coverVerticalPadding = 8.dp,
        progressVerticalOffset = if (compactHeight) controlAreaOffset * 2 else (-24).dp,
        controlAreaVerticalOffset = controlAreaOffset
    )
}

private fun nowPlayingWideContentHeight(availableHeight: Dp, phoneLandscape: Boolean): Dp =
    if (phoneLandscape) maxOf(availableHeight, NowPlayingPhoneLandscapeMinimumContentHeight) else availableHeight

private fun nowPlayingWideControlAreaOffset(phoneLandscape: Boolean, compactHeight: Boolean): Dp = when {
    phoneLandscape -> 0.dp
    compactHeight -> (-4).dp
    else -> (-20).dp
}

internal enum class NowPlayingWideControlSlot { PROGRESS, CONTROLS }

internal data class NowPlayingWideControlPlacement(
    val leading: List<NowPlayingWideControlSlot>,
    val trailing: List<NowPlayingWideControlSlot>
)

internal fun resolveNowPlayingWideControlPlacement(
    controlsAtBottom: Boolean,
    progressAtBottom: Boolean,
    compactHeight: Boolean = false
): NowPlayingWideControlPlacement = NowPlayingWideControlPlacement(
    leading = buildList {
        if (!progressAtBottom) add(NowPlayingWideControlSlot.PROGRESS)
        if (!controlsAtBottom) add(NowPlayingWideControlSlot.CONTROLS)
    },
    trailing = buildList {
        if (!compactHeight || controlsAtBottom) {
            if (progressAtBottom) add(NowPlayingWideControlSlot.PROGRESS)
            if (controlsAtBottom) add(NowPlayingWideControlSlot.CONTROLS)
        }
    }
)

internal data class NowPlayingWideControlOffsets(
    val identity: Dp,
    val progress: Dp,
    val controls: Dp,
    val toolbar: Dp
)

internal fun resolveNowPlayingWideControlOffsets(
    spec: NowPlayingWideLayoutSpec,
    placement: NowPlayingWideControlPlacement
): NowPlayingWideControlOffsets {
    // 宽松横屏把信息和其下控件一起抬高，短横屏顶部没有同样的留白
    val leadingOffset = if (spec.compactHeight) 0.dp else spec.controlAreaVerticalOffset
    val controlsAdjustment = if (spec.compactHeight) 0.dp else 8.dp
    return NowPlayingWideControlOffsets(
        identity = leadingOffset,
        progress = if (NowPlayingWideControlSlot.PROGRESS in placement.trailing) {
            spec.progressVerticalOffset
        } else {
            spec.progressVerticalOffset - spec.controlAreaVerticalOffset + leadingOffset
        },
        controls = (if (NowPlayingWideControlSlot.CONTROLS in placement.trailing) {
            spec.controlAreaVerticalOffset
        } else {
            leadingOffset
        }) + controlsAdjustment,
        toolbar = spec.controlAreaVerticalOffset
    )
}

internal data class NowPlayingWideLyricViewport(
    val offset: Dp,
    val topFadeLength: Dp,
    val bottomFadeLength: Dp
)

internal fun resolveNowPlayingWideLyricViewport(availableHeight: Dp): NowPlayingWideLyricViewport =
    NowPlayingWideLyricViewport(
        offset = minOf(availableHeight * 0.18f, 72.dp),
        topFadeLength = minOf(availableHeight * 0.20f, 132.dp),
        bottomFadeLength = minOf(availableHeight * 0.28f, 220.dp)
    )

internal data class NowPlayingTabletPortraitLayoutSpec(
    val contentWidth: Dp,
    val coverSize: Dp,
    val controlsWidth: Dp,
    val toolbarWidth: Dp
)

internal fun resolveNowPlayingTabletPortraitLayoutSpec(
    availableWidth: Dp,
    availableHeight: Dp
): NowPlayingTabletPortraitLayoutSpec {
    val contentWidth = minOf(availableWidth, 560.dp)
    // 短竖屏优先为控件和歌词留空间，封面只占预留后高度的一半
    val coverHeightBudget = (availableHeight - NowPlayingTabletPortraitFixedContentReserve)
        .coerceAtLeast(0.dp) * 0.5f
    return NowPlayingTabletPortraitLayoutSpec(
        contentWidth = contentWidth,
        coverSize = minOf(
            contentWidth * 0.66f, availableHeight * 0.30f,
            NowPlayingTabletPortraitCoverMaxSize, coverHeightBudget
        ),
        controlsWidth = minOf(contentWidth, 440.dp),
        toolbarWidth = minOf(contentWidth, 400.dp)
    )
}

@Composable
internal fun NowPlayingTabletPortraitLayout(
    controlsAtBottom: Boolean,
    progressAtBottom: Boolean,
    lyricsVisible: Boolean,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier, Dp) -> Unit,
    identity: @Composable () -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
    lyrics: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.testTag("nowPlayingTabletPortraitLayout"),
        contentAlignment = Alignment.TopCenter
    ) {
        val spec = resolveNowPlayingTabletPortraitLayoutSpec(maxWidth, maxHeight)
        val centeredProgress: @Composable () -> Unit = {
            Box(Modifier.width(spec.controlsWidth)) { progress() }
        }
        val centeredControls: @Composable () -> Unit = {
            Box(Modifier.width(spec.controlsWidth), contentAlignment = Alignment.Center) { controls() }
        }
        Column(
            modifier = Modifier.width(spec.contentWidth).fillMaxHeight()
                .testTag("nowPlayingTabletPortraitContent"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topBar()
            Spacer(Modifier.height(12.dp))
            cover(Modifier.fillMaxWidth().height(spec.coverSize), spec.coverSize)
            Spacer(Modifier.height(16.dp))
            identity()
            NowPlayingLeadingProgress(progressAtBottom, false, centeredProgress)
            NowPlayingLeadingControls(controlsAtBottom, centeredControls)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp)
                    .testTag("nowPlayingTabletPortraitLyrics")
            ) {
                if (lyricsVisible) lyrics()
            }
            NowPlayingTrailingControls(
                controlsAtBottom, progressAtBottom, false, centeredProgress, centeredControls
            )
            Box(Modifier.width(spec.toolbarWidth)) { toolbar() }
        }
    }
}

@Composable
internal fun NowPlayingWideLayout(
    controlsAtBottom: Boolean,
    progressAtBottom: Boolean,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    phoneLandscape: Boolean,
    phoneTopActions: @Composable () -> Unit,
    tvTopAction: @Composable () -> Unit = {},
    /** 播控按钮之间的间距，电视左栏用它推导进度条/播控/工具栏的间隙 */
    controlSpacing: Dp = 12.dp
) {
    BoxWithConstraints(modifier.testTag("nowPlayingWideLayout")) {
        // 电视始终走自己这一套：左栏一列到底、歌词常驻右栏。
        // 手机/平板的 phoneLandscape、tabletLandscape 分支保持原样不动。
        if (LocalIsTelevision.current) {
            NowPlayingTvLandscape(
                topBar = topBar,
                tvTopAction = tvTopAction,
                cover = cover,
                identity = identity,
                progress = progress,
                controls = controls,
                toolbar = toolbar,
                lyrics = lyrics,
                controlSpacing = controlSpacing
            )
            return@BoxWithConstraints
        }
        val spec = resolveNowPlayingWideLayoutSpec(maxWidth, maxHeight, phoneLandscape)
        if (phoneLandscape) {
            NowPlayingPhoneLandscape(
                spec, topBar, cover, identity, phoneTopActions,
                progress, controls, toolbar, lyrics
            )
        } else {
            NowPlayingTabletLandscape(
                spec, controlsAtBottom, progressAtBottom, topBar, cover,
                identity, progress, controls, toolbar, lyrics
            )
        }
    }
}

/**
 * 电视版播放页：照搬桌面端那种「左一列、右歌词」的分栏。
 *
 * 左栏是一条平铺的 Column，四段：
 *   ① 顶栏 → 封面（顶边钉死，空间不够只往下缩，上限 240dp）
 *   ② 歌名 / 歌手 —— 固定高度槽位，紧跟封面
 *   ③ 进度条 + 5 颗播控 —— 上下各一个等权 Spacer，落在「歌名」和「6 键」正中间
 *   ④ 6 颗工具按钮 —— 锁在栏底（Column 撑满高度，它就是最后一个子项）
 *
 * 所有尺寸都是 dp，跟随全局「界面缩放」（LocalDensity）一起放大缩小；
 * 封面靠权重 + `fill = false` 吃掉「高度不够」的那部分，避免底部那排被挤出屏幕。
 */
@Composable
private fun NowPlayingTvLandscape(
    topBar: @Composable () -> Unit,
    tvTopAction: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit,
    controlSpacing: Dp
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("nowPlayingTvLayout")) {
        // 宽度直接跟那排工具按钮走，不再按屏幕比例。
        // 兜底系数 0.70：播放页左右各让出 55dp 后，界面缩放到 1.5 倍时可用宽只剩约 498dp，
        // 0.70 × 498 = 348 刚好放得下 6 颗 48dp + 5 个 12dp 间距（348dp），
        // 不会掉进工具栏的紧凑分支把图标缩小。常规 960dp 电视仍被 364dp 上限截住，无影响。
        val playerPaneWidth = minOf(NowPlayingTvPlayerPaneWidth, maxWidth * 0.70f)
        // 进度条与播控之间的间距：跟着播控按钮的间距一起缩放，
        // 而不是写死一个 dp —— 调界面缩放时按钮变大变小，间隙按同一系数跟着变。
        val bottomGap = (controlSpacing * 2f).coerceAtLeast(12.dp)
        // 歌名 / 歌手名槽位高度：歌名行高 + 歌手名节点高度（行高 + 焦点框上下内边距）。
        // 按当前排版字号算，所以跟着界面缩放一起变，不写死成某个 dp；
        // 槽位恒定，歌名歌手怎么换行、入场动画怎么位移，都影响不到上下邻居。
        val identitySlotHeight = with(LocalDensity.current) {
            MaterialTheme.typography.titleLarge.lineHeight.toDp() +
                MaterialTheme.typography.bodyLarge.lineHeight.toDp() +
                NowPlayingArtistFocusVerticalPadding * 2
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(NowPlayingTvPaneSpacing)
        ) {
            TvPlayerPane(
                paneWidth = playerPaneWidth,
                coverIdentitySpacing = NowPlayingTvCoverIdentitySpacing,
                identitySlotHeight = identitySlotHeight,
                groupGap = bottomGap,
                coverMaxSide = NowPlayingTvCoverSide,
                modifier = Modifier.fillMaxHeight().testTag("nowPlayingPlayerPane"),
                topBar = topBar,
                cover = cover,
                identity = { identity(true) },
                progress = progress,
                controls = controls,
                toolbar = { toolbar(true) }
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // 歌词上下留同样的边，当前唱到的那句落在这条竖线的中间：
                // 「下边到屏幕边缘的距离」和「上边到屏幕顶的距离」严格相等。
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(vertical = NowPlayingTvLyricVerticalInset)
                ) {
                    NowPlayingWideLyricPane(
                        compactHeight = false,
                        lyrics = lyrics,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // 「更多」回到原来那一角：右栏右上角，和以前顶部那排的位置一致
                Box(Modifier.align(Alignment.TopEnd)) { tvTopAction() }
            }
        }
    }
}

/**
 * 电视左栏：顶栏 / 封面 / 歌名槽位 / 播控组 / 6 键，一次算好位置。
 *
 * 为什么不用 Column + 权重：[NowPlayingTvCoverSide] 是硬上限，封面吃不满自己那份时，
 * Compose 不会把没用完的空间分给别的兄弟（weight 的 KDoc 明确写了这点），会全堆在末尾。
 * 于是「界面缩放 0.6×、屏幕 dp 高度变大」时封面先顶到 240dp，多出来的几百 dp 全堆到
 * 播控组下面，看上去就是「播控组跑到屏幕中间、上方空白小、下方空白一大片」。
 *
 * 这里改成量一次、算一次、摆一次：
 *   - 封面顶边钉死，边长 = min(栏宽, [coverMaxSide], 剩余高度)，空间不够只往下缩；
 *   - 播控组（进度条 + 间距 + 5 键）落在「歌名槽位底边」与「6 键顶边」的正中间；
 *   - 6 键贴栏底。
 * 三个位置都由实际测量结果算出，跟缩放比例无关，也不产生任何"剩余空间"。
 */
@Composable
private fun TvPlayerPane(
    paneWidth: Dp,
    coverIdentitySpacing: Dp,
    identitySlotHeight: Dp,
    groupGap: Dp,
    coverMaxSide: Dp,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable () -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable () -> Unit
) {
    Layout(
        modifier = modifier.width(paneWidth),
        content = {
            topBar()
            Box { cover(Modifier.fillMaxSize()) }
            // 歌名那一路是 AnimatedVisibility，第一帧还没组合、量到 0。这里把它的高度
            // 框死在槽位上，量出来的永远是同一个值，不会把下面整条链推来推去。
            // 对齐必须显式写 TopCenter：Box 默认 TopStart，内容会靠左排。
            Box(
                modifier = Modifier.fillMaxWidth().height(identitySlotHeight),
                contentAlignment = Alignment.TopCenter
            ) { identity() }
            progress()
            controls()
            toolbar()
        }
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val childConstraints = Constraints(maxWidth = w, maxHeight = h)
        val topBarM = measurables[0].measure(childConstraints)
        val identityM = measurables[2].measure(childConstraints)
        val progressM = measurables[3].measure(childConstraints)
        val controlsM = measurables[4].measure(childConstraints)
        val toolbarM = measurables[5].measure(childConstraints)

        val spacing = coverIdentitySpacing.roundToPx()
        val gap = groupGap.roundToPx()
        // 槽位只认按字号算出来的常量，**不要** maxOf(常量, 实测)：
        // 实测值在第一帧是 0、第二帧才是真实高度，取 max 等于让整页在第二帧挪一下
        // （缩放 ≤0.95 时播控组下沉、>1.0 时封面缩小）。
        val slot = identitySlotHeight.roundToPx()
        val fixed = topBarM.height + spacing + slot +
            progressM.height + gap + controlsM.height + toolbarM.height
        val coverPx = minOf(w, coverMaxSide.roundToPx(), h - fixed).coerceAtLeast(0)
        val coverM = measurables[1].measure(Constraints.fixed(coverPx, coverPx))

        val identityTop = topBarM.height + coverPx + spacing
        val contentBottom = identityTop + slot
        val groupHeight = progressM.height + gap + controlsM.height
        val blank = ((h - toolbarM.height - contentBottom - groupHeight) / 2).coerceAtLeast(0)

        layout(w, h) {
            topBarM.placeRelative(0, 0)
            coverM.placeRelative((w - coverPx) / 2, topBarM.height)
            identityM.placeRelative((w - identityM.width) / 2, identityTop)
            val progressY = contentBottom + blank
            progressM.placeRelative((w - progressM.width) / 2, progressY)
            controlsM.placeRelative(
                (w - controlsM.width) / 2,
                progressY + progressM.height + gap
            )
            toolbarM.placeRelative((w - toolbarM.width) / 2, h - toolbarM.height)
        }
    }
}

@Composable
private fun NowPlayingTabletLandscape(
    spec: NowPlayingWideLayoutSpec,
    controlsAtBottom: Boolean,
    progressAtBottom: Boolean,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit
) {
    val placement = resolveNowPlayingWideControlPlacement(
        controlsAtBottom, progressAtBottom, spec.compactHeight
    )
    val offsets = resolveNowPlayingWideControlOffsets(spec, placement)
    val tabletIdentity: @Composable (Boolean) -> Unit = { compact ->
        Box(Modifier.offset(y = offsets.identity)) { identity(compact) }
    }
    val tabletProgress: @Composable () -> Unit = {
        // 利用上方已有空白抬高进度区，不挤占封面和歌词的测量高度
        Box(Modifier.offset(y = offsets.progress)) { progress() }
    }
    val tabletControls: @Composable () -> Unit = {
        Box(Modifier.offset(y = offsets.controls)) { controls() }
    }
    val tabletToolbar: @Composable (Boolean) -> Unit = { compact ->
        Box(Modifier.offset(y = offsets.toolbar)) { toolbar(compact) }
    }
    if (spec.compactHeight) {
        NowPlayingCompactLandscape(
            spec, placement, topBar, cover, tabletIdentity, tabletProgress, tabletControls, tabletToolbar, lyrics
        )
    } else {
        NowPlayingExpandedLandscape(
            spec, placement, topBar, cover, tabletIdentity, tabletProgress, tabletControls, tabletToolbar, lyrics
        )
    }
}

@Composable
private fun NowPlayingExpandedLandscape(
    spec: NowPlayingWideLayoutSpec,
    placement: NowPlayingWideControlPlacement,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(spec.paneSpacing)
    ) {
        Column(
            modifier = Modifier.width(spec.playerPaneWidth).fillMaxHeight()
                .testTag("nowPlayingPlayerPane"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topBar()
            cover(
                Modifier.width(spec.playerPaneWidth * NOW_PLAYING_EXPANDED_TABLET_COVER_WIDTH_FRACTION)
                    .weight(1f).padding(vertical = spec.coverVerticalPadding)
            )
            Spacer(Modifier.height(NowPlayingExpandedCoverIdentitySpacing))
            identity(false)
            Spacer(Modifier.height(spec.sectionSpacing))
            NowPlayingWideControlSection(placement.leading, progress, controls)
            // 封面多分到一些高度，同时保留 LOWER 与置底控件之间的空白差别
            Spacer(Modifier.weight(NOW_PLAYING_EXPANDED_CONTROLS_SPACER_WEIGHT))
            NowPlayingWideControlSection(placement.trailing, progress, controls)
            Spacer(Modifier.height(spec.sectionSpacing))
            toolbar(false)
        }
        NowPlayingWideLyricPane(
            compactHeight = false,
            lyrics = lyrics,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun NowPlayingWideControlSection(
    slots: List<NowPlayingWideControlSlot>,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit
) {
    slots.forEach { slot -> NowPlayingWideControl(slot, progress, controls) }
}

@Composable
private fun NowPlayingWideControl(
    slot: NowPlayingWideControlSlot,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit
) {
    if (slot == NowPlayingWideControlSlot.PROGRESS) progress() else controls()
}

@Composable
private fun NowPlayingPhoneLandscape(
    spec: NowPlayingWideLayoutSpec,
    backBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    topActions: @Composable () -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit
) {
    // 极短横屏保留封面和操作入口的空间，内容可滚动到完整底栏
    val scrollState = rememberScrollState()
    val viewportModifier = Modifier.fillMaxSize().verticalScroll(scrollState, enabled = spec.scrollEnabled)
    Column(viewportModifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(spec.contentHeight),
            horizontalArrangement = Arrangement.spacedBy(spec.paneSpacing)
        ) {
            Column(
                modifier = Modifier.width(spec.playerPaneWidth).fillMaxHeight()
                    .testTag("nowPlayingPlayerPane"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                backBar()
                cover(Modifier.fillMaxWidth().weight(1f).padding(vertical = spec.coverVerticalPadding))
                // 电视：这一排和右栏那排播放控制要落在同一条水平线上。
                // 两边都套同样高度的行容器、内容垂直居中，行高差/触摸目标差就不会把两排错开。
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .then(
                            if (LocalIsTelevision.current) {
                                Modifier.height(NowPlayingTvBottomRowHeight)
                            } else {
                                Modifier
                            }
                        )
                        .testTag("nowPlayingPhoneAuxiliaryActions"),
                    contentAlignment = Alignment.Center
                ) { toolbar(true) }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().testTag("nowPlayingDetailsPane"),
                horizontalAlignment = Alignment.Start
            ) {
                if (LocalIsTelevision.current) {
                    // 电视：右栏只占屏幕右边那一半，它的中线比屏幕中线右移了半个
                    // 「左栏宽 + 栏间距」。所以在右栏里居中之后，再把内容整体左移这一半，
                    // 中线就正好落回屏幕中线。
                    // 不用固定像素：左栏宽是按屏幕比例算的，后面改界面缩放也一样成立。
                    val centerShift = (spec.playerPaneWidth + spec.paneSpacing) / 2
                    Box(
                        modifier = Modifier.fillMaxWidth().testTag("nowPlayingPhoneHeader"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.offset(x = -centerShift),
                            contentAlignment = Alignment.Center
                        ) { identity(true) }
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd)
                                .testTag("nowPlayingTopActions")
                        ) { topActions() }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("nowPlayingPhoneHeader"),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(Modifier.weight(1f).padding(end = 8.dp)) { identity(true) }
                        Box(Modifier.testTag("nowPlayingTopActions")) { topActions() }
                    }
                }
                Spacer(Modifier.height(spec.sectionSpacing))
                NowPlayingWideLyricPane(
                    compactHeight = true,
                    lyrics = lyrics,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Spacer(Modifier.height(spec.sectionSpacing))
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("nowPlayingBottomControls"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    progress()
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .then(
                                if (LocalIsTelevision.current) {
                                    Modifier.height(NowPlayingTvBottomRowHeight)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) { controls() }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCompactLandscape(
    spec: NowPlayingWideLayoutSpec,
    placement: NowPlayingWideControlPlacement,
    topBar: @Composable () -> Unit,
    cover: @Composable (Modifier) -> Unit,
    identity: @Composable (Boolean) -> Unit,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    toolbar: @Composable (Boolean) -> Unit,
    lyrics: @Composable (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spec.paneSpacing)
        ) {
            Column(
                modifier = Modifier.width(spec.playerPaneWidth).fillMaxHeight()
                    .testTag("nowPlayingPlayerPane"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                topBar()
                cover(
                    Modifier.width(spec.playerPaneWidth * NOW_PLAYING_TABLET_LANDSCAPE_COVER_WIDTH_FRACTION)
                        .weight(1f).padding(vertical = spec.coverVerticalPadding)
                )
                toolbar(true)
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                identity(true)
                Spacer(Modifier.height(spec.sectionSpacing))
                NowPlayingWideControlSection(placement.leading, progress, controls)
                NowPlayingWideLyricPane(
                    compactHeight = true,
                    lyrics = lyrics,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
        if (placement.trailing.isEmpty()) return@Column
        Spacer(Modifier.height(spec.sectionSpacing))
        NowPlayingCompactControlFooter(spec, placement.trailing, progress, controls)
    }
}

@Composable
private fun NowPlayingCompactControlFooter(
    spec: NowPlayingWideLayoutSpec,
    slots: List<NowPlayingWideControlSlot>,
    progress: @Composable () -> Unit,
    controls: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("nowPlayingBottomControls"),
        horizontalArrangement = Arrangement.spacedBy(spec.paneSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEach { slot ->
            // 双区底栏沿用上方分栏，单区底栏仍使用整行
            val slotModifier = if (slots.size > 1 && slot == NowPlayingWideControlSlot.PROGRESS) {
                Modifier.width(spec.playerPaneWidth)
            } else {
                Modifier.weight(1f)
            }
            Box(slotModifier, contentAlignment = Alignment.Center) {
                NowPlayingWideControl(slot, progress, controls)
            }
        }
    }
}

@Composable
private fun NowPlayingWideLyricPane(
    compactHeight: Boolean,
    lyrics: @Composable (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.testTag("nowPlayingLyricPane")) {
        Box(Modifier.fillMaxSize().padding(horizontal = if (compactHeight) 8.dp else 20.dp)) {
            lyrics(compactHeight)
        }
    }
}
