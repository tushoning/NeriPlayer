package moe.ouom.neriplayer.ui.navigation

import moe.ouom.neriplayer.ui.theme.NeriDesign
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.layout.boundsInWindow
import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import moe.ouom.neriplayer.ui.component.navigation.TvTopBarFocus
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.download.processing.ManagedLibraryProcessingCoordinator
import moe.ouom.neriplayer.core.download.ManagedDownloadStorage
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.component.navigation.NeriBottomBar
import moe.ouom.neriplayer.ui.component.navigation.NeriTvTopBar
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvDirectionKeyGate
import moe.ouom.neriplayer.ui.component.navigation.resolveBottomBarSelectionAlpha
import moe.ouom.neriplayer.ui.component.playback.NeriMiniPlayer
import moe.ouom.neriplayer.ui.component.playback.NeriMiniPlayerDefaults
import moe.ouom.neriplayer.ui.component.playback.MiniPlayerTabletControls
import moe.ouom.neriplayer.ui.component.playback.resolvePlaybackWaiting
import moe.ouom.neriplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassBackdrops
import moe.ouom.neriplayer.ui.effect.glass.captureAdvancedGlassBackdrop
import moe.ouom.neriplayer.ui.feedback.AppFeedbackHostEffect
import moe.ouom.neriplayer.ui.feedback.NeriSnackbarHost
import moe.ouom.neriplayer.ui.util.hasKnownEmptyLocalCover
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.BottomBarLayoutInsets
import moe.ouom.neriplayer.ui.banner.MANAGED_LIBRARY_PROCESSING_DRAG_THRESHOLD
import moe.ouom.neriplayer.ui.banner.MANAGED_LIBRARY_PROCESSING_REVEAL_EDGE
import moe.ouom.neriplayer.ui.banner.MANAGED_LIBRARY_PROCESSING_Z_INDEX
import moe.ouom.neriplayer.ui.MINI_PLAYER_OVERLAY_Z_INDEX
import moe.ouom.neriplayer.ui.banner.ManagedLibraryProcessingBanner
import moe.ouom.neriplayer.ui.banner.OfflineModeBottomBanner
import moe.ouom.neriplayer.ui.banner.AppManagedProcessingBannerOwner
import moe.ouom.neriplayer.ui.banner.AppManagedProcessingBannerPresentation
import moe.ouom.neriplayer.ui.banner.managedProcessingRevealGesture

internal data class AppBottomBarPresentation(
    val items: List<Pair<Destinations, ImageVector>>,
    val currentDestination: NavDestination?,
    val showNowPlaying: Boolean,
    val offlineMode: Boolean,
    val alwaysUseNewTabStyle: Boolean,
    val backgroundImageUri: String?
) {
    val hasCustomBackground: Boolean get() = backgroundImageUri != null
}

internal fun shouldShowMiniPlayer(hasSong: Boolean, showNowPlaying: Boolean): Boolean =
    hasSong && !showNowPlaying

internal fun shouldSuppressPlaybackNavigation(
    playbackOpen: Boolean,
    backGestureRevealing: Boolean = false
): Boolean =
    // 底栏和迷你播放器与播放页退出同步恢复，避免两段动画串联等待
    !backGestureRevealing && playbackOpen

internal fun playbackSourceNavigationAction(
    sourceRoute: String?,
    navigate: (String) -> Unit
): (() -> Unit)? = sourceRoute?.let { route -> { navigate(route) } }

internal fun reservedMiniPlayerHeight(hasSong: Boolean, showNowPlaying: Boolean, smallestScreenWidthDp: Int) =
    if (shouldShowMiniPlayer(hasSong, showNowPlaying)) {
        if (smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP) {
            NeriMiniPlayerDefaults.TabletHeight
        } else NeriMiniPlayerDefaults.Height
    } else 0.dp

