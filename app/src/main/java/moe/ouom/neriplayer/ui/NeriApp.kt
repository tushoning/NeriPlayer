package moe.ouom.neriplayer.ui

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
 * File: moe.ouom.neriplayer.ui/NeriApp
 * Created: 2025/8/8
 */

import moe.ouom.neriplayer.data.identity.stableKey
import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.LocalTvInputTracker
import moe.ouom.neriplayer.ui.tv.TvInputActivityTracker
import moe.ouom.neriplayer.ui.tv.TvInputTrace
import moe.ouom.neriplayer.ui.tv.TvReturnFocus
import moe.ouom.neriplayer.ui.tv.isTvDirectionKey
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import coil.Coil
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.ouom.neriplayer.data.model.bilibili.video.VideoBasicInfo
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.download.GlobalDownloadManager
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.core.player.lifecycle.recoverUsbExclusivePlaybackOnForeground
import moe.ouom.neriplayer.core.player.lifecycle.updateUsbExclusiveForegroundState
import moe.ouom.neriplayer.core.player.policy.usb.shouldPromptForUsbExclusiveBackgroundPermission
import moe.ouom.neriplayer.core.player.service.AudioPlayerService
import moe.ouom.neriplayer.data.local.media.LocalSongSupport
import moe.ouom.neriplayer.data.local.storage.LocalAssetInvalidationBus
import moe.ouom.neriplayer.core.startup.player.PlayerStartupBootstrapper
import moe.ouom.neriplayer.core.startup.player.PlayerStartupAudioFocusRefresher
import moe.ouom.neriplayer.core.startup.player.PlayerStartupHistoryRecorder
import moe.ouom.neriplayer.core.startup.player.PlayerStartupServicePlanner
import moe.ouom.neriplayer.core.startup.player.PlayerStartupServiceSyncCoordinator
import moe.ouom.neriplayer.core.startup.theme.StartupThemeResolver
import moe.ouom.neriplayer.data.identity.playbackVisualKey
import moe.ouom.neriplayer.data.identity.playbackVisualKeyAliases
import moe.ouom.neriplayer.data.model.stableKey
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.settings.appearance.DEFAULT_ENHANCED_ADVANCED_BLUR_RADIUS_DP
import moe.ouom.neriplayer.data.settings.appearance.AdvancedBlurQualityPreference
import moe.ouom.neriplayer.data.model.settings.playback.PlaybackPreferenceSnapshot
import moe.ouom.neriplayer.data.model.settings.navigation.StartDestinationSnapshot
import moe.ouom.neriplayer.data.settings.appearance.ThemeDefaults
import moe.ouom.neriplayer.data.model.settings.appearance.ThemeMode
import moe.ouom.neriplayer.data.model.settings.appearance.ThemePreferenceSnapshot
import moe.ouom.neriplayer.data.settings.appearance.isCurrentBuildDimensity
import moe.ouom.neriplayer.data.settings.playback.readPlaybackPreferenceSnapshotCached
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.component.navigation.TvTopBarFocus
import moe.ouom.neriplayer.ui.tv.rememberIsTelevision
import moe.ouom.neriplayer.ui.tv.TvTabRootBus
import moe.ouom.neriplayer.data.model.navigation.LauncherShortcutAction
import moe.ouom.neriplayer.data.model.navigation.LauncherShortcutRequest
import moe.ouom.neriplayer.navigation.launcherShortcutMainTabRoute
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassController
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassHost
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassNavigationHandoff
import moe.ouom.neriplayer.ui.effect.glass.captureAdvancedGlassBackdrop
import moe.ouom.neriplayer.ui.effect.glass.isAdvancedGlassBackendSupported
import moe.ouom.neriplayer.ui.effect.glass.rememberAdvancedGlassBackdrop
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import moe.ouom.neriplayer.ui.screen.nowplaying.NowPlayingScreen
import moe.ouom.neriplayer.ui.screen.debug.DebugHomeScreen
import moe.ouom.neriplayer.ui.screen.host.ExploreHostScreen
import moe.ouom.neriplayer.ui.screen.host.HomeHostScreen
import moe.ouom.neriplayer.ui.screen.host.LibraryHostScreen
import moe.ouom.neriplayer.ui.screen.host.rememberHomeHostRuntimeState
import moe.ouom.neriplayer.ui.screen.tab.home.shouldShowHomeContinueSection
import moe.ouom.neriplayer.ui.screen.tab.home.rememberHomeUsageSnapshot
import moe.ouom.neriplayer.ui.screen.tab.home.HomeContinueDiagnostics
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsEntryFocus
import moe.ouom.neriplayer.ui.theme.NeriTheme
import moe.ouom.neriplayer.ui.theme.biasCoverSeedTowardBlue
import moe.ouom.neriplayer.ui.theme.rememberActualSystemDarkTheme
import moe.ouom.neriplayer.ui.util.rememberSongDisplayCoverUrl
import moe.ouom.neriplayer.ui.util.hasKnownEmptyLocalCover
import moe.ouom.neriplayer.data.model.NeteaseArtistSummary
import moe.ouom.neriplayer.data.model.BiliUploaderSummary
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.util.media.CoverArtColorCache
import moe.ouom.neriplayer.common.logging.NPLogger
import moe.ouom.neriplayer.ui.debug.appDebugCrashActionOwner
import moe.ouom.neriplayer.ui.dialog.AppTrafficRiskDialogHost
import moe.ouom.neriplayer.ui.dialog.AppUsbBackgroundPermissionDialogHost
import moe.ouom.neriplayer.util.platform.openAppBackgroundSettings
import moe.ouom.neriplayer.util.platform.readBackgroundBehaviorAllowance
import moe.ouom.neriplayer.util.platform.requestIgnoreBatteryOptimizationsCompat
import moe.ouom.neriplayer.common.locale.LanguageManager
import moe.ouom.neriplayer.util.media.isRemoteImageSource
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import moe.ouom.neriplayer.ui.network.rememberOfflineModeState
import moe.ouom.neriplayer.ui.haptic.syncHapticFeedbackSetting
import moe.ouom.neriplayer.ui.navigation.AppBottomBarPresentation
import moe.ouom.neriplayer.ui.navigation.playbackSourceNavigationAction
import moe.ouom.neriplayer.ui.navigation.shouldSuppressPlaybackNavigation
import moe.ouom.neriplayer.ui.navigation.AppNavigationGraph
import moe.ouom.neriplayer.ui.navigation.AppNavigationGraphOwner
import moe.ouom.neriplayer.ui.navigation.AppNavigationGraphPresentation
import moe.ouom.neriplayer.ui.navigation.AppNavigationMediaActions
import moe.ouom.neriplayer.ui.navigation.AppNavigationScaffold
import moe.ouom.neriplayer.ui.navigation.AppNavigationSceneRenderer
import moe.ouom.neriplayer.ui.navigation.AppStartupDestinationEffect
import moe.ouom.neriplayer.ui.navigation.MainTabGlassOwner
import moe.ouom.neriplayer.ui.navigation.MainTabLayerHost
import moe.ouom.neriplayer.ui.navigation.MainTabPredictiveBackHandler
import moe.ouom.neriplayer.ui.navigation.biliPlaylistSourceRoute
import moe.ouom.neriplayer.ui.navigation.biliUploaderSourceRoute
import moe.ouom.neriplayer.ui.navigation.localPlaylistSourceRoute
import moe.ouom.neriplayer.ui.navigation.MAIN_TAB_DETAIL_OPEN_DURATION_MS
import moe.ouom.neriplayer.ui.navigation.mainTabDetailContentOffsetEasing
import moe.ouom.neriplayer.ui.navigation.navigationGson
import moe.ouom.neriplayer.ui.navigation.neteaseAlbumSourceRoute
import moe.ouom.neriplayer.ui.navigation.neteasePlaylistSourceRoute
import moe.ouom.neriplayer.ui.navigation.rememberMainTabLayerTransitionState
import moe.ouom.neriplayer.ui.navigation.resolveMainStartDestination
import moe.ouom.neriplayer.ui.navigation.resolveMainTabBackgroundMotionDurationMillis
import moe.ouom.neriplayer.ui.navigation.resolveMainTabNavigationMotionState
import moe.ouom.neriplayer.ui.navigation.resolveMainTabNavigationMotionTarget
import moe.ouom.neriplayer.ui.navigation.selectMainTabRouteContent
import moe.ouom.neriplayer.ui.navigation.shouldAcceptObservedMainTabRoute
import moe.ouom.neriplayer.ui.navigation.shouldDispatchMainTabNavigation
import moe.ouom.neriplayer.ui.navigation.shouldUseAdvancedGlassNavigationHandoff
import moe.ouom.neriplayer.ui.playback.visual.AppNowPlayingOverlay
import moe.ouom.neriplayer.ui.playback.visual.NowPlayingCoverAssetRefresh
import moe.ouom.neriplayer.ui.playback.visual.NowPlayingOverlayBackground
import moe.ouom.neriplayer.ui.playback.visual.NowPlayingOverlayCover
import moe.ouom.neriplayer.ui.playback.visual.NowPlayingOverlayTheme
import moe.ouom.neriplayer.ui.playback.visual.PLAYBACK_COVER_SEED_GRACE_MS
import moe.ouom.neriplayer.ui.playback.visual.PlaybackCoverSeed
import moe.ouom.neriplayer.ui.playback.visual.playbackVisualCoverRequest
import moe.ouom.neriplayer.ui.playback.visual.rememberPlaybackVisualCoverState
import moe.ouom.neriplayer.ui.playback.visual.resolveActiveCoverSeedHex
import moe.ouom.neriplayer.ui.playback.visual.resolveCoverSeedWarmupDelayMillis
import moe.ouom.neriplayer.ui.settings.route.AppSettingsHostEnvironment
import moe.ouom.neriplayer.ui.settings.route.isAppSettingsVisible
import moe.ouom.neriplayer.ui.settings.route.AppSettingsRoute
import moe.ouom.neriplayer.ui.theme.background.CustomBackground
import moe.ouom.neriplayer.data.model.settings.appearance.UiStyle
import moe.ouom.neriplayer.ui.animation.LocalSharedCoverFlight
import moe.ouom.neriplayer.ui.animation.SharedCoverFlightState
import moe.ouom.neriplayer.ui.effect.glass.rememberLiquidGlassDeviceAllowed
import moe.ouom.neriplayer.ui.navigation.LocalPredictiveBackPreviewEnabled
import moe.ouom.neriplayer.ui.navigation.PredictiveBackPreviewGate
import moe.ouom.neriplayer.ui.theme.reveal.shouldReverseThemeReveal
import moe.ouom.neriplayer.ui.theme.LocalHasCustomBackground
import moe.ouom.neriplayer.ui.screen.tab.home.LocalHomeOpenSearch
import moe.ouom.neriplayer.ui.theme.reveal.AppThemeRevealOverlayHost
import moe.ouom.neriplayer.ui.theme.reveal.THEME_REVEAL_WATCHDOG_DELAY_MILLIS
import moe.ouom.neriplayer.ui.theme.reveal.appThemeRevealPresentation
import moe.ouom.neriplayer.ui.theme.reveal.awaitStableDraw
import moe.ouom.neriplayer.ui.theme.reveal.captureThemeRevealSnapshot
import moe.ouom.neriplayer.ui.theme.reveal.resolveThemeToggleTarget
import moe.ouom.neriplayer.ui.theme.reveal.shouldBlockThemeModeChange
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import moe.ouom.neriplayer.ui.component.common.rememberPredictiveDismissState
import moe.ouom.neriplayer.ui.component.common.rememberRevealsUnderlying
import moe.ouom.neriplayer.R as AppR

private val EmptyLauncherShortcutRequestFlow =
    MutableStateFlow<LauncherShortcutRequest?>(null)
private const val LAUNCHER_SHORTCUT_PLAYLIST_READY_TIMEOUT_MS = 5000L
/** 电视上「连按两次返回退出」的判定窗口 */
private const val TV_EXIT_BACK_WINDOW_MS = 2000L
/**
 * 逐帧把光标稳到顶部标签栏的某个标签上，最多约 0.4 秒。
 *
 * 页面切换/返回时旧层的焦点节点正在被移除，Compose 会把焦点交给树里第一个可聚焦元素，
 * 单次 requestFocus 经常被这次兜底覆盖，所以要「请求一次、看一帧、没到就再来」。
 */
