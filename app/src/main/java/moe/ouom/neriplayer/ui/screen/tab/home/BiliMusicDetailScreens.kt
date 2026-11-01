package moe.ouom.neriplayer.ui.screen.tab.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import moe.ouom.neriplayer.ui.i18n.asString
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.haptic.HapticFilledIconButton
import moe.ouom.neriplayer.ui.screen.tab.library.LibrarySecondaryTabs
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.tvClampLeft
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import moe.ouom.neriplayer.ui.component.playlist.NeriSongRow
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.component.playlist.PlaylistChromeTopBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.ui.tv.rememberTvMenuFirstItemFocus
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.random.Random
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistActionSheetBleed
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistChromeTopBarHeight
import moe.ouom.neriplayer.ui.screen.playlist.playlistTopBarBleed
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistTopBarCoverBackdrop
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernActionSheet
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernHeroHeader
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernHeroHeight
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernListItemSurface
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernPlaybackActions
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernVisualColorsProvider
import moe.ouom.neriplayer.ui.screen.playlist.tvPlaylistTopBarShape
import moe.ouom.neriplayer.ui.theme.LocalHasCustomBackground
import moe.ouom.neriplayer.ui.tv.tvScrollToTopOnUp

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.local.playlist.LocalPlaylistRepository
import moe.ouom.neriplayer.data.local.playlist.launchLocalPlaylistMutation
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicHotVideo
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicRankSong
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicRankType
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableBiliVideoItem
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableHotVideoItems
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableRankVideoItems
import moe.ouom.neriplayer.platform.bilibili.api.image.buildBiliThumbnailUrl
import moe.ouom.neriplayer.platform.bilibili.playback.resolver.buildBiliSongAlbum
import moe.ouom.neriplayer.ui.component.common.NeriChoiceChip
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel
import moe.ouom.neriplayer.util.format.formatDate
import moe.ouom.neriplayer.util.format.formatDurationSec
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import moe.ouom.neriplayer.util.platform.tryStartActivity