internal fun appSnackbarStartInset(useNavigationRail: Boolean, playbackOverlayVisible: Boolean) =
    if (useNavigationRail && !playbackOverlayVisible) AppNavigationRailWidth else 0.dp

private fun positiveBottomBarHeight(nextHeight: Int, previousHeight: Int): Int =
    if (nextHeight > 0) nextHeight else previousHeight

/**
 * 电视上焦点整个丢失后，等这么久再把光标拉回顶部标签栏。
 * 留出这段时间是让 Compose 自己的焦点重分配（列表项回收、页面切换）先完成。
 */
private const val TV_FOCUS_RESTORE_DEBOUNCE_MS = 220L

@Composable
internal fun AppNavigationScaffold(
    bottomBar: AppBottomBarPresentation,
    hasCurrentSong: Boolean,
    isPlaying: Boolean,
    baseBlurRequested: Boolean,
    snackbarHostState: SnackbarHostState,
    onMainTabSelected: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onTabConfirm: (String) -> Unit = {},
    content: @Composable (BottomBarLayoutInsets) -> Unit
) {
    val configuration = LocalConfiguration.current
    val dragBoundsOwner = remember { AppDragBoundsOwner() }
    val yieldsToIme = cleanFloatingChromeYieldsToIme()
    // 底部迷你播放条已经删掉，播放入口改为顶部标签栏的第一个格子
    CompositionLocalProvider(LocalMiniPlayerHeight provides 0.dp) {
        AppFeedbackHostEffect(snackbarHostState)
        // 电视机上底栏在滚动容器下面，遥控器永远走不到，改由顶部固定标签栏承担导航
        val isTelevision = LocalIsTelevision.current
        // 电视上这里**不做任何自动拉焦点**。
        // 之前的实现会在失焦后「请求本页标签 → moveFocus(Down)」，效果是光标被
        // 反复拽回页面第一个按钮（很多时候就是「正在播放」），按一下方向键就跳走。
        // 现在只让页面自己决定落点：路由切换由 NeriApp 校准一次，控件消失由打开
        // 该控件的页面自己把光标还回去。
        BoxWithConstraints(Modifier.fillMaxSize().tvDirectionKeyGate()) {
            val useNavigationRail =
                !isTelevision && shouldUseAppNavigationRail(configuration.smallestScreenWidthDp, maxWidth)
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                topBar = {
                    if (isTelevision) {
                        NeriTvTopBar(
                            items = bottomBar.items,
                            currentDestination = bottomBar.currentDestination,
                            hasCurrentSong = hasCurrentSong,
                            isPlaying = isPlaying,
                            onItemSelected = { onMainTabSelected(it.route) },
                            onOpenNowPlaying = onOpenNowPlaying,
                            onTabConfirm = onTabConfirm
                        )
                    }
                },
                snackbarHost = {
                    NeriSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(
                            start = appSnackbarStartInset(useNavigationRail, bottomBar.showNowPlaying)
                        ),
                        bottomPadding = LocalMiniPlayerHeight.current
                    )
                },
                bottomBar = {
                    if (!useNavigationRail && !isTelevision) {
                        AppScaffoldBottomBar(
                            bottomBar = bottomBar,
                            yieldsToIme = yieldsToIme,
                            onMainTabSelected = onMainTabSelected,
                            onTabBarBoundsChanged = dragBoundsOwner::updateTabBarBounds
                        )
                    }
                }
            ) { innerPadding ->
                val layoutInsets = resolveAppNavigationLayoutInsets(
                    useNavigationRail = useNavigationRail,
                    baseBlurRequested = baseBlurRequested,
                    bottomBarInset = innerPadding.calculateBottomPadding().coerceAtLeast(0.dp),
                    reservedMiniPlayerHeight = 0.dp
                )
                val dragBounds = dragBoundsOwner.visibleBounds(
                    false,
                    bottomBar.showNowPlaying || yieldsToIme
                )
                CompositionLocalProvider(
                    LocalMiniPlayerHeight provides layoutInsets.screenBottomInset,
                    LocalMiniPlayerBoundsInRoot provides dragBounds.miniPlayer,
                    LocalBottomTabBarBoundsInRoot provides dragBounds.bottomTabBar.takeUnless {
                        useNavigationRail || isTelevision
                    }
                ) {
                    AppAdaptiveNavigationContent(
                        useNavigationRail = useNavigationRail,
                        showNowPlaying = bottomBar.showNowPlaying,
                        offlineMode = bottomBar.offlineMode,
                        railContent = { AppNavigationRail(bottomBar, onMainTabSelected) },
                        // 电视上标签栏占了顶部一条，页面内容整体下移，避免被它盖住
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (isTelevision) innerPadding.calculateTopPadding() else 0.dp)
                    ) {
                        AppManagedProcessingLayer(
                            layoutInsets = layoutInsets,
                            showNowPlaying = bottomBar.showNowPlaying,
                            content = content
                        )
                    }
                }
            }
        }
    }
}

