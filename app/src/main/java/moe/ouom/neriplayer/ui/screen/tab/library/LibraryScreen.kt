package moe.ouom.neriplayer.ui.screen.tab.library

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
 * File: moe.ouom.neriplayer.ui.screen.tab.library/LibraryScreen
 * Created: 2025/8/8
 */

import androidx.compose.runtime.CompositionLocalProvider
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.platform.youtube.config.YouTubeFeatureGate
import moe.ouom.neriplayer.data.model.stats.PlaybackStatsPeriod
import moe.ouom.neriplayer.data.stats.PlaybackStatsHotPlaylistPreview
import moe.ouom.neriplayer.data.stats.PlaybackStatsRepository
import moe.ouom.neriplayer.data.stats.hotPlaybackStatsQuery
import moe.ouom.neriplayer.ui.screen.history.stats.StatsQueryDay
import moe.ouom.neriplayer.ui.screen.history.stats.rememberStatsQueryDay
import moe.ouom.neriplayer.data.model.playlist.LocalArtistSummary
import moe.ouom.neriplayer.data.model.playlist.LocalPlaylist
import moe.ouom.neriplayer.data.local.playlist.LocalPlaylistRepository
import moe.ouom.neriplayer.ui.component.playlist.showPlaylistDeleteResultGlobally
import moe.ouom.neriplayer.ui.util.shouldAllowCollapsingTopAppBar
import moe.ouom.neriplayer.data.model.NeteaseArtistSummary
import moe.ouom.neriplayer.data.model.BiliUploaderSummary
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.LibraryViewModel
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.TvTabConfirmBus
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.R as AppR
import moe.ouom.neriplayer.platform.subsonic.repository.ServerAlbum
import moe.ouom.neriplayer.platform.subsonic.repository.ServerArtist

enum class LibraryTab(val labelResId: Int) {
    LOCAL(CoreCommonR.string.library_tab_local),
    FAVORITE(CoreCommonR.string.library_tab_favorite),
    MY_MUSIC(CoreCommonR.string.server_my_music),
    YTMUSIC(CoreCommonR.string.library_tab_youtube_music),
    NETEASE(CoreCommonR.string.library_tab_netease),
    NETEASEALBUM(CoreCommonR.string.library_tab_netease_album),
    BILI(CoreCommonR.string.library_tab_bilibili),
    QQMUSIC(CoreCommonR.string.library_tab_qqmusic)
}

internal const val NETEASE_CATEGORY_PLAYLIST = 0
internal const val NETEASE_CATEGORY_ALBUM = 1
internal const val FAVORITE_CATEGORY_PLAYLIST = 0
internal const val FAVORITE_CATEGORY_ARTIST = 1
internal const val FAVORITE_CATEGORY_HOT = 2
internal const val LOCAL_CATEGORY_PLAYLIST = 0
internal const val LOCAL_CATEGORY_ARTIST = 1
internal const val LIBRARY_UI_PREFS = "library_ui_preferences"
internal const val KEY_LOCAL_ARTIST_SORT_MODE = "local_artist_sort_mode"
internal val LibraryPrimaryTabShape = RoundedCornerShape(20.dp)
internal val LibrarySearchFieldShape = RoundedCornerShape(16.dp)

internal val HotPlaylistPeriods = listOf(
    PlaybackStatsPeriod.WEEK,
    PlaybackStatsPeriod.MONTH
)

internal fun hotPlaylistTitleResId(period: PlaybackStatsPeriod): Int = when (period) {
    PlaybackStatsPeriod.MONTH -> CoreCommonR.string.library_hot_playlist_month
    else -> CoreCommonR.string.library_hot_playlist_week
}

internal enum class LocalArtistSortMode {
    SONG_COUNT,
    RECENT_ADDED,
    NAME
}

internal fun resolveLocalArtistSortMode(storageValue: String?): LocalArtistSortMode {
    return LocalArtistSortMode.entries.firstOrNull { it.name == storageValue }
        ?: LocalArtistSortMode.SONG_COUNT
}

internal fun localArtistSortModeStorageValue(sortMode: LocalArtistSortMode): String {
    return sortMode.name
}