/**
 * 「Bilibili 音乐榜」查看全部页面。
 *
 * 与首页共用同一个 [HomeViewModel]（同一 ViewModelStoreOwner），因此榜单类型、期数与曲目状态保持一致，
 * 不在这里重复发起请求，除非用户主动刷新或切换榜单类型。
 *
 * 详情页打开时首页不再组合，[HomeScreen] 的 `setOfflineMode` 不会执行，所以这里必须把 [offlineMode]
 * 同步给共享 ViewModel，刷新、切榜等入口才会走仅缓存分支而不是继续发网络请求。
 *
 * 返回手势由 [moe.ouom.neriplayer.ui.screen.host.HomeHostScreen] 的预测性返回转场统一接管，
 * 此处不注册 [BackHandler]，只保留 toolbar 导航用的 [onBack]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BiliMusicRankDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean,
    activeRankType: BiliMusicRankType,
    onRankTypeSelected: (BiliMusicRankType) -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(appContext as Application)
            }
        }
    )
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val state = ui.biliMusicRank
    // 头图 / 玻璃卡片 / 顶栏背景都拿第一首的封面（顶栏要用，所以提到 Scaffold 外面）
    val heroCoverUrl = remember(state.songs) {
        state.songs.firstOrNull()?.videoCover
            ?.takeIf { it.isNotBlank() }
            ?.let { buildBiliThumbnailUrl(imageUrl = it, width = 640, height = 360) }
            .orEmpty()
    }
    // 顶栏要跟着头图滚动状态变化，列表状态提到这里
    val listState = rememberLazyListState()
    // 播放中时详情页会被常驻迷你播放器遮住底部，列表末尾需要额外预留它的高度
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    // 详情页打开时首页不再组合，HomeScreen 的 wasOffline 恢复刷新不会抵达这个目的地；因此这里
    // 自己负责同步 offlineMode，并在从离线切回在线时重启一次因离线被跳过而遗留的空加载。
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(offlineMode) {
        val cameBackOnline = wasOffline && !offlineMode
        vm.setOfflineMode(offlineMode)
        if (cameBackOnline) {
            wasOffline = false
            val current = vm.uiState.value.biliMusicRank
            if (current.songs.isEmpty() && !current.loading) {
                vm.loadBiliMusicRank(force = true)
            }
        } else if (offlineMode) {
            wasOffline = true
        }
    }
    // 榜单类型由宿主持有（首页「查看全部」时记住的选中项，进程恢复后同样带回），而共享
    // HomeViewModel 可能已被重建回默认 HOT；这里把它对齐一次，避免展示与用户选择不一致的榜单。
    LaunchedEffect(activeRankType) {
        if (activeRankType != state.type) {
            vm.selectBiliMusicRankType(activeRankType)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PlaylistChromeTopBar(
                title = stringResource(CoreCommonR.string.home_bili_music_rank),
                coverUrl = heroCoverUrl,
                offlineMode = offlineMode,
                heroHeight = PlaylistModernHeroHeight,
                listState = listState,
                onBack = onBack,
                actions = {
                    HapticIconButton(
                        enabled = !offlineMode,
                        onClick = { vm.loadBiliMusicRank(force = true) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(CoreCommonR.string.recommend_refresh)
                        )
                    }
                }
            )
        }
    ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = {
                    if (!offlineMode) {
                        vm.loadBiliMusicRank(force = true)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val playableVideos = remember(state.songs) {
                    state.songs.toPlayableRankVideoItems()
                }
                // 头图 / 玻璃卡片都拿第一首的封面当底图（和歌单详情页同一套）
                val heroTitle = stringResource(CoreCommonR.string.home_bili_music_rank)
                val periodText = state.period?.let {
                    stringResource(CoreCommonR.string.home_bili_music_rank_period, it.periodNumber)
                }
                val updatedText = state.period?.takeIf { it.publishTimeSec > 0L }?.let {
                    stringResource(
                        CoreCommonR.string.home_bili_music_rank_updated,
                        formatDate(it.publishTimeSec * 1000L)
                    )
                }
                val heroSubtitle = listOfNotNull(
                    periodText,
                    updatedText,
                    stringResource(CoreCommonR.string.home_bili_music_source_note)
                ).joinToString(" · ")
                val shuffleEnabled by PlayerManager.shuffleModeFlow.collectAsState()
                val repeatMode by PlayerManager.repeatModeFlow.collectAsState()
                val hasCustomBackground = LocalHasCustomBackground.current
                when {
                    state.songs.isNotEmpty() -> {
                        PlaylistModernVisualColorsProvider(
                            coverUrl = heroCoverUrl,
                            offlineMode = offlineMode
                        ) {
                            LazyColumn(
                                state = listState,
                                // 和歌单详情页完全一致的边距：外层不缩进，各行自带内边距
                                contentPadding = PaddingValues(
                                    start = pageSidePaddingOr(0.dp),
                                    end = pageSidePaddingOr(0.dp),
                                    bottom = 24.dp + miniPlayerHeight
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "bili-music-rank-hero") {
                                    PlaylistModernHeroHeader(
                                        displayName = heroTitle,
                                        coverUrl = heroCoverUrl,
                                        subtitle = heroSubtitle,
                                        offlineMode = offlineMode,
                                        height = PlaylistModernHeroHeight,
                                        // 头图继续铺到「全部播放」那一行下面
                                        topBleed = playlistTopBarBleed(),
                                            backgroundBleed = PlaylistActionSheetBleed
                                    )
                                }
                                item(key = "bili-music-rank-actions") {
                                    PlaylistModernActionSheet(
                                        coverUrl = heroCoverUrl,
                                        offlineMode = offlineMode,
                                        hasCustomBackground = hasCustomBackground,
                                        overHeroImage = true
                                    ) {
                                        PlaylistModernPlaybackActions(
                                            // 头图不是可聚焦节点：在第一屏内容上按上键先滚到顶
                                            modifier = Modifier.tvScrollToTopOnUp(listState),
                                            songCount = playableVideos.size,
                                            shuffleEnabled = shuffleEnabled,
                                            repeatMode = repeatMode,
                                            showExport = false,
                                            onPlayInOrder = {
                                                if (playableVideos.isNotEmpty()) {
                                                    onPlay(playableVideos, 0)
                                                }
                                            },
                                            onShufflePlay = {
                                                if (playableVideos.isNotEmpty()) {
                                                    onPlay(
                                                        playableVideos,
                                                        Random.nextInt(playableVideos.size)
                                                    )
                                                }
                                            },
                                            onToggleShuffle = {
                                                PlayerManager.setShuffle(!shuffleEnabled)
                                            },
                                            onCycleRepeatMode = {
                                                PlayerManager.cycleRepeatMode()
                                            },
                                            onExportToLocalPlaylist = {}
                                        )
                                    }
                                }
                                item(key = "bili-music-rank-types") {
                                    BiliMusicRankTypeRow(
                                        selected = state.type,
                                        onTypeSelected = { type ->
                                            // 详情页内切换榜单时同步回宿主，保证退出再进入或进程恢复后仍是这个榜单
                                            onRankTypeSelected(type)
                                            vm.selectBiliMusicRankType(type)
                                        }
                                    )
                                }
                                itemsIndexed(
                                    items = state.songs,
                                    key = { index, song ->
                                        song.musicId.ifBlank { "rank-row-$index-${song.creationBvid}" }
                                    }
                                ) { _, song ->
                                    PlaylistModernListItemSurface(
                                        coverUrl = heroCoverUrl,
                                        offlineMode = offlineMode
                                    ) {
                                        BiliMusicRankDetailRow(
                                            song = song,
                                            offlineMode = offlineMode,
                                            onClick = {
                                                val target = song.toPlayableBiliVideoItem()
                                                val index = if (target == null) {
                                                    -1
                                                } else {
                                                    playableVideos.indexOfFirst {
                                                        it.id == target.id && it.bvid == target.bvid
                                                    }
                                                }
                                                if (index >= 0) {
                                                    onPlay(playableVideos, index)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.loading -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_loading))

                    state.error != null -> BiliMusicDetailError(
                        detail = state.error.asString(),
                        onRetry = { vm.loadBiliMusicRank(force = true) }
                    )

                    else -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_bili_music_empty))
                }
            }
    }
}

/**
 * 「Bilibili 音乐热门」查看全部页面（音乐分区视频榜）。
 *
 * 与 [BiliMusicRankDetailScreen] 一样共用首页的 [HomeViewModel]，因此同样需要把 [offlineMode]
 * 同步给 ViewModel，并且不注册根级 [BackHandler]，让宿主的预测性返回转场继续生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BiliMusicHotDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(appContext as Application)
            }
        }
    )
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val state = ui.biliMusicHot
                val heroCoverUrl = remember(state.items) {
                    state.items.firstOrNull()?.coverUrl
                        ?.takeIf { it.isNotBlank() }
                        ?.let { buildBiliThumbnailUrl(imageUrl = it, width = 640, height = 360) }
                        .orEmpty()
                }
                // 顶栏要跟着头图滚动状态变化，列表状态提到这里
                val listState = rememberLazyListState()
    // 播放中时详情页会被常驻迷你播放器遮住底部，列表末尾需要额外预留它的高度
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    // 与 [BiliMusicRankDetailScreen] 同理：详情页打开时首页不再组合，HomeScreen 的 wasOffline
    // 恢复刷新不会抵达这个目的地；这里自己同步 offlineMode，并在从离线切回在线时重启一次因离线
    // 被跳过而遗留的空加载，否则会一直停在「暂无内容」直到手动刷新。
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(offlineMode) {
        val cameBackOnline = wasOffline && !offlineMode
        vm.setOfflineMode(offlineMode)
        if (cameBackOnline) {
            wasOffline = false
            val current = vm.uiState.value.biliMusicHot
            if (current.items.isEmpty() && !current.loading) {
                vm.loadBiliMusicHot(force = true)
            }
        } else if (offlineMode) {
            wasOffline = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PlaylistChromeTopBar(
                title = stringResource(CoreCommonR.string.home_bili_music_hot),
                coverUrl = heroCoverUrl,
                offlineMode = offlineMode,
                heroHeight = PlaylistModernHeroHeight,
                listState = listState,
                onBack = onBack,
                actions = {
                    HapticIconButton(
                        enabled = !offlineMode,
                        onClick = { vm.loadBiliMusicHot(force = true) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(CoreCommonR.string.recommend_refresh)
                        )
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = {
                if (!offlineMode) {
                    vm.loadBiliMusicHot(force = true)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val playableVideos = remember(state.items) {
                state.items.toPlayableHotVideoItems()
            }
            val heroTitle = stringResource(CoreCommonR.string.home_bili_music_hot)
            val shuffleEnabled by PlayerManager.shuffleModeFlow.collectAsState()
            val repeatMode by PlayerManager.repeatModeFlow.collectAsState()
            val hasCustomBackground = LocalHasCustomBackground.current
            when {
                state.items.isNotEmpty() -> {
                    PlaylistModernVisualColorsProvider(
                        coverUrl = heroCoverUrl,
                        offlineMode = offlineMode
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = pageSidePaddingOr(0.dp),
                                end = pageSidePaddingOr(0.dp),
                                bottom = 24.dp + miniPlayerHeight
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "bili-music-hot-hero") {
                                PlaylistModernHeroHeader(
                                    displayName = heroTitle,
                                    coverUrl = heroCoverUrl,
                                    subtitle = stringResource(CoreCommonR.string.home_bili_music_source_note),
                                    offlineMode = offlineMode,
                                    height = PlaylistModernHeroHeight,
                                    // 头图继续铺到「全部播放」那一行下面
                                    topBleed = playlistTopBarBleed(),
                                            backgroundBleed = PlaylistActionSheetBleed
                                )
                            }
                            item(key = "bili-music-hot-actions") {
                                PlaylistModernActionSheet(
                                    coverUrl = heroCoverUrl,
                                    offlineMode = offlineMode,
                                    hasCustomBackground = hasCustomBackground,
                                    overHeroImage = true
                                ) {
                                    PlaylistModernPlaybackActions(
                                        modifier = Modifier.tvScrollToTopOnUp(listState),
                                        songCount = playableVideos.size,
                                        shuffleEnabled = shuffleEnabled,
                                        repeatMode = repeatMode,
                                        showExport = false,
                                        onPlayInOrder = {
                                            if (playableVideos.isNotEmpty()) {
                                                onPlay(playableVideos, 0)
                                            }
                                        },
                                        onShufflePlay = {
                                            if (playableVideos.isNotEmpty()) {
                                                onPlay(
                                                    playableVideos,
                                                    Random.nextInt(playableVideos.size)
                                                )
                                            }
                                        },
                                        onToggleShuffle = {
                                            PlayerManager.setShuffle(!shuffleEnabled)
                                        },
                                        onCycleRepeatMode = {
                                            PlayerManager.cycleRepeatMode()
                                        },
                                        onExportToLocalPlaylist = {}
                                    )
                                }
                            }
                            itemsIndexed(
                                items = state.items,
                                key = { index, video ->
                                    video.bvid.ifBlank { "hot-row-$index-${video.aid}" }
                                }
                            ) { index, video ->
                                PlaylistModernListItemSurface(
                                    coverUrl = heroCoverUrl,
                                    offlineMode = offlineMode
                                ) {
                                    BiliMusicHotDetailRow(
                                        rank = index + 1,
                                        video = video,
                                        offlineMode = offlineMode,
                                        onClick = {
                                            val target = playableVideos.indexOfFirst {
                                                it.id == video.aid && it.bvid == video.bvid
                                            }
                                            if (target >= 0) {
                                                onPlay(playableVideos, target)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                state.loading -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_loading))

                state.error != null -> BiliMusicDetailError(
                    detail = state.error.asString(),
                    onRetry = { vm.loadBiliMusicHot(force = true) }
                )

                else -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_bili_music_empty))
            }
        }
    }
}

/** 详情页顶部的榜单类型切换（热门榜 / 原创榜），切换后由 ViewModel 重新加载对应期数 */
@Composable
private fun BiliMusicRankTypeRow(
    selected: BiliMusicRankType,
    onTypeSelected: (BiliMusicRankType) -> Unit
) {
    val types = BiliMusicRankType.values()
    val labels = types.map { type ->
        stringResource(
            when (type) {
                BiliMusicRankType.HOT -> CoreCommonR.string.home_bili_music_rank_hot
                BiliMusicRankType.ORIGINAL -> CoreCommonR.string.home_bili_music_rank_original
            }
        )
    }
    // 和媒体库「歌单/歌手」用同一套二级标签栏，左右边距也就一致了
    LibrarySecondaryTabs(
        labels = labels,
        selectedIndex = types.indexOf(selected).coerceAtLeast(0),
        onSelected = { index -> onTypeSelected(types[index]) },
        // 外层列表已不再缩进（和歌单详情页一致），这里自己补上标签栏该有的边距
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

/**
 * 「全部播放」：左边一个可聚焦的圆形播放图标按钮，右边是**不可点击**的纯文字标签。
 *
 * 和歌单详情页 `PlaylistModernPlaybackActions` 的播放那一段完全同构：
 * 44dp 的实心圆形图标按钮 + titleMedium 文字，外层边距 start 18 / end 22 / 上下 8。
 * 原来是一颗 FilledTonalButton，图标和文字都是同一个点击区、边距也和别处对不上。
 */
@Composable
private fun BiliMusicPlayAllButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(CoreCommonR.string.player_play_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 8.dp, end = 22.dp, bottom = 8.dp)
            // 电视：这一行在最左边，再按左不能再往上跳到主标签栏
            .tvClampLeft(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HapticFilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(50),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = label,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 榜单详情行：名次、100x60 缩略图、标题、歌手、播放来源与时长，右侧可跳转原视频页。
 */
@Composable
private fun BiliMusicRankDetailRow(
    song: BiliMusicRankSong,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val favoriteActions = rememberBiliSongFavoriteActions()
    val playableItem = remember(song) { song.toPlayableBiliVideoItem() }
    val songItem = remember(playableItem) { playableItem?.toSongItem() }
    val title = song.title.ifBlank { song.album }
    val uploader = song.singer.ifBlank { song.uploader }
    val thumbnailUrl = remember(song.videoCover) {
        buildBiliThumbnailUrl(imageUrl = song.videoCover, width = 192, height = 108)
    }
    val coverRequest = remember(context, thumbnailUrl, offlineMode) {
        offlineCachedImageRequest(
            context = context,
            data = thumbnailUrl,
            sizePx = 192,
            allowHardware = false,
            offlineMode = offlineMode
        )
    }
    // 不可播放的视频没有 SongItem，这里造一份只用于展示的占位
    val rowSong = songItem ?: SongItem(
        id = 0L,
        name = title,
        artist = uploader,
        album = "",
        albumId = 0L,
        durationMs = song.displayDurationSec * 1000L,
        coverUrl = song.videoCover,
        channelId = "bilibili",
        audioId = null
    )
    val sourceLabel = song.playbackSourceLabel()

    NeriSongRow(
        index = song.rank,
        song = rowSong,
        onClick = onClick,
        // 整行在最左边，按左不能再往上跳到主标签栏
        modifier = Modifier.tvClampLeft(),
        title = title,
        subtitleContent = {
            Column {
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // 时长挪到行尾三点前面了（和歌单页一致），这里只留来源
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        titleStyle = MaterialTheme.typography.bodyLarge,
        coverWidth = 100.dp,
        coverHeight = 60.dp,
        coverCornerRadius = 8.dp,
        coverContent = {
            AsyncImage(
                model = coverRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .clip(neriRoundedShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        offlineMode = offlineMode,
        durationText = formatDurationSec(song.displayDurationSec),
        indexTextStyle = MaterialTheme.typography.titleSmall,
        enabled = song.isPlayable,
        isFavorite = songItem != null && favoriteActions.lookup?.contains(songItem) == true,
        onFavoriteToggle = if (songItem != null) favoriteActions.toggle else null,
        onPlayNext = songItem?.let { item -> { PlayerManager.addToQueueNext(item) } },
        onAddToQueueEnd = songItem?.let { item -> { PlayerManager.addToQueueEnd(item) } }
    )
}

/**
 * 音乐热门详情行：与榜单详情行同构，第三行改为分区名、播放量与时长。
 *
 * @param rank 展示用名次（调用方传 index + 1）
 */
@Composable
private fun BiliMusicHotDetailRow(
    rank: Int,
    video: BiliMusicHotVideo,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val favoriteActions = rememberBiliSongFavoriteActions()
    val playableItem = remember(video) { video.toPlayableBiliVideoItem() }
    val songItem = remember(playableItem) { playableItem?.toSongItem() }
    val thumbnailUrl = remember(video.coverUrl) {
        buildBiliThumbnailUrl(imageUrl = video.coverUrl, width = 192, height = 108)
    }
    val coverRequest = remember(context, thumbnailUrl, offlineMode) {
        offlineCachedImageRequest(
            context = context,
            data = thumbnailUrl,
            sizePx = 192,
            allowHardware = false,
            offlineMode = offlineMode
        )
    }
    // 不可播放的视频没有 SongItem，这里造一份只用于展示的占位
    val rowSong = songItem ?: SongItem(
        id = 0L,
        name = video.title,
        artist = video.uploader,
        album = "",
        albumId = 0L,
        durationMs = video.durationSec * 1000L,
        coverUrl = video.coverUrl,
        channelId = "bilibili",
        audioId = null
    )
    val metaLabel = listOf(video.tname, formatPlayCount(context, video.viewCount))
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    NeriSongRow(
        index = rank,
        song = rowSong,
        onClick = onClick,
        modifier = Modifier.tvClampLeft(),
        title = video.title,
        subtitleContent = {
            Column {
                Text(
                    text = video.uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        titleStyle = MaterialTheme.typography.bodyLarge,
        coverWidth = 100.dp,
        coverHeight = 60.dp,
        coverCornerRadius = 8.dp,
        coverContent = {
            AsyncImage(
                model = coverRequest,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .clip(neriRoundedShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        offlineMode = offlineMode,
        durationText = formatDurationSec(video.durationSec),
        indexTextStyle = MaterialTheme.typography.titleSmall,
        enabled = video.isPlayable,
        isFavorite = songItem != null && favoriteActions.lookup?.contains(songItem) == true,
        onFavoriteToggle = if (songItem != null) favoriteActions.toggle else null,
        onPlayNext = songItem?.let { item -> { PlayerManager.addToQueueNext(item) } },
        onAddToQueueEnd = songItem?.let { item -> { PlayerManager.addToQueueEnd(item) } }
    )
}



/**
 * 榜单/热门页的收藏状态与切换。
 *
 * 这两个页面没有 ViewModel，直接用本地歌单仓库读「我喜欢」，和 B 站歌单页同一套
 * （`FavoriteSongLookup` + `FavoritesPlaylist`）。
 */
private class BiliSongFavoriteActions(
    val lookup: FavoriteSongLookup?,
    val toggle: (SongItem, Boolean) -> Unit
)

@Composable
private fun rememberBiliSongFavoriteActions(): BiliSongFavoriteActions {
    val context = LocalContext.current
    val repo = remember(context) { LocalPlaylistRepository.getInstance(context) }
    val playlists by repo.playlists.collectAsState(initial = emptyList())
    val lookup = remember(playlists, context) {
        FavoriteSongLookup(
            FavoritesPlaylist.firstOrNull(playlists, context)?.songs.orEmpty()
        )
    }
    val scope = rememberCoroutineScope()
    val addedText = stringResource(CoreCommonR.string.favorite_added)
    val removedText = stringResource(CoreCommonR.string.favorite_removed)
    val toggle: (SongItem, Boolean) -> Unit = remember(repo, scope, addedText, removedText, context) {
        { song, isFav ->
            val message = if (isFav) removedText else addedText
            scope.launchLocalPlaylistMutation(
                operation = "toggleBiliMusicRankFavorite",
                onResult = { result ->
                    if (result.isSuccess) {
                        AppFeedback.show(context = context, message = message)
                    }
                }
            ) {
                if (isFav) repo.removeFromFavorites(song) else repo.addToFavorites(song)
            }
        }
    }
    return BiliSongFavoriteActions(lookup = lookup, toggle = toggle)
}

/** B 站条目转成本地歌单/队列能用的条目（和 B 站歌单页同一套字段） */
private fun BiliVideoItem.toSongItem(): SongItem {
    return SongItem(
        id = id,
        name = title,
        artist = uploader,
        album = buildBiliSongAlbum(bvid = bvid),
        albumId = 0L,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = id.toString()
    )
}

/** 详情页的居中提示（加载中 / 无内容） */
@Composable
private fun BiliMusicDetailHint(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 详情页加载失败提示：错误详情 + 重试按钮 */
@Composable
private fun BiliMusicDetailError(
    detail: String,
    onRetry: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(CoreCommonR.string.home_bili_music_section_error_prefix, detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(CoreCommonR.string.recommend_refresh))
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** 在浏览器或 B 站客户端打开视频页，便于核对榜单曲目的实际来源 */
private fun openBiliVideoPage(context: android.content.Context, bvid: String) {
    if (bvid.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/video/$bvid"))
    context.tryStartActivity(intent)
}