private suspend fun settleTopBarFocusOn(route: String) {
    repeat(24) {
        // 用「标签栏里真的有焦点」来判定成功，不能只看 focusedTabRoute：
        // 那个字段在层切换时会失同步（值对了但光标不在），会导致这里提前返回、
        // 而屏幕上焦点框其实没跟过去。
        if (TvTopBarFocus.barHasFocus && TvTopBarFocus.focusedTabRoute == route) return
        TvTopBarFocus.requestTab(route)
        withFrameNanos { }
    }
}


/** 电视顶部标签栏对应的四个主页面路由 */
private val TvMainTabRoutes: Set<String> = setOf(
    Destinations.Home.route,
    Destinations.Explore.route,
    Destinations.Library.route,
    Destinations.Settings.route
)
internal const val MAIN_TAB_LAYER_Z_INDEX = 0f
internal const val NAV_HOST_LAYER_Z_INDEX = 1f
internal const val MINI_PLAYER_OVERLAY_Z_INDEX = 2f
internal data class BottomBarLayoutInsets(
    val navContentBottomPadding: Dp,
    val screenBottomInset: Dp,
    val miniPlayerBottomPadding: Dp
)

internal fun resolveBottomBarLayoutInsets(
    baseBlurRequested: Boolean,
    bottomBarInset: Dp,
    reservedMiniPlayerHeight: Dp
): BottomBarLayoutInsets = if (baseBlurRequested) {
    BottomBarLayoutInsets(
        navContentBottomPadding = 0.dp,
        screenBottomInset = reservedMiniPlayerHeight + bottomBarInset,
        miniPlayerBottomPadding = bottomBarInset
    )
} else {
    BottomBarLayoutInsets(
        navContentBottomPadding = bottomBarInset,
        screenBottomInset = reservedMiniPlayerHeight,
        miniPlayerBottomPadding = 0.dp
    )
}

internal fun localPlaylistIdFromSourceRoute(sourceRoute: String?): Long? {
    return sourceRoute
        ?.takeIf { it.startsWith("local_playlist_detail/") }
        ?.removePrefix("local_playlist_detail/")
        ?.toLongOrNull()
}

/**
 * 播放页能直接跳进去的「详情页」路由。
 *
 * 播放页里点歌手名 / 专辑 / 歌单 / 播放来源 / 歌曲百科里的歌单和艺人，都会落到这些路由上。
 * 从这里按下返回时应该回到播放页（而不是回到详情页下面那一层主页面），
 * 由 [restoreNowPlayingAfterDetail] 那套逻辑负责。
 */
private val NowPlayingSourceDetailRoutes: Set<String> = setOf(
    Destinations.NeteaseArtistDetail.route,
    Destinations.NeteaseAlbumDetail.route,
    Destinations.PlaylistDetail.route,
    Destinations.BiliUploaderDetail.route,
    Destinations.BiliPlaylistDetail.route,
    Destinations.YouTubeMusicCreatorDetail.route,
    Destinations.YouTubeMusicPlaylistDetail.route,
    Destinations.LocalPlaylistDetail.route
)

/**
 * 应用主界面入口.
 *
 * @param initialThemeSnapshot 启动时同步读到的主题偏好, 让首帧就用正确的明暗与样式.
 * @param initialStartDestinationSnapshot 启动时同步读到的启动页偏好.
 *   `null` 表示缓存尚未就绪, 此时沿用"先落在兜底路由, 真值到达后再纠正"的原有路径.
 */
@Composable
fun NeriApp(
    initialThemeSnapshot: ThemePreferenceSnapshot = ThemePreferenceSnapshot(),
    initialStartDestinationSnapshot: StartDestinationSnapshot? = null,
    launcherShortcutRequestFlow: StateFlow<LauncherShortcutRequest?> =
        EmptyLauncherShortcutRequestFlow,
    onLauncherShortcutRequestConsumed: (LauncherShortcutRequest) -> Unit = {},
    onIsDarkChanged: (Boolean) -> Unit = {},
    onNowPlayingVisibilityChanged: (Boolean) -> Unit = {},
    onNowPlayingOpenChanged: (Boolean) -> Unit = {},
    onPhoneLandscapeBack: (() -> Unit)? = null,
    onLanguageChanged: (LanguageManager.Language) -> Unit = {},
    onGlobalDialogVisibilityChanged: (Boolean) -> Unit = {},
    canShowGlobalDialogs: Boolean = true
) {
    // 电视：面板会吃掉屏幕最下面一小条（overscan），所有页面底部内容都被切掉一点。
    // 在根上统一往上抬 24dp（= 48px，约屏高 4.4%，落在电视 overscan 安全区内）。
    // 之前加到 40dp 是为了兜「焦点框贴底被裁」，后来定位到那问题是滚动中途行贴视口边，
    // 已由滚动余量（TvFocusScrollMargin 12dp）解决，所以收回到 24dp。
    val tvBottomSafeInset = if (LocalIsTelevision.current) 24.dp else 0.dp
    // 电视：不向无障碍服务暴露语义树。
    // 电视系统常年启用一个只订阅「窗口状态变化」的无障碍守护服务（桌面守护），
    // 只要系统里任意一个无障碍服务在跑，Compose 就会在每次绘制后全量遍历语义节点、
    // 比对差异并跨进程发送变更。实测这三项（checkForSemanticsChanges /
    // getAllUncoveredSemanticsNodesToIntObjectMap / sendAccessibilitySemanticsStructureChangeEvents）
    // 合计占主线程 34%（242 次调用累计约 11.7 秒），而那个服务根本不需要这些数据。
    // 清空整棵子树的语义后，Compose 只剩一个空节点需要比对，这部分开销归零。
    val tvSemanticsModifier = if (LocalIsTelevision.current) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(tvSemanticsModifier)
    ) {
        AppContentMountGate(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = tvBottomSafeInset)
        ) {
            NeriAppContent(
                initialThemeSnapshot = initialThemeSnapshot,
                initialStartDestinationSnapshot = initialStartDestinationSnapshot,
                launcherShortcutRequestFlow = launcherShortcutRequestFlow,
                onLauncherShortcutRequestConsumed = onLauncherShortcutRequestConsumed,
                onIsDarkChanged = onIsDarkChanged,
                onNowPlayingVisibilityChanged = onNowPlayingVisibilityChanged,
                onNowPlayingOpenChanged = onNowPlayingOpenChanged,
                onPhoneLandscapeBack = onPhoneLandscapeBack,
                onLanguageChanged = onLanguageChanged,
                onGlobalDialogVisibilityChanged = onGlobalDialogVisibilityChanged,
                canShowGlobalDialogs = canShowGlobalDialogs
            )
        }
    }
}

@Composable
internal fun AppContentMountGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // mount the interactive tree immediately; storage and recovery work run
    // from lifecycle coroutines after the first usable frame
    Box(modifier = modifier) {
        content()
    }
}

@Composable
internal fun AppUnderlayBackScope(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val parentOwner = LocalNavigationEventDispatcherOwner.current
    if (parentOwner == null) {
        content()
        return
    }
    val underlayOwner = rememberNavigationEventDispatcherOwner(enabled = enabled, parent = parentOwner)
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides underlayOwner, content = content)
}

/**
 * 用同步快照填补偏好 Flow 首次发射前的首帧，之后以持久偏好驱动界面
 * 导航图起点随导航状态保存，重建时保留已恢复栈的起点，避免启动页偏好变化破坏返回路径
 */
