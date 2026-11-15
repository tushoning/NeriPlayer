package moe.ouom.neriplayer.ui.screen.tab.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlin.random.Random
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.player.PlayerManager
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
import moe.ouom.neriplayer.ui.component.playlist.NeriSongRow
import moe.ouom.neriplayer.ui.component.playlist.PlaylistChromeTopBar
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.i18n.asString
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistActionSheetBleed
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernActionSheet
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernHeroHeader
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernHeroHeight
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernListItemSurface
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernPlaybackActions
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernVisualColorsProvider
import moe.ouom.neriplayer.ui.screen.playlist.playlistTopBarBleed
import moe.ouom.neriplayer.ui.screen.tab.library.LibrarySecondaryTabs
import moe.ouom.neriplayer.ui.theme.LocalHasCustomBackground
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.rememberTvDeferredImage
import moe.ouom.neriplayer.ui.tv.tvRowStart
import moe.ouom.neriplayer.ui.tv.tvScrollToTopOnUp
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel
import moe.ouom.neriplayer.util.format.formatDate
import moe.ouom.neriplayer.util.format.formatDurationSec
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

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
 * 此处不注册 BackHandler，只保留 toolbar 导航用的 [onBack]。
 */
@Composable
internal fun BiliMusicRankDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean,
    activeRankType: BiliMusicRankType,
    onRankTypeSelected: (BiliMusicRankType) -> Unit = {}
) {
    val vm = rememberBiliMusicDetailViewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val state = ui.biliMusicRank
    // 头图 / 玻璃卡片 / 顶栏背景都拿第一首的封面（顶栏要用，所以提到 Scaffold 外面）
    val heroCoverUrl = remember(state.songs) {
        state.songs.firstOrNull()?.videoCover
            ?.takeIf { it.isNotBlank() }
            ?.let { buildBiliThumbnailUrl(imageUrl = it, width = 640, height = 360) }
            .orEmpty()
    }
    rememberBiliMusicOfflineSync(
        vm = vm,
        offlineMode = offlineMode,
        // lambda 只读 ViewModel 的实时状态，不依赖组合快照，因此不需要 rememberUpdatedState
        isEmpty = {
            val current = vm.uiState.value.biliMusicRank
            current.songs.isEmpty() && !current.loading
        },
        onReload = { vm.loadBiliMusicRank(force = true) }
    )
    // 榜单类型由宿主持有（首页「查看全部」时记住的选中项，进程恢复后同样带回），而共享
    // HomeViewModel 可能已被重建回默认 HOT；这里把它对齐一次，避免展示与用户选择不一致的榜单。
    LaunchedEffect(activeRankType) {
        if (vm.uiState.value.biliMusicRank.type != activeRankType) {
            vm.selectBiliMusicRankType(activeRankType)
        }
    }
    val playableVideos = remember(state.songs) { state.songs.toPlayableRankVideoItems() }
    val titleText = stringResource(CoreCommonR.string.home_bili_music_rank)
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

    BiliMusicDetailLayout(
        title = titleText,
        heroCoverUrl = heroCoverUrl,
        heroSubtitle = heroSubtitle,
        offlineMode = offlineMode,
        loading = state.loading,
        errorText = state.error?.asString(),
        isEmpty = state.songs.isEmpty(),
        playableCount = playableVideos.size,
        onBack = onBack,
        onRefresh = { vm.loadBiliMusicRank(force = true) },
        onPlayInOrder = { if (playableVideos.isNotEmpty()) onPlay(playableVideos, 0) },
        onShufflePlay = {
            if (playableVideos.isNotEmpty()) {
                onPlay(playableVideos, Random.nextInt(playableVideos.size))
            }
        },
        itemKeyPrefix = "bili-music-rank",
        headerContent = {
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
        }
    ) {
        itemsIndexed(
            items = state.songs,
            key = { index, song ->
                song.musicId.ifBlank { "rank-row-$index-${song.creationBvid}" }
            }
        ) { _, song ->
            val playableItem = remember(song) { song.toPlayableBiliVideoItem() }
            PlaylistModernListItemSurface(
                coverUrl = heroCoverUrl,
                offlineMode = offlineMode
            ) {
                BiliMusicDetailRow(
                    index = song.rank,
                    title = song.title.ifBlank { song.album },
                    uploader = song.singer.ifBlank { song.uploader },
                    coverUrl = song.videoCover,
                    durationSec = song.displayDurationSec,
                    playableItem = playableItem,
                    metaLabel = song.playbackSourceLabel(),
                    offlineMode = offlineMode,
                    onClick = {
                        val index = playableItem?.let { target ->
                            playableVideos.indexOfFirst {
                                it.id == target.id && it.bvid == target.bvid
                            }
                        } ?: -1
                        if (index >= 0) {
                            onPlay(playableVideos, index)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 「Bilibili 音乐热门」查看全部页面（音乐分区视频榜）。
 *
 * 与 [BiliMusicRankDetailScreen] 一样共用首页的 [HomeViewModel]，因此同样需要把 [offlineMode]
 * 同步给 ViewModel，并且不注册根级 BackHandler，让宿主的预测性返回转场继续生效。
 */
@Composable
internal fun BiliMusicHotDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val vm = rememberBiliMusicDetailViewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val state = ui.biliMusicHot
    val heroCoverUrl = remember(state.items) {
        state.items.firstOrNull()?.coverUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { buildBiliThumbnailUrl(imageUrl = it, width = 640, height = 360) }
            .orEmpty()
    }
    rememberBiliMusicOfflineSync(
        vm = vm,
        offlineMode = offlineMode,
        isEmpty = {
            val current = vm.uiState.value.biliMusicHot
            current.items.isEmpty() && !current.loading
        },
        onReload = { vm.loadBiliMusicHot(force = true) }
    )
    val playableVideos = remember(state.items) { state.items.toPlayableHotVideoItems() }

    BiliMusicDetailLayout(
        title = stringResource(CoreCommonR.string.home_bili_music_hot),
        heroCoverUrl = heroCoverUrl,
        heroSubtitle = stringResource(CoreCommonR.string.home_bili_music_source_note),
        offlineMode = offlineMode,
        loading = state.loading,
        errorText = state.error?.asString(),
        isEmpty = state.items.isEmpty(),
        playableCount = playableVideos.size,
        onBack = onBack,
        onRefresh = { vm.loadBiliMusicHot(force = true) },
        onPlayInOrder = { if (playableVideos.isNotEmpty()) onPlay(playableVideos, 0) },
        onShufflePlay = {
            if (playableVideos.isNotEmpty()) {
                onPlay(playableVideos, Random.nextInt(playableVideos.size))
            }
        },
        itemKeyPrefix = "bili-music-hot"
    ) {
        itemsIndexed(
            items = state.items,
            key = { index, video ->
                video.bvid.ifBlank { "hot-row-$index-${video.aid}" }
            }
        ) { index, video ->
            val playableItem = remember(video) { video.toPlayableBiliVideoItem() }
            PlaylistModernListItemSurface(
                coverUrl = heroCoverUrl,
                offlineMode = offlineMode
            ) {
                BiliMusicDetailRow(
                    index = index + 1,
                    title = video.title,
                    uploader = video.uploader,
                    coverUrl = video.coverUrl,
                    durationSec = video.durationSec,
                    playableItem = playableItem,
                    metaLabel = listOf(video.tname, formatPlayCount(context, video.viewCount))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
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

/**
 * 榜单 / 热门详情页共用的骨架：顶栏 + 下拉刷新 + 头图 + 「全部播放」行 + 列表。
 *
 * 两个页面除了数据来源不同，滚动结构、头图铺法、列表行样式完全一致，合并在这里以免
 * 改一处漏一处（此前两套实现就出现过一边补了 `tvScrollToTopOnUp` 另一边漏掉的情况）。
 *
 * @param headerContent 插在「全部播放」行和列表之间的额外内容（榜单页的类型切换标签栏）
 * @param rows 列表内容；调用方自己在里面写 `itemsIndexed`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiliMusicDetailLayout(
    title: String,
    heroCoverUrl: String,
    heroSubtitle: String,
    offlineMode: Boolean,
    loading: Boolean,
    errorText: String?,
    isEmpty: Boolean,
    playableCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlayInOrder: () -> Unit,
    onShufflePlay: () -> Unit,
    itemKeyPrefix: String,
    headerContent: (LazyListScope.() -> Unit)? = null,
    rows: LazyListScope.() -> Unit
) {
    // 顶栏要跟着头图滚动状态变化，列表状态提到这里
    val listState = rememberLazyListState()
    // 播放中时详情页会被常驻迷你播放器遮住底部，列表末尾需要额外预留它的高度
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val shuffleEnabled by PlayerManager.shuffleModeFlow.collectAsState()
    val repeatMode by PlayerManager.repeatModeFlow.collectAsState()
    val hasCustomBackground = LocalHasCustomBackground.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PlaylistChromeTopBar(
                title = title,
                coverUrl = heroCoverUrl,
                offlineMode = offlineMode,
                heroHeight = PlaylistModernHeroHeight,
                listState = listState,
                onBack = onBack,
                actions = {
                    HapticIconButton(
                        enabled = !offlineMode,
                        onClick = onRefresh
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
            isRefreshing = loading,
            onRefresh = {
                if (!offlineMode) {
                    onRefresh()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !isEmpty -> PlaylistModernVisualColorsProvider(
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
                        item(key = "$itemKeyPrefix-hero") {
                            PlaylistModernHeroHeader(
                                displayName = title,
                                coverUrl = heroCoverUrl,
                                subtitle = heroSubtitle,
                                offlineMode = offlineMode,
                                height = PlaylistModernHeroHeight,
                                // 头图继续铺到「全部播放」那一行下面
                                topBleed = playlistTopBarBleed(),
                                backgroundBleed = PlaylistActionSheetBleed
                            )
                        }
                        item(key = "$itemKeyPrefix-actions") {
                            PlaylistModernActionSheet(
                                coverUrl = heroCoverUrl,
                                offlineMode = offlineMode,
                                hasCustomBackground = hasCustomBackground,
                                overHeroImage = true
                            ) {
                                PlaylistModernPlaybackActions(
                                    // 头图不是可聚焦节点：在第一屏内容上按上键先滚到顶
                                    modifier = Modifier.tvScrollToTopOnUp(listState),
                                    songCount = playableCount,
                                    shuffleEnabled = shuffleEnabled,
                                    repeatMode = repeatMode,
                                    showExport = false,
                                    onPlayInOrder = onPlayInOrder,
                                    onShufflePlay = onShufflePlay,
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
                        headerContent?.invoke(this)
                        rows()
                    }
                }

                loading -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_loading))

                errorText != null -> BiliMusicDetailError(
                    detail = errorText,
                    onRetry = onRefresh
                )

                else -> BiliMusicDetailHint(text = stringResource(CoreCommonR.string.home_bili_music_empty))
            }
        }
    }
}

/**
 * 详情页与首页共用同一个 ViewModel 实例（同一 ViewModelStoreOwner），这里只是把构造样板收起来。
 */
@Composable
private fun rememberBiliMusicDetailViewModel(): HomeViewModel {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    return viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(appContext as Application)
            }
        }
    )
}

/**
 * 把 [offlineMode] 同步给共享 ViewModel，并在从离线切回在线时重启一次因离线被跳过而遗留的空加载。
 *
 * 详情页打开时首页不再组合，[HomeScreen] 的 wasOffline 恢复刷新不会抵达这个目的地，
 * 所以这一页必须自己负责。留空不处理的话会一直停在「暂无内容」直到手动刷新。
 */
@Composable
private fun rememberBiliMusicOfflineSync(
    vm: HomeViewModel,
    offlineMode: Boolean,
    isEmpty: () -> Boolean,
    onReload: () -> Unit
) {
    var wasOffline by remember { mutableStateOf(false) }
    // isEmpty / onReload 内部只读 ViewModel 的实时状态，闭包捕获的是稳定的 vm 引用，不会读到过期快照
    LaunchedEffect(offlineMode) {
        val cameBackOnline = wasOffline && !offlineMode
        vm.setOfflineMode(offlineMode)
        if (cameBackOnline) {
            wasOffline = false
            if (isEmpty()) {
                onReload()
            }
        } else if (offlineMode) {
            wasOffline = true
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
 * 榜单 / 热门详情行：名次、100x60 缩略图、标题、上传者、第三行元信息与时长。
 *
 * 两个页面只有第三行文字不同（榜单是播放来源，热门是分区 + 播放量），其余完全同构，
 * 所以第三行由调用方算好 [metaLabel] 传进来。
 *
 * @param index 展示用名次（榜单用接口返回的 rank，热门用 index + 1）
 * @param playableItem 可播放条目；为 null 表示该条没有可播放音源，行置灰
 */
@Composable
private fun BiliMusicDetailRow(
    index: Int,
    title: String,
    uploader: String,
    coverUrl: String,
    durationSec: Int,
    playableItem: BiliVideoItem?,
    metaLabel: String,
    offlineMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val favoriteActions = rememberBiliSongFavoriteActions()
    val songItem = remember(playableItem) { playableItem?.toSongItem() }
    val thumbnailUrl = remember(coverUrl) {
        buildBiliThumbnailUrl(imageUrl = coverUrl, width = 192, height = 108)
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
    // 电视：遥控器连续操作期间先不发起新加载（和首页卡片、歌曲行同一套）
    val deferredCover = rememberTvDeferredImage(key = thumbnailUrl) { coverRequest }
    // 不可播放的视频没有 SongItem，这里造一份只用于展示的占位
    val rowSong = songItem ?: SongItem(
        id = 0L,
        name = title,
        artist = uploader,
        album = "",
        albumId = 0L,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = null
    )

    NeriSongRow(
        index = index,
        song = rowSong,
        onClick = onClick,
        // 整行在最左边 = 第 0 列，按左直接回侧栏本页标签。
        // 注意必须挂在 NeriSongRow 这个自己可聚焦的节点上：挂到外层 Row / IconButton(modifier=)
        // 只会成为祖先观察者，isFocused 恒为 false，静默失效。
        modifier = Modifier.tvRowStart(),
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
                    // 时长挪到行尾三点前面了（和歌单页一致），这里只留来源 / 分区
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
                model = deferredCover.model,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                onSuccess = { deferredCover.markLoaded() },
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .clip(neriRoundedShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        offlineMode = offlineMode,
        durationText = formatDurationSec(durationSec),
        indexTextStyle = MaterialTheme.typography.titleSmall,
        // 两个模型的 isPlayable 与「能否转成 BiliVideoItem」等价，因此直接用 playableItem 判定
        enabled = playableItem != null,
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