internal fun readLocalArtistSortMode(context: Context): LocalArtistSortMode {
    val prefs = context.getSharedPreferences(LIBRARY_UI_PREFS, Context.MODE_PRIVATE)
    return resolveLocalArtistSortMode(prefs.getString(KEY_LOCAL_ARTIST_SORT_MODE, null))
}

internal fun persistLocalArtistSortMode(context: Context, sortMode: LocalArtistSortMode) {
    context.getSharedPreferences(LIBRARY_UI_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(KEY_LOCAL_ARTIST_SORT_MODE, localArtistSortModeStorageValue(sortMode))
        }
}

@Composable
internal fun rememberHotPlaylists(
    retry: Int = 0,
    queryDay: StatsQueryDay = rememberStatsQueryDay().value,
    repository: PlaybackStatsRepository? = null
): Result<List<PlaybackStatsHotPlaylistPreview>>? {
    val hotPlaylists by produceState<Result<List<PlaybackStatsHotPlaylistPreview>>?>(
        initialValue = null, key1 = retry, key2 = queryDay.key, key3 = repository
    ) {
        value = null
        val statsRepository = repository ?: withContext(Dispatchers.IO) { AppContainer.playbackStatsRepo }
        statsRepository.revisionFlow.catch { error ->
            if (error is CancellationException) throw error
            value = Result.failure(error)
        }.collectLatest {
            value = try {
                Result.success(HotPlaylistPeriods.map { period ->
                    val query = hotPlaybackStatsQuery(period, queryDay.nowMillis)
                    PlaybackStatsHotPlaylistPreview(period, statsRepository.readPage(query, pageSize = 4).tracks, statsRepository.readSummary(query))
                })
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Result.failure(error)
            }
        }
    }
    return hotPlaylists
}

internal fun libraryTabDisplayOrder(
    isInternational: Boolean,
    youtubeEnabled: Boolean = true
): List<LibraryTab> {
    val orderedTabs = if (isInternational && youtubeEnabled) {
        listOf(
            LibraryTab.LOCAL,
            LibraryTab.FAVORITE,
            LibraryTab.YTMUSIC,
            LibraryTab.NETEASE,
            LibraryTab.BILI,
            LibraryTab.MY_MUSIC
        )
    } else {
        listOf(
            LibraryTab.LOCAL,
            LibraryTab.FAVORITE,
            LibraryTab.NETEASE,
            LibraryTab.YTMUSIC,
            LibraryTab.BILI,
            LibraryTab.MY_MUSIC
        )
    }
    return if (youtubeEnabled) orderedTabs else orderedTabs - LibraryTab.YTMUSIC
}

internal fun LibraryTab.asVisibleLibraryTab(): LibraryTab {
    return if (this == LibraryTab.NETEASEALBUM) LibraryTab.NETEASE else this
}

