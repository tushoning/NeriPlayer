package moe.ouom.neriplayer.ui.playback.visual

import moe.ouom.neriplayer.ui.navigation.LocalPredictiveBackPreviewEnabled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.StateFlow
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.settings.appearance.UiStyle
import moe.ouom.neriplayer.ui.component.common.PredictiveDismissHandler
import moe.ouom.neriplayer.ui.component.common.PredictiveDismissState
import moe.ouom.neriplayer.ui.component.common.blockUnderlyingTouches
import moe.ouom.neriplayer.ui.component.common.predictiveDismissTransform
import moe.ouom.neriplayer.ui.component.common.rememberPredictiveDismissState
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.animation.LocalSharedCoverFlight
import moe.ouom.neriplayer.ui.animation.SharedCoverFlightState
import moe.ouom.neriplayer.util.media.RetainedPlaybackCoverBitmapCache
import moe.ouom.neriplayer.ui.theme.NeriTheme
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.TvCleanFocusRingRadius
import moe.ouom.neriplayer.ui.tv.TvNowPlayingFocus
import moe.ouom.neriplayer.ui.tv.TvReturnFocus
import moe.ouom.neriplayer.ui.util.hasKnownEmptyLocalCover
import moe.ouom.neriplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP

internal data class NowPlayingOverlayCover(
    val url: String?,
    val songKey: String?,
    val song: SongItem?,
    val assetRefreshKey: Int,
    val assetRefresh: NowPlayingCoverAssetRefresh? = null
) {
    val clearCover: Boolean get() = song.hasKnownEmptyLocalCover()
}

internal data class NowPlayingOverlayTheme(
    val dynamicColorEnabled: Boolean,
    val activeCoverSeedHex: String?,
    val seedColorHex: String,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val uiStyle: UiStyle = UiStyle.Material,
    val liquidGlass: Boolean = false
)

internal data class NowPlayingThemeSelection(
    val seedColorHex: String,
    val useSystemDynamic: Boolean
)

internal fun selectNowPlayingOverlayTheme(
    theme: NowPlayingOverlayTheme,
    coverUrl: String?
): NowPlayingThemeSelection = NowPlayingThemeSelection(
    seedColorHex = if (theme.dynamicColorEnabled) {
        theme.activeCoverSeedHex ?: theme.seedColorHex
    } else {
        theme.seedColorHex
    },
    useSystemDynamic = theme.dynamicColorEnabled &&
        theme.activeCoverSeedHex == null && coverUrl == null
)

internal data class NowPlayingOverlayBackground(
    val blurEnabled: Boolean,
    val blurAmount: Float,
    val blurDarken: Float,
    val dynamicEnabled: Boolean,
    val offlineMode: Boolean
)

internal fun releaseNowPlayingInputSession(
    focusManager: FocusManager,
    textToolbar: TextToolbar,
    keyboardController: SoftwareKeyboardController?
) {
    // 先结束底层输入会话，再关闭独立的系统选择菜单和键盘
    focusManager.clearFocus(force = true)
    textToolbar.hide()
    keyboardController?.hide()
}

internal fun resolveNowPlayingOverlayArtwork(
    songKey: String?,
    flight: SharedCoverFlightState?,
    clearCover: Boolean = false
): ImageBitmap? {
    if (clearCover) {
        flight?.clearArtwork()
        return null
    }
    return RetainedPlaybackCoverBitmapCache.getLatestForOwner(songKey)?.bitmap
        ?: flight?.miniCoverBitmap
}

internal fun shouldUseNowPlayingContainerMorph(
    smallestScreenWidthDp: Int
): Boolean = smallestScreenWidthDp < PHONE_SMALLEST_SCREEN_WIDTH_DP

/** 手机 fallback 分支用的动画时长（电视已改成直接切换，不再需要这一组常量） */
private const val NowPlayingOverlayEnterFadeDurationMs = 150
private const val NowPlayingOverlayExitFadeDurationMs = 150

