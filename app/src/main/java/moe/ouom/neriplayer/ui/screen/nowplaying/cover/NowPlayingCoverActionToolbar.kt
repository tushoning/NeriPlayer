package moe.ouom.neriplayer.ui.screen.nowplaying.cover

import moe.ouom.neriplayer.core.player.audio.icon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.screen.nowplaying.LocalNowPlayingLyricsBoundsTransform
import moe.ouom.neriplayer.ui.screen.nowplaying.PlaybackActionToolbarLayout
import moe.ouom.neriplayer.ui.screen.nowplaying.rememberAudioDeviceInfo
import moe.ouom.neriplayer.ui.screen.nowplaying.resolvePlaybackActionToolbarLayout
import androidx.compose.ui.focus.focusProperties
import moe.ouom.neriplayer.ui.screen.nowplaying.NowPlayingSpeedPitchIcon
import moe.ouom.neriplayer.ui.tv.TvNowPlayingFocus

internal data class NowPlayingCoverToolbarLayoutSpec(
    val wideLandscape: Boolean,
    val compactPortrait: Boolean,
    val docked: Boolean,
    val iconSize: Dp,
    val minimumTouchTarget: Dp,
    val compactHeight: Boolean = false,
    val lyricsAdjustBehavior: Boolean = wideLandscape,
    /**
     * 电视版播放页歌词常驻右栏、没有独立歌词页了，歌词按钮点了没有去处，
     * 所以整颗隐藏掉；手机/平板保持原样。
     */
    val showLyricsButton: Boolean = true,
    /**
     * 指定后按钮按固定间距居中排列，而不是 SpaceEvenly 摊满整行。
     * 电视版用它把这排 6 颗按钮的间距对齐到上方 5 颗播控的间距。
     */
    val rowSpacing: Dp? = null
)

internal fun resolveNowPlayingCoverToolbarLayoutSpec(
    wideLandscape: Boolean,
    compactPortrait: Boolean,
    dockEnabled: Boolean,
    iconSize: Dp,
    minimumTouchTarget: Dp,
    compactHeight: Boolean,
    lyricsAdjustBehavior: Boolean = wideLandscape,
    showLyricsButton: Boolean = true,
    rowSpacing: Dp? = null
): NowPlayingCoverToolbarLayoutSpec = NowPlayingCoverToolbarLayoutSpec(
    wideLandscape = wideLandscape,
    compactPortrait = compactPortrait,
    // 短横屏把高度留给封面和歌词，不使用增加留白的装饰底座
    docked = dockEnabled && !compactHeight,
    iconSize = iconSize,
    minimumTouchTarget = minimumTouchTarget,
    compactHeight = compactHeight,
    lyricsAdjustBehavior = lyricsAdjustBehavior,
    showLyricsButton = showLyricsButton,
    rowSpacing = rowSpacing
)

internal data class NowPlayingCoverToolbarStatus(
    val sleepTimerActive: Boolean,
    val lyricsAvailable: Boolean,
    val lyricsShowing: Boolean,
    val activeColor: Color,
    /** 电视：收藏被挪进工具栏最左侧，状态由这里带进来 */
    val isFavorite: Boolean = false,
    val favoriteEnabled: Boolean = true,
    /** 电视：评论被挪进工具栏最右侧，没有评论源时整颗按钮不出现 */
    val commentAvailable: Boolean = false
)

internal data class NowPlayingCoverToolbarActions(
    val onQueue: () -> Unit,
    val onSleepTimer: () -> Unit,
    val onSpeedPitch: () -> Unit,
    val onLyrics: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    /** 非 null 时在工具栏最左侧插入收藏（电视版把右上角的红心挪下来） */
    val onFavorite: (() -> Unit)? = null,
    /** 非 null 时在工具栏最右侧插入评论 */
    val onComment: (() -> Unit)? = null,
    /** 电视：歌词页/歌词相关面板关掉后，光标要还回这个按钮 */
    val lyricsRequester: FocusRequester? = null,
    /** 电视：播放队列面板关掉后，光标要还回这个按钮 */
    val queueRequester: FocusRequester? = null,
    /** 电视：添加到歌单面板关掉后，光标要还回这个按钮 */
    val addToPlaylistRequester: FocusRequester? = null,
    /** 电视：收藏面板(收藏成功提示)关掉后光标还回这里 */
    val favoriteRequester: FocusRequester? = null,
    /** 电视：评论面板关掉后，光标要还回这个按钮 */
    val commentRequester: FocusRequester? = null
)

