package moe.ouom.neriplayer.ui.screen.tab.home

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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomeScreen
 * Created: 2025/8/8
 */

import moe.ouom.neriplayer.ui.i18n.UiText
import moe.ouom.neriplayer.ui.i18n.asString
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.cleanCornerRadiusFor
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import android.app.Application
import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.download.GlobalDownloadManager
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.playlist.favorite.FavoritePlaylistRepository
import moe.ouom.neriplayer.data.local.playlist.LocalPlaylistRepository
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicRankType
import moe.ouom.neriplayer.data.model.playlist.LocalPlaylist
import moe.ouom.neriplayer.data.playlist.usage.PlaylistUsageRepository
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.model.stats.UsageEntry
import moe.ouom.neriplayer.data.playlist.usage.buildLocalPlaylistUsageLookup
import moe.ouom.neriplayer.data.playlist.usage.resolveLocalPlaylistUsageId
import moe.ouom.neriplayer.platform.youtube.api.transport.buildYouTubeMusicMediaUri
import moe.ouom.neriplayer.data.local.media.displayAlbum
import moe.ouom.neriplayer.ui.util.shouldAllowCollapsingTopAppBar
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.platform.youtube.api.transport.stableYouTubeMusicId
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeNeteasePlaylistSectionState
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeNeteaseSongSectionState
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeSectionState
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel
import moe.ouom.neriplayer.ui.viewmodel.tab.NeteaseHomeSongSource
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.favoriteId
import moe.ouom.neriplayer.ui.util.rememberSongDisplayCoverUrl
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP
import moe.ouom.neriplayer.ui.feedback.NeriOverlaySnackbarHost
import moe.ouom.neriplayer.ui.feedback.showNeriSnackbar
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicHomeShelf
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicHomeItem
import moe.ouom.neriplayer.platform.youtube.api.parser.YouTubeMusicParser
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import moe.ouom.neriplayer.util.format.formatPlayCount
import kotlin.math.ceil
import kotlin.math.min
import java.util.Locale
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvBringGridToTopOnUp
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOuterOr
import moe.ouom.neriplayer.ui.tv.LocalTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.ui.tv.rememberTvMenuFirstItemFocus
import moe.ouom.neriplayer.ui.tv.rememberTvDeferredImage
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvStripItemFocus
import moe.ouom.neriplayer.ui.tv.tvReturnFocus
import moe.ouom.neriplayer.ui.tv.tvStripClamp
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.ui.tv.TvTuning
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    showContinueCard: Boolean = true,
    showTrendingCard: Boolean = true,
    showRadarCard: Boolean = true,
    showRecommendedCard: Boolean = true,
    showBilibiliCard: Boolean = true,
    usageEntries: List<UsageEntry> = emptyList(),
    usageLoaded: Boolean = true,
    offlineMode: Boolean = false,
    continuePagerPage: Int = 0,
    onContinuePagerPageChanged: (Int) -> Unit = {},
    onItemClick: (PlaylistSummary) -> Unit = {},
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    gridState: LazyGridState,
    radarPlaylistListState: LazyListState,
    topAppBarState: TopAppBarState,
    onScrollAnchorIndexesChanged: (Map<String, Int>) -> Unit = {},
    onOpenRecent: (UsageEntry) -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onBiliMusicRankMore: (BiliMusicRankType) -> Unit = {},
    onBiliMusicHotMore: () -> Unit = {},
    onBiliMusicRankPlay: (List<BiliVideoItem>, Int) -> Unit = { _, _ -> },
    onBiliMusicHotPlay: (List<BiliVideoItem>, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = appContext as Application
                HomeViewModel(app)
            }
        }
    )
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var localPlaylistRepo by remember { mutableStateOf<LocalPlaylistRepository?>(null) }
    var localPlaylists by remember {
        mutableStateOf<List<LocalPlaylist>>(
            value = emptyList(),
            policy = referentialEqualityPolicy()
        )
    }
    var localPlaylistsReady by remember { mutableStateOf(false) }
    var localPlaylistSnapshotVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(appContext) {
        localPlaylistsReady = false
        val initializedRepo = withContext(Dispatchers.IO) {
            LocalPlaylistRepository.getInstance(appContext)
                .takeIf { repository -> repository.awaitInitialized() }
        }
        localPlaylistRepo = initializedRepo
        if (initializedRepo == null) return@LaunchedEffect
        initializedRepo.playlists.collect { playlists ->
            localPlaylists = playlists
            localPlaylistSnapshotVersion += 1
            localPlaylistsReady = true
        }
    }
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val downloadedSongs by GlobalDownloadManager.downloadedSongs.collectAsStateWithLifecycle()
    val downloadedPlaybackCoverCandidates = homeLocalFilesCoverCandidates(downloadedSongs)
    val favoriteKeys = remember(favorites) {
        favorites.mapTo(mutableSetOf()) { "${it.source}:${it.id}" }
    }
    val favoriteSongLookup = remember(localPlaylistSnapshotVersion, context) {
        FavoriteSongLookup(
            localPlaylists
                .firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
                ?.songs
                .orEmpty()
        )
    }
    val localPlaylistCoverFallbackIds = usageEntries.asSequence()
        .take(HomeContinueEntryLimit)
        .filter { entry ->
            entry.source == PlaylistUsageRepository.SOURCE_LOCAL &&
                    shouldValidateHomeContinueCoverReference(entry.picUrl)
        }
        .mapTo(linkedSetOf()) { entry -> resolveLocalPlaylistUsageId(entry, appContext) }
    val localPlaylistUsageLookup by produceState(
        initialValue = emptyMap(),
        key1 = localPlaylistSnapshotVersion,
        key2 = localPlaylistCoverFallbackIds,
        key3 = appContext
    ) {
        value = if (localPlaylistCoverFallbackIds.isEmpty()) {
            emptyMap()
        } else {
            withContext(Dispatchers.Default) {
                buildLocalPlaylistUsageLookup(
                    playlists = localPlaylists,
                    context = appContext,
                    requestedPlaylistIds = localPlaylistCoverFallbackIds,
                    maxSongsPerPlaylist = HomeLocalPlaylistCoverCandidateLimit
                )
            }
        }
    }

    val hasLocalPlaylistUsage = usageEntries.any { entry ->
        entry.source == PlaylistUsageRepository.SOURCE_LOCAL
    }
    val hasLocalArtistUsage = usageEntries.any { entry ->
        entry.source == PlaylistUsageRepository.SOURCE_LOCAL_ARTIST
    }
    LaunchedEffect(
        hasLocalPlaylistUsage,
        hasLocalArtistUsage,
        localPlaylistsReady,
        localPlaylistSnapshotVersion,
        downloadedPlaybackCoverCandidates
    ) {
        if ((hasLocalPlaylistUsage || hasLocalArtistUsage) && localPlaylistsReady) {
            withContext(Dispatchers.Default) {
                if (hasLocalPlaylistUsage) {
                    AppContainer.playlistUsageRepo.syncLocalEntries(
                        playlists = localPlaylists,
                        localFilesCoverCandidates = downloadedPlaybackCoverCandidates,
                        resolveLocalMetadataFallback = false
                    )
                }
                if (hasLocalArtistUsage) {
                    AppContainer.playlistUsageRepo.syncLocalArtistEntries(
                        playlists = localPlaylists,
                        resolveLocalMetadataFallback = false
                    )
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val guessYouLikeTitle = stringResource(CoreCommonR.string.home_ytmusic_guess_you_like)
    val dailyDiscoverTitle = stringResource(CoreCommonR.string.home_ytmusic_daily_discover)
    val moreRecommendationsTitle = stringResource(CoreCommonR.string.home_ytmusic_more_recommendations)
    val favoriteAddedText = stringResource(CoreCommonR.string.favorite_added)
    val favoriteRemovedText = stringResource(CoreCommonR.string.favorite_removed)
    val ytmSections = remember(ui.ytMusicHomeShelves.items) {
        classifyYouTubeMusicShelves(ui.ytMusicHomeShelves.items)
    }
    val hasVisibleYtMusicFeed = remember(ytmSections) {
        ytmSections.guessYouLike != null ||
            ytmSections.dailyDiscover != null ||
            ytmSections.remaining.any { shelf ->
                shelf.shouldRenderAsSongShelf() || shelf.hasRenderablePlaylistItems()
            }
    }
    val scope = rememberCoroutineScope()
    val showContinue = shouldShowHomeContinueSection(
        showContinueCard = showContinueCard,
        usageLoaded = usageLoaded,
        hasUsage = usageEntries.isNotEmpty()
    )
    val isInternational = ui.internationalizationEnabled
    val showBiliMusic = showBilibiliCard && ui.biliMusicHomeEnabled
    val showNeteaseTrending = showTrendingCard
    val showNeteaseRadar = showRadarCard
    val showOnlineFeeds = !offlineMode
    var wasOffline by remember { mutableStateOf(offlineMode) }
    val windowWidthDp = currentWindowWidthDp()
    val isTabletLayout = windowWidthDp >= 720.dp
    val isTabletDevice =
        LocalConfiguration.current.smallestScreenWidthDp >= PHONE_SMALLEST_SCREEN_WIDTH_DP
    val isTelevision = LocalIsTelevision.current
    // clean style trades a little density for calmer rhythm: wider gutters, larger tablet tiles
    val cleanStyle = NeriDesign.isClean
    val pageHorizontalPadding = if (isTabletLayout) 28.dp else 16.dp
    val gridMinCellSize = when {
        // 电视：格子更大、每屏卡片更少。10 列缩略图在弱 SoC 上要同时合成几十张封面，
        // 滑动时掉帧非常明显；5~6 列既看得清，也少一半的解码和绘制
        isTelevision -> if (isTabletLayout) 240.dp else 200.dp
        isTabletLayout && cleanStyle -> 172.dp
        isTabletLayout -> 156.dp
        // two large tiles per row on phones reads calmer than three thumbnails
        cleanStyle -> 150.dp
        else -> 120.dp
    }
    val gridContentPadding = if (isTabletLayout) 14.dp else 8.dp
    val gridSpacing = when {
        isTabletLayout && cleanStyle -> 18.dp
        isTabletLayout -> 14.dp
        cleanStyle -> 14.dp
        else -> 10.dp
    }
    val gridColumns = remember(gridMinCellSize, cleanStyle, isTabletLayout) {
        if (cleanStyle && !isTabletLayout) HomePhoneGridCells(gridMinCellSize)
        else GridCells.Adaptive(gridMinCellSize)
    }
    val appBarTitleInset = 0.dp // 标题栏已移除，保留变量避免影响其他引用
    fun toggleHomeSongFavorite(song: SongItem, isFavorite: Boolean) {
        scope.launch {
            val repository = localPlaylistRepo ?: withContext(Dispatchers.IO) {
                LocalPlaylistRepository.getInstance(appContext)
            }
            if (isFavorite) {
                repository.removeFromFavorites(song)
                snackbarHostState.showNeriSnackbar(favoriteRemovedText)
            } else {
                repository.addToFavorites(song)
                snackbarHostState.showNeriSnackbar(favoriteAddedText)
            }
        }
    }
    val showHomeSnackbar: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showNeriSnackbar(message)
        }
    }

    LaunchedEffect(offlineMode, isInternational) {
        vm.setOfflineMode(offlineMode)
        if (offlineMode) {
            wasOffline = true
            return@LaunchedEffect
        }

        if (!wasOffline) return@LaunchedEffect

        wasOffline = false
        if (isInternational) {
            vm.refreshYtMusicHome()
        } else {
            vm.refreshNeteaseHome()
        }
    }

    // 刷新入口挂在顶部「首页」标签上（见 NeriTvTopBar）：标签栏发信号，这里真正去拉数据
    LaunchedEffect(isInternational, offlineMode) {
        HomeRefreshBus.tick.drop(1).collect {
            if (offlineMode) return@collect
            // 刷新时顺带回顶部：停在原位的话，新内容加载出来也看不见
            runCatching { gridState.scrollToItem(0) }
            if (isInternational) {
                vm.refreshYtMusicHome()
            } else {
                vm.refreshNeteaseHome()
            }
        }
    }

    val hasVisibleBiliMusicSections = showBiliMusic && showOnlineFeeds && (
            ui.biliMusicRank.songs.isNotEmpty() ||
                    ui.biliMusicRank.loading ||
                    ui.biliMusicRank.error != null ||
                    ui.biliMusicHot.items.isNotEmpty() ||
                    ui.biliMusicHot.loading ||
                    ui.biliMusicHot.error != null
            )

    val hasVisibleSections =
            showContinue || hasVisibleBiliMusicSections ||
                    (showOnlineFeeds && (showNeteaseTrending || showNeteaseRadar || showRecommendedCard || isInternational))

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = if (isTabletDevice) 8.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    // 电视：左右留白跟其它页面用同一个值；同时不再套 1240dp 上限——
                    // 窗口宽度会随用户的缩放变化，这个上限会把整页压成中间的一条。
                    .padding(horizontal = pageSidePaddingOuterOr(pageHorizontalPadding), vertical = 4.dp)
                    .then(if (isTelevision) Modifier else Modifier.widthIn(max = 1240.dp))
                    .fillMaxWidth()
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (!hasVisibleSections) {
                    SideEffect {
                        onScrollAnchorIndexesChanged(emptyMap())
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (offlineMode) {
                                stringResource(CoreCommonR.string.home_offline_no_continue)
                            } else {
                                stringResource(CoreCommonR.string.home_all_cards_hidden)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Box
                }

                val miniPlayerHeight = LocalMiniPlayerHeight.current
                val homeLoadingText = stringResource(CoreCommonR.string.home_loading)
                val scrollAnchorIndexes = linkedMapOf<String, Int>()
                var nextGridItemIndex = 0
                fun registerGridItemKey(key: String): String {
                    scrollAnchorIndexes[key] = nextGridItemIndex
                    nextGridItemIndex += 1
                    return key
                }
                // Bilibili 音乐模块只在非国际化模式展示，排在私人雷达之后
                val addBiliMusicSections: LazyGridScope.() -> Unit = biliSections@ {
                    if (!showBiliMusic) return@biliSections
                    addBiliMusicRankSection(
                        registerKey = ::registerGridItemKey,
                        state = ui.biliMusicRank,
                        onTypeSelected = vm::selectBiliMusicRankType,
                        onMoreClick = { onBiliMusicRankMore(ui.biliMusicRank.type) },
                        onPlay = onBiliMusicRankPlay,
                        onRetry = { vm.loadBiliMusicRank(force = true) },
                        offlineMode = offlineMode
                    )
                    addBiliMusicHotSection(
                        registerKey = ::registerGridItemKey,
                        state = ui.biliMusicHot,
                        onMoreClick = onBiliMusicHotMore,
                        onPlay = onBiliMusicHotPlay,
                        onRetry = { vm.loadBiliMusicHot(force = true) },
                        offlineMode = offlineMode
                    )
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = gridColumns,
                    contentPadding = PaddingValues(
                        // 电视的左右边距由侧栏宽度 + 内容区右侧留白负责，这里不能再加，
                        // 否则内容会比其它页面多推 8dp
                        start = if (isTelevision) 0.dp else gridContentPadding,
                        end = if (isTelevision) 0.dp else gridContentPadding,
                        top = gridContentPadding,
                        bottom = gridContentPadding + miniPlayerHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    // 电视：顶部的「继续播放」表头是纯文字、拿不到焦点，只能靠上键把网格拉回顶部
                    modifier = Modifier.fillMaxSize().tvBringGridToTopOnUp(gridState)
                ) {
                    if (showContinue) {
                        item(
                            key = registerGridItemKey(HomeScrollKeyContinueHeader),
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            SectionHeader(
                                icon = Icons.Outlined.History,
                                title = stringResource(CoreCommonR.string.player_continue)
                            )
                        }
                        item(
                            key = registerGridItemKey(HomeScrollKeyContinueContent),
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            if (usageLoaded) {
                                val continueContext = LocalContext.current
                                ContinueSection(
                                    // 「我喜欢的音乐」钉死在第一个格子，其余按最近打开排序
                                    items = pinHomeContinueFavorites(
                                        usageEntries,
                                        continueContext
                                    ).take(HomeContinueEntryLimit),
                                    localPlaylistLookup = localPlaylistUsageLookup,
                                    localFilesCoverCandidates = downloadedPlaybackCoverCandidates,
                                    onClick = { entry -> onOpenRecent(entry) },
                                    savedPage = continuePagerPage,
                                    onPageChanged = onContinuePagerPageChanged,
                                    offlineMode = offlineMode
                                )
                            } else {
                                SectionLoadingState(homeLoadingText)
                            }
                        }
                    }

                    if (showOnlineFeeds) {
                        if (isInternational) {
                            if (showNeteaseTrending && ytmSections.guessYouLike != null) {
                                addYouTubeMusicSongShelfSection(
                                    sectionKey = HomeScrollKeyYtGuess,
                                    registerKey = ::registerGridItemKey,
                                    shelf = ytmSections.guessYouLike,
                                    icon = Icons.Outlined.Bolt,
                                    title = guessYouLikeTitle,
                                    onSongClick = onSongClick,
                                    favoriteSongLookup = favoriteSongLookup,
                                    onFavoriteToggle = ::toggleHomeSongFavorite,
                                    onShowSnackbar = showHomeSnackbar,
                                    offlineMode = offlineMode
                                )
                            }

                            if (showNeteaseRadar && ytmSections.dailyDiscover != null) {
                                addYouTubeMusicSongShelfSection(
                                    sectionKey = HomeScrollKeyYtDaily,
                                    registerKey = ::registerGridItemKey,
                                    shelf = ytmSections.dailyDiscover,
                                    icon = Icons.Outlined.Explore,
                                    title = dailyDiscoverTitle,
                                    onSongClick = onSongClick,
                                    favoriteSongLookup = favoriteSongLookup,
                                    onFavoriteToggle = ::toggleHomeSongFavorite,
                                    onShowSnackbar = showHomeSnackbar,
                                    offlineMode = offlineMode
                                )
                            }

                            if (showRecommendedCard) {
                                item(
                                    key = registerGridItemKey(HomeScrollKeyYtMoreHeader),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    SectionHeader(
                                        icon = Icons.Outlined.Star,
                                        title = moreRecommendationsTitle
                                    )
                                }

                                when {
                                    ui.ytMusicPlaylists.items.isNotEmpty() -> {
                                        ui.ytMusicPlaylists.items.forEach { playlist ->
                                            item(
                                                key = registerGridItemKey(
                                                    homeYtMusicPlaylistScrollKey(playlist)
                                                )
                                            ) {
                                                YtMusicPlaylistCard(
                                                    playlist = playlist,
                                                    isFavorite = favoriteKeys.contains("youtubeMusic:${playlist.favoriteId()}"),
                                                    onClick = { onYouTubeMusicPlaylistClick(playlist) },
                                                    onShowSnackbar = { message ->
                                                        scope.launch {
                                                            snackbarHostState.showNeriSnackbar(message)
                                                        }
                                                    },
                                                    offlineMode = offlineMode
                                                )
                                            }
                                        }
                                    }
                                    ui.ytMusicPlaylists.loading -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtMoreLoading),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionLoadingState(homeLoadingText)
                                        }
                                    }
                                    ui.ytMusicPlaylists.error != null -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtMoreError),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionErrorState(detail = ui.ytMusicPlaylists.error?.asString().orEmpty())
                                        }
                                    }
                                }

                                when {
                                    ytmSections.remaining.any { shelf ->
                                        shelf.shouldRenderAsSongShelf() || shelf.hasRenderablePlaylistItems()
                                    } -> {
                                        ytmSections.remaining.forEachIndexed { shelfIndex, shelf ->
                                            val shelfKey = homeYtMusicShelfScrollKey(
                                                shelfIndex = shelfIndex,
                                                title = shelf.title
                                            )
                                            if (shelf.shouldRenderAsSongShelf()) {
                                                addYouTubeMusicSongShelfSection(
                                                    sectionKey = shelfKey,
                                                    registerKey = ::registerGridItemKey,
                                                    shelf = shelf,
                                                    icon = Icons.Outlined.Explore,
                                                    title = shelf.title,
                                                    onSongClick = onSongClick,
                                                    favoriteSongLookup = favoriteSongLookup,
                                                    onFavoriteToggle = ::toggleHomeSongFavorite,
                                                    onShowSnackbar = showHomeSnackbar,
                                                    offlineMode = offlineMode
                                                )
                                            } else {
                                                val playlistItems = shelf.items.filter { it.isPlaylistItem() }
                                                if (playlistItems.isEmpty()) {
                                                    return@forEachIndexed
                                                }
                                                item(
                                                    key = registerGridItemKey("$shelfKey:header"),
                                                    span = { GridItemSpan(maxLineSpan) }
                                                ) {
                                                    SectionHeader(
                                                        icon = Icons.Outlined.Explore,
                                                        title = shelf.title
                                                    )
                                                }
                                                playlistItems.forEachIndexed { itemIndex, homeItem ->
                                                    item(
                                                        key = registerGridItemKey(
                                                            homeYtMusicHomeItemScrollKey(
                                                                shelfKey = shelfKey,
                                                                itemIndex = itemIndex,
                                                                item = homeItem
                                                            )
                                                        )
                                                    ) {
                                                        YtMusicHomeItemCard(
                                                            item = homeItem,
                                                            isFavorite = homeItem.toPlaylist()
                                                                ?.favoriteId()
                                                                ?.let { favoriteKeys.contains("youtubeMusic:$it") } == true,
                                                            onClick = {
                                                                val playlist = homeItem.toPlaylist()
                                                                if (playlist != null) {
                                                                    onYouTubeMusicPlaylistClick(playlist)
                                                                } else if (homeItem.videoId.isNotBlank()) {
                                                                    val songs = listOfNotNull(
                                                                        homeItem.toPlayableSongItem(shelf.title)
                                                                    )
                                                                    if (songs.isNotEmpty()) {
                                                                        onSongClick(songs, 0)
                                                                    }
                                                                }
                                                            },
                                                            onShowSnackbar = { message ->
                                                                scope.launch {
                                                                    snackbarHostState.showNeriSnackbar(message)
                                                                }
                                                            },
                                                            offlineMode = offlineMode
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    ui.ytMusicHomeShelves.loading -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtShelvesLoading),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionLoadingState(homeLoadingText)
                                        }
                                    }
                                    ui.ytMusicHomeShelves.error != null -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtShelvesError),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionErrorState(detail = ui.ytMusicHomeShelves.error?.asString().orEmpty())
                                        }
                                    }
                                }
                            }

                            if (!hasVisibleYtMusicFeed && (showNeteaseTrending || showNeteaseRadar || showRecommendedCard)) {
                                when {
                                    ui.ytMusicHomeShelves.loading -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtEmptyFeedLoading),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionLoadingState(homeLoadingText)
                                        }
                                    }
                                    ui.ytMusicHomeShelves.error != null -> {
                                        item(
                                            key = registerGridItemKey(HomeScrollKeyYtEmptyFeedError),
                                            span = { GridItemSpan(maxLineSpan) }
                                        ) {
                                            SectionErrorState(detail = ui.ytMusicHomeShelves.error?.asString().orEmpty())
                                        }
                                    }
                                }
                            }
                        } else {
                            if (showNeteaseRadar) {
                                ui.radarSongSections
                                    .filter { it.source == NeteaseHomeSongSource.PERSONAL_RADAR }
                                    .forEach { sectionState ->
                                    val sectionKey = homeNeteaseSongSectionKey(
                                        group = "radar",
                                        source = sectionState.source
                                    )
                                    addNeteaseSongSection(
                                        sectionKey = sectionKey,
                                        registerKey = ::registerGridItemKey,
                                        sectionState = sectionState,
                                        icon = neteaseSongSectionIcon(sectionState.source),
                                        loadingText = homeLoadingText,
                                        onSongClick = onSongClick,
                                        favoriteSongLookup = favoriteSongLookup,
                                        onFavoriteToggle = ::toggleHomeSongFavorite,
                                        onShowSnackbar = showHomeSnackbar,
                                        offlineMode = offlineMode
                                    )
                                }

                                val radarPlaylistState = ui.radarPlaylists
                                item(
                                    key = registerGridItemKey(
                                        HomeScrollKeyNeteaseRadarPlaylistsHeader
                                    ),
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    SectionHeader(
                                        icon = Icons.Outlined.Explore,
                                        title = stringResource(CoreCommonR.string.home_netease_radar_playlists)
                                    )
                                }
                                sectionContent(
                                    section = radarPlaylistState,
                                    loadingText = homeLoadingText,
                                    errorDetail = radarPlaylistState.error,
                                    keyPrefix = HomeScrollKeyNeteaseRadarPlaylists,
                                    registerKey = ::registerGridItemKey
                                ) {
                                    item(
                                        key = registerGridItemKey(HomeScrollKeyNeteaseRadarPlaylistsContent),
                                        span = { GridItemSpan(maxLineSpan) }
                                    ) {
                                        RadarPlaylistStrip(
                                            playlists = radarPlaylistState.items,
                                            favoriteKeys = favoriteKeys,
                                            listState = radarPlaylistListState,
                                            onClick = onItemClick,
                                            onShowSnackbar = { message ->
                                                scope.launch {
                                                    snackbarHostState.showNeriSnackbar(message)
                                                }
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                ui.radarSongSections
                                    .filter { it.source != NeteaseHomeSongSource.PERSONAL_RADAR }
                                    .forEach { sectionState ->
                                        val sectionKey = homeNeteaseSongSectionKey(
                                            group = "radar",
                                            source = sectionState.source
                                        )
                                        addNeteaseSongSection(
                                            sectionKey = sectionKey,
                                            registerKey = ::registerGridItemKey,
                                            sectionState = sectionState,
                                            icon = neteaseSongSectionIcon(sectionState.source),
                                            loadingText = homeLoadingText,
                                            onSongClick = onSongClick,
                                            favoriteSongLookup = favoriteSongLookup,
                                            onFavoriteToggle = ::toggleHomeSongFavorite,
                                            onShowSnackbar = showHomeSnackbar,
                                            offlineMode = offlineMode
                                        )
                                    }
                            }

                            // Bilibili 音乐模块：版权音乐榜（热门榜/原创榜）与音乐分区热门视频。
                            // 排在「私人雷达 / 雷达歌单」之后、「热力飙升 / 我的推荐」之前，
                            // 使 B 站榜单与网易云榜单相邻，且不拆散网易云的雷达组合。
                            addBiliMusicSections()

                            if (showNeteaseTrending) {
                                ui.trendingSongSections.forEach { sectionState ->
                                    val sectionKey = homeNeteaseSongSectionKey(
                                        group = "trending",
                                        source = sectionState.source
                                    )
                                    addNeteaseSongSection(
                                        sectionKey = sectionKey,
                                        registerKey = ::registerGridItemKey,
                                        sectionState = sectionState,
                                        icon = neteaseSongSectionIcon(sectionState.source),
                                        loadingText = homeLoadingText,
                                        onSongClick = onSongClick,
                                        favoriteSongLookup = favoriteSongLookup,
                                        onFavoriteToggle = ::toggleHomeSongFavorite,
                                        onShowSnackbar = showHomeSnackbar,
                                        offlineMode = offlineMode
                                    )
                                }
                            }

                            if (showRecommendedCard) {
                                ui.playlistSections.forEach { sectionState ->
                                    addNeteasePlaylistSection(
                                        sectionKey = homeNeteasePlaylistSectionKey(sectionState.source),
                                        registerKey = ::registerGridItemKey,
                                        sectionState = sectionState,
                                        loadingText = homeLoadingText,
                                        favoriteKeys = favoriteKeys,
                                        onItemClick = onItemClick,
                                        onShowSnackbar = { message ->
                                            scope.launch {
                                                snackbarHostState.showNeriSnackbar(message)
                                            }
                                        },
                                        offlineMode = offlineMode
                                    )
                                }
                            }

                        }
                    }
                }
                SideEffect {
                    onScrollAnchorIndexesChanged(scrollAnchorIndexes.toMap())
                }
            }
        }

        // 刷新入口不再单独放按钮：电视上它挂在「首页」标签上（焦点在该标签时按确定触发），
        // 见 NeriTvTopBar / HomeRefreshBus
        NeriOverlaySnackbarHost(
            hostState = snackbarHostState,
            bottomPadding = LocalMiniPlayerHeight.current
        )
    }
}