internal fun LibraryTab?.isRefreshable(): Boolean {
    return when (this?.asVisibleLibraryTab()) {
        LibraryTab.BILI,
        LibraryTab.YTMUSIC,
        LibraryTab.NETEASE -> true
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    initialTab: LibraryTab = LibraryTab.LOCAL,
    onTabChange: (LibraryTab) -> Unit = {},
    localListState: LazyListState,
    favoriteListState: LazyListState,
    neteaseAlbumState: LazyListState,
    neteaseListState: LazyListState,
    youtubeMusicListState: LazyListState,
    biliListState: LazyListState,
    qqMusicListState: LazyListState,
    topAppBarState: TopAppBarState,
    onLocalPlaylistClick: (LocalPlaylist) -> Unit = {},
    onServerAlbumClick: (ServerAlbum) -> Unit = {},
    onServerArtistClick: (ServerArtist) -> Unit = {},
    onLocalArtistClick: (LocalArtistSummary) -> Unit = {},
    onHotPlaylistClick: (PlaybackStatsPeriod) -> Unit = {},
    onNeteasePlaylistClick: (PlaylistSummary) -> Unit = {},
    onNeteaseAlbumClick: (AlbumSummary) -> Unit = {},
    onNeteaseArtistClick: (NeteaseArtistSummary) -> Unit = {},
    onBiliUploaderClick: (BiliUploaderSummary) -> Unit = {},
    onYouTubeMusicCreatorClick: (YouTubeMusicCreatorSummary) -> Unit = {},
    onYouTubeMusicPlaylistClick: (YouTubeMusicPlaylist) -> Unit = {},
    onBiliPlaylistClick: (BiliPlaylist) -> Unit = {},
    onOpenRecent: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    offlineMode: Boolean = false
) {
    val vm: LibraryViewModel = viewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultPlaylistName = stringResource(CoreCommonR.string.library_create_playlist_default)
    val localPlaylistRepo = remember(context) {
        LocalPlaylistRepository.getInstance(context)
    }
    val isInternational by AppContainer.settingsRepo.internationalizationEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val youtubeEnabled by AppContainer.settingsRepo.youtubeEnabledFlow
        .collectAsStateWithLifecycle(initialValue = YouTubeFeatureGate.isEnabled())
    val orderedTabs = remember(isInternational, youtubeEnabled) {
        libraryTabDisplayOrder(isInternational, youtubeEnabled)
    }
    val initialPage = remember(orderedTabs, initialTab) {
        orderedTabs.indexOf(initialTab.asVisibleLibraryTab()).takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { orderedTabs.size }
    )
    // MY_MUSIC（音乐服务器）分页自己维护滚动状态，这里拿不到，返回 null 由调用方跳过
    fun listStateOf(tab: LibraryTab): LazyListState? = when (tab) {
        LibraryTab.LOCAL -> localListState
        LibraryTab.FAVORITE -> favoriteListState
        LibraryTab.NETEASE -> neteaseListState
        LibraryTab.NETEASEALBUM -> neteaseAlbumState
        LibraryTab.YTMUSIC -> youtubeMusicListState
        LibraryTab.BILI -> biliListState
        LibraryTab.QQMUSIC -> qqMusicListState
        LibraryTab.MY_MUSIC -> null
    }
    // 电视：停在「媒体库」标签上再按一次确定键 = 刷新当前分页 + 回到顶部
    //（本地/收藏/QQ音乐没有远端数据可刷，只回顶部）
    LaunchedEffect(pagerState, orderedTabs) {
        TvTabConfirmBus.events.collect { route ->
            if (route != Destinations.Library.route) return@collect
            val tab = orderedTabs.getOrNull(pagerState.currentPage) ?: return@collect
            listStateOf(tab)?.let { state -> runCatching { state.scrollToItem(0) } }
            when (tab.asVisibleLibraryTab()) {
                LibraryTab.NETEASE -> vm.refreshNeteaseLibrary()
                LibraryTab.YTMUSIC -> vm.refreshYouTubeMusicPlaylists()
                LibraryTab.BILI -> vm.refreshBilibili()
                else -> Unit
            }
        }
    }
    var selectedNeteaseCategory by rememberSaveable {
        mutableIntStateOf(NETEASE_CATEGORY_PLAYLIST)
    }
    val pullGate = remember { LibraryPullGate() }
    var serverListState by remember { mutableStateOf<LazyListState?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        // follow the finger and stay put; a release snap reads as an unwanted bounce
        snapAnimationSpec = null,
        canScroll = {
            // an active pull owns the upward drag so it can be pushed back and cancelled
            if (pullGate.pulling) return@exitUntilCollapsedScrollBehavior false
            when (orderedTabs.getOrNull(pagerState.currentPage)) {
                LibraryTab.LOCAL -> shouldAllowCollapsingTopAppBar(
                    localListState.canScrollForward,
                    localListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.FAVORITE -> shouldAllowCollapsingTopAppBar(
                    favoriteListState.canScrollForward,
                    favoriteListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.NETEASE,
                LibraryTab.NETEASEALBUM -> {
                    val activeListState = if (
                        selectedNeteaseCategory == NETEASE_CATEGORY_ALBUM
                    ) {
                        neteaseAlbumState
                    } else {
                        neteaseListState
                    }
                    shouldAllowCollapsingTopAppBar(
                        activeListState.canScrollForward,
                        activeListState.canScrollBackward,
                        topAppBarState.collapsedFraction
                    )
                }
                LibraryTab.YTMUSIC -> shouldAllowCollapsingTopAppBar(
                    youtubeMusicListState.canScrollForward,
                    youtubeMusicListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.BILI -> shouldAllowCollapsingTopAppBar(
                    biliListState.canScrollForward,
                    biliListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.QQMUSIC -> shouldAllowCollapsingTopAppBar(
                    qqMusicListState.canScrollForward,
                    qqMusicListState.canScrollBackward,
                    topAppBarState.collapsedFraction
                )
                LibraryTab.MY_MUSIC -> shouldAllowCollapsingTopAppBar(
                    canScrollForward = serverListState?.canScrollForward == true,
                    canScrollBackward = serverListState?.canScrollBackward == true,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
                null -> shouldAllowCollapsingTopAppBar(
                    canScrollForward = false,
                    canScrollBackward = false,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
            }
        }
    )
    val scope = rememberCoroutineScope()
    val windowWidthDp = currentWindowWidthDp()
    val isTabletLayout = windowWidthDp >= 720.dp
    val isTelevision = LocalIsTelevision.current
    val pageHorizontalPadding = if (isTabletLayout) 28.dp else 0.dp

    LaunchedEffect(initialTab, orderedTabs) {
        val targetPage = orderedTabs.indexOf(initialTab.asVisibleLibraryTab()).takeIf { it >= 0 } ?: 0
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, orderedTabs, initialTab) {
        val currentTab = orderedTabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (currentTab != initialTab) {
            onTabChange(currentTab)
        }
    }

    CompositionLocalProvider(LocalLibraryPullGate provides pullGate) {
    Column(
        Modifier
            .fillMaxSize()
            .then(
                // 电视上没有可折叠的大标题，这个 scrollBehavior 却仍然挂在嵌套滚动链上，
                // 会把列表的滚动吃掉（表现为焦点能走出屏幕、页面不动）。电视上不接。
                if (LocalIsTelevision.current) Modifier
                else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 电视上不放大标题。原来这里还有一行放「最近播放」图标按钮，
        // 现在「最近播放」和「播放统计」都并进下面的标签栏了，这一行整个去掉，
        // 省下 44dp 的高度。
        if (!LocalIsTelevision.current) {
            LargeTopAppBar(
                modifier = Modifier.padding(horizontal = pageSidePaddingOr(0.dp)),
                title = { Text(stringResource(CoreCommonR.string.library_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    HapticIconButton(onClick = onOpenStats) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = stringResource(AppR.string.stats_title)
                        )
                    }
                    HapticIconButton(onClick = onOpenRecent) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = stringResource(CoreCommonR.string.library_recent_played)
                        )
                    }
                }
            )
        }

        Card(
            shape = neriRoundedShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .padding(horizontal = pageHorizontalPadding, vertical = 12.dp)
                .widthIn(max = 1180.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(Modifier.fillMaxSize()) {
                val currentTab = orderedTabs.getOrNull(pagerState.currentPage)
                LibraryMainTabs(
                    tabs = orderedTabs,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        if (isTelevision) {
                            // 电视上直接跳，不做逐页滑动的动画：
                            // 动画期间 currentPage 会依次经过中间几页，标签高亮跟着乱跳，
                            // 还会把 onTabChange 一股脑甩给宿主，看起来就是「几个标签来回跳」。
                            scope.launch { pagerState.scrollToPage(index) }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    },
                    onOpenStats = onOpenStats,
                    onOpenRecent = onOpenRecent,
                    onRefreshCurrentTab = {
                        when (orderedTabs.getOrNull(pagerState.currentPage)) {
                            LibraryTab.NETEASE,
                            LibraryTab.NETEASEALBUM -> vm.refreshNeteaseLibrary()
                            LibraryTab.YTMUSIC -> vm.refreshYouTubeMusicPlaylists()
                            LibraryTab.BILI -> vm.refreshBilibili()
                            // 本地/收藏/QQ音乐没有可刷新的远端数据，保持不动
                            else -> Unit
                        }
                    }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                    // 电视上没有滑动操作，翻页只由标签点击驱动。
                    // 打开横向滑动后，遥控器方向键可以把焦点带到相邻页，
                    // 分页器会跟着一路翻过去（B站搜索框聚焦后"往左跑"就是这个）。
                    userScrollEnabled = !isTelevision
                ) { page ->
                    when (orderedTabs[page]) {
                        LibraryTab.MY_MUSIC -> moe.ouom.neriplayer.ui.screen.server.MusicServerScreen(
                            onAlbumClick = onServerAlbumClick,
                            onArtistClick = onServerArtistClick,
                            showTitle = false,
                            onListStateAvailable = { serverListState = it },
                            offlineMode = offlineMode
                        )
                        LibraryTab.LOCAL -> LocalPlaylistList(
                            playlists = ui.localPlaylists,
                            listState = localListState,
                            onCreate = { name ->
                                val finalName = name.trim().ifBlank { defaultPlaylistName }
                                vm.createLocalPlaylist(finalName)
                            },
                            onClick = onLocalPlaylistClick,
                            onArtistClick = onLocalArtistClick,
                            onRename = { playlistId, newName ->
                                vm.renameLocalPlaylist(playlistId, newName)
                            },
                            onDelete = { playlistIds ->
                                vm.deleteLocalPlaylists(playlistIds) { result ->
                                    showPlaylistDeleteResultGlobally(
                                        context = context,
                                        repository = localPlaylistRepo,
                                        result = result
                                    )
                                }
                            },
                            onReorder = { order ->
                                vm.reorderLocalPlaylists(order)
                            },
                            offlineMode = offlineMode
                        )

                        LibraryTab.FAVORITE -> FavoritePlaylistList(
                            listState = favoriteListState,
                            onHotPlaylistClick = onHotPlaylistClick,
                            onNeteasePlaylistClick = onNeteasePlaylistClick,
                            onNeteaseAlbumClick = onNeteaseAlbumClick,
                            onNeteaseArtistClick = onNeteaseArtistClick,
                            onBiliPlaylistClick = onBiliPlaylistClick,
                            onYouTubeMusicPlaylistClick = onYouTubeMusicPlaylistClick,
                            onYouTubeMusicCreatorClick = onYouTubeMusicCreatorClick,
                            onBiliUploaderClick = onBiliUploaderClick,
                            offlineMode = offlineMode
                        )

                        LibraryTab.NETEASE,
                        LibraryTab.NETEASEALBUM -> LibraryPullToRefresh(
                            refreshing = ui.neteaseRefreshing,
                            enabled = !offlineMode && topAppBarState.collapsedFraction <= 0.01f,
                            onRefresh = vm::refreshNeteaseLibrary
                        ) { NeteaseLibraryList(
                            playlists = ui.neteasePlaylists,
                            albums = ui.neteaseAlbums,
                            playlistListState = neteaseListState,
                            albumListState = neteaseAlbumState,
                            selectedCategory = selectedNeteaseCategory,
                            onCategoryChange = { selectedNeteaseCategory = it },
                            onPlaylistClick = onNeteasePlaylistClick,
                            onAlbumClick = onNeteaseAlbumClick,
                            offlineMode = offlineMode
                        ) }

                        LibraryTab.YTMUSIC -> LibraryPullToRefresh(
                            refreshing = ui.youtubeMusicRefreshing,
                            enabled = !offlineMode && topAppBarState.collapsedFraction <= 0.01f,
                            onRefresh = vm::refreshYouTubeMusicPlaylists
                        ) { YouTubeMusicPlaylistList(
                            playlists = ui.youtubeMusicPlaylists,
                            error = ui.youtubeMusicError,
                            listState = youtubeMusicListState,
                            onClick = onYouTubeMusicPlaylistClick,
                            onRetry = { vm.refreshYouTubeMusicPlaylists() },
                            offlineMode = offlineMode
                        ) }

                        LibraryTab.BILI -> LibraryPullToRefresh(
                            refreshing = ui.biliRefreshing,
                            enabled = !offlineMode && topAppBarState.collapsedFraction <= 0.01f,
                            onRefresh = vm::refreshBilibili
                        ) { BiliPlaylistList(
                            playlists = ui.biliPlaylists,
                            error = ui.biliError,
                            listState = biliListState,
                            onClick = onBiliPlaylistClick,
                            offlineMode = offlineMode
                        ) }

                        LibraryTab.QQMUSIC -> QqMusicPlaylistList(
                            listState = qqMusicListState
                        )
                    }
                }
            }
        }
    }
    }
}