internal fun resolveNowPlayingLyricsToolbarAction(
    lyricsAdjustBehavior: Boolean,
    onAdjust: () -> Unit,
    onSwitchPage: () -> Unit
): () -> Unit = if (lyricsAdjustBehavior) onAdjust else onSwitchPage

internal fun shouldAdjustNowPlayingLyricsBehavior(
    isLandscape: Boolean,
    phoneLandscape: Boolean
): Boolean = isLandscape && !phoneLandscape

internal fun nowPlayingLyricsToolbarIcon(lyricsAdjustBehavior: Boolean): ImageVector =
    if (lyricsAdjustBehavior) Icons.Outlined.Tune else Icons.Outlined.LibraryMusic

internal fun nowPlayingLyricsToolbarDescription(
    lyricsAdjustBehavior: Boolean,
    pageSwitchDescription: Int = CoreCommonR.string.lyrics_title
): Int = if (lyricsAdjustBehavior) CoreCommonR.string.lyrics_adjust_behavior else pageSwitchDescription

internal fun toolbarWidthFraction(spec: NowPlayingCoverToolbarLayoutSpec): Float =
    if (spec.wideLandscape && !spec.compactHeight) 0.9f else 1f

private fun toolbarHorizontalInset(spec: NowPlayingCoverToolbarLayoutSpec): Dp =
    if (spec.wideLandscape) 0.dp else toolbarPortraitHorizontalInset(spec.compactPortrait)

private fun toolbarPortraitHorizontalInset(compactPortrait: Boolean): Dp =
    if (compactPortrait) 4.dp else 16.dp

private fun toolbarVerticalInset(wideLandscape: Boolean): Dp = if (wideLandscape) 0.dp else 8.dp

private fun toolbarBottomInset(spec: NowPlayingCoverToolbarLayoutSpec): Dp =
    if (spec.wideLandscape) 0.dp else toolbarPortraitBottomInset(spec.docked)

private fun toolbarPortraitBottomInset(docked: Boolean): Dp = if (docked) 2.dp else 0.dp

internal fun toolbarPreferredPadding(spec: NowPlayingCoverToolbarLayoutSpec): Dp =
    if (spec.compactPortrait || spec.compactHeight) 0.dp else toolbarExpandedPreferredPadding(spec)

private fun toolbarExpandedPreferredPadding(spec: NowPlayingCoverToolbarLayoutSpec): Dp =
    if (spec.docked || spec.wideLandscape) 18.dp else 6.dp

internal fun toolbarRowVerticalPadding(spec: NowPlayingCoverToolbarLayoutSpec): Dp =
    if (spec.compactHeight) 0.dp
    else if (spec.docked || spec.wideLandscape) 12.dp
    else 8.dp

internal fun toolbarRowArrangement(
    layout: PlaybackActionToolbarLayout,
    spec: NowPlayingCoverToolbarLayoutSpec
): Arrangement.Horizontal {
    if (layout.useEqualWidthSlots) return Arrangement.Start
    // 指定了行内间距就居中按固定间距排，别摊满（电视要和上方播控对齐）
    val spacing = spec.rowSpacing
    return if (spacing != null) {
        Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
    } else {
        toolbarSpacedArrangement(spec)
    }
}

private fun toolbarSpacedArrangement(spec: NowPlayingCoverToolbarLayoutSpec): Arrangement.Horizontal =
    if (spec.wideLandscape || spec.docked) Arrangement.SpaceEvenly else Arrangement.SpaceBetween

private fun Modifier.toolbarActionModifier(
    equalWidthSlots: Boolean,
    rowScope: RowScope
): Modifier = with(rowScope) {
    if (equalWidthSlots) this@toolbarActionModifier.weight(1f) else this@toolbarActionModifier
}

private fun toolbarActiveTint(active: Boolean, activeColor: Color, normalColor: Color): Color =
    if (active) activeColor else normalColor