private fun <T> LazyGridScope.sectionContent(
    section: HomeSectionState<T>,
    loadingText: String,
    errorDetail: UiText?,
    keyPrefix: String,
    registerKey: (String) -> String,
    content: LazyGridScope.() -> Unit
) {
    when {
        section.items.isNotEmpty() -> content()
        section.loading -> {
            item(
                key = registerKey("$keyPrefix:loading"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SectionLoadingState(loadingText)
            }
        }

        errorDetail != null -> {
            item(
                key = registerKey("$keyPrefix:error"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SectionErrorState(errorDetail.asString())
            }
        }
    }
}

private fun LazyGridScope.addNeteaseSongSection(
    sectionKey: String,
    registerKey: (String) -> String,
    sectionState: HomeNeteaseSongSectionState,
    icon: ImageVector,
    loadingText: String,
    onSongClick: (List<SongItem>, Int) -> Unit,
    favoriteSongLookup: FavoriteSongLookup,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    item(
        key = registerKey("$sectionKey:header"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        SectionHeader(
            icon = icon,
            title = stringResource(sectionState.source.titleRes)
        )
    }
    sectionContent(
        section = sectionState.section,
        loadingText = loadingText,
        errorDetail = sectionState.section.error,
        keyPrefix = sectionKey,
        registerKey = registerKey
    ) {
        item(
            key = registerKey("$sectionKey:content"),
            span = { GridItemSpan(maxLineSpan) }
        ) {
            ResponsiveSongPagerList(
                songs = sectionState.section.items,
                onSongClick = onSongClick,
                favoriteSongLookup = favoriteSongLookup,
                onFavoriteToggle = onFavoriteToggle,
                onShowSnackbar = onShowSnackbar,
                offlineMode = offlineMode
            )
        }
    }
}

private fun LazyGridScope.addNeteasePlaylistSection(
    sectionKey: String,
    registerKey: (String) -> String,
    sectionState: HomeNeteasePlaylistSectionState,
    loadingText: String,
    favoriteKeys: Set<String>,
    onItemClick: (PlaylistSummary) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    item(
        key = registerKey("$sectionKey:header"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        SectionHeader(
            icon = Icons.Outlined.Star,
            title = stringResource(sectionState.source.titleRes)
        )
    }
    when {
        sectionState.section.items.isNotEmpty() -> {
            item(
                key = registerKey("$sectionKey:content"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                PlaylistSectionStrip(
                    sectionKey = sectionKey,
                    playlists = sectionState.section.items,
                    favoriteKeys = favoriteKeys,
                    onItemClick = onItemClick,
                    onShowSnackbar = onShowSnackbar,
                    offlineMode = offlineMode
                )
            }
        }
        sectionState.section.loading -> {
            item(
                key = registerKey("$sectionKey:loading"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SectionLoadingState(loadingText)
            }
        }
        sectionState.section.error != null -> {
            item(
                key = registerKey("$sectionKey:error"),
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SectionErrorState(detail = sectionState.section.error.asString())
            }
        }
    }
}

private fun neteaseSongSectionIcon(source: NeteaseHomeSongSource): ImageVector {
    return when (source) {
        NeteaseHomeSongSource.PERSONAL_RADAR -> Icons.Outlined.Radar
        NeteaseHomeSongSource.TOP_SOARING,
        NeteaseHomeSongSource.PERSONALIZED_NEW_SONGS,
        NeteaseHomeSongSource.TOP_HOT,
        NeteaseHomeSongSource.TOP_NEW -> Icons.Outlined.Bolt
        NeteaseHomeSongSource.DAILY_RECOMMEND,
        NeteaseHomeSongSource.PRIVATE_FM -> Icons.Outlined.Explore
    }
}

@Composable
private fun SectionLoadingState(text: String) {
    // 加载中画「与真实卡片行等高」的骨架，而不是一个 20dp 的转圈。
    //
    // 原来的转圈整体只有约 60dp 高，而它代表的真实内容是「一排 172dp 的卡片」，
    // 高约 226dp。数据一到，这个 item 的高度从 60dp 跳到 226dp —— LazyVerticalGrid
    // 会把**所有 item** 重新测量一遍、重新定位焦点。冷启动时好几个区块同时从
    // 「加载中」变成「有数据」，于是整页重排，按键全部排队，表现就是
    // 「进去要等一会才能动」。
    //
    // 改成等高骨架后，数据到达前后高度一致，只换内容、不触发整页重排。
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val cardWidth = if (screenWidthDp >= 600) 172.dp else 148.dp
    // 与 HomeCleanComponents 的 FeaturedLabelHeight 保持一致（封面下方那条文字栏）
    val rowHeight = cardWidth + 54.dp
    val horizontalPadding = 8.dp
    val spacing = 12.dp
    val available = screenWidthDp.dp - horizontalPadding * 2
    val perRow = (((available + spacing) / (cardWidth + spacing)).toInt()).coerceIn(1, 8)
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(perRow) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(rowHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(placeholderColor)
            )
        }
    }
}

@Composable
private fun SectionErrorState(detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(CoreCommonR.string.home_retry_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SongRowMini(
    index: Int,
    song: SongItem,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val coverUrl = rememberSongDisplayCoverUrl(song)
    var showMenu by remember { mutableStateOf(false) }
    // 电视上三点按钮不参与焦点，改成「光标停在这一行时按菜单键」
    val isTelevision = LocalIsTelevision.current
    val tvRowMenuState = rememberTvRowMenuKeyState()
    TvRowMenuKeyEffect(state = tvRowMenuState) { showMenu = true }
    val firstItemFocus = rememberTvMenuFirstItemFocus(showMenu)
    val tvRingIndication = rememberTvFocusRingIndication(8.dp) ?: LocalIndication.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(neriRoundedShape(8.dp))
            .tvRowMenuKeyPress(tvRowMenuState, enabled = isTelevision)
            // 电视「返回来源」：从这首歌进播放页，退出时光标要回到这一行
            .tvReturnFocus("song:${song.channelId.orEmpty()}:${song.id}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication
            ) { onClick() }
            // 内边距必须放在 clickable **之后**：焦点环是画在节点边界上的，
            // 节点比内容大出多少，环就离内容多远。放前面 = 节点就等于内容，
            // 环会直接压在序号上（只留 1dp 内缩，视觉上盖住数字）。
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        if (!coverUrl.isNullOrBlank()) {
            // 电视：遥控器连续操作期间先不发起新加载（见 rememberTvDeferredImage）。
            // 歌曲行和卡片走的是同一套 —— 之前只给卡片接了，歌曲分区（每日推荐/热歌/日推…）
            // 还在滚动时现场解码封面，所以那一屏一直卡。
            val deferredCover = rememberTvDeferredImage(key = coverUrl) {
                fastScrollableImageRequest(
                    context = context,
                    data = coverUrl,
                    sizePx = 128,
                    offlineMode = offlineMode
                )
            }
            AsyncImage(
                model = deferredCover.model,
                contentDescription = song.displayName(),
                contentScale = ContentScale.Crop,
                onSuccess = { deferredCover.markLoaded() },
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(1f)
                    .clip(neriRoundedShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(10.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Text(
                text = song.displayName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = listOfNotNull(
                    song.displayArtist().takeIf { it.isNotBlank() },
                    song.displayAlbum(context).takeIf { it.isNotBlank() }
                ).joinToString(" / "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(CoreCommonR.string.common_more_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(CoreCommonR.string.local_playlist_play_next)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.focusRequester(firstItemFocus),
                    onClick = {
                        PlayerManager.addToQueueNext(song)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(CoreCommonR.string.playlist_add_to_end)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        PlayerManager.addToQueueEnd(song)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isFavorite) {
                                    CoreCommonR.string.favorite_remove
                                } else {
                                    CoreCommonR.string.favorite_add
                                }
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onFavoriteToggle(song, isFavorite)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(CoreCommonR.string.action_copy_song_info)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText("text", buildHomeSongInfo(song))
                                )
                            )
                            onShowSnackbar(composeResources.getString(CoreCommonR.string.toast_copied))
                        }
                        showMenu = false
                    }
                )
            }
        }
    }
}

/**
 * 电视：整页翻页的卡片横条（和「继续播放」同一套结构）。
 *
 * 为什么不用 LazyRow：
 * LazyRow 里滚过去之后，屏幕左侧的卡片会被回收，焦点「往左」找不到已经不再组合的节点，
 * 于是翻到下一页以后就回不去上一页。HorizontalPager 的每一页是一个固定槽位的 Row，
 * 翻页由分页器自己负责，左右都能正常走。
 *
 * 卡片宽度按容器宽度反算（自适应内部间距），保证每一页正好填满一行，
 * 卡片大小也就和首页雷达卡片一致。
 */
@Composable
private fun PagedCardStrip(
    itemCount: Int,
    itemKey: (Int) -> Any,
    targetCardWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, cardModifier: Modifier) -> Unit
) {
    if (itemCount <= 0) return
    // 分帧组合：首帧只建前两张卡片，下一帧补满。
    //
    // 冷启动那一帧实测 1435ms（组合 556ms + 测量 878ms）—— 因为首屏同时有三四个
    // 区块、每个区块的 Pager 一次把整排（含预取的相邻页）七八张卡片全建出来。
    // 这一秒多里主线程被填满，按键只能排队，表现就是「进去要等一会才能动」。
    //
    // 摊到两帧之后，单帧工作量大约减半，帧与帧之间主线程有空隙，按键能被及时处理，
    // 焦点就能在内容还没加载完的时候正常移动。卡片位置不变，只是晚一帧出现。
    var stagedLimit by remember { mutableStateOf(2) }
    LaunchedEffect(itemCount) {
        withFrameNanos { }
        stagedLimit = Int.MAX_VALUE
    }
    val shownCount = if (stagedLimit < itemCount) stagedLimit else itemCount
    val spacing = 12.dp
    val horizontalPadding = 8.dp
    if (!LocalIsTelevision.current || TvTuning.useLazyRow) {
        // 手机/平板保持原来的自由滚动
        // 诊断：neri_tv_pager=0 时电视也退回 LazyRow，用来量化 Pager 结构的帧成本
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            // vertical 4dp 是给焦点环留的：环外扩在卡片外侧 4dp，不留空间会被滚动容器裁掉
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(shownCount) { index ->
                content(index, Modifier.width(targetCardWidth))
            }
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = (maxWidth - horizontalPadding * 2).coerceAtLeast(1.dp)
        val perPage = (((available + spacing) / (targetCardWidth + spacing)).toInt()).coerceAtLeast(1)
        val slotWidth = ((available - spacing * (perPage - 1)) / perPage)
            .coerceAtLeast(80.dp)
        // 用户要求卡片整体缩小 10%：槽位不变，卡片画小一点并居中，行距布局不动
        val cardWidth = slotWidth * 0.9f
        val pageCount = ceil(shownCount / perPage.toFloat()).toInt().coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount })
        val focus = remember { TvPagerFocus() }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            // vertical 4dp 给外扩的焦点环留空间
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 4.dp),
            // 页与页之间留出和卡片之间一样的间距：原来是 0，
            // 上一页最后一张和下一页第一张会直接贴在一起
            pageSpacing = spacing
        ) { page ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(perPage) { slot ->
                    val index = page * perPage + slot
                    if (index < shownCount) {
                        Box(
                            modifier = Modifier.width(slotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            // 用 itemKey 做稳定身份，翻页时卡片不会重建
                            androidx.compose.runtime.key(itemKey(index)) {
                                content(
                                    index,
                                    tvPagerItemModifier(
                                        pagerState = pagerState,
                                        focus = focus,
                                        index = index,
                                        perPage = perPage,
                                        itemCount = itemCount,
                                        atPageLeftEdge = index % perPage == 0,
                                        pageBackTargetIndex = index - 1,
                                        base = Modifier.width(cardWidth)
                                    )
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(slotWidth))
                    }
                }
            }
        }
    }
}

/**
 * 电视：分页横条（HorizontalPager）里单张卡片的焦点与翻页。
 *
 * 「页首按左」必须显式翻页：Compose 会先把焦点送到上一页最后一张卡上，
 * 而那张卡正好贴着视口左边缘，bringIntoView 判定「已经可见」→ 页面不翻，
 * 用户看到的就是「焦点跳到上一页最后一张，再按一下才翻页」。这里直接
 * `animateScrollToPage(page - 1)` 再把光标放过去，一次按键就翻。
 *
 * 全局首尾（index 0 / itemCount - 1）夹住，避免焦点搜索上溯到顶部标签栏。
 */
@Composable
private fun tvPagerItemModifier(
    pagerState: PagerState,
    focus: TvPagerFocus,
    index: Int,
    perPage: Int,
    itemCount: Int,
    /** 这一项是不是所在页最左边那一个（卡片横条 = 页内第一张；多列歌曲区 = 第一列任意一行） */
    atPageLeftEdge: Boolean,
    /** 翻回上一页后光标要落在哪一项；-1 表示到头了，直接吞掉左键 */
    pageBackTargetIndex: Int,
    base: Modifier = Modifier
): Modifier {
    if (!LocalIsTelevision.current) return base
    val scope = rememberCoroutineScope()
    val page = index / perPage
    return base
        .focusRequester(focus.requester(index))
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> when {
                    index <= 0 -> true
                    !atPageLeftEdge -> false
                    pageBackTargetIndex < 0 -> true
                    else -> {
                        scope.launch {
                            if (page <= 0) return@launch
                            runCatching { pagerState.animateScrollToPage(page - 1) }
                            repeat(4) {
                                withFrameNanos { }
                                if (
                                    runCatching {
                                        focus.requester(pageBackTargetIndex).requestFocus()
                                    }.getOrDefault(false)
                                ) {
                                    return@launch
                                }
                            }
                        }
                        true
                    }
                }

                Key.DirectionRight -> index >= itemCount - 1
                else -> false
            }
        }
}