/**
 * The clean floating tab bar and mini player sit over a fullscreen glass backdrop; when the IME
 * resizes the window they would ride above the keyboard with a stale blur region, so they step
 * aside until it closes, like iOS does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun cleanFloatingChromeYieldsToIme(): Boolean =
    NeriDesign.isClean && WindowInsets.isImeVisible

@Composable
private fun floatingPlaybackChromeVisible(requested: Boolean): Boolean =
    shouldShowFloatingPlaybackChrome(requested, cleanFloatingChromeYieldsToIme())

internal fun shouldShowFloatingPlaybackChrome(requested: Boolean, yieldsToIme: Boolean): Boolean =
    requested && !yieldsToIme

private fun bottomBarVisibilityTarget(visible: Boolean): Float =
    if (visible) 1f else 0f

private fun bottomBarVisibilityDuration(visible: Boolean): Int =
    if (visible) 280 else 220

private fun Modifier.collapseBottomBarSlot(collapsed: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    // 保留自然测量和动画实例，只释放隐藏chrome占用的Scaffold槽位
    layout(placeable.width, if (collapsed) 0 else placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

@Composable
private fun AppScaffoldBottomBar(
    bottomBar: AppBottomBarPresentation,
    yieldsToIme: Boolean,
    onMainTabSelected: (String) -> Unit,
    onTabBarBoundsChanged: (Rect) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val tabInsets = NavigationBarDefaults.windowInsets
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    var navigationBarHeightPx by remember { mutableIntStateOf(0) }
    val barVisible = shouldShowFloatingPlaybackChrome(!bottomBar.showNowPlaying, yieldsToIme)
    val visibilityProgress by animateFloatAsState(
        targetValue = bottomBarVisibilityTarget(barVisible),
        animationSpec = tween(
            durationMillis = bottomBarVisibilityDuration(barVisible),
            easing = FastOutSlowInEasing
        ),
        label = "bottom_bar_visibility"
    )
    val selectAlpha = resolveBottomBarSelectionAlpha(
        hasCustomBackground = bottomBar.hasCustomBackground,
        alwaysUseNewTabStyle = bottomBar.alwaysUseNewTabStyle
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .collapseBottomBarSlot(yieldsToIme)
            .onGloballyPositioned { coordinates ->
                if (!yieldsToIme) {
                    // 拖拽目标使用未位移的槽位，避免IME关闭时入场动画的空边界覆盖目标
                    val slotBounds = coordinates.boundsInRoot()
                    val navigationBounds = slotBounds.copy(
                        top = (slotBounds.bottom - navigationBarHeightPx).coerceAtLeast(slotBounds.top)
                    )
                    onTabBarBoundsChanged(
                        resolveBottomTabDragBounds(
                            navigationBounds,
                            tabInsets.getLeft(density, layoutDirection),
                            tabInsets.getRight(density, layoutDirection),
                            tabInsets.getBottom(density)
                        )
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size ->
                    bottomBarHeightPx = positiveBottomBarHeight(size.height, bottomBarHeightPx)
                }
                .graphicsLayer {
                    translationY = (1f - visibilityProgress) * bottomBarHeightPx.toFloat()
                    alpha = visibilityProgress
                }
        ) {
            AnimatedVisibility(visible = bottomBar.offlineMode) {
                OfflineModeBottomBanner()
            }
            NeriBottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appBottomNavigation")
                    .onSizeChanged { navigationBarHeightPx = it.height },
                selectAlpha = selectAlpha,
                items = bottomBar.items,
                currentDestination = bottomBar.currentDestination,
                onItemSelected = { onMainTabSelected(it.route) }
            )
        }
    }
}

@Composable
private fun rememberManagedProcessingBannerSession(): Pair<
        AppManagedProcessingBannerOwner,
        AppManagedProcessingBannerPresentation
> {
    val currentState by ManagedLibraryProcessingCoordinator.state.collectAsStateWithLifecycle()
    val currentProgress by ManagedDownloadStorage.migrationProgressFlow.collectAsStateWithLifecycle()
    val collapsedState = rememberSaveable { mutableStateOf(false) }
    val owner = remember(collapsedState) { AppManagedProcessingBannerOwner(collapsedState) }
    LaunchedEffect(currentState, currentProgress) {
        owner.observe(currentState, currentProgress)
    }
    LaunchedEffect(currentState.operationId) {
        owner.onOperationChanged(currentState)
    }
    return owner to owner.presentation(currentState, currentProgress)
}

@Composable
private fun AppManagedProcessingLayer(
    layoutInsets: BottomBarLayoutInsets,
    showNowPlaying: Boolean,
    content: @Composable (BottomBarLayoutInsets) -> Unit
) {
    val (owner, presentation) = rememberManagedProcessingBannerSession()
    val backdrops = LocalAdvancedGlassBackdrops.current
    val contentCaptureModifier = if (backdrops == null) {
        Modifier
    } else {
        Modifier.captureAdvancedGlassBackdrop(backdrops.content)
    }
    val edgePx = with(LocalDensity.current) { MANAGED_LIBRARY_PROCESSING_REVEAL_EDGE.toPx() }
    val thresholdPx = with(LocalDensity.current) { MANAGED_LIBRARY_PROCESSING_DRAG_THRESHOLD.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = layoutInsets.navContentBottomPadding)
            .clipToBounds()
            .managedProcessingRevealGesture(
                collapsed = presentation.revealGestureEnabled,
                edgePx = edgePx,
                thresholdPx = thresholdPx,
                onExpand = owner::expand
            )
    ) {
        AppManagedProcessingBanner(presentation, owner::collapse)
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            AppDragOverlayHost(modifier = Modifier.fillMaxSize().then(contentCaptureModifier)) {
                content(layoutInsets)
            }
        }
    }
}

private fun managedProcessingBannerEnterTransition() =
    fadeIn(animationSpec = tween(durationMillis = 180)) +
        slideInVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialOffsetY = { -it / 2 }
        ) +
        expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        )

private fun managedProcessingBannerExitTransition() =
    fadeOut(animationSpec = tween(durationMillis = 160)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            targetOffsetY = { -it / 2 }
        ) +
        shrinkVertically(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        )

@Composable
private fun BoxScope.AppManagedProcessingBanner(
    presentation: AppManagedProcessingBannerPresentation,
    onCollapsedChange: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = presentation.visible,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .zIndex(MANAGED_LIBRARY_PROCESSING_Z_INDEX),
        enter = managedProcessingBannerEnterTransition(),
        exit = managedProcessingBannerExitTransition()
    ) {
        ManagedLibraryProcessingBanner(
            state = presentation.state,
            migrationProgress = presentation.progress,
            modifier = Modifier,
            interactive = presentation.visible,
            onCollapsedChange = onCollapsedChange
        )
    }
}