@Composable
private fun NeriAppContent(
    initialThemeSnapshot: ThemePreferenceSnapshot = ThemePreferenceSnapshot(),
    initialStartDestinationSnapshot: StartDestinationSnapshot? = null,
    launcherShortcutRequestFlow: StateFlow<LauncherShortcutRequest?> =
        EmptyLauncherShortcutRequestFlow,
    onLauncherShortcutRequestConsumed: (LauncherShortcutRequest) -> Unit = {},
    onIsDarkChanged: (Boolean) -> Unit = {},
    onNowPlayingVisibilityChanged: (Boolean) -> Unit = {},
    onNowPlayingOpenChanged: (Boolean) -> Unit = {},
    onPhoneLandscapeBack: (() -> Unit)? = null,
    onLanguageChanged: (LanguageManager.Language) -> Unit = {},
    onGlobalDialogVisibilityChanged: (Boolean) -> Unit = {},
    canShowGlobalDialogs: Boolean = true
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val composeResources = LocalResources.current
    val latestOnNowPlayingVisibilityChanged by rememberUpdatedState(
        onNowPlayingVisibilityChanged
    )
    val latestOnLauncherShortcutRequestConsumed by rememberUpdatedState(
        onLauncherShortcutRequestConsumed
    )
    val launcherShortcutRequest by launcherShortcutRequestFlow.collectAsStateWithLifecycle()
    val offlineMode by rememberOfflineModeState()
    val rootView = LocalView.current
    val repo = remember { AppContainer.settingsRepo }
    val systemDark = rememberActualSystemDarkTheme()
    val application = remember(context) { context.applicationContext as Application }
    SideEffect {
        // 播放点击可能早于启动预加载完成, 先绑定上下文避免懒初始化缺入口
        PlayerManager.bindApplication(application)
    }
    val startupPlaybackPreferences = remember(application) {
        readPlaybackPreferenceSnapshotCached(application) ?: PlaybackPreferenceSnapshot()
    }
    val coverArtImageLoader = remember(context) { Coil.imageLoader(context) }

    val storedFollowSystemDark by repo.followSystemDarkFlow.collectAsStateWithLifecycle(
        initialValue = initialThemeSnapshot.followSystemDark
    )
    val dynamicColorEnabled by repo.dynamicColorFlow.collectAsStateWithLifecycle(
        initialValue = initialThemeSnapshot.dynamicColor
    )
    val storedForceDark by repo.forceDarkFlow.collectAsStateWithLifecycle(
        initialValue = initialThemeSnapshot.forceDark
    )
    val (uiStyle, liquidGlassEnabled, predictiveBackPreviewEnabled) =
        rememberAppStylePreferences(repo, initialThemeSnapshot.uiStyle)
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var nowPlayingOverlayMounted by remember { mutableStateOf(showNowPlaying) }
    val nowPlayingDismissState = rememberPredictiveDismissState()
    val nowPlayingBackRevealing = nowPlayingDismissState.rememberRevealsUnderlying()
    val latestOnNowPlayingOpenChanged by rememberUpdatedState(onNowPlayingOpenChanged)
    LaunchedEffect(showNowPlaying) {
        // 方向跟随页面状态，不能让旋转重建时的旧覆盖层销毁关闭横屏
        latestOnNowPlayingOpenChanged(showNowPlaying)
    }
    var showNowPlayingLyrics by rememberSaveable { mutableStateOf(false) }
    var currentPlaybackSourceRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreLyricsAfterAlbumBack by rememberSaveable { mutableStateOf(false) }
    var lyricsAlbumRouteObserved by rememberSaveable { mutableStateOf(false) }
    // 从播放页跳进歌手 / 专辑 / 歌单等详情页后，返回时要回到播放页，而不是退回下面那一层主页面。
    // detailRouteObserved = 真的进过详情页（避免「点了但没跳成」也把播放页重新拉起来）。
    var restoreNowPlayingAfterDetail by rememberSaveable { mutableStateOf(false) }
    var nowPlayingDetailRouteObserved by rememberSaveable { mutableStateOf(false) }
    var nowPlayingReturnRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val devModeEnabled by repo.devModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val alwaysRecordLogsEnabled by repo.alwaysRecordLogsEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val themeSeedColor by repo.themeSeedColorFlow.collectAsStateWithLifecycle(initialValue = ThemeDefaults.DEFAULT_SEED_COLOR_HEX)
    val themePaletteStyleValue by repo.themePaletteStyleFlow.collectAsStateWithLifecycle(
        initialValue = ThemeDefaults.DEFAULT_PALETTE_STYLE
    )
    val themeColorSpecValue by repo.themeColorSpecFlow.collectAsStateWithLifecycle(
        initialValue = ThemeDefaults.DEFAULT_COLOR_SPEC
    )
    val themePaletteStyle = remember(themePaletteStyleValue) {
        PaletteStyle.valueOf(ThemeDefaults.normalizePaletteStyle(themePaletteStyleValue))
    }
    val themeColorSpec = remember(themeColorSpecValue) {
        ColorSpec.SpecVersion.valueOf(ThemeDefaults.normalizeColorSpec(themeColorSpecValue))
    }
    val lyricBlurEnabled by repo.lyricBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val lyricBlurAmount by repo.lyricBlurAmountFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val advancedLyricsEnabled by repo.advancedLyricsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    // 电视机上关掉玻璃模糊与相干反馈动效：都是纯装饰，老 SoC 上每帧都要多取样一遍位图
    val tvDevice = rememberIsTelevision()
    val coherentFeedbackPreference by repo.coherentFeedbackEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val coherentFeedbackEnabled = coherentFeedbackPreference && !tvDevice
    val advancedBlurEnabled by repo.advancedBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val enhancedAdvancedBlurEnabled by repo.enhancedAdvancedBlurEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val enhancedAdvancedBlurRadiusDp by repo.enhancedAdvancedBlurRadiusDpFlow
        .collectAsStateWithLifecycle(
            initialValue = DEFAULT_ENHANCED_ADVANCED_BLUR_RADIUS_DP
        )
    val initialAdvancedBlurQuality = remember {
        AdvancedBlurQualityPreference.defaultForDevice(isCurrentBuildDimensity())
    }
    val advancedBlurQuality by repo.advancedBlurQualityFlow.collectAsStateWithLifecycle(
        initialValue = initialAdvancedBlurQuality
    )
    val advancedBlurAvailable = isAdvancedGlassBackendSupported(Build.VERSION.SDK_INT)
    val effectiveAdvancedBlurEnabled = advancedBlurAvailable && advancedBlurEnabled && !tvDevice
    val nowPlayingAudioReactiveEnabled by repo.nowPlayingAudioReactiveEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingDynamicBackgroundEnabled by repo.nowPlayingDynamicBackgroundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingCoverBlurBackgroundEnabled by repo.nowPlayingCoverBlurBackgroundEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val nowPlayingCoverBlurAmount by repo.nowPlayingCoverBlurAmountFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val nowPlayingCoverBlurDarken by repo.nowPlayingCoverBlurDarkenFlow.collectAsStateWithLifecycle(initialValue = 0.2f)
    val lyricFontScales by repo.lyricFontScalesFlow.collectAsStateWithLifecycle(
        initialValue = repo.defaultLyricFontScales
    )
    val backgroundImageUri by repo.backgroundImageUriFlow.collectAsStateWithLifecycle(initialValue = null)
    val hasCustomBackground = backgroundImageUri != null
    val backgroundImageBlur by repo.backgroundImageBlurFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val backgroundImageAlpha by repo.backgroundImageAlphaFlow.collectAsStateWithLifecycle(initialValue = 0.3f)
    val hapticFeedbackEnabled by repo.hapticFeedbackEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val showCoverSourceBadge by repo.showCoverSourceBadgeFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingKeepScreenOn by repo.nowPlayingKeepScreenOnFlow.collectAsStateWithLifecycle(initialValue = true)
    val showLyricTranslation by repo.showLyricTranslationFlow.collectAsStateWithLifecycle(initialValue = true)
    val defaultStartDestination: String? by repo.defaultStartDestinationFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val showHomeContinueCard by repo.homeCardContinueFlow.collectAsStateWithLifecycle(initialValue = true)
    val showHomeTrendingCard by repo.homeCardTrendingFlow.collectAsStateWithLifecycle(initialValue = true)
    val showHomeRadarCard by repo.homeCardRadarFlow.collectAsStateWithLifecycle(initialValue = true)
    val showHomeRecommendedCard by repo.homeCardRecommendedFlow.collectAsStateWithLifecycle(initialValue = true)
    val homeBilibiliCardEnabled by repo.homeCardBilibiliFlow.collectAsStateWithLifecycle(initialValue = true)
    val internationalizationEnabled by repo.internationalizationEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val showHomeBilibiliCard = homeBilibiliCardEnabled && !internationalizationEnabled
    val storedUsbExclusivePlayback by repo.usbExclusivePlaybackFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val usbExclusivePlayback = storedUsbExclusivePlayback
        ?: startupPlaybackPreferences.usbExclusivePlayback
    val usbExclusiveBackgroundPermissionPromptSuppressed by repo
        .usbExclusiveBackgroundPermissionPromptSuppressedFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val allowMixedPlayback by repo.allowMixedPlaybackFlow.collectAsStateWithLifecycle(initialValue = false)
    val preemptAudioFocus by repo.preemptAudioFocusFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.preemptAudioFocus
    )
    val homeUsageSnapshot by rememberHomeUsageSnapshot(context) {
        AppContainer.playlistUsageRepo
    }
    HomeContinueDiagnostics(showHomeContinueCard, homeUsageSnapshot)
    val showHomeTab =
        shouldShowHomeContinueSection(
            showContinueCard = showHomeContinueCard,
            usageLoaded = homeUsageSnapshot.isLoaded,
            hasUsage = homeUsageSnapshot.entries.isNotEmpty()
        ) ||
            showHomeTrendingCard ||
            showHomeRadarCard ||
            showHomeRecommendedCard ||
            showHomeBilibiliCard
    var pendingFollowSystemDark by remember { mutableStateOf<Boolean?>(null) }
    var pendingForceDark by remember { mutableStateOf<Boolean?>(null) }
    var themeRevealSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var themeRevealOriginWindow by remember { mutableStateOf<Offset?>(null) }
    var themeRevealStartRadiusPx by remember { mutableFloatStateOf(0f) }
    var themeRevealFallbackColorArgb by remember { mutableStateOf<Int?>(null) }
    var themeRevealReverse by remember { mutableStateOf(false) }
    var themeRevealCaptureInFlight by remember { mutableStateOf(false) }
    var themeRevealCaptureJob by remember { mutableStateOf<Job?>(null) }
    var themeRevealCaptureToken by remember { mutableIntStateOf(0) }
    var themeModeWriteInFlight by remember { mutableStateOf(false) }
    var pendingBackgroundImageAlpha by remember { mutableStateOf<Float?>(null) }
    var coverArtRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var showUsbExclusiveBackgroundPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var usbExclusiveBackgroundPermissionPromptHandled by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val startupAudioFocusRefresher = remember(context) {
        PlayerStartupAudioFocusRefresher(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleResumed = when (event) {
                Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> false
                else -> lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        lifecycleResumed,
        storedUsbExclusivePlayback,
        usbExclusiveBackgroundPermissionPromptSuppressed
    ) {
        val usbEnabled = storedUsbExclusivePlayback ?: return@LaunchedEffect
        val promptSuppressed = usbExclusiveBackgroundPermissionPromptSuppressed ?: return@LaunchedEffect
        if (!usbEnabled) {
            usbExclusiveBackgroundPermissionPromptHandled = false
            showUsbExclusiveBackgroundPermissionDialog = false
            return@LaunchedEffect
        }
        val shouldInspectBackgroundBehavior = lifecycleResumed &&
            !promptSuppressed &&
            !usbExclusiveBackgroundPermissionPromptHandled
        val backgroundBehaviorAllowed = if (shouldInspectBackgroundBehavior) {
            context.readBackgroundBehaviorAllowance().fullyAllowed
        } else {
            true
        }
        if (
            shouldPromptForUsbExclusiveBackgroundPermission(
                usbExclusiveEnabled = usbEnabled,
                appResumed = lifecycleResumed,
                promptSuppressed = promptSuppressed,
                backgroundBehaviorAllowed = backgroundBehaviorAllowed,
                promptHandledInCurrentSession = usbExclusiveBackgroundPermissionPromptHandled
            )
        ) {
            showUsbExclusiveBackgroundPermissionDialog = true
        }
        if (shouldInspectBackgroundBehavior) {
            usbExclusiveBackgroundPermissionPromptHandled = true
        }
    }

    val followSystemDark = pendingFollowSystemDark ?: storedFollowSystemDark
    val forceDark = pendingForceDark ?: storedForceDark
    val themeMode = remember(followSystemDark, forceDark) {
        ThemeMode.fromPreferenceFlags(
            forceDark = forceDark,
            followSystemDark = followSystemDark
        )
    }
    val effectiveBackgroundImageAlpha = pendingBackgroundImageAlpha ?: backgroundImageAlpha

    val clearThemeRevealVisualState = {
        themeRevealSnapshot = null
        themeRevealOriginWindow = null
        themeRevealStartRadiusPx = 0f
        themeRevealFallbackColorArgb = null
    }
    val clearPendingThemeModeChange = {
        pendingFollowSystemDark = null
        pendingForceDark = null
    }
    val clearThemeRevealState = {
        themeRevealCaptureToken += 1
        themeRevealCaptureJob?.cancel()
        themeRevealCaptureJob = null
        themeRevealCaptureInFlight = false
        themeModeWriteInFlight = false
        clearPendingThemeModeChange()
        clearThemeRevealVisualState()
    }
    val finishThemeReveal = { captureToken: Int ->
        if (themeRevealCaptureToken == captureToken) {
            clearThemeRevealVisualState()
        }
    }

    val currentSong by PlayerManager.currentSongFlow.collectAsStateWithLifecycle()
    // 顶部「正在播放」标签用：播放中频谱柱动、暂停静止
    val playerIsPlaying by PlayerManager.isPlayingFlow.collectAsStateWithLifecycle()
    val displayCoverUrl = rememberSongDisplayCoverUrl(currentSong)
    val currentSongKey = remember(currentSong) { currentSong?.stableKey() }
    val coverAssetRootGeneration by LocalAssetInvalidationBus.rootGenerationFlow
        .collectAsStateWithLifecycle()
    val coverAssetSongRevisionFlow = remember(currentSongKey) {
        LocalAssetInvalidationBus.revisionFlow(currentSongKey.orEmpty())
    }
    val coverAssetSongRevision by coverAssetSongRevisionFlow.collectAsStateWithLifecycle(
        initialValue = LocalAssetInvalidationBus.currentSongRevision(currentSongKey.orEmpty())
    )
    val currentSongVisualKey = remember(currentSong) { currentSong?.playbackVisualKey() }
    val currentSongVisualKeyAliases = remember(currentSong) {
        currentSong?.playbackVisualKeyAliases().orEmpty()
    }
    val playbackVisualCoverState = rememberPlaybackVisualCoverState(
        playbackVisualCoverRequest(displayCoverUrl, currentSongVisualKey, currentSong.hasKnownEmptyLocalCover())
    )
    val playbackVisualCoverUrl = playbackVisualCoverState.url
    // 旋转重建时直接复用当前封面颜色，避免首帧回到默认主题
    var coverSeed by remember {
        mutableStateOf(playbackVisualCoverUrl?.let { url ->
            CoverArtColorCache.peek(url)?.let { sample ->
                PlaybackCoverSeed(url, sample.seedHex, currentSongVisualKey)
            }
        })
    }
    val coverAssetRefresh = remember(
        coverArtRefreshToken,
        coverAssetRootGeneration
    ) {
        NowPlayingCoverAssetRefresh(coverArtRefreshToken, coverAssetRootGeneration)
    }
    val coverAssetRefreshKey = remember(coverAssetRefresh, coverAssetSongRevision) {
        coverAssetRefresh.key(coverAssetSongRevision)
    }
    val scope = rememberCoroutineScope()
    // 电视：遥控器连续操作期间，图片先不发起新加载（详见 TvInputActivityTracker 的注释）。
    val tvInputTracker = remember(scope) { TvInputActivityTracker(scope) }
    var pendingTrafficRiskDownloadRequest by remember {
        mutableStateOf<GlobalDownloadManager.TrafficRiskDownloadRequest?>(null)
    }
    val usbBackgroundPermissionCheckPending = storedUsbExclusivePlayback == null ||
        usbExclusiveBackgroundPermissionPromptSuppressed == null ||
        (usbExclusivePlayback && lifecycleResumed &&
            usbExclusiveBackgroundPermissionPromptSuppressed == false &&
            !usbExclusiveBackgroundPermissionPromptHandled)
    // 在组合期读取待确认状态，让父级门控随弹窗请求更新
    val globalDialogVisible = showUsbExclusiveBackgroundPermissionDialog ||
        usbBackgroundPermissionCheckPending || pendingTrafficRiskDownloadRequest != null
    SideEffect { onGlobalDialogVisibilityChanged(globalDialogVisible) }
    LaunchedEffect(Unit) {
        GlobalDownloadManager.trafficRiskDownloadRequests.collect { request ->
            pendingTrafficRiskDownloadRequest = request
        }
    }

    val currentBootstrapServiceStart by rememberUpdatedState {
        PlayerStartupServicePlanner.plan(
            hasItems = PlayerManager.hasItems(),
            shouldBootstrapPlaybackService = PlayerManager.shouldBootstrapPlaybackServiceOnAppLaunch(),
            preemptAudioFocus = preemptAudioFocus,
            allowMixedPlayback = allowMixedPlayback
        )
    }
    val serviceSyncCoordinator = remember(application) {
        PlayerStartupServiceSyncCoordinator(
            isServiceReadyForPassiveLocalPlaybackSync = AudioPlayerService::isReadyForPassiveLocalPlaybackSync,
            hasItems = PlayerManager::hasItems,
            hasLocalCurrentSong = {
                PlayerManager.currentSongFlow.value?.let { song ->
                    LocalSongSupport.isLocalSong(song, context)
                } == true
            },
            isUsbExclusivePlaybackActiveForForegroundService =
                PlayerManager::isUsbExclusivePlaybackActiveForForegroundService,
            shouldRunPlaybackServiceInForeground = PlayerManager::shouldRunPlaybackServiceInForeground,
            currentBootstrapServiceStart = { currentBootstrapServiceStart() },
            isServiceInstanceActiveForDiagnostics = AudioPlayerService::isInstanceActiveForDiagnostics,
            isServiceForegroundActiveForDiagnostics = AudioPlayerService::isForegroundActiveForDiagnostics,
            startService = { source, forceForeground ->
                AudioPlayerService.startSyncService(
                    context,
                    source,
                    forceForeground = forceForeground
                )
            },
            playbackCommandFlow = PlayerManager.playbackCommandFlow
        )
    }
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(serviceSyncCoordinator, lifecycleOwner, windowInfo) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { windowInfo.isWindowFocused }.collectLatest { windowFocused ->
                if (windowFocused) {
                    serviceSyncCoordinator.pendingServiceStartFlow.collect { pendingStart ->
                        if (pendingStart != null) {
                            serviceSyncCoordinator.retryPendingServiceStart()
                        }
                    }
                }
            }
        }
    }
    val scheduleAudioServiceStart: (String, Boolean) -> Unit = { source, forceForeground ->
        scope.launch {
            serviceSyncCoordinator.requestServiceStart(
                source = source,
                forceForeground = forceForeground
            )
        }
    }
    var playbackBootstrapReady by remember { mutableStateOf(false) }

    fun updateStartupAudioFocus(reason: String) {
        startupAudioFocusRefresher.refreshForeground(
            lifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
            reason = reason,
            preemptAudioFocus = preemptAudioFocus,
            allowMixedPlayback = allowMixedPlayback,
            usbExclusivePlayback = usbExclusivePlayback
        )
    }

    LaunchedEffect(application) {
        playbackBootstrapReady = false
        PlayerStartupBootstrapper(
            app = application,
            context = context,
            awaitUiFrameBeforePlayerInit = {
                withFrameNanos { }
            }
        ).bootstrap().serviceStart?.let { serviceStart ->
            scheduleAudioServiceStart(serviceStart.source, serviceStart.forceForeground)
        }
        playbackBootstrapReady = true

        launch {
            serviceSyncCoordinator.collectLocalPlaybackCommands()
        }

        val historyRecorder = PlayerStartupHistoryRecorder(
            currentSongFlow = PlayerManager.currentSongFlow,
            recordSong = AppContainer.playHistoryRepo::record,
            startupSongToSkip = PlayerManager.currentSongFlow.value
        )
        launch {
            historyRecorder.run()
        }

    }

    LaunchedEffect(preemptAudioFocus, allowMixedPlayback, usbExclusivePlayback) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            updateStartupAudioFocus("settings_changed")
        } else {
            startupAudioFocusRefresher.releaseForInactiveSettingsChange(
                preemptAudioFocus = preemptAudioFocus,
                usbExclusivePlayback = usbExclusivePlayback,
                allowMixedPlayback = allowMixedPlayback
            )
        }
    }

    LaunchedEffect(storedFollowSystemDark, pendingFollowSystemDark) {
        if (pendingFollowSystemDark != null && pendingFollowSystemDark == storedFollowSystemDark) {
            pendingFollowSystemDark = null
        }
    }
    LaunchedEffect(backgroundImageAlpha, pendingBackgroundImageAlpha) {
        if (
            pendingBackgroundImageAlpha != null &&
            abs((pendingBackgroundImageAlpha ?: backgroundImageAlpha) - backgroundImageAlpha) < 0.001f
        ) {
            pendingBackgroundImageAlpha = null
        }
    }

    LaunchedEffect(storedForceDark, pendingForceDark) {
        if (pendingForceDark != null && pendingForceDark == storedForceDark) {
            pendingForceDark = null
        }
    }

    val latestPlaybackVisualCoverUrl by rememberUpdatedState(playbackVisualCoverUrl)
    val latestPlaybackSongKey by rememberUpdatedState(currentSongVisualKey)
    LaunchedEffect(
        playbackVisualCoverUrl,
        currentSongVisualKey,
        coverArtRefreshToken,
        showNowPlaying,
        dynamicColorEnabled,
        offlineMode
    ) {
        if (!dynamicColorEnabled) {
            coverSeed = null
            return@LaunchedEffect
        }
        if (playbackVisualCoverUrl.isNullOrBlank()) {
            delay(PLAYBACK_COVER_SEED_GRACE_MS.milliseconds)
            if (
                latestPlaybackVisualCoverUrl.isNullOrBlank() &&
                latestPlaybackSongKey == currentSongVisualKey
            ) {
                coverSeed = null
            }
            return@LaunchedEffect
        }
        val cachedSample = CoverArtColorCache.peek(playbackVisualCoverUrl)
        currentCoroutineContext().ensureActive()
        if (
            cachedSample != null &&
            latestPlaybackVisualCoverUrl == playbackVisualCoverUrl &&
            latestPlaybackSongKey == currentSongVisualKey
        ) {
            coverSeed = PlaybackCoverSeed(
                coverUrl = playbackVisualCoverUrl,
                seedHex = cachedSample.seedHex,
                songKey = currentSongVisualKey
            )
        }

        if (showNowPlaying && isRemoteImageSource(playbackVisualCoverUrl)) {
            coverArtImageLoader.enqueue(
                offlineCachedImageRequest(
                    context = context,
                    data = playbackVisualCoverUrl,
                    sizePx = 256,
                    allowHardware = false,
                    offlineMode = offlineMode
                )
            )
        }

        val warmupDelayMillis = resolveCoverSeedWarmupDelayMillis(
            showNowPlaying = showNowPlaying,
            dynamicColorEnabled = dynamicColorEnabled,
            hasCachedSample = cachedSample != null
        )
        if (warmupDelayMillis > 0L) {
            delay(warmupDelayMillis.milliseconds)
        }

        CoverArtColorCache.preload(context, playbackVisualCoverUrl, offlineMode)?.let { sample ->
            currentCoroutineContext().ensureActive()
            if (
                latestPlaybackVisualCoverUrl == playbackVisualCoverUrl &&
                latestPlaybackSongKey == currentSongVisualKey
            ) {
                coverSeed = PlaybackCoverSeed(
                    coverUrl = playbackVisualCoverUrl,
                    seedHex = sample.seedHex,
                    songKey = currentSongVisualKey
                )
            }
        }
    }

    // 同步触感反馈设置
    LaunchedEffect(hapticFeedbackEnabled) {
        syncHapticFeedbackSetting(hapticFeedbackEnabled)
    }


    val isDark = StartupThemeResolver.resolveModeUseDark(
        mode = themeMode,
        systemDark = systemDark
    )
    val initialMainStartDestination = resolveMainStartDestination(
        preferredRoute = defaultStartDestination
            ?: initialStartDestinationSnapshot?.startDestination,
        showHomeTab = showHomeTab,
        devModeEnabled = devModeEnabled
    )
    val currentDefaultStartDestination =
        defaultStartDestination ?: initialMainStartDestination
    val backgroundGlassBackdrop = rememberAdvancedGlassBackdrop()
    val contentGlassBackdrop = rememberAdvancedGlassBackdrop()
    val liquidGlassDeviceAllowed = rememberLiquidGlassDeviceAllowed(advancedBlurQuality)
    val liquidGlassRequested = uiStyle == UiStyle.Clean && liquidGlassEnabled && liquidGlassDeviceAllowed
    val advancedGlassController = remember(
        advancedBlurEnabled,
        enhancedAdvancedBlurEnabled,
        enhancedAdvancedBlurRadiusDp,
        advancedBlurQuality,
        liquidGlassRequested
    ) {
        AdvancedGlassController(
            sdkInt = Build.VERSION.SDK_INT,
            advancedBlurEnabled = advancedBlurEnabled && !tvDevice,
            enhancedAdvancedBlurEnabled = enhancedAdvancedBlurEnabled && !tvDevice,
            backendReady = isAdvancedGlassBackendSupported(Build.VERSION.SDK_INT),
            enhancedAdvancedBlurRadiusDp = enhancedAdvancedBlurRadiusDp,
            advancedBlurQuality = advancedBlurQuality,
            liquidGlassRequested = liquidGlassRequested
        )
    }
    val sharedCoverFlight = remember { SharedCoverFlightState() }
    val currentThemeBackgroundArgb = MaterialTheme.colorScheme.background.toArgb()
    // retained main-tab scenes can keep an earlier callback, so read the current theme state at click time
    val latestThemeMode by rememberUpdatedState(themeMode)
    val latestIsDark by rememberUpdatedState(isDark)
    val latestSystemDark by rememberUpdatedState(systemDark)
    val latestThemeBackgroundArgb by rememberUpdatedState(currentThemeBackgroundArgb)
    val themeRevealActive =
        themeRevealOriginWindow != null &&
            themeRevealFallbackColorArgb != null
    val latestThemeRevealActive by rememberUpdatedState(themeRevealActive)

    LaunchedEffect(isDark, themeRevealActive, themeRevealCaptureInFlight) {
        if (!themeRevealActive && !themeRevealCaptureInFlight) {
            onIsDarkChanged(isDark)
        }
    }

    val activeThemeRevealToken = themeRevealCaptureToken
    LaunchedEffect(themeRevealActive, activeThemeRevealToken) {
        if (!themeRevealActive) {
            return@LaunchedEffect
        }
        delay(THEME_REVEAL_WATCHDOG_DELAY_MILLIS.milliseconds)
        finishThemeReveal(activeThemeRevealToken)
    }

    fun requestThemeModeChange(
        targetMode: ThemeMode,
        originInWindow: Offset,
        startRadiusPx: Float
    ) {
        if (
            shouldBlockThemeModeChange(
                captureInFlight = themeRevealCaptureInFlight,
                writeInFlight = themeModeWriteInFlight,
                revealActive = latestThemeRevealActive,
                hasPendingThemePreference = pendingFollowSystemDark != null ||
                        pendingForceDark != null
            )
        ) {
            return
        }

        if (targetMode == latestThemeMode) {
            return
        }

        val nextFollowSystemDark = targetMode.followSystemDark
        val nextForceDark = targetMode.forceDark
        val nextDark = targetMode.resolveUseDark(latestSystemDark)
        if (nextDark == latestIsDark) {
            themeModeWriteInFlight = true
            scope.launch {
                try {
                    repo.setThemeMode(
                        followSystemDark = nextFollowSystemDark,
                        forceDark = nextForceDark
                    )
                } finally {
                    themeModeWriteInFlight = false
                }
            }
            return
        }

        val captureView = activity?.window?.decorView?.rootView ?: rootView.rootView
        val captureToken = themeRevealCaptureToken + 1
        themeRevealCaptureToken = captureToken
        themeRevealCaptureInFlight = true

        val captureJob = scope.launch {
            var themeWriteStarted = false
            var themeWriteCompleted = false
            try {
                awaitStableDraw(captureView)
                val snapshot = runCatching {
                    captureThemeRevealSnapshot(
                        activity = activity,
                        fallbackView = captureView
                    )
                }.getOrNull()
                val lifecycleActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                val activityValid = activity == null || (!activity.isFinishing && !activity.isDestroyed)
                if (themeRevealCaptureToken != captureToken || !lifecycleActive || !activityValid) {
                    return@launch
                }

                clearThemeRevealVisualState()
                themeRevealSnapshot = snapshot
                themeRevealFallbackColorArgb = latestThemeBackgroundArgb
                themeRevealOriginWindow = originInWindow
                themeRevealStartRadiusPx = startRadiusPx.coerceAtLeast(1f)
                themeRevealReverse = shouldReverseThemeReveal(nextDark)
                pendingFollowSystemDark = nextFollowSystemDark
                pendingForceDark = nextForceDark
                themeModeWriteInFlight = true
                themeWriteStarted = true
                repo.setThemeMode(
                    followSystemDark = nextFollowSystemDark,
                    forceDark = nextForceDark
                )
                themeWriteCompleted = true
            } finally {
                if (themeRevealCaptureToken == captureToken) {
                    if (themeWriteStarted && !themeWriteCompleted) {
                        clearPendingThemeModeChange()
                        clearThemeRevealVisualState()
                    }
                    themeRevealCaptureJob = null
                    themeRevealCaptureInFlight = false
                    themeModeWriteInFlight = false
                }
            }
        }
        themeRevealCaptureJob = captureJob
    }

    fun requestThemeToggle(originInWindow: Offset, startRadiusPx: Float) {
        val targetMode = resolveThemeToggleTarget(latestIsDark)
        requestThemeModeChange(
            targetMode = targetMode,
            originInWindow = originInWindow,
            startRadiusPx = startRadiusPx
        )
    }

    DisposableEffect(lifecycleOwner, preemptAudioFocus, allowMixedPlayback, usbExclusivePlayback) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    PlayerManager.updateUsbExclusiveForegroundState(
                        foreground = true,
                        reason = "lifecycle_resume"
                    )
                    coverArtRefreshToken += 1
                    if (!PlayerManager.isUsbExclusiveNativePlaybackStable()) {
                        updateStartupAudioFocus("lifecycle_resume")
                    }
                    PlayerManager.recoverUsbExclusivePlaybackOnForeground("lifecycle_resume")
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    PlayerManager.updateUsbExclusiveForegroundState(
                        foreground = false,
                        reason = "lifecycle_${event.name.lowercase()}"
                    )
                    clearThemeRevealState()
                    val keepUsbExclusiveFocus = PlayerManager.isPlayerInitialized() &&
                        PlayerManager.usbExclusivePlaybackEnabled &&
                        PlayerManager.shouldUseUsbExclusiveFocusGuard()
                    if (keepUsbExclusiveFocus) {
                        updateStartupAudioFocus("lifecycle_${event.name.lowercase()}_keep_usb")
                    } else {
                        startupAudioFocusRefresher.release("lifecycle_${event.name.lowercase()}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    clearThemeRevealState()
                    startupAudioFocusRefresher.release("lifecycle_${event.name.lowercase()}")
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(rootView, backgroundImageUri) {
        clearThemeRevealState()
    }

    fun playSongsAndOpenNowPlaying(
        songs: List<SongItem>,
        index: Int,
        sourceRoute: String? = null
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        PlayerManager.prefetchYouTubePlayableUrlWindow(
            playlist = songs,
            startIndex = index,
            source = "ui_click_before_play"
        )
        // 播放队列可能包含歌词等大字段, 避免通过 Binder 传整份歌单导致崩溃
        val localPlaylistId = localPlaylistIdFromSourceRoute(sourceRoute)
        if (localPlaylistId == null) {
            PlayerManager.playPlaylist(songs, index)
        } else {
            PlayerManager.playLocalPlaylist(
                playlistId = localPlaylistId,
                songs = songs,
                startIndex = index
            )
        }
        // 先提交当前歌曲, 播放页首帧不能继续绘制上一首封面
        showNowPlaying = true
        scheduleAudioServiceStart(
            "play_songs_and_open_now_playing",
            true
        )
    }

    fun playSongPreservingQueueAndOpenNowPlaying(song: SongItem) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = null
        PlayerManager.prefetchYouTubePlayableUrlWindow(
            playlist = listOf(song),
            startIndex = 0,
            source = "ui_click_preserve_queue_before_play"
        )
        PlayerManager.replaceCurrentInQueueAndPlay(song)
        // 先提交当前歌曲, 播放页首帧不能继续绘制上一首封面
        showNowPlaying = true
        scheduleAudioServiceStart(
            "play_search_result_preserve_queue",
            true
        )
    }

    fun addSongToQueueNextFromSearch(song: SongItem) {
        PlayerManager.addToQueueNext(song)
        scheduleAudioServiceStart("search_result_play_next", false)
    }

    fun addSongToQueueEndFromSearch(song: SongItem) {
        PlayerManager.addToQueueEnd(song)
        scheduleAudioServiceStart("search_result_add_to_queue_end", false)
    }

    fun ensureAudioServiceStarted(source: String = "ensure_audio_service_started") {
        NPLogger.d(
            "NERI-App",
            "ensureAudioServiceStarted hasItems=${PlayerManager.hasItems()} transportActive=${PlayerManager.isTransportActive()} isPlaying=${PlayerManager.isPlayingFlow.value}"
        )
        scheduleAudioServiceStart(source, false)
    }

    fun playBiliAudioAndOpenNowPlayingWithSource(
        videos: List<BiliVideoItem>,
        index: Int,
        sourceRoute: String?
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        NPLogger.d("NERI-App", "Playing audio from Bili video: ${videos[index].title}")
        PlayerManager.playBiliVideoAsAudio(videos, index)
        showNowPlaying = true
        ensureAudioServiceStarted(source = "play_bili_audio_and_open_now_playing")
    }

    fun playBiliPartsAndOpenNowPlayingWithSource(
        videoInfo: VideoBasicInfo,
        index: Int,
        coverUrl: String,
        sourceRoute: String?
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        NPLogger.d("NERI-App", "Playing parts from Bili video: ${videoInfo.title}")
        PlayerManager.playBiliVideoParts(videoInfo, index, coverUrl)
        showNowPlaying = true
        ensureAudioServiceStarted(source = "play_bili_parts_and_open_now_playing")
    }

    fun playBiliPartsAndOpenNowPlaying(
        videoInfo: VideoBasicInfo,
        index: Int,
        coverUrl: String
    ) {
        playBiliPartsAndOpenNowPlayingWithSource(
            videoInfo = videoInfo,
            index = index,
            coverUrl = coverUrl,
            null
        )
    }

    val activeCoverSeedHex = resolveActiveCoverSeedHex(
        visualCoverUrl = playbackVisualCoverUrl,
        sampledCoverUrl = coverSeed?.coverUrl,
        sampledSeedHex = coverSeed?.seedHex,
        currentSongKey = currentSongVisualKey,
        sampledSongKey = coverSeed?.songKey
    )?.let(::biasCoverSeedTowardBlue)
        val effectiveSeedHex = if (dynamicColorEnabled) {
            activeCoverSeedHex ?: themeSeedColor
        } else {
            themeSeedColor
        }
        val useSystemDynamic =
            dynamicColorEnabled && activeCoverSeedHex == null && playbackVisualCoverUrl == null

    NeriTheme(
            followSystemDark = followSystemDark,
            forceDark = forceDark,
            dynamicColor = useSystemDynamic,
            seedColorHex = effectiveSeedHex,
            paletteStyle = themePaletteStyle,
            colorSpec = themeColorSpec,
            systemDark = systemDark,
            uiStyle = uiStyle,
            liquidGlass = advancedGlassController.isLiquidGlassEnabled,
            // 揭示快照完全遮住界面，颜色直接落到目标值，避免与扩散动画争抢帧
            animateColors = themeRevealSnapshot == null
    ) {
            // 恢复导航栈时保留原起点，避免启动页偏好变化后 popUpTo 找不到栈内目标
            val navHostStartDestination = rememberSaveable { initialMainStartDestination }
            val navController = rememberNavController()
            // 标签页界面状态的托管者：切标签时要按 route 清掉「上次停在哪一页」，
            // 见下方 resetTabState。
            val mainTabSaveableStateHolder = rememberSaveableStateHolder()
            // 首页的滚动/折叠状态建在下面那个 holder 之外，清 holder 碰不到它。
            val homeHostRuntimeState = rememberHomeHostRuntimeState()
            val navigationVisibility = rememberAppNavigationVisibility(navController, lifecycleOwner)
            val currentRoute = navigationVisibility.currentRoute
            val visibleNavigationOwners = navigationVisibility.owners
            val visibleNavigationRoutes = navigationVisibility.routes
            val mainTabMotionTarget = resolveMainTabNavigationMotionTarget(
                currentRoute = currentRoute,
                visibleRoutes = visibleNavigationRoutes,
                coherentFeedbackEnabled = coherentFeedbackEnabled
            )
            var mainTabDetailContentHeightPx by remember {
                mutableIntStateOf(0)
            }
            val mainTabBackgroundProgress = animateFloatAsState(
                targetValue = mainTabMotionTarget.targetProgress,
                animationSpec = tween(
                    durationMillis = resolveMainTabBackgroundMotionDurationMillis(
                        targetProgress = mainTabMotionTarget.targetProgress,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        debugSceneVisible = mainTabMotionTarget.debugSceneVisible
                    ),
                    easing = mainTabDetailContentOffsetEasing()
                ),
                label = "main_tab_detail_content_handoff"
            )
            val mainTabNavigationMotion = remember(
                mainTabMotionTarget.backgroundMotion,
                mainTabBackgroundProgress
            ) {
                {
                    resolveMainTabNavigationMotionState(
                        backgroundMotion = mainTabMotionTarget.backgroundMotion,
                        progress = mainTabBackgroundProgress.value
                    )
                }
            }
            val effectiveStartDestination = remember(
                currentDefaultStartDestination,
                showHomeTab,
                devModeEnabled
            ) {
                resolveMainStartDestination(
                    preferredRoute = currentDefaultStartDestination,
                    showHomeTab = showHomeTab,
                    devModeEnabled = devModeEnabled
                )
            }
            var selectedMainTabRoute by rememberSaveable(navHostStartDestination) {
                mutableStateOf(navHostStartDestination)
            }
            var pendingMainTabRoute by remember(navHostStartDestination) {
                mutableStateOf<String?>(null)
            }
            val mainTabTransitionState = rememberMainTabLayerTransitionState(
                selectedMainTabRoute
            )
            LaunchedEffect(currentRoute, navHostStartDestination) {
                if (
                    shouldAcceptObservedMainTabRoute(
                        observedRoute = currentRoute,
                        pendingRoute = pendingMainTabRoute
                    )
                ) {
                    selectedMainTabRoute = checkNotNull(currentRoute)
                    if (pendingMainTabRoute == currentRoute) {
                        pendingMainTabRoute = null
                    }
                }
            }
            var visibleMainTabGlassOwners by remember(navHostStartDestination) {
                mutableStateOf(
                    setOf(MainTabGlassOwner(navHostStartDestination))
                )
            }
            val activeAdvancedGlassOwners: Set<Any> = remember(
                visibleNavigationOwners,
                visibleMainTabGlassOwners,
                selectedMainTabRoute
            ) {
                visibleNavigationOwners +
                    visibleMainTabGlassOwners +
                        MainTabGlassOwner(selectedMainTabRoute)
            }
            fun resetTabState(route: String, resetHomeRuntime: Boolean) {
                // 目标标签此刻不在组合里 —— 直接删掉上次存下的界面状态。
                // 离开的标签此刻还在组合里（正在淡出）—— removeState 只会标记
                // 「销毁时别保存」，用户看不到任何变化。
                mainTabSaveableStateHolder.removeState(route)
                TvReturnFocus.clear()
                SettingsEntryFocus.clearLastFocused()
                if (resetHomeRuntime && route == Destinations.Home.route) {
                    homeHostRuntimeState.resetForTabBarClick()
                }
            }

            fun navigateToMainTab(route: String, resetTabMemory: Boolean = false) {
                val leavingRoute = selectedMainTabRoute
                if (selectedMainTabRoute != route) {
                    selectedMainTabRoute = route
                }
                mainTabTransitionState.request(route)
                if (
                    !shouldDispatchMainTabNavigation(
                        currentRoute = currentRoute,
                        pendingRoute = pendingMainTabRoute,
                        targetRoute = route
                    )
                ) {
                    return
                }
                if (resetTabMemory) {
                    // 目标：连列表位置一起归零
                    resetTabState(route, resetHomeRuntime = true)
                    // 离开的：只清状态，不复位首页运行时 —— 它正在退场，
                    // 此刻把网格拉回顶部会让淡出画面肉眼可见地跳一下
                    if (leavingRoute != route) {
                        resetTabState(leavingRoute, resetHomeRuntime = false)
                    }
                }
                pendingMainTabRoute = route
                navController.navigate(route) {
                    // 主标签在 NavHost 里是空壳，saveState/restoreState 对标签本身没有意义，
                    // 唯一的实际效果是「把刚才被 popUpTo 弹掉的详情页还原回来」——
                    // 那正是「点标签回根」要杜绝的，所以两个都去掉。
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
            fun showLauncherShortcutToast(messageRes: Int) {
                AppFeedback.showToast(
                    context = context,
                    message = composeResources.getString(messageRes)
                )
            }
            LaunchedEffect(launcherShortcutRequest, playbackBootstrapReady) {
                val request = launcherShortcutRequest ?: return@LaunchedEffect
                if (!playbackBootstrapReady) return@LaunchedEffect

                launcherShortcutMainTabRoute(request.action)?.let { route ->
                    navigateToMainTab(route)
                    latestOnLauncherShortcutRequestConsumed(request)
                    return@LaunchedEffect
                }

                when (request.action) {
                    LauncherShortcutAction.ContinuePlayback -> {
                        if (PlayerManager.hasItems()) {
                            showNowPlaying = true
                            PlayerManager.play()
                            scheduleAudioServiceStart(
                                "launcher_shortcut_continue_playback",
                                true
                            )
                        } else {
                            navigateToMainTab(Destinations.Library.route)
                            showLauncherShortcutToast(
                                AppR.string.launcher_shortcut_no_resumable_queue
                            )
                        }
                    }
                    LauncherShortcutAction.ShuffleFavorites -> {
                        val playlistsReady = withTimeoutOrNull(
                            LAUNCHER_SHORTCUT_PLAYLIST_READY_TIMEOUT_MS.milliseconds
                        ) {
                            PlayerManager.localPlaylistsReadyFlow.first { ready -> ready }
                        } == true
                        val favoritesSongs = if (playlistsReady) {
                            FavoritesPlaylist
                                .firstOrNull(PlayerManager.playlistsFlow.value, context)
                                ?.songs
                                .orEmpty()
                        } else {
                            emptyList()
                        }
                        if (favoritesSongs.isEmpty()) {
                            navigateToMainTab(Destinations.Library.route)
                            showLauncherShortcutToast(
                                AppR.string.launcher_shortcut_favorites_empty
                            )
                        } else {
                            PlayerManager.setShuffle(true)
                            playSongsAndOpenNowPlaying(
                                songs = favoritesSongs,
                                index = Random.nextInt(favoritesSongs.size),
                                sourceRoute = localPlaylistSourceRoute(
                                    FavoritesPlaylist.SYSTEM_ID
                                )
                            )
                        }
                    }
                    LauncherShortcutAction.OpenExplore,
                    LauncherShortcutAction.OpenLibrary -> Unit
                }

                latestOnLauncherShortcutRequestConsumed(request)
            }
            suspend fun preloadNeteaseDetailRouteCover(route: String) {
                val coverUrl = runCatching {
                    when {
                        route.startsWith("playlist_detail/") -> {
                            val json = Uri.decode(route.substringAfter("playlist_detail/"))
                            navigationGson.fromJson(json, PlaylistSummary::class.java).picUrl
                        }
                        route.startsWith("netease_album_detail/") -> {
                            val json = Uri.decode(route.substringAfter("netease_album_detail/"))
                            navigationGson.fromJson(json, AlbumSummary::class.java).picUrl
                        }
                        else -> null
                    }
                }.getOrNull()
                CoverArtColorCache.preload(context, coverUrl, offlineMode)
            }
            fun navigateToNeteaseAlbum(
                album: AlbumSummary,
                afterNavigate: () -> Unit = {}
            ) {
                scope.launch {
                    CoverArtColorCache.preload(context, album.picUrl, offlineMode)
                    navController.navigate(neteaseAlbumSourceRoute(album)) {
                        launchSingleTop = true
                    }
                    afterNavigate()
                }
            }
            fun navigateToNeteasePlaylist(playlist: PlaylistSummary) {
                if (playlist.id <= 0L) return
                scope.launch {
                    CoverArtColorCache.preload(context, playlist.picUrl, offlineMode)
                    navController.navigate(neteasePlaylistSourceRoute(playlist)) {
                        launchSingleTop = true
                    }
                }
            }
            fun navigateToPlaybackSourceRoute(route: String) {
                scope.launch {
                    preloadNeteaseDetailRouteCover(route)
                    showNowPlayingLyrics = false
                    // 拐了弯的「关了就走」：详情页马上要进场，锚点作废别归还
                    TvReturnFocus.discardNowPlayingSource()
                    showNowPlaying = false
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }
            fun navigateToNeteaseArtist(artist: NeteaseArtistSummary) = navController.navigateToNeteaseArtist(artist)
            fun navigateToBiliUploader(uploader: BiliUploaderSummary) = navController.navigateToBiliUploader(uploader)
            fun navigateToYouTubeMusicCreator(creator: YouTubeMusicCreatorSummary) =
                navController.navigateToYouTubeMusicCreator(creator)
            fun navigateToYouTubeMusicPlaylist(playlist: YouTubeMusicPlaylist) =
                navController.navigateToYouTubeMusicPlaylist(playlist)

            // 播放页里所有会「跳出去到另一个页面」的入口都先举起这个标志：
            // 从详情页按返回时，把播放页重新盖回来（而不是退回详情页下面那一层）。
            // 同时记住「是从哪个页面跳出去的」：只有回到这一页才算返回，
            // 中途点了别处的标签 / 去了别的页面就不该莫名奇妙弹回播放页。
            fun openDetailFromNowPlaying(navigate: () -> Unit) {
                nowPlayingReturnRoute = currentRoute
                restoreNowPlayingAfterDetail = true
                // 去程一并压住标签的选中底板：详情页的 route 不属于任何主标签，
                // 本来就没有底板，压住是为了保证回来时那一下「从无到有」不会露出来。
                // 复位点在播放页真正挂载之后（见 onVisibilityChanged）。
                TvTopBarFocus.suppressSelectionPill = true
                navigate()
            }

            LaunchedEffect(currentRoute, restoreNowPlayingAfterDetail) {
                if (!restoreNowPlayingAfterDetail) {
                    nowPlayingDetailRouteObserved = false
                    return@LaunchedEffect
                }
                if (currentRoute in NowPlayingSourceDetailRoutes) {
                    nowPlayingDetailRouteObserved = true
                    return@LaunchedEffect
                }
                if (!nowPlayingDetailRouteObserved) {
                    // 点了但没真的跳出去（例如同一位歌手又点一次，导航被 launchSingleTop 吃掉）：
                    // 过一会儿把标志丢掉，免得以后从音乐库进详情页时莫名其妙弹回播放页。
                    delay(600)
                    if (!nowPlayingDetailRouteObserved) {
                        restoreNowPlayingAfterDetail = false
                        // 没跳出去也就没有「返回」这回事，底板立刻恢复正常
                        TvTopBarFocus.suppressSelectionPill = false
                    }
                    return@LaunchedEffect
                }
                val backToOrigin = currentRoute == nowPlayingReturnRoute
                restoreNowPlayingAfterDetail = false
                nowPlayingDetailRouteObserved = false
                nowPlayingReturnRoute = null
                // 回到了原页面才把播放页盖回来；去了别的地方（切标签、跳到别的页面）就不动
                if (backToOrigin) {
                    // 路由已经变回首页，标签底板会立刻亮起，而播放页要下一拍才盖住 ——
                    // 那一拍就是用户看到的那道闪，所以这里继续压住，等浮层挂载后再恢复。
                    showNowPlaying = true
                    // 兜底：浮层万一没挂上（被别的东西顶掉），别把底板长期压住
                    scope.launch {
                        delay(800)
                        TvTopBarFocus.suppressSelectionPill = false
                    }
                } else {
                    // 没回来源（切了标签 / 去了别处）：底板要恢复正常
                    TvTopBarFocus.suppressSelectionPill = false
                }
            }
            LaunchedEffect(currentRoute, restoreLyricsAfterAlbumBack) {
                if (!restoreLyricsAfterAlbumBack) {
                    lyricsAlbumRouteObserved = false
                    return@LaunchedEffect
                }
                if (currentRoute == Destinations.NeteaseAlbumDetail.route) {
                    lyricsAlbumRouteObserved = true
                    return@LaunchedEffect
                }
                if (lyricsAlbumRouteObserved) {
                    restoreLyricsAfterAlbumBack = false
                    lyricsAlbumRouteObserved = false
                    showNowPlayingLyrics = true
                    showNowPlaying = true
                }
            }
            val bottomBarItems = remember(showHomeTab, devModeEnabled) {
                appMainTabItems(showHomeTab, devModeEnabled)
            }

            val snackbarHostState = remember { SnackbarHostState() }

            val navigationSceneRenderer = AppNavigationSceneRenderer(
                advancedGlassController = advancedGlassController,
                backgroundImageUri = backgroundImageUri,
                backgroundImageBlur = backgroundImageBlur,
                effectiveBackgroundImageAlpha = effectiveBackgroundImageAlpha,
                coherentFeedbackEnabled = coherentFeedbackEnabled,
                currentRoute = currentRoute,
                visibleNavigationRoutes = visibleNavigationRoutes,
                mainTabNavigationMotion = mainTabNavigationMotion
            )

            @Composable
            fun RenderMainTabRoute(route: String) {
                val homeContent: @Composable () -> Unit = {
                    CompositionLocalProvider(
                        LocalHomeOpenSearch provides { navigateToMainTab(Destinations.Explore.route) }
                    ) {
                    HomeHostScreen(
                        showContinueCard = showHomeContinueCard,
                        showTrendingCard = showHomeTrendingCard,
                        showRadarCard = showHomeRadarCard,
                        showRecommendedCard = showHomeRecommendedCard,
                        showBilibiliCard = showHomeBilibiliCard,
                        homeUsageEntries = homeUsageSnapshot.entries,
                        homeUsageLoaded = homeUsageSnapshot.isLoaded,
                        offlineMode = offlineMode,
                        runtimeState = homeHostRuntimeState,
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        onPlayBiliAudioWithSourceRoute = ::playBiliAudioAndOpenNowPlayingWithSource,
                        onPlayBiliPartsWithSourceRoute = ::playBiliPartsAndOpenNowPlayingWithSource,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        neteaseAlbumSourceRoute = ::neteaseAlbumSourceRoute,
                        biliPlaylistSourceRoute = ::biliPlaylistSourceRoute,
                        localPlaylistSourceRoute = ::localPlaylistSourceRoute,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            navigationSceneRenderer.RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )
                }
                }
                val exploreContent: @Composable () -> Unit = {
                    ExploreHostScreen(
                        offlineMode = offlineMode,
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        onSongPlayPreservingQueue =
                            ::playSongPreservingQueueAndOpenNowPlaying,
                        onSongPlayNext = ::addSongToQueueNextFromSearch,
                        onSongAddToQueueEnd = ::addSongToQueueEndFromSearch,
                        onPlayParts = ::playBiliPartsAndOpenNowPlaying,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            navigationSceneRenderer.RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )
                }
                val libraryContent: @Composable () -> Unit = {
                    LibraryHostScreen(
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        onPlayBiliAudioWithSourceRoute = ::playBiliAudioAndOpenNowPlayingWithSource,
                        onPlayBiliPartsWithSourceRoute = ::playBiliPartsAndOpenNowPlayingWithSource,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        neteaseAlbumSourceRoute = ::neteaseAlbumSourceRoute,
                        biliPlaylistSourceRoute = ::biliPlaylistSourceRoute,
                        localPlaylistSourceRoute = ::localPlaylistSourceRoute,
                        onOpenRecent = {
                            navController.navigate(Destinations.Recent.route)
                        },
                        onOpenStats = {
                            navController.navigate(Destinations.PlaybackStats.route)
                        },
                        offlineMode = offlineMode,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            navigationSceneRenderer.RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )
                }
                val settingsContent: @Composable () -> Unit = {
                    AppSettingsRoute(
                        repo = repo,
                        application = AppContainer.applicationContext,
                        initialThemeSnapshot = initialThemeSnapshot,
                        startupPlaybackPreferences = startupPlaybackPreferences,
                        environment = AppSettingsHostEnvironment(
                            isDarkTheme = isDark,
                            themeMode = themeMode,
                            onThemeToggleRequest = ::requestThemeToggle,
                            onThemeModeRequest = ::requestThemeModeChange,
                            backgroundImageAlpha = effectiveBackgroundImageAlpha,
                            defaultStartDestination = currentDefaultStartDestination,
                            homeHasRecentUsage = homeUsageSnapshot.entries.isNotEmpty(),
                            onBeforeLanguageRestart = clearThemeRevealState,
                            onLanguageChanged = onLanguageChanged,
                            coherentFeedbackEnabled = coherentFeedbackEnabled,
                            settingsVisible = isAppSettingsVisible(selectedMainTabRoute, showNowPlaying)
                        ),
                        onBackgroundImageAlphaPreview = { pendingBackgroundImageAlpha = it },
                        snackbarHostState = snackbarHostState,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            navigationSceneRenderer.RenderMainTabNavigationScene(
                                revealTop, translationY, scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )
                }
                val debugContent: @Composable () -> Unit = {
                        val debugHomeScrollState = rememberScrollState()
                        val crashActionOwner = remember(context, composeResources) {
                            appDebugCrashActionOwner(context) {
                                composeResources.getString(AppR.string.test_exception_message)
                            }
                        }
                        navigationSceneRenderer.RenderMainTabNavigationScene(
                            revealTopFraction = 0f,
                            contentTranslationYFraction = 0f,
                            contentScale = 1f,
                            sceneDepth = 0
                        ) {
                            DebugHomeScreen(
                            scrollState = debugHomeScrollState,
                            alwaysRecordLogsEnabled = alwaysRecordLogsEnabled,
                            onAlwaysRecordLogsChange = { enabled ->
                                scope.launch { repo.setAlwaysRecordLogsEnabled(enabled) }
                            },
                            onOpenListenTogetherDebug = {
                                navController.navigate(Destinations.DebugListenTogether.route)
                            },
                            onOpenUsbExclusiveDebug = {
                                navController.navigate(Destinations.DebugUsbExclusive.route)
                            },
                            onOpenYouTubeDebug = {
                                navController.navigate(Destinations.DebugYouTube.route)
                            },
                            onOpenBiliDebug = {
                                navController.navigate(Destinations.DebugBili.route)
                            },
                            onOpenNeteaseDebug = {
                                navController.navigate(Destinations.DebugNetease.route)
                            },
                            onOpenSearchDebug = {
                                navController.navigate(Destinations.DebugSearch.route)
                            },
                            onOpenLogs = {
                                navController.navigate(Destinations.DebugLogsList.route)
                            },
                            onOpenCrashLogs = {
                                navController.navigate(Destinations.DebugCrashLogsList.route)
                            },
                            onTestExceptionHandler = crashActionOwner::dispatch,
                            onHideDebugMode = {
                                scope.launch { repo.setDevModeEnabled(false) }
                                navController.navigate(Destinations.Settings.route) {
                                    popUpTo(Destinations.Debug.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            )
                        }
                }
                selectMainTabRouteContent(
                    route = route,
                    home = homeContent,
                    explore = exploreContent,
                    library = libraryContent,
                    settings = settingsContent,
                    debug = debugContent
                )?.invoke()
            }

            val backgroundMode = resolveNowPlayingBackgroundMode(
                nowPlayingDynamicBackgroundEnabled,
                nowPlayingCoverBlurBackgroundEnabled,
                nowPlayingAudioReactiveEnabled
            )

            NowPlayingPresentationEffects(
                showNowPlaying = showNowPlaying,
                lifecycleResumed = lifecycleResumed,
                // 电视：永远不开「音频响应」。
                // 它会在音频链路上挂一个 TeeAudioProcessor，**每个 PCM buffer 都在 Kotlin 里
                // 逐样本算能量/节拍**，而且只在播放页打开时才启用/禁用——等于每次进出播放页
                // 都要重配一次音频管线。在 4 核 1.45GHz 的盒子上这是实打实的持续 CPU 负载，
                // 而它算出来的 level/beat 目前在 UI 里没有任何消费者。
                audioReactiveEnabled = backgroundMode.audioReactive && !LocalIsTelevision.current,
                keepScreenOn = nowPlayingKeepScreenOn
            )

            val onOpenCurrentPlaybackSource = playbackSourceNavigationAction(
                currentPlaybackSourceRoute,
                ::navigateToPlaybackSourceRoute
            )

            CompositionLocalProvider(
                LocalSharedCoverFlight provides sharedCoverFlight,
                LocalHasCustomBackground provides hasCustomBackground,
                LocalPredictiveBackPreviewEnabled provides predictiveBackPreviewEnabled,
                // 电视：连续按键期间不发起新的封面加载，把帧预算让给焦点滚动。
                // 下发的是追踪器对象（引用稳定），不是 busy 布尔值 —— 后者会让这里整棵树重组。
                LocalTvInputTracker provides if (LocalIsTelevision.current) tvInputTracker else null
            ) {
            AdvancedGlassHost(
                controller = advancedGlassController,
                backgroundBackdrop = backgroundGlassBackdrop,
                contentBackdrop = contentGlassBackdrop,
                activeNavigationOwners = activeAdvancedGlassOwners,
                disableStretchOverscroll = hasCustomBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 只观察，不消费：方向键照常往下传，只是顺手记一笔「用户还在连续操作」
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                // 用户接管了：正在逐帧归还的焦点立刻让位，别再拽回去
                                TvReturnFocus.onUserInput()
                                if (event.key.isTvDirectionKey()) {
                                    tvInputTracker.onDirectionKey()
                                    TvInputTrace.onKeyDown(event.key)
                                }
                            }
                            false
                        }
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .captureAdvancedGlassBackdrop(backgroundGlassBackdrop)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    CustomBackground(
                        imageUri = backgroundImageUri,
                        blur = backgroundImageBlur,
                        alpha = effectiveBackgroundImageAlpha
                    )
                }

                    AppStartupDestinationEffect(
                        navController = navController,
                        currentRoute = currentRoute,
                        showHomeTab = showHomeTab,
                        effectiveStartDestination = effectiveStartDestination,
                        defaultStartDestination = defaultStartDestination,
                        navHostStartDestination = navHostStartDestination
                    )

                    // 播放层退场结束前，底层重建的页面也不能抢走系统返回
                    AppUnderlayBackScope(enabled = !showNowPlaying && !nowPlayingOverlayMounted) {
                    Box(
                        Modifier.fillMaxSize()
                            .nowPlayingBackdropRecede(sharedCoverFlight)
                            .drawNowPlayingUnderlay(
                                flight = sharedCoverFlight,
                                playbackVisible = { showNowPlaying },
                                backProgress = { nowPlayingDismissState.progress },
                                // 电视：播放页整屏遮挡，一出现就是完整覆盖（进出没有动画了）
                                alwaysCovered = LocalIsTelevision.current
                            )
                    ) {
                    AppNavigationScaffold(
                        bottomBar = AppBottomBarPresentation(
                            items = bottomBarItems,
                            currentDestination = navigationVisibility.destination,
                            showNowPlaying = shouldSuppressPlaybackNavigation(
                                showNowPlaying,
                                backGestureRevealing = nowPlayingBackRevealing.value
                            ),
                            offlineMode = offlineMode,
                            alwaysUseNewTabStyle = true,
                            backgroundImageUri = backgroundImageUri
                        ),
                        hasCurrentSong = currentSong != null,
                        isPlaying = playerIsPlaying,
                        baseBlurRequested = advancedGlassController.isBaseBlurRequested,
                        snackbarHostState = snackbarHostState,
                        onMainTabSelected = { route ->
                            // 点标签 = 那个标签回到根：不记得上次开着哪个歌单
                            navigateToMainTab(route, resetTabMemory = true)
                        },
                        onTabConfirm = { route ->
                            // 侧栏只知道 route：详情态和根态都发这一条，由各标签宿主自己分流。
                            // 详情态 = 回根 + 回顶，根态 = 原来的刷新逻辑。
                            TvTabRootBus.request(route)
                        },
                        onOpenNowPlaying = {
                            showNowPlaying = true
                        }
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            val tvFocusManager = LocalFocusManager.current
                            AdvancedGlassNavigationHandoff(
                                enabled = shouldUseAdvancedGlassNavigationHandoff(
                                    visibleNavigationRoutes
                                )
                            ) {
                                PredictiveBackPreviewGate(previewEnabled = predictiveBackPreviewEnabled) {
                                AppNavigationGraph(
                                    owner = AppNavigationGraphOwner(
                                        navController = navController,
                                        presentation = AppNavigationGraphPresentation(
                                            navHostStartDestination,
                                            coherentFeedbackEnabled,
                                            offlineMode
                                        ) { route, content ->
                                            navigationSceneRenderer.run {
                                                RenderNavHostScene(route, content)
                                            }
                                        },
                                        mediaActions = AppNavigationMediaActions(
                                            playSongs = { songs, index, sourceRoute ->
                                                playSongsAndOpenNowPlaying(
                                                    songs,
                                                    index,
                                                    sourceRoute
                                                )
                                            },
                                            playBiliAudio = ::playBiliAudioAndOpenNowPlayingWithSource,
                                            playBiliParts = ::playBiliPartsAndOpenNowPlayingWithSource,
                                            onNeteaseAlbumClick = { navigateToNeteaseAlbum(it) },
                                            onYouTubePlaylistClick = ::navigateToYouTubeMusicPlaylist,
                                            onYouTubeCreatorClick = ::navigateToYouTubeMusicCreator
                                        )
                                    )
                                )
                                }
                            }
                            MainTabPredictiveBackHandler(
                                navController, mainTabTransitionState, selectedMainTabRoute,
                                enabled = !showNowPlaying && !nowPlayingOverlayMounted && pendingMainTabRoute == null,
                                onBackCommitted = { selectedMainTabRoute = it }
                            )
                            // 电视返回键：页面跳转交给系统/各页自己的返回处理，这里只管
                            // 「已经停在首页」的情况——光标不在首页标签上时先回到首页标签，
                            // 已经在首页标签上时 2 秒内再按一次退出应用
                            var tvLastBackAt by remember { mutableStateOf(0L) }
                            var tvBackHomeFocusTick by remember { mutableIntStateOf(0) }
                            // 一级页面按返回只收光标（不切页）时的落点：本页标签
                            var tvBackTabTarget by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(tvBackTabTarget) {
                                val target = tvBackTabTarget ?: return@LaunchedEffect
                                settleTopBarFocusOn(target)
                                tvBackTabTarget = null
                            }
                            // 一级页面按返回 = 回首页，光标也跟着落到「首页」标签上。
                            // 只等两帧然后请求一次是不够的：页面切换期间旧层还在销毁，
                            // 焦点会被「节点移除 → 交给树里第一个可聚焦元素」的兜底覆盖掉。
                            // 这里改成逐帧重试，直到光标真的落在首页标签上为止。
                            LaunchedEffect(tvBackHomeFocusTick) {
                                if (tvBackHomeFocusTick == 0) return@LaunchedEffect
                                settleTopBarFocusOn(Destinations.Home.route)
                            }
                            // 「在二级页面按返回」走的是 MainTabPredictiveBackHandler，
                            // 不经过上面那个 tick。主标签切回首页时同样要把光标稳回来。
                            LaunchedEffect(selectedMainTabRoute) {
                                if (selectedMainTabRoute != Destinations.Home.route) {
                                    return@LaunchedEffect
                                }
                                settleTopBarFocusOn(Destinations.Home.route)
                            }
                            // 冷启动：光标默认落在「首页」标签上。以前没人接，Compose 会把
                            // 光标丢给树里第一个可聚焦元素（「正在播放」那一格），第一次按
                            // 方向键等于从那里起步。这里主动请求一次，按键就从首页开始走。
                            LaunchedEffect(tvDevice) {
                                if (!tvDevice) return@LaunchedEffect
                                repeat(30) {
                                    withFrameNanos { }
                                    if (TvTopBarFocus.requestTab(Destinations.Home.route)) {
                                        return@LaunchedEffect
                                    }
                                }
                            }
                            // 电视：播放页退出后把光标送回**打开它的那个入口**（歌曲行 / 卡片 / 顶栏那格）。
                            // 来源锚点由 AppNowPlayingOverlay 在 clearFocus 之前抓下来 —— 那一刻之后
                            // 焦点树已经清空，再想找「从哪进来的」就晚了。
                            // 等浮层真正卸载（nowPlayingOverlayMounted 也 false）再归还，
                            // 否则请求会被「节点移除 → 交给第一个可聚焦元素」的兜底顶回来。
                            var nowPlayingWasOpen by remember { mutableStateOf(showNowPlaying) }
                            LaunchedEffect(showNowPlaying, nowPlayingOverlayMounted) {
                                if (showNowPlaying) {
                                    nowPlayingWasOpen = true
                                    return@LaunchedEffect
                                }
                                if (!nowPlayingWasOpen || nowPlayingOverlayMounted) return@LaunchedEffect
                                nowPlayingWasOpen = false
                                val anchor = TvReturnFocus.consumeNowPlayingSource()
                                    ?: return@LaunchedEffect
                                if (!TvReturnFocus.restoreAnchor(anchor)) {
                                    // 兜底落本页标签，绝不落「正在播放」那一格
                                    TvTopBarFocus.currentTabRoute?.let { settleTopBarFocusOn(it) }
                                }
                            }
                            // 只有「四个主标签的页根」才由这里接管返回键。
                            // 不能用 previousBackStackEntry == null 判断：主标签之间切换走的是
                            // navigate + popUpTo(start)，栈里始终留着上一级页面，这个条件恒为假，
                            // 于是按返回直接落到 MainTabPredictiveBackHandler 上 ——
                            // 表现就是「一级页面按返回，跟原来一样跳回首页」。
                            val atMainTabRoot = currentRoute != null && currentRoute in TvMainTabRoutes
                            BackHandler(
                                enabled = tvDevice && atMainTabRoot &&
                                    pendingMainTabRoute == null &&
                                    !showNowPlaying && !nowPlayingOverlayMounted
                            ) {
                                // 一级页面按返回：光标不在**本页标签**上时，先收到本页标签上，不切页面。
                                // 「正在播放」伪标签也走这一支 —— 它的 route 永远不等于任何真实标签，
                                // 所以焦点停在那一格时按返回，只会把光标收回本页标签。
                                // 判定用 barHasFocus（读焦点树）+ focusedTabRoute 两个信号，
                                // 任一为假都按「不在标签栏」处理 —— 宁可只是把光标收上去，也不误跳页。
                                // （原来只看 focusedTabRoute，它在层切换时会失同步，于是表现为
                                //   「页面跳到首页、焦点框却没跟过去，再按两下直接触发首页的退出确认」。）
                                val pageTab = TvTopBarFocus.currentTabRoute
                                    ?: currentRoute
                                    ?: selectedMainTabRoute
                                val inTabBar = TvTopBarFocus.barHasFocus &&
                                    TvTopBarFocus.focusedTabRoute != null
                                if (!inTabBar || TvTopBarFocus.focusedTabRoute != pageTab) {
                                    tvLastBackAt = 0L
                                    tvBackTabTarget = pageTab
                                    TvTopBarFocus.requestTab(pageTab)
                                    return@BackHandler
                                }
                                if (selectedMainTabRoute != Destinations.Home.route) {
                                    tvLastBackAt = 0L
                                    // 回的是「首页根」：连列表位置一起复位
                                    navigateToMainTab(Destinations.Home.route, resetTabMemory = true)
                                    tvBackHomeFocusTick++
                                    return@BackHandler
                                }
                                val nowMs = SystemClock.elapsedRealtime()
                                if (tvLastBackAt != 0L && nowMs - tvLastBackAt <= TV_EXIT_BACK_WINDOW_MS) {
                                    (context as? Activity)?.finish()
                                } else {
                                    tvLastBackAt = nowMs
                                    AppFeedback.showToast(context, "再按一次返回键退出")
                                }
                            }
                            // 电视：路由变化后不再干预光标，落点交给 Compose 自己决定。
                            // （曾在这里把光标往「本页标签」送，反而打乱了原生行为：
                            //  进二级页落不到返回按钮、返回时也回不到原来那张卡片。）
                            // 播放页关掉之后不主动抢焦点：Compose 会把光标还回打开播放页的那个按钮
                            // （用户要求「从哪首歌进去，返回就回到那首歌」）
                            MainTabLayerHost(
                                selectedRoute = selectedMainTabRoute,
                                transitionState = mainTabTransitionState,
                                backHandlingEnabled = currentRoute == selectedMainTabRoute,
                                saveableStateHolder = mainTabSaveableStateHolder,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { size ->
                                        if (size.height > 0) {
                                            mainTabDetailContentHeightPx = size.height
                                        }
                                    }
                                    .offset {
                                        IntOffset(
                                            x = 0,
                                            y = (
                                                    mainTabNavigationMotion().tabLayerTransform
                                                        .translationYFraction *
                                                            mainTabDetailContentHeightPx
                                                    ).roundToInt()
                                        )
                                    }
                                    .graphicsLayer {
                                        val tabLayerTransform = mainTabNavigationMotion().tabLayerTransform
                                        scaleX = tabLayerTransform.scale
                                        scaleY = tabLayerTransform.scale
                                        alpha = tabLayerTransform.alpha
                                        transformOrigin = TransformOrigin.Center
                                    }
                                    .zIndex(MAIN_TAB_LAYER_Z_INDEX),
                                onVisibleGlassOwnersChanged = {
                                    visibleMainTabGlassOwners = it
                                },
                                content = { route ->
                                    RenderMainTabRoute(route)
                                }
                            )
                        }
                    }
                    }
                    }

                    AppNowPlayingOverlay(
                        visible = showNowPlaying,
                        cover = NowPlayingOverlayCover(
                            url = playbackVisualCoverUrl,
                            songKey = currentSongVisualKey,
                            song = currentSong,
                            assetRefreshKey = coverAssetRefreshKey,
                            assetRefresh = coverAssetRefresh
                        ),
                        queueFlow = PlayerManager.currentQueueFlow,
                        theme = NowPlayingOverlayTheme(
                            dynamicColorEnabled = dynamicColorEnabled,
                            activeCoverSeedHex = activeCoverSeedHex,
                            seedColorHex = themeSeedColor,
                            paletteStyle = themePaletteStyle,
                            colorSpec = themeColorSpec,
                            uiStyle = uiStyle,
                            liquidGlass = advancedGlassController.isLiquidGlassEnabled
                        ),
                        background = NowPlayingOverlayBackground(
                            blurEnabled = nowPlayingCoverBlurBackgroundEnabled,
                            blurAmount = nowPlayingCoverBlurAmount,
                            blurDarken = nowPlayingCoverBlurDarken,
                            dynamicEnabled = backgroundMode.dynamic,
                            offlineMode = offlineMode
                        ),
                        onVisibilityChanged = { mounted ->
                            nowPlayingOverlayMounted = mounted
                            // 播放页真挂上了（全屏盖住），标签底板恢复；
                            // 这一下发生在最上层，用户看不到「底板重新亮起」。
                            if (mounted) TvTopBarFocus.suppressSelectionPill = false
                            latestOnNowPlayingVisibilityChanged(mounted)
                        },
                        onClose = {
                            // 电视返回键走的就是这里（PredictiveBackHandler → onDismiss）。
                            // 先交还焦点再收浮层：被移除的播放键本就不持有焦点，
                            // 帧末「交给第一个可聚焦元素」的兜底不会发生。
                            // 只在电视做：手机浮层有 150ms fade，提前移焦点会看到焦点环
                            // 先在下面那层出现。
                            if (tvDevice) TvReturnFocus.restoreNowPlayingSourceNow()
                            showNowPlaying = false
                        },
                        dismissState = nowPlayingDismissState
                    ) {
                        NowPlayingScreen(
                            onNavigateUp = {
                                if (restoreNowPlayingAfterDetail) {
                                    // 详情页正在下面滑进来（转场是「从主界面底下往上滑」）。
                                    // 这里立刻收播放页的话，中间就会闪一下主界面。等它进场结束再收，
                                    // 视觉上就是「播放页 -> 直接到详情页」。
                                    // 这条路上绝不能归还焦点：详情页顶栏刚拿到焦点，
                                    // 归还等于从它手里抢走（这就是「去程闪标签」的来源）。
                                    TvReturnFocus.discardNowPlayingSource()
                                    scope.launch {
                                        delay(MAIN_TAB_DETAIL_OPEN_DURATION_MS.toLong())
                                        showNowPlaying = false
                                    }
                                } else {
                                    if (tvDevice) TvReturnFocus.restoreNowPlayingSourceNow()
                                    showNowPlaying = false
                                }
                            },
                            onPhoneLandscapeBack = onPhoneLandscapeBack,
                            onOpenCurrentPlaybackSource = onOpenCurrentPlaybackSource?.let { open ->
                                { openDetailFromNowPlaying(open) }
                            },
                            showLyricsScreen = showNowPlayingLyrics,
                            onShowLyricsScreenChange = { showNowPlayingLyrics = it },
                            onEnterAlbum = { album ->
                                val shouldRestoreLyrics = showNowPlayingLyrics
                                openDetailFromNowPlaying {
                                    navigateToNeteaseAlbum(album) {
                                        if (shouldRestoreLyrics) {
                                            restoreLyricsAfterAlbumBack = true
                                        }
                                    }
                                }
                            },
                            onEnterArtist = { artist ->
                                openDetailFromNowPlaying { navigateToNeteaseArtist(artist) }
                            },
                            onEnterBiliUploader = { uploader ->
                                openDetailFromNowPlaying { navigateToBiliUploader(uploader) }
                            },
                            onEnterYouTubeCreator = { creator ->
                                openDetailFromNowPlaying { navigateToYouTubeMusicCreator(creator) }
                            },
                            onEnterPlaylist = { playlist ->
                                openDetailFromNowPlaying { navigateToNeteasePlaylist(playlist) }
                            },
                            lyricBlurEnabled = lyricBlurEnabled,
                            lyricBlurAmount = lyricBlurAmount,
                            lyricFontScales = lyricFontScales,
                            onLyricFontScaleChange = { target, scale ->
                                scope.launch { repo.setLyricFontScale(target, scale) }
                            },
                            advancedLyricsEnabled = advancedLyricsEnabled,
                            showCoverSourceBadge = showCoverSourceBadge,
                            showLyricTranslation = showLyricTranslation,
                            offlineMode = offlineMode,
                            resolvedCoverUrl = displayCoverUrl,
                            visualCoverUrl = playbackVisualCoverUrl,
                            playbackSongKey = currentSongVisualKey,
                            playbackSongKeyAliases = currentSongVisualKeyAliases,
                            visualCoverSongKey = playbackVisualCoverState.ownerSongKey
                        )
                    }

                    AppThemeRevealOverlayHost(
                        presentation = appThemeRevealPresentation(
                            themeRevealOriginWindow,
                            themeRevealFallbackColorArgb,
                            themeRevealCaptureToken,
                            themeRevealReverse
                        ),
                        snapshot = themeRevealSnapshot,
                        startRadiusPx = themeRevealStartRadiusPx,
                        onFinished = finishThemeReveal
                    )

                    AppTrafficRiskDialogHost(
                        request = pendingTrafficRiskDownloadRequest.takeIf { canShowGlobalDialogs },
                        onConfirm = { request ->
                            pendingTrafficRiskDownloadRequest = null
                            GlobalDownloadManager.confirmTrafficRiskDownload(context, request)
                        },
                        onDismiss = { pendingTrafficRiskDownloadRequest = null }
                    )

                    AppUsbBackgroundPermissionDialogHost(
                        visible = showUsbExclusiveBackgroundPermissionDialog && canShowGlobalDialogs &&
                            pendingTrafficRiskDownloadRequest == null,
                        readBatteryOptimizationAllowed = {
                            context.readBackgroundBehaviorAllowance().ignoringBatteryOptimizations
                        },
                        onRequestBatteryOptimization = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            context.requestIgnoreBatteryOptimizationsCompat()
                        },
                        onOpenAppSettings = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            context.openAppBackgroundSettings()
                        },
                        onNeverShowAgain = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            scope.launch {
                                repo.setUsbExclusiveBackgroundPermissionPromptSuppressed(true)
                            }
                        },
                        onDismiss = { showUsbExclusiveBackgroundPermissionDialog = false }
                    )

            }
        }
            }
    }
}