/** 分页横条里每张卡的 FocusRequester 登记表（按 item index） */
private class TvPagerFocus {
    private val requesters = HashMap<Int, FocusRequester>()
    fun requester(index: Int): FocusRequester =
        requesters.getOrPut(index) { FocusRequester() }
}

/**
 * 「为你推荐 / 我的推荐」这类歌单区块：整页翻页的卡片横条。
 */
@Composable
private fun PlaylistSectionStrip(
    sectionKey: String,
    playlists: List<PlaylistSummary>,
    favoriteKeys: Set<String>,
    onItemClick: (PlaylistSummary) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val cardWidth = if (currentWindowWidthDp() >= 600.dp) 172.dp else 148.dp
    PagedCardStrip(
        itemCount = playlists.size,
        itemKey = { index -> "home:netease:playlist-strip:${playlists[index].id}" },
        targetCardWidth = cardWidth
    ) { index, cardModifier ->
        val playlist = playlists[index]
        PlaylistCard(
            playlist = playlist,
            isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
            onClick = { onItemClick(playlist) },
            onShowSnackbar = onShowSnackbar,
            offlineMode = offlineMode,
            bringIntoViewOnFocus = false,
            // 带区块前缀：不同分区的同名歌单不能共用同一个焦点 key
            returnFocusKey = "$sectionKey:${playlist.id}",
            modifier = cardModifier
        )
    }
}