@Composable
internal fun AppNowPlayingOverlay(
    visible: Boolean,
    cover: NowPlayingOverlayCover,
    queueFlow: StateFlow<List<SongItem>>,
    theme: NowPlayingOverlayTheme,
    background: NowPlayingOverlayBackground,
    onVisibilityChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    dismissState: PredictiveDismissState = rememberPredictiveDismissState(),
    content: @Composable () -> Unit
) {
    val latestOnVisibilityChanged by rememberUpdatedState(onVisibilityChanged)
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTelevision = LocalIsTelevision.current
    // 电视上覆盖层一出来就得把光标抢进来，否则方向键还在下面那层看不见的界面上跑。
    // 请求器必须绑在浮层里真实存在的按钮上（播放/暂停），不能绑在浮层根节点上：
    // 根节点挂的是 canFocus=false，那种节点不接受焦点请求。
    //
    // 顺序不能拆成两个 effect：它们在同一帧依次执行，先抢焦点、后 clearFocus，
    // 抢到的那一格会被 clearFocus(force = true) 清掉，playPause 又成功返回不再重试，
    // 结果就是「进了播放页方向键完全没反应」。所以必须清完再抢。
    LaunchedEffect(visible, isTelevision) {
        if (!visible) return@LaunchedEffect
        dismissState.reset()
        // 必须赶在 clearFocus 之前记下来源：一旦清掉焦点，
        // 顶栏那格的 focusedTabRoute 也随之变 null，就再也找不到从哪进来的了。
        if (isTelevision) TvReturnFocus.captureNowPlayingSource()
        releaseNowPlayingInputSession(focusManager, textToolbar, keyboardController)
        if (!isTelevision) return@LaunchedEffect
        // 节点要过一次布局才 placement，第一帧常常请求不到：先试，失败才逐帧重试。
        // 第一帧抢到也再补一次（attempt = 1）：封面/歌词那几块常在这一帧重新挂载，
        // 跟着进来的焦点失效会把刚抢到的光标带走。
        repeat(6) { attempt ->
            val ok = TvNowPlayingFocus.request(TvNowPlayingFocus.playPause)
            if (ok && attempt > 0) return@LaunchedEffect
            withFrameNanos { }
        }
    }
    val useContainerMorph = shouldUseNowPlayingContainerMorph(
        smallestScreenWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    )
    val sharedCoverFlight = LocalSharedCoverFlight.current
    val overlayBody: @Composable () -> Unit = {
        DisposableEffect(Unit) {
            latestOnVisibilityChanged(true)
            onDispose {
                // 任何没走 onClose / onNavigateUp 的收起路径（例如收完立刻导航）：
                // 在这里交还焦点，赶在帧末焦点失效兜底之前 —— onDispose 在 applyChanges
                // 派发阶段，早于 onEndApplyChanges 的焦点事务。
                if (isTelevision) TvReturnFocus.restoreNowPlayingSourceNow()
                latestOnVisibilityChanged(false)
            }
        }
        val themeSelection = selectNowPlayingOverlayTheme(theme, cover.url)
        NeriTheme(
            followSystemDark = false,
            forceDark = true,
            dynamicColor = themeSelection.useSystemDynamic,
            seedColorHex = themeSelection.seedColorHex,
            paletteStyle = theme.paletteStyle,
            colorSpec = theme.colorSpec,
            uiStyle = theme.uiStyle,
            liquidGlass = theme.liquidGlass,
            // text sits on the cover-tinted backdrop, so keep the cover palette instead of greys
            neutralSurfaces = false
        ) {
            PredictiveDismissHandler(state = dismissState, onDismiss = onClose)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // phone morphs own the back preview, while tablets keep the default transform
                    .then(if (useContainerMorph) Modifier else Modifier.predictiveDismissTransform(dismissState))
                    .blockUnderlyingTouches()
                    // 电视：焦点进入这个盒子（会被转交给第一个可聚焦的控件），
                    // 并且不允许再往外跑，避免方向键把光标送回浮层下面
                    .then(
                        if (isTelevision) {
                            Modifier
                                .focusProperties {
                                    // 这一层只当「焦点边界」用，本身不是可聚焦目标，
                                    // 免得方向键在浮层里停在一个看不见的空位上。
                                    // 真正的落焦由内部播放/暂停按钮上的 requestFocus 完成。
                                    canFocus = false
                                    // 方向键想跑出浮层时原地取消，避免焦点回到下面看不见的界面。
                                    // 收起过程中放开，好让焦点交还给顶部标签栏。
                                    // 程序化交还焦点（关闭时送回来源行）也不能被拦掉。
                                    onExit = {
                                        if (visible && !TvReturnFocus.isRestoringSource) {
                                            cancelFocusChange()
                                        }
                                    }
                                }
                                .focusTarget()
                        } else {
                            Modifier
                        }
                    )
            ) {
                NowPlayingBackdrop(
                    NowPlayingBackdropRequest(cover, queueFlow, background)
                )

                CompositionLocalProvider(LocalMiniPlayerHeight provides 0.dp) {
                    // 电视：这一层里的焦点环圆角统一按「简洁风格」那一档画。
                    // 播放页的浮层是一级套一级的自绘面板，各自传各自的圆角，
                    // 换个主题风格就会一层一个样。
                    TvCleanFocusRingRadius {
                        content()
                    }
                }
            }
        }
    }
    // 电视：不进「容器变形」（那是从底部迷你播放器的胶囊长成整页，电视上没有迷你播放器，
    // 起点对不上），也不做滑入/淡入 —— 整屏页面切换，直接出现、直接消失最干脆。
    if (isTelevision) {
        // visible 判断不能省：overlayBody 里的 DisposableEffect 卸载时才回调
        // onVisibilityChanged(false)，那是 NeriApp 里 nowPlayingOverlayMounted 的唯一来源。
        // 少了它这一层永远"挂载中"，电视的返回键会被 BackHandler 一直挡住。
        if (visible) overlayBody()
        return
    }
    if (useContainerMorph) {
        val backPreviewEnabled = LocalPredictiveBackPreviewEnabled.current
        val morph = rememberNowPlayingMorphState(
            visible = visible,
            flight = sharedCoverFlight,
            artwork = { resolveNowPlayingOverlayArtwork(cover.songKey, sharedCoverFlight, cover.clearCover) },
            backProgress = {
                if (backPreviewEnabled) {
                    FastOutLinearInEasing.transform(dismissState.progress.coerceIn(0f, 1f))
                } else 0f
            }
        )
        if (morph.mounted) {
            NowPlayingContainerMorph(
                morph = morph,
                flight = sharedCoverFlight,
                capsuleInWindow = { sharedCoverFlight?.miniPlayerContainerBounds },
                cardColor = remember(theme.activeCoverSeedHex) {
                    nowPlayingMorphCardColor(theme.activeCoverSeedHex)
                },
                content = overlayBody
            )
        }
        return
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        overlayBody()
    }
}