private fun toolbarLyricsTint(
    available: Boolean,
    showing: Boolean,
    activeColor: Color,
    normalColor: Color
): Color = if (available) toolbarActiveTint(showing, activeColor, normalColor)
else normalColor.copy(alpha = 0.38f)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NowPlayingCoverActionToolbar(
    spec: NowPlayingCoverToolbarLayoutSpec,
    status: NowPlayingCoverToolbarStatus,
    actions: NowPlayingCoverToolbarActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(toolbarWidthFraction(spec))
            .then(
                if (spec.wideLandscape) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
            .padding(
                horizontal = toolbarHorizontalInset(spec),
                vertical = toolbarVerticalInset(spec.wideLandscape)
            )
            .padding(bottom = toolbarBottomInset(spec)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NowPlayingCoverToolbarDock(spec, status, actions, sharedTransitionScope, animatedVisibilityScope)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingCoverToolbarDock(
    spec: NowPlayingCoverToolbarLayoutSpec,
    status: NowPlayingCoverToolbarStatus,
    actions: NowPlayingCoverToolbarActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (spec.docked) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
            )
        ) {
            NowPlayingCoverToolbarContent(spec, status, actions, sharedTransitionScope, animatedVisibilityScope)
        }
    } else {
        NowPlayingCoverToolbarContent(spec, status, actions, sharedTransitionScope, animatedVisibilityScope)
    }
}

/**
 * 这一排眼下到底有几颗按钮。
 *
 * 默认的 5 颗是「队列 / 定时 / 倍速 / 歌词 / 加歌单」；电视版拿掉歌词、
 * 两头各加收藏和评论，所以槽位宽度必须按实际数量算，否则会算大。
 */
internal fun resolveNowPlayingCoverToolbarItemCount(
    spec: NowPlayingCoverToolbarLayoutSpec,
    status: NowPlayingCoverToolbarStatus,
    actions: NowPlayingCoverToolbarActions
): Int {
    var count = NowPlayingCoverToolbarDefaultItemCount
    if (!spec.showLyricsButton) count -= 1
    if (actions.onFavorite != null) count += 1
    if (actions.onComment != null && status.commentAvailable) count += 1
    return count.coerceAtLeast(1)
}