@Composable
private fun RadarPlaylistStrip(
    playlists: List<PlaylistSummary>,
    favoriteKeys: Set<String>,
    listState: LazyListState,
    onClick: (PlaylistSummary) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    // 电视上改成整页翻页（listState 现在不再用于滚动，保留参数以免改动宿主）
    val cardWidth = if (currentWindowWidthDp() >= 600.dp) 172.dp else 148.dp
    PagedCardStrip(
        itemCount = playlists.size,
        itemKey = { index -> homeNeteaseRadarPlaylistScrollKey(playlists[index].id) },
        targetCardWidth = cardWidth
    ) { index, cardModifier ->
        val playlist = playlists[index]
        RadarPlaylistCard(
            playlist = playlist,
            isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
            onClick = { onClick(playlist) },
            onShowSnackbar = onShowSnackbar,
            offlineMode = offlineMode,
            modifier = cardModifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadarPlaylistCard(
    playlist: PlaylistSummary,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    var showMenu by remember { mutableStateOf(false) }
    val unfavoritedText = stringResource(CoreCommonR.string.home_unfavorited)
    val favoriteSuccessText = stringResource(CoreCommonR.string.favorite_success)

    // 环跟着卡片圆角（卡片 = neriRoundedShape(8.dp)：Material 8dp / 简洁 12dp）
    val radarCardCorner = if (NeriDesign.isClean) cleanCornerRadiusFor(8.dp) else 8.dp
    val tvRingIndication = rememberTvFocusRingIndication(
        radius = radarCardCorner,
        outset = 4.dp,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    Column(
        modifier = modifier
            // 电视「返回来源」：进这个歌单详情，退出时光标回到这张卡
            .tvReturnFocus("radar:${playlist.id}")
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication,
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .clip(neriRoundedShape(8.dp))
    ) {
        Box(
            // 文字在图片外面，外层 Column 的 clip 只圆整卡四角，图片下边缘是卡片内部的直角，
            // 所以这里单独把封面裁一次（同 shape 同半径，和卡片边同心）
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clip(neriRoundedShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.picUrl.isNotBlank()) {
                // 电视：遥控器连续操作期间先不发起新加载
                val deferredCover = rememberTvDeferredImage(key = playlist.picUrl) {
                    fastScrollableImageRequest(
                        context = context,
                        data = playlist.picUrl,
                        sizePx = 384,
                        offlineMode = offlineMode
                    )
                }
                AsyncImage(
                    model = deferredCover.model,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    onSuccess = { deferredCover.markLoaded() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(42.dp)
                )
            }
            Text(
                text = stringResource(CoreCommonR.string.home_netease_radar_badge),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    // clip + background 合成一个绘制操作：背景直接按圆角形状画，
                    // 不再先建一层裁剪再画矩形
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.86f),
                        shape = neriRoundedShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = playlist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    CoreCommonR.string.home_play_count_format,
                    formatPlayCount(context, playlist.playCount),
                    playlist.trackCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) {
                            stringResource(CoreCommonR.string.home_unfavorite_playlist)
                        } else {
                            stringResource(CoreCommonR.string.home_favorite_playlist)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = null
                    )
                },
                onClick = {
                    showMenu = false
                    scope.launch {
                        if (isFavorite) {
                            favoriteRepo.removeFavorite(playlist.id, "netease")
                            onShowSnackbar(unfavoritedText)
                        } else {
                            favoriteRepo.addFavorite(
                                id = playlist.id,
                                name = playlist.name,
                                coverUrl = playlist.picUrl,
                                trackCount = playlist.trackCount,
                                source = "netease",
                                songs = emptyList()
                            )
                            onShowSnackbar(favoriteSuccessText)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: PlaylistSummary,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    offlineMode: Boolean = false,
    /** 横条里的卡片要关掉：bringIntoView 会把卡片滚到屏幕正中 */
    bringIntoViewOnFocus: Boolean = true,
    /**
     * 电视「返回来源」的焦点 key。
     *
     * 同一个歌单会同时出现在「为你推荐」和下面的「每日推荐歌单」里，若两处共用
     * `playlist:$id`，两张卡就争同一个 FocusRequester，返回时光标会落到下面那张。
     * 所以每个区块必须带自己的前缀（「为你推荐」/「每日推荐歌单」分别传）。
     */
    returnFocusKey: String = "playlist:${playlist.id}",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    var showMenu by remember { mutableStateOf(false) }

    val unfavoritedText = stringResource(CoreCommonR.string.home_unfavorited)
    val favoriteSuccessText = stringResource(CoreCommonR.string.favorite_success)

    // 电视：遥控器连续操作期间先不发起新加载（见 rememberTvDeferredImage）
    val deferredCover = rememberTvDeferredImage(key = playlist.picUrl) {
        fastScrollableImageRequest(
            context = context,
            data = playlist.picUrl,
            sizePx = 384,
            offlineMode = offlineMode
        )
    }

    // 卡片圆角 8dp；环外扩 4dp 画在卡片外侧 —— 卡片尺寸一点不变，环也不压封面
    // 简洁风格是苹果那种大圆角：卡片 20dp，焦点环跟着卡片圆角走（不再额外放大）
    val cleanStyle = NeriDesign.isClean
    val cardCorner = if (cleanStyle) 20.dp else 8.dp
    val cardShape = if (cleanStyle) NeriDesign.tokens.cardShape else neriRoundedShape(8.dp)
    val tvRingIndication = rememberTvFocusRingIndication(
        radius = cardCorner,
        outset = 4.dp,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    Column(
        modifier = modifier
            .then(if (bringIntoViewOnFocus) Modifier.tvBringIntoViewOnFocus() else Modifier)
            // 电视「返回来源」：进这个歌单详情，退出时光标回到这张卡
            .tvReturnFocus(returnFocusKey)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication,
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            // 圆角裁剪必须放在 clickable **之后**：放前面它会把外扩到卡片外的焦点环一起切掉
            .clip(cardShape)
    ) {
        if (NeriDesign.isClean) {
            HomeFeaturedArtwork(
                model = deferredCover.model,
                title = playlist.name,
                subtitle = stringResource(
                    CoreCommonR.string.home_play_count_format,
                    formatPlayCount(context, playlist.playCount),
                    playlist.trackCount
                ),
                onImageLoaded = { deferredCover.markLoaded() }
            )
        } else {
            AsyncImage(
                model = deferredCover.model,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                onSuccess = { deferredCover.markLoaded() },
                // 这里不再单独 clip：外层的 Modifier.clip(cardShape) 已经会裁剪子节点绘制，
                // 重复裁一层只是多一个离屏图层（首屏十几张卡就是十几层）。
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                Text(
                    text = playlist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        CoreCommonR.string.home_play_count_format,
                        formatPlayCount(context, playlist.playCount),
                        playlist.trackCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) {
                            stringResource(CoreCommonR.string.home_unfavorite_playlist)
                        } else {
                            stringResource(CoreCommonR.string.home_favorite_playlist)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = null
                    )
                },
                onClick = {
                    showMenu = false
                    scope.launch {
                        if (isFavorite) {
                            favoriteRepo.removeFavorite(playlist.id, "netease")
                            onShowSnackbar(unfavoritedText)
                        } else {
                            favoriteRepo.addFavorite(
                                id = playlist.id,
                                name = playlist.name,
                                coverUrl = playlist.picUrl,
                                trackCount = playlist.trackCount,
                                source = "netease",
                                songs = emptyList()
                            )
                            onShowSnackbar(favoriteSuccessText)
                        }
                    }
                }
            )
        }
    }
}

private data class ClassifiedYouTubeMusicShelves(
    val guessYouLike: YouTubeMusicHomeShelf?,
    val dailyDiscover: YouTubeMusicHomeShelf?,
    val remaining: List<YouTubeMusicHomeShelf>
)

private val YouTubeMusicGuessYouLikeKeywords = listOf(
    "猜你喜欢",
    "guess you like",
    "recommended for you"
)

private val YouTubeMusicDailyDiscoverKeywords = listOf(
    "每日发现",
    "daily discover",
    "discover daily"
)

private val YouTubeMusicSongShelfKeywords = listOf(
    "再听一遍",
    "老歌重温",
    "翻唱与混音",
    "每日发现",
    "猜你喜欢",
    "listen again",
    "oldies",
    "covers and remixes",
    "daily discover"
)

private fun classifyYouTubeMusicShelves(
    shelves: List<YouTubeMusicHomeShelf>
): ClassifiedYouTubeMusicShelves {
    val guessYouLike = shelves.firstOrNull { shelf ->
        shelf.shouldRenderAsSongShelf() &&
            shelf.title.matchesYouTubeMusicShelfKeywords(YouTubeMusicGuessYouLikeKeywords)
    }
    val dailyDiscover = shelves.firstOrNull { shelf ->
        shelf != guessYouLike &&
            shelf.shouldRenderAsSongShelf() &&
            shelf.title.matchesYouTubeMusicShelfKeywords(YouTubeMusicDailyDiscoverKeywords)
    }
    val remaining = shelves.filterNot { shelf ->
        shelf == guessYouLike || shelf == dailyDiscover
    }
    return ClassifiedYouTubeMusicShelves(
        guessYouLike = guessYouLike,
        dailyDiscover = dailyDiscover,
        remaining = remaining
    )
}

private fun YouTubeMusicHomeShelf.shouldRenderAsSongShelf(): Boolean {
    if (items.isEmpty()) {
        return false
    }
    val playableCount = items.count { it.videoId.isNotBlank() }
    if (playableCount == 0) {
        return false
    }
    if (playableCount == items.size) {
        return true
    }
    return title.matchesYouTubeMusicShelfKeywords(YouTubeMusicSongShelfKeywords)
}

private fun YouTubeMusicHomeShelf.hasRenderablePlaylistItems(): Boolean {
    return items.any { it.isPlaylistItem() }
}

private fun YouTubeMusicHomeItem.isPlaylistItem(): Boolean {
    val normalizedPageType = pageType.uppercase(Locale.US)
    return when {
        normalizedPageType.contains("PLAYLIST") -> true
        normalizedPageType.isNotBlank() -> false
        else -> browseId.startsWith("VL")
    }
}

internal fun YouTubeMusicHomeItem.toPlaylist(): YouTubeMusicPlaylist? {
    if (!isPlaylistItem()) {
        return null
    }
    return YouTubeMusicPlaylist(
        browseId = browseId,
        playlistId = browseId.removePrefix("VL"),
        title = title,
        subtitle = subtitle,
        coverUrl = coverUrl,
        trackCount = 0
    )
}

private fun String.matchesYouTubeMusicShelfKeywords(keywords: List<String>): Boolean {
    val normalized = lowercase(Locale.ROOT)
        .replace(Regex("[\\s·•・/\\\\|:_-]+"), "")
    return keywords.any { keyword ->
        val normalizedKeyword = keyword.lowercase(Locale.ROOT)
            .replace(Regex("[\\s·•・/\\\\|:_-]+"), "")
        normalized.contains(normalizedKeyword)
    }
}

internal fun YouTubeMusicHomeItem.toPlayableSongItem(sectionTitle: String): SongItem? {
    if (videoId.isBlank()) {
        return null
    }
    val metadata = YouTubeMusicParser.parseHomeSongMetadata(
        subtitle = subtitle,
        fallbackAlbum = sectionTitle
    )
    val playlistId = browseId.removePrefix("VL").ifBlank { null }
    return SongItem(
        id = stableYouTubeMusicId(videoId),
        name = title,
        artist = metadata.artist,
        album = metadata.album,
        albumId = stableYouTubeMusicId((playlistId ?: sectionTitle).ifBlank { videoId }),
        durationMs = durationMs,
        coverUrl = coverUrl.ifBlank { null },
        mediaUri = buildYouTubeMusicMediaUri(
            videoId = videoId,
            playlistId = playlistId
        ),
        originalName = title,
        originalArtist = metadata.artist,
        originalCoverUrl = coverUrl.ifBlank { null }
    )
}

private fun LazyGridScope.addYouTubeMusicSongShelfSection(
    sectionKey: String,
    registerKey: (String) -> String,
    shelf: YouTubeMusicHomeShelf,
    icon: ImageVector,
    title: String,
    onSongClick: (List<SongItem>, Int) -> Unit,
    favoriteSongLookup: FavoriteSongLookup,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val songs = shelf.items.mapNotNull { it.toPlayableSongItem(shelf.title) }
    if (songs.isEmpty()) {
        return
    }
    item(
        key = registerKey("$sectionKey:header"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        SectionHeader(
            icon = icon,
            title = title
        )
    }
    item(
        key = registerKey("$sectionKey:content"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        val warmupKey = remember(songs) {
            songs.joinToString("|") { song ->
                song.audioId ?: song.mediaUri.orEmpty()
            }
        }
        LaunchedEffect(warmupKey) {
            PlayerManager.prefetchYouTubePlayableUrlWindow(
                playlist = songs,
                startIndex = 0,
                source = "yt_home_shelf_visible"
            )
        }
        ResponsiveSongPagerList(
            songs = songs,
            onSongClick = onSongClick,
            favoriteSongLookup = favoriteSongLookup,
            onFavoriteToggle = onFavoriteToggle,
            onShowSnackbar = onShowSnackbar,
            offlineMode = offlineMode
        )
    }
}

@Composable
private fun ResponsiveSongPagerList(
    songs: List<SongItem>,
    onSongClick: (List<SongItem>, Int) -> Unit,
    favoriteSongLookup: FavoriteSongLookup,
    onFavoriteToggle: (SongItem, Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean
) {
    val widthDp = currentWindowWidthDp().value
    val columns = when {
        widthDp >= 840 -> 3
        widthDp >= 600 -> 2
        else -> 1
    }
    val rowsPerColumn = 3
    val perPage = (columns * rowsPerColumn).coerceAtLeast(1)

    val pageCount = ceil(songs.size / perPage.toFloat()).toInt().coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val focus = remember { TvPagerFocus() }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        // 页间留白，和行内列间距一致（否则上一页最后一列会和下一页第一列贴住）
        pageSpacing = 12.dp
    ) { page ->
        val start = page * perPage
        val end = min(start + perPage, songs.size)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (columnIndex in 0 until columns) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (rowIndex in 0 until rowsPerColumn) {
                        val absoluteIndex = start + (columnIndex * rowsPerColumn + rowIndex)
                        if (absoluteIndex < end) {
                            val song = songs[absoluteIndex]
                            SongRowMini(
                                index = absoluteIndex + 1,
                                song = song,
                                onClick = { onSongClick(songs, absoluteIndex) },
                                isFavorite = favoriteSongLookup.contains(song),
                                onFavoriteToggle = onFavoriteToggle,
                                onShowSnackbar = onShowSnackbar,
                                offlineMode = offlineMode,
                                // 电视：第一列任意一行按左 = 本页左边界 → 显式翻回上一页，
                                // 光标落在上一页同一行的最后一列；整条全局首尾则夹住。
                                // 以前是「每页第一列都吞左键」，所以翻到第二页就回不去了。
                                modifier = tvPagerItemModifier(
                                    pagerState = pagerState,
                                    focus = focus,
                                    index = absoluteIndex,
                                    perPage = perPage,
                                    itemCount = songs.size,
                                    atPageLeftEdge = columnIndex == 0,
                                    pageBackTargetIndex = absoluteIndex - rowsPerColumn,
                                    base = Modifier
                                )
                            )
                        } else {
                            Spacer(Modifier.height(0.dp))
                        }
                    }
                }
            }
        }
    }
}

internal fun buildHomeSongInfo(song: SongItem): String {
    return "${song.displayName()}-${song.displayArtist()}"
}