internal const val NowPlayingCoverToolbarDefaultItemCount = 5

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingCoverToolbarContent(
    spec: NowPlayingCoverToolbarLayoutSpec,
    status: NowPlayingCoverToolbarStatus,
    actions: NowPlayingCoverToolbarActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layout =
            resolvePlaybackActionToolbarLayout(
                availableWidth = maxWidth,
                preferredHorizontalPadding = toolbarPreferredPadding(spec),
                defaultIconSize = spec.iconSize,
                preferredMinimumTouchTarget = spec.minimumTouchTarget,
                itemCount = resolveNowPlayingCoverToolbarItemCount(spec, status, actions)
            )
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides layout.minimumInteractiveComponentSize
        ) {
            NowPlayingCoverToolbarRow(spec, layout, status, actions, sharedTransitionScope, animatedVisibilityScope)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingCoverToolbarRow(
    spec: NowPlayingCoverToolbarLayoutSpec,
    layout: PlaybackActionToolbarLayout,
    status: NowPlayingCoverToolbarStatus,
    actions: NowPlayingCoverToolbarActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = layout.horizontalPadding,
            vertical = toolbarRowVerticalPadding(spec)
        ),
        horizontalArrangement = toolbarRowArrangement(layout, spec),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val buttonModifier = Modifier.toolbarActionModifier(layout.useEqualWidthSlots, this)
        // 电视版把右上角的红心挪到这一排的最左侧
        actions.onFavorite?.let { onFavorite ->
            NowPlayingFavoriteButton(
                isFavorite = status.isFavorite,
                enabled = status.favoriteEnabled,
                buttonSize = layout.minimumInteractiveComponentSize,
                iconSize = layout.iconSize,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = onFavorite,
                requester = actions.favoriteRequester
            )
        }
        NowPlayingCoverToolbarButton(
            "btn_queue", Icons.AutoMirrored.Outlined.QueueMusic, stringResource(CoreCommonR.string.playlist_queue),
            layout.iconSize,
            if (actions.queueRequester != null) {
                buttonModifier.focusRequester(actions.queueRequester)
            } else {
                buttonModifier
            },
            sharedTransitionScope, animatedVisibilityScope,
            actions.onQueue, LocalContentColor.current
        )
        NowPlayingCoverToolbarButton(
            "btn_timer", Icons.Outlined.Timer, stringResource(CoreCommonR.string.sleep_timer_short),
            layout.iconSize, buttonModifier, sharedTransitionScope, animatedVisibilityScope,
            actions.onSleepTimer,
            toolbarActiveTint(
                status.sleepTimerActive, status.activeColor, LocalContentColor.current
            )
        )
        // 原来的「选择扬声器」换成「倍速与音调」：直接改播放速度 / 音调，和设置里的同一套值。
        NowPlayingCoverToolbarButton(
            "btn_speed_pitch", NowPlayingSpeedPitchIcon, SpeedPitchToolbarDescription,
            layout.iconSize, buttonModifier, sharedTransitionScope, animatedVisibilityScope,
            actions.onSpeedPitch, LocalContentColor.current
        )
        if (spec.showLyricsButton) {
            NowPlayingCoverLyricsButton(
                status, layout.iconSize,
                if (actions.lyricsRequester != null) {
                    buttonModifier.focusRequester(actions.lyricsRequester)
                } else {
                    buttonModifier
                },
                sharedTransitionScope,
                animatedVisibilityScope, actions.onLyrics, spec.lyricsAdjustBehavior
            )
        }
        NowPlayingCoverToolbarButton(
            "btn_add", Icons.AutoMirrored.Outlined.PlaylistAdd, stringResource(CoreCommonR.string.playlist_add_to),
            layout.iconSize,
            if (actions.addToPlaylistRequester != null) {
                buttonModifier
                    .focusRequester(actions.addToPlaylistRequester)
                    // 电视旧版：左排最右的「添加到歌单」按右键直接落到右栏最左的「随机播放」。
                    // 不显式连的话，方向搜索会挑中横跨整个右栏的进度条。
                    // 新版把播控挪到了同一列的上方，这条横线不再成立，交给几何搜索。
                    .focusProperties {
                        if (actions.onComment == null) right = TvNowPlayingFocus.shuffle
                    }
            } else {
                buttonModifier
            },
            sharedTransitionScope, animatedVisibilityScope,
            actions.onAddToPlaylist, LocalContentColor.current
        )
        // 电视版把右上角的评论挪到这一排的最右侧
        actions.onComment?.let { onComment ->
            NowPlayingCommentButton(
                available = status.commentAvailable,
                buttonSize = layout.minimumInteractiveComponentSize,
                iconSize = layout.iconSize,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = onComment,
                requester = actions.commentRequester
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingCoverToolbarButton(
    sharedKey: String,
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    tint: Color
) {
    with(sharedTransitionScope) {
        HapticIconButton(
            onClick = onClick,
            modifier = modifier.sharedBounds(
                rememberSharedContentState(key = sharedKey),
                boundsTransform = LocalNowPlayingLyricsBoundsTransform.current,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ).zIndex(1f)
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingCoverLyricsButton(
    status: NowPlayingCoverToolbarStatus,
    iconSize: Dp,
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    lyricsAdjustBehavior: Boolean
) {
    with(sharedTransitionScope) {
        HapticIconButton(
            onClick = onClick,
            enabled = status.lyricsAvailable,
            modifier = modifier.testTag("nowPlayingLyricsAction").sharedBounds(
                rememberSharedContentState(key = "btn_lyrics"),
                boundsTransform = LocalNowPlayingLyricsBoundsTransform.current,
                animatedVisibilityScope = animatedVisibilityScope,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ).zIndex(1f)
        ) {
            AnimatedContent(targetState = status.lyricsShowing, label = "lyrics_icon") { showing ->
                Icon(
                    imageVector = nowPlayingLyricsToolbarIcon(lyricsAdjustBehavior),
                    contentDescription = stringResource(nowPlayingLyricsToolbarDescription(lyricsAdjustBehavior)),
                    tint = toolbarLyricsTint(
                        status.lyricsAvailable, showing, status.activeColor, LocalContentColor.current
                    ),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/** 工具栏按钮的无障碍描述 */
private const val SpeedPitchToolbarDescription = "倍速与音调"
