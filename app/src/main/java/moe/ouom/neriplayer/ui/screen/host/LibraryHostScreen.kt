package moe.ouom.neriplayer.ui.screen.host

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
 * File: moe.ouom.neriplayer.ui.screen.host/LibraryHostScreen
 * Created: 2025/1/17
 */

import android.os.Parcelable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlinx.parcelize.Parcelize
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.ui.screen.artist.NeteaseArtistDetailScreen
import moe.ouom.neriplayer.ui.screen.artist.BiliUploaderDetailScreen
import moe.ouom.neriplayer.ui.screen.artist.YouTubeMusicCreatorNavigationScreen
import moe.ouom.neriplayer.ui.screen.history.RecentScreen
import moe.ouom.neriplayer.ui.screen.history.stats.PlaybackStatsScreen
import moe.ouom.neriplayer.ui.screen.playlist.LocalArtistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.HotPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.LocalPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteaseAlbumDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.BiliPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.tab.library.LibraryTab
import moe.ouom.neriplayer.ui.screen.tab.library.LibraryScreen
import moe.ouom.neriplayer.data.model.NeteaseArtistSummary
import moe.ouom.neriplayer.data.model.BiliUploaderSummary
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import moe.ouom.neriplayer.ui.component.navigation.TvTopBarFocus
import moe.ouom.neriplayer.ui.navigation.biliUploaderSourceRoute
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.data.model.stats.PlaybackStatsPeriod
import moe.ouom.neriplayer.data.model.bilibili.video.VideoBasicInfo
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.local.media.displayCoverUrl
import moe.ouom.neriplayer.data.playlist.usage.PlaylistUsageRepository
import moe.ouom.neriplayer.platform.youtube.api.transport.stableYouTubeMusicId
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSceneMotion
import moe.ouom.neriplayer.ui.effect.glass.advancedGlassHostNavigationTransition
import moe.ouom.neriplayer.ui.effect.glass.animateAdvancedGlassSceneMotion
import moe.ouom.neriplayer.ui.navigation.animateMainTabDetailCloseRootRevealFraction
import moe.ouom.neriplayer.ui.navigation.clipMainTabDetailCloseRoot
import moe.ouom.neriplayer.ui.navigation.rememberMainTabSceneRestoredEntry
import moe.ouom.neriplayer.ui.navigation.shouldSuppressRestoredMainTabHostEntry
import moe.ouom.neriplayer.ui.util.SAVED_TYPE_KEY
import moe.ouom.neriplayer.ui.util.restoreSavedByType
import moe.ouom.neriplayer.ui.util.savedMap
import moe.ouom.neriplayer.ui.util.savedNonBlankString
import moe.ouom.neriplayer.ui.util.savedNumber
import moe.ouom.neriplayer.ui.util.savedString
import moe.ouom.neriplayer.ui.util.toSaveMap
import moe.ouom.neriplayer.ui.util.restoreBiliPlaylist
import moe.ouom.neriplayer.ui.util.restoreAlbumSummary
import moe.ouom.neriplayer.ui.util.restorePlaylistSummary
import moe.ouom.neriplayer.ui.util.restoreYouTubeMusicPlaylist
import moe.ouom.neriplayer.util.media.CoverArtColorCache
import moe.ouom.neriplayer.platform.subsonic.repository.ServerAlbum as MusicServerAlbum
import moe.ouom.neriplayer.platform.subsonic.repository.ServerArtist as MusicServerArtist
import moe.ouom.neriplayer.ui.screen.server.album.ServerAlbumDetailScreen
import moe.ouom.neriplayer.ui.screen.server.artist.ServerArtistDetailScreen
import moe.ouom.neriplayer.ui.screen.server.artist.serverArtistDetailKey
import moe.ouom.neriplayer.ui.tv.LocalTvReturnDepth
import moe.ouom.neriplayer.ui.tv.LocalTvReturnScope
import moe.ouom.neriplayer.ui.tv.TvReturnFocusPageBridge
import moe.ouom.neriplayer.ui.tv.TvTabConfirmBus
import moe.ouom.neriplayer.ui.tv.TvTabRootBus

@Parcelize
sealed class LibrarySelectedItem : Parcelable {
    internal abstract fun saveState(): Map<String, Any?>

    @Parcelize
    data class Local(val playlistId: Long) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "local", "playlistId" to playlistId)
    }
    @Parcelize
    data class LocalArtist(val artistName: String) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "localArtist", "artistName" to artistName)
    }
    @Parcelize
    data class ServerAlbum(
        val profileId: String,
        val albumId: String,
        val name: String,
        val artist: String,
        val coverUrl: String?,
        val songCount: Int
    ) : LibrarySelectedItem() {
        fun summary() = MusicServerAlbum(profileId, albumId, name, artist, coverUrl, songCount)

        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "serverAlbum", "profileId" to profileId, "albumId" to albumId,
            "name" to name, "artist" to artist, "coverUrl" to coverUrl, "songCount" to songCount
        )
    }
    @Parcelize
    data class ServerArtist(
        val profileId: String,
        val artistId: String,
        val name: String,
        val coverUrl: String?,
        val albumCount: Int,
        val fallbackCoverUrl: String? = null
    ) : LibrarySelectedItem() {
        fun summary() = MusicServerArtist(profileId, artistId, name, coverUrl, albumCount, fallbackCoverUrl)

        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "serverArtist", "profileId" to profileId, "artistId" to artistId,
            "name" to name, "coverUrl" to coverUrl, "albumCount" to albumCount
        ).apply { fallbackCoverUrl?.let { put("fallbackCoverUrl", it) } }
    }
    @Parcelize
    data class Hot(val monthly: Boolean) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "hot", "monthly" to monthly)
    }
    @Parcelize
    data class Netease(val playlist: PlaylistSummary) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "netease", "playlist" to playlist.toSaveMap())
    }
    @Parcelize
    data class NeteaseAlbum(val album: AlbumSummary) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "neteaseAlbum", "album" to album.toSaveMap())
    }
    @Parcelize
    data class NeteaseArtist(val artist: NeteaseArtistSummary) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "neteaseArtist",
            "artistId" to artist.id,
            "artistName" to artist.name
        )
    }
    @Parcelize
    data class NeteaseArtistAlbum(
        val artist: NeteaseArtistSummary,
        val album: AlbumSummary
    ) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "neteaseArtistAlbum",
            "artistId" to artist.id,
            "artistName" to artist.name,
            "album" to album.toSaveMap()
        )
    }
    /**
     * 「最近播放」「播放统计」也是媒体库的二级页：走宿主自己的场景栈，
     * 盖上来以后媒体库列表不再参与组合，焦点和触摸就不会再穿透到下面那一层。
     */
    @Parcelize
    data class Recent(val entry: Int = 0) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf<String, Any?>(SAVED_TYPE_KEY to "recent")
    }
    @Parcelize
    data class Stats(val entry: Int = 0) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf<String, Any?>(SAVED_TYPE_KEY to "stats")
    }
    @Parcelize
    data class Bili(val playlist: BiliPlaylist) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "bili", "playlist" to playlist.toSaveMap())
    }
    @Parcelize
    data class YouTubeMusic(val playlist: YouTubeMusicPlaylist) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(SAVED_TYPE_KEY to "ytmusic", "playlist" to playlist.toSaveMap())
    }
    @Parcelize
    data class BiliUploader(val uploader: BiliUploaderSummary) : LibrarySelectedItem() {
        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "biliArtist",
            "mid" to uploader.mid,
            "name" to uploader.name,
            "avatar" to uploader.avatarUrl
        )
    }
    @Parcelize
    data class YouTubeCreator(
        val browseId: String,
        val title: String,
        val subtitle: String,
        val coverUrl: String
    ) : LibrarySelectedItem() {
        fun summary() = YouTubeMusicCreatorSummary(browseId, title, subtitle, coverUrl)

        override fun saveState() = hashMapOf(
            SAVED_TYPE_KEY to "youtubeArtist",
            "browseId" to browseId,
            "title" to title,
            "subtitle" to subtitle,
            "coverUrl" to coverUrl
        )
    }
}

private val LibrarySelectedItem?.navigationDepth: Int
    get() = when (this) {
        null -> 0
        is LibrarySelectedItem.NeteaseArtistAlbum -> 2
        else -> 1
    }

internal data class LibraryNavigationScene(
    val item: LibrarySelectedItem?,
    val navigationDepth: Int
)

internal fun libraryNavigationScene(
    item: LibrarySelectedItem?,
    creatorParents: List<LibrarySelectedItem>
): LibraryNavigationScene = LibraryNavigationScene(
    item = item,
    navigationDepth = if (item == null) 0 else item.navigationDepth + creatorParents.size
)

internal fun libraryDetailBackTarget(current: LibrarySelectedItem?): LibrarySelectedItem? =
    when (current) {
        is LibrarySelectedItem.NeteaseArtistAlbum -> LibrarySelectedItem.NeteaseArtist(current.artist)
        else -> null
    }

/** 与 closeSelectedDetail 的结果一致，供预测性返回提前展示上一层 */
internal fun resolveLibraryBackScene(
    selected: LibrarySelectedItem?,
    creatorParents: List<LibrarySelectedItem>
): LibraryNavigationScene {
    val parent = creatorParents.lastOrNull()
    return if (parent != null) {
        libraryNavigationScene(parent, creatorParents.dropLast(1))
    } else {
        libraryNavigationScene(libraryDetailBackTarget(selected), creatorParents)
    }
}

private fun LibrarySelectedItem.Hot.period(): PlaybackStatsPeriod {
    return if (monthly) PlaybackStatsPeriod.MONTH else PlaybackStatsPeriod.WEEK
}

private enum class LibraryScrollSource {
    Local,
    Favorite,
    NeteasePlaylist,
    NeteaseAlbum,
    YouTubeMusic,
    Bili
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHostScreen(
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onSongClickWithSourceRoute: (List<SongItem>, Int, String?) -> Unit = { songs, index, _ ->
        onSongClick(songs, index)
    },
    onPlayBiliAudioWithSourceRoute: (List<BiliVideoItem>, Int, String?) -> Unit = { videos, index, _ ->
        PlayerManager.playBiliVideoAsAudio(videos, index)
    },
    onPlayBiliPartsWithSourceRoute: (
        VideoBasicInfo,
        Int,
        String,
        String?
    ) -> Unit = { videoInfo, index, coverUrl, _ ->
        PlayerManager.playBiliVideoParts(videoInfo, index, coverUrl)
    },
    neteasePlaylistSourceRoute: (PlaylistSummary) -> String? = { null },
    neteaseAlbumSourceRoute: (AlbumSummary) -> String? = { null },
    biliPlaylistSourceRoute: (BiliPlaylist) -> String? = { null },
    localPlaylistSourceRoute: (Long) -> String? = { null },
    onOpenRecent: () -> Unit,
    onOpenStats: () -> Unit = {},
    offlineMode: Boolean = false,
    coherentFeedbackEnabled: Boolean = false,
    renderScene: @Composable (
        revealTopFraction: Float,
        contentTranslationYFraction: Float,
        contentScale: Float,
        sceneDepth: Int,
        content: @Composable () -> Unit
    ) -> Unit = { _, _, _, _, content ->
        content()
    }
) {
    var selected by rememberSaveable(stateSaver = librarySelectedItemSaver) {
        mutableStateOf(null)
    }
    var creatorParents by rememberSaveable(
        stateSaver = listSaver<List<LibrarySelectedItem>, LibrarySelectedItem>(
            save = { it }, restore = { it }
        )
    ) {
        mutableStateOf(emptyList())
    }
    var skipDetailCloseAnimation by rememberSaveable { mutableStateOf(false) }
    var pendingScrollSource by rememberSaveable {
        mutableStateOf<LibraryScrollSource?>(null)
    }
    var pendingListRestoreIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingListRestoreOffset by rememberSaveable { mutableIntStateOf(0) }
    var pendingTopAppBarHeightOffset by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var pendingTopAppBarContentOffset by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    // 保存当前选中的标签页类型，避免国际化切换后索引错位
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.LOCAL) }
    val libraryStateHolder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingNeteaseCoverWarmupJob by remember { mutableStateOf<Job?>(null) }
    var pendingNeteaseCoverWarmupToken by remember { mutableIntStateOf(0) }

    fun cancelPendingNeteaseCoverWarmup() {
        pendingNeteaseCoverWarmupToken += 1
        pendingNeteaseCoverWarmupJob?.cancel()
        pendingNeteaseCoverWarmupJob = null
    }

    fun openAfterNeteaseCoverWarmup(
        coverUrl: String?,
        item: LibrarySelectedItem
    ) {
        val token = pendingNeteaseCoverWarmupToken + 1
        pendingNeteaseCoverWarmupToken = token
        pendingNeteaseCoverWarmupJob?.cancel()
        pendingNeteaseCoverWarmupJob = scope.launch {
            CoverArtColorCache.preload(context, coverUrl, offlineMode)
            if (pendingNeteaseCoverWarmupToken == token) {
                selected = item
                pendingNeteaseCoverWarmupJob = null
            }
        }
    }

    fun openLibrarySelectedItem(item: LibrarySelectedItem) {
        when (item) {
            is LibrarySelectedItem.Netease -> {
                openAfterNeteaseCoverWarmup(item.playlist.picUrl, item)
            }
            is LibrarySelectedItem.NeteaseAlbum -> {
                openAfterNeteaseCoverWarmup(item.album.picUrl, item)
            }
            is LibrarySelectedItem.NeteaseArtistAlbum -> {
                openAfterNeteaseCoverWarmup(item.album.picUrl, item)
            }
            else -> {
                cancelPendingNeteaseCoverWarmup()
                selected = item
            }
        }
    }

    fun closeSelectedDetail() {
        cancelPendingNeteaseCoverWarmup()
        skipDetailCloseAnimation = false
        val parent = creatorParents.lastOrNull()
        if (parent != null) {
            selected = parent
            creatorParents = creatorParents.dropLast(1)
            return
        }
        selected = libraryDetailBackTarget(selected)
    }

    fun openNeteaseArtist(artist: NeteaseArtistSummary) {
        val currentArtist = (selected as? LibrarySelectedItem.NeteaseArtist)?.artist
        if (currentArtist?.id == artist.id) return
        cancelPendingNeteaseCoverWarmup()
        skipDetailCloseAnimation = false
        selected = LibrarySelectedItem.NeteaseArtist(artist)
    }

    fun closeDeletedLocalPlaylist() {
        cancelPendingNeteaseCoverWarmup()
        skipDetailCloseAnimation = true
        selected = null
    }

    LaunchedEffect(selected) {
        if (selected != null) {
            skipDetailCloseAnimation = false
        }
    }

    // 保存各个列表的滚动状态
    val localListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val favoriteListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val neteaseAlbumSaver: Saver<LazyListState, *> = LazyListState.Saver
    val neteaseListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val youtubeMusicListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val biliListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val qqMusicListSaver: Saver<LazyListState, *> = LazyListState.Saver

    val localListState = rememberSaveable(saver = localListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val favoriteListState = rememberSaveable(saver = favoriteListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val neteaseListState = rememberSaveable(saver = neteaseListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val neteaseAlbumState = rememberSaveable(saver = neteaseAlbumSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val youtubeMusicListState = rememberSaveable(saver = youtubeMusicListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val biliListState = rememberSaveable(saver = biliListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val qqMusicListState = rememberSaveable(saver = qqMusicListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val topAppBarState = rememberTopAppBarState()
    fun listStateFor(source: LibraryScrollSource): LazyListState = when (source) {
        LibraryScrollSource.Local -> localListState
        LibraryScrollSource.Favorite -> favoriteListState
        LibraryScrollSource.NeteasePlaylist -> neteaseListState
        LibraryScrollSource.NeteaseAlbum -> neteaseAlbumState
        LibraryScrollSource.YouTubeMusic -> youtubeMusicListState
        LibraryScrollSource.Bili -> biliListState
    }

    fun captureLibraryScrollPosition(source: LibraryScrollSource) {
        val position = listStateFor(source).captureHostScrollPosition()
        pendingScrollSource = source
        pendingListRestoreIndex = position.index
        pendingListRestoreOffset = position.offset
        pendingTopAppBarHeightOffset = topAppBarState.heightOffset
        pendingTopAppBarContentOffset = topAppBarState.contentOffset
    }

    fun sourceForFavoriteAwareDestination(
        regularSource: LibraryScrollSource
    ): LibraryScrollSource = if (selectedTab == LibraryTab.FAVORITE) {
        LibraryScrollSource.Favorite
    } else {
        regularSource
    }

    /** 当前标签页对应的根列表（MY_MUSIC 由服务端列表自己管，没有独立 LazyListState） */
    fun currentTabListState(): LazyListState? = when (selectedTab) {
        LibraryTab.LOCAL -> localListState
        LibraryTab.FAVORITE -> favoriteListState
        LibraryTab.NETEASE -> neteaseListState
        LibraryTab.NETEASEALBUM -> neteaseAlbumState
        LibraryTab.YTMUSIC -> youtubeMusicListState
        LibraryTab.BILI -> biliListState
        LibraryTab.QQMUSIC -> qqMusicListState
        LibraryTab.MY_MUSIC -> null
    }

    val navigationTransition = rememberHostPredictiveBackTransition(
        targetState = libraryNavigationScene(selected, creatorParents),
        backEnabled = selected != null,
        backTargetState = resolveLibraryBackScene(selected, creatorParents),
        onBack = { closeSelectedDetail() },
        label = "library_host_switch"
    )

    LaunchedEffect(
        selected,
        pendingScrollSource,
        pendingListRestoreIndex
    ) {
        val source = pendingScrollSource ?: return@LaunchedEffect
        val restoreIndex = pendingListRestoreIndex ?: return@LaunchedEffect
        if (selected != null) return@LaunchedEffect
        listStateFor(source).restoreHostScrollPosition(
            HostScrollPosition(
                index = restoreIndex,
                offset = pendingListRestoreOffset
            )
        )
        if (!pendingTopAppBarHeightOffset.isNaN()) {
            topAppBarState.heightOffset = pendingTopAppBarHeightOffset
        }
        if (!pendingTopAppBarContentOffset.isNaN()) {
            topAppBarState.contentOffset = pendingTopAppBarContentOffset
        }
        pendingScrollSource = null
        pendingListRestoreIndex = null
        pendingListRestoreOffset = 0
        pendingTopAppBarHeightOffset = Float.NaN
        pendingTopAppBarContentOffset = Float.NaN
    }
    // 电视「返回来源」：进详情前记住是哪张卡，退出时把光标送回去。
    // 放在上面「恢复滚动位置」之后，先滚回视口再请求焦点。
    // 媒体库有「歌手 → 创作者 → 专辑」这种多层后退，深度取场景自带的 navigationDepth。
    val libraryReturnDepth = libraryNavigationScene(selected, creatorParents).navigationDepth
    TvReturnFocusPageBridge("library", depth = libraryReturnDepth)
    // 电视：侧栏再按一次「归集」= 回到归集根。
    // 详情态下 LibraryScreen 不在组合里，刷新总线没人收，所以先在这里把「回根」
    // 翻译成「退出详情 + 清掉残留的滚动恢复标记 + 回顶」；根态才走原来的刷新逻辑。
    LaunchedEffect(Unit) {
        TvTabRootBus.events.collect { route ->
            if (route != Destinations.Library.route) return@collect
            if (selected != null) {
                cancelPendingNeteaseCoverWarmup()
                skipDetailCloseAnimation = false
                // 先清恢复标记：否则退出详情后会被 pending 的位置又拉回去
                pendingScrollSource = null
                pendingListRestoreIndex = null
                pendingListRestoreOffset = 0
                pendingTopAppBarHeightOffset = Float.NaN
                pendingTopAppBarContentOffset = Float.NaN
                creatorParents = emptyList()
                selected = null
                currentTabListState()?.requestScrollToItem(0)
            } else {
                TvTabConfirmBus.request(Destinations.Library.route)
            }
        }
    }
    val suppressRestoredSceneEntry = rememberMainTabSceneRestoredEntry()
    val detailCloseRootRevealFraction =
        navigationTransition.animateMainTabDetailCloseRootRevealFraction(
            navigationDepth = { item -> item.navigationDepth },
            label = "library_host_detail_close"
        )

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
    // 电视：整页转场（300ms、双场景、reveal 裁剪 + 缩放重测）在弱 GPU/弱 CPU 上是
    // 「切页面卡一下」的主要来源，且没人要求电视上有这个动效。
    val tvSkipSceneMotion = moe.ouom.neriplayer.ui.tv.LocalIsTelevision.current
        navigationTransition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            contentKey = { scene ->
                when (val item = scene.item) {
                    is LibrarySelectedItem.NeteaseArtist -> "netease_artist_${item.artist.id}"
                    is LibrarySelectedItem.YouTubeCreator -> youtubeMusicCreatorDetailStateKey(item.summary())
                    is LibrarySelectedItem.BiliUploader -> "bili_uploader_${item.uploader.mid}"
                    is LibrarySelectedItem.ServerArtist -> serverArtistDetailKey(item.summary())
                    else -> item
                }
            },
            transitionSpec = {
                if (tvSkipSceneMotion) {

                    EnterTransition.None togetherWith ExitTransition.None

                } else if (
                    shouldSuppressRestoredMainTabHostEntry(
                        restoredEntry = suppressRestoredSceneEntry,
                        initialDepth = initialState.navigationDepth,
                        targetDepth = targetState.navigationDepth
                    )
                ) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (targetState.item == null && skipDetailCloseAnimation) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    advancedGlassHostNavigationTransition(
                        forward = targetState.navigationDepth > initialState.navigationDepth,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        targetContentZIndex = targetState.navigationDepth.toFloat()
                    )
                }.using(SizeTransform(clip = true))
            }
        ) { scene ->
            val current = scene.item
            val suppressRestoredSceneMotion = shouldSuppressRestoredMainTabHostEntry(
                restoredEntry = suppressRestoredSceneEntry,
                initialDepth = navigationTransition.currentState.navigationDepth,
                targetDepth = navigationTransition.targetState.navigationDepth
            )
            // 电视上场景动效一个像素都不画，但每帧仍会产出新的 SceneMotion 让整棵子树重组
            val sceneMotion = if (suppressRestoredSceneMotion || tvSkipSceneMotion) {
                AdvancedGlassSceneMotion.None
            } else {
                navigationTransition.animateAdvancedGlassSceneMotion(
                    sceneState = scene,
                    coherentFeedbackEnabled = coherentFeedbackEnabled,
                    navigationDepth = { item -> item.navigationDepth },
                    label = "library_host_scene"
                )
            }
            renderScene(
                sceneMotion.revealTopFraction,
                sceneMotion.contentTranslationYFraction,
                sceneMotion.contentScale,
                scene.navigationDepth
            ) {
                // 电视「返回来源」：卡片只传稳定 key，宿主 + 层级由这里下发。
                CompositionLocalProvider(
                    LocalTvReturnScope provides
                        if (moe.ouom.neriplayer.ui.tv.LocalIsTelevision.current) "library" else null,
                    LocalTvReturnDepth provides scene.navigationDepth
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (current == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipMainTabDetailCloseRoot(
                                    detailCloseRootRevealFraction
                                )
                        ) {
                            libraryStateHolder.SaveableStateProvider("library_screen") {
                                LibraryScreen(
                            initialTab = selectedTab,
                            onTabChange = { selectedTab = it },
                            onServerAlbumClick = { album ->
                                skipDetailCloseAnimation = false
                                openLibrarySelectedItem(LibrarySelectedItem.ServerAlbum(
                                    album.profileId, album.id, album.name, album.artist, album.coverUrl, album.songCount
                                ))
                            },
                            onServerArtistClick = { artist ->
                                skipDetailCloseAnimation = false
                                openLibrarySelectedItem(LibrarySelectedItem.ServerArtist(
                                    artist.profileId, artist.id, artist.name, artist.coverUrl, artist.albumCount, artist.fallbackCoverUrl
                                ))
                            },
                            localListState = localListState,
                            favoriteListState = favoriteListState,
                            neteaseAlbumState = neteaseAlbumState,
                            neteaseListState = neteaseListState,
                            youtubeMusicListState = youtubeMusicListState,
                            biliListState = biliListState,
                            qqMusicListState = qqMusicListState,
                            topAppBarState = topAppBarState,
                            offlineMode = offlineMode,
                            onLocalPlaylistClick = { playlist ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(LibraryScrollSource.Local)
                                openLibrarySelectedItem(LibrarySelectedItem.Local(playlist.id))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = playlist.id,
                                        name = playlist.name,
                                        picUrl = playlist.displayCoverUrl(context),
                                        trackCount = playlist.songs.size,
                                        source = "local"
                                    )
                                }
                            },
                            onLocalArtistClick = { artist ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(LibraryScrollSource.Local)
                                openLibrarySelectedItem(LibrarySelectedItem.LocalArtist(artist.name))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = artist.id,
                                        name = artist.name,
                                        picUrl = artist.displayCoverUrl(context),
                                        trackCount = artist.songs.size,
                                        source = PlaylistUsageRepository.SOURCE_LOCAL_ARTIST
                                    )
                                }
                            },
                            onHotPlaylistClick = { period ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(LibraryScrollSource.Favorite)
                                openLibrarySelectedItem(
                                    LibrarySelectedItem.Hot(
                                        monthly = period == PlaybackStatsPeriod.MONTH
                                    )
                                )
                            },
                            onNeteasePlaylistClick = { playlist ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(
                                    sourceForFavoriteAwareDestination(
                                        LibraryScrollSource.NeteasePlaylist
                                    )
                                )
                                openLibrarySelectedItem(LibrarySelectedItem.Netease(playlist))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = playlist.id,
                                        name = playlist.name,
                                        picUrl = playlist.picUrl,
                                        trackCount = playlist.trackCount,
                                        source = "netease"
                                    )
                                }
                            },
                            onNeteaseAlbumClick = { album ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(
                                    sourceForFavoriteAwareDestination(
                                        LibraryScrollSource.NeteaseAlbum
                                    )
                                )
                                openLibrarySelectedItem(LibrarySelectedItem.NeteaseAlbum(album))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = album.id,
                                        name = album.name,
                                        picUrl = album.picUrl,
                                        trackCount = album.size,
                                        source = "neteaseAlbum"
                                    )
                                }
                            },
                            onNeteaseArtistClick = { artist ->
                                captureLibraryScrollPosition(LibraryScrollSource.Favorite)
                                openNeteaseArtist(artist)
                            },
                            onBiliUploaderClick = { uploader ->
                                captureLibraryScrollPosition(LibraryScrollSource.Favorite)
                                openLibrarySelectedItem(LibrarySelectedItem.BiliUploader(uploader))
                            },
                            onYouTubeMusicCreatorClick = { creator ->
                                captureLibraryScrollPosition(LibraryScrollSource.Favorite)
                                openLibrarySelectedItem(
                                    LibrarySelectedItem.YouTubeCreator(
                                        creator.browseId, creator.title, creator.subtitle, creator.coverUrl
                                    )
                                )
                            },
                            onYouTubeMusicPlaylistClick = { playlist ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(
                                    sourceForFavoriteAwareDestination(
                                        LibraryScrollSource.YouTubeMusic
                                    )
                                )
                                openLibrarySelectedItem(LibrarySelectedItem.YouTubeMusic(playlist))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = stableYouTubeMusicId(
                                            playlist.playlistId.ifBlank { playlist.browseId }
                                        ),
                                        name = playlist.title,
                                        picUrl = playlist.coverUrl,
                                        trackCount = playlist.trackCount,
                                        source = "youtubeMusic",
                                        browseId = playlist.browseId,
                                        playlistId = playlist.playlistId
                                    )
                                }
                            },
                            onBiliPlaylistClick = { playlist ->
                                skipDetailCloseAnimation = false
                                captureLibraryScrollPosition(
                                    sourceForFavoriteAwareDestination(LibraryScrollSource.Bili)
                                )
                                openLibrarySelectedItem(LibrarySelectedItem.Bili(playlist))
                                AppContainer.launchBackgroundIo {
                                    AppContainer.playlistUsageRepo.recordOpen(
                                        id = playlist.mediaId,
                                        name = playlist.title,
                                        picUrl = playlist.coverUrl,
                                        trackCount = playlist.count,
                                        source = "bili",
                                        mid = playlist.mid,
                                        fid = playlist.fid,
                                        subtype = playlist.kind.name,
                                        subtitle = playlist.subtitle
                                    )
                                }
                            },
                            // 标签只是入口：点了就在宿主里压一个新场景，和歌单详情同样的层级
                            onOpenRecent = {
                                skipDetailCloseAnimation = false
                                openLibrarySelectedItem(LibrarySelectedItem.Recent())
                            },
                            onOpenStats = {
                                skipDetailCloseAnimation = false
                                openLibrarySelectedItem(LibrarySelectedItem.Stats())
                            }
                        )
                        }
                        }
                    } else {
                        when (current) {
                        is LibrarySelectedItem.Local -> {
                            LocalPlaylistDetailScreen(
                                playlistId = current.playlistId,
                                onBack = { closeSelectedDetail() },
                                onDeleted = { closeDeletedLocalPlaylist() },
                                onSongClick = { songs, index ->
                                    onSongClickWithSourceRoute(
                                        songs,
                                        index,
                                        localPlaylistSourceRoute(current.playlistId)
                                    )
                                },
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.Recent -> {
                            RecentScreen(
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.Stats -> {
                            PlaybackStatsScreen(
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.LocalArtist -> {
                            LocalArtistDetailScreen(
                                artistName = current.artistName,
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.ServerAlbum -> {
                            ServerAlbumDetailScreen(
                                album = current.summary(),
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.ServerArtist -> {
                            libraryStateHolder.SaveableStateProvider(serverArtistDetailKey(current.summary())) {
                                ServerArtistDetailScreen(
                                    artist = current.summary(),
                                    onBack = { closeSelectedDetail() },
                                    onAlbumClick = { album ->
                                        creatorParents = creatorParents + current
                                        openLibrarySelectedItem(LibrarySelectedItem.ServerAlbum(
                                            album.profileId, album.id, album.name, album.artist, album.coverUrl, album.songCount
                                        ))
                                    },
                                    offlineMode = offlineMode
                                )
                            }
                        }

                        is LibrarySelectedItem.Hot -> {
                            HotPlaylistDetailScreen(
                                period = current.period(),
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.NeteaseAlbum -> {
                            NeteaseAlbumDetailScreen(
                                onBack = { selected = null },
                                onSongClick = { songs, index ->
                                    onSongClickWithSourceRoute(
                                        songs,
                                        index,
                                        neteaseAlbumSourceRoute(current.album)
                                    )
                                },
                                album = current.album,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.Netease -> {
                            NeteasePlaylistDetailScreen(
                                playlist = current.playlist,
                                onBack = { selected = null },
                                onSongClick = { songs, index ->
                                    onSongClickWithSourceRoute(
                                        songs,
                                        index,
                                        neteasePlaylistSourceRoute(current.playlist)
                                    )
                                },
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.NeteaseArtist -> {
                            libraryStateHolder.SaveableStateProvider(
                                "netease_artist_${current.artist.id}"
                            ) {
                                NeteaseArtistDetailScreen(
                                    artist = current.artist,
                                    onBack = { selected = null },
                                    onSongClick = onSongClick,
                                    offlineMode = offlineMode,
                                    onAlbumClick = { album ->
                                        openLibrarySelectedItem(
                                            LibrarySelectedItem.NeteaseArtistAlbum(
                                                current.artist,
                                                album
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        is LibrarySelectedItem.NeteaseArtistAlbum -> {
                            NeteaseAlbumDetailScreen(
                                onBack = {
                                    selected = LibrarySelectedItem.NeteaseArtist(current.artist)
                                },
                                onSongClick = { songs, index ->
                                    onSongClickWithSourceRoute(
                                        songs,
                                        index,
                                        neteaseAlbumSourceRoute(current.album)
                                    )
                                },
                                album = current.album,
                                offlineMode = offlineMode
                            )
                        }

                        is LibrarySelectedItem.YouTubeMusic -> {
                            YouTubeMusicPlaylistDetailScreen(
                                playlist = current.playlist,
                                onBack = { closeSelectedDetail() },
                                onSongClick = onSongClick,
                                offlineMode = offlineMode
                            )
                        }
                        is LibrarySelectedItem.YouTubeCreator -> {
                            libraryStateHolder.SaveableStateProvider(
                                key = youtubeMusicCreatorDetailStateKey(current.summary())
                            ) {
                                YouTubeMusicCreatorNavigationScreen(
                                    creator = current.summary(),
                                    onBack = { closeSelectedDetail() },
                                    onSongClick = onSongClick,
                                    onPlaylistClick = { playlist ->
                                        creatorParents = creatorParents + current
                                        openLibrarySelectedItem(LibrarySelectedItem.YouTubeMusic(playlist))
                                    },
                                    onCreatorClick = { creator ->
                                        if (creator.browseId != current.browseId) {
                                            creatorParents = creatorParents + current
                                            openLibrarySelectedItem(
                                                LibrarySelectedItem.YouTubeCreator(
                                                    creator.browseId, creator.title, creator.subtitle, creator.coverUrl
                                                )
                                            )
                                        }
                                    },
                                    offlineMode = offlineMode
                                )
                            }
                        }
                        is LibrarySelectedItem.BiliUploader -> {
                            libraryStateHolder.SaveableStateProvider(
                                "bili_uploader_${current.uploader.mid}"
                            ) {
                                BiliUploaderDetailScreen(
                                    uploader = current.uploader,
                                    onBack = { closeSelectedDetail() },
                                    onPlayAudio = { videos, index ->
                                        onPlayBiliAudioWithSourceRoute(
                                            videos, index, biliUploaderSourceRoute(current.uploader)
                                        )
                                    },
                                    onPlayParts = { videoInfo, index, coverUrl ->
                                        onPlayBiliPartsWithSourceRoute(
                                            videoInfo, index, coverUrl, biliUploaderSourceRoute(current.uploader)
                                        )
                                    },
                                    onContentClick = { playlist ->
                                        creatorParents = creatorParents + current
                                        openLibrarySelectedItem(LibrarySelectedItem.Bili(playlist))
                                    },
                                    offlineMode = offlineMode
                                )
                            }
                        }

                        is LibrarySelectedItem.Bili -> {
                            BiliPlaylistDetailScreen(
                                playlist = current.playlist,
                                onBack = { closeSelectedDetail() },
                                onPlayAudio = { videos, index ->
                                    onPlayBiliAudioWithSourceRoute(
                                        videos,
                                        index,
                                        biliPlaylistSourceRoute(current.playlist)
                                    )
                                },
                                    onPlayParts = { videoInfo, index, coverUrl ->
                                        onPlayBiliPartsWithSourceRoute(
                                        videoInfo,
                                        index,
                                        coverUrl,
                                            biliPlaylistSourceRoute(current.playlist)
                                        )
                                    },
                                    offlineMode = offlineMode
                                )
                        }
                        }
                    }
                }
                }
            }
        }
    }
}

internal val librarySelectedItemSaver = mapSaver<LibrarySelectedItem?>(
    save = { item -> item?.saveState().orEmpty() },
    restore = { saved -> restoreSavedByType(saved, librarySelectedItemRestorers) }
)

private val librarySelectedItemRestorers: Map<String, (Map<String, Any?>) -> LibrarySelectedItem?> = mapOf(
    "local" to { saved -> saved.savedNumber("playlistId")?.toLong()?.let(LibrarySelectedItem::Local) },
    "localArtist" to { saved -> saved.savedNonBlankString("artistName")?.let(LibrarySelectedItem::LocalArtist) },
    "serverAlbum" to ::restoreServerAlbumItem,
    "serverArtist" to ::restoreServerArtistItem,
    "hot" to { saved -> LibrarySelectedItem.Hot(saved["monthly"] == true) },
    "neteaseAlbum" to { saved -> restoreAlbumSummary(saved.savedMap("album"))?.let(LibrarySelectedItem::NeteaseAlbum) },
    "netease" to { saved -> restorePlaylistSummary(saved.savedMap("playlist"))?.let(LibrarySelectedItem::Netease) },
    "neteaseArtist" to { saved -> saved.savedNeteaseArtist()?.let(LibrarySelectedItem::NeteaseArtist) },
    "neteaseArtistAlbum" to ::restoreNeteaseArtistAlbumItem,
    "recent" to { _ -> LibrarySelectedItem.Recent() },
    "stats" to { _ -> LibrarySelectedItem.Stats() },
    "bili" to { saved -> restoreBiliPlaylist(saved.savedMap("playlist"))?.let(LibrarySelectedItem::Bili) },
    "ytmusic" to { saved -> restoreYouTubeMusicPlaylist(saved.savedMap("playlist"))?.let(LibrarySelectedItem::YouTubeMusic) },
    "biliArtist" to ::restoreBiliUploaderItem,
    "youtubeArtist" to ::restoreYouTubeCreatorItem
)

private fun restoreServerAlbumItem(saved: Map<String, Any?>): LibrarySelectedItem? {
    val profileId = saved.savedNonBlankString("profileId") ?: return null
    val albumId = saved.savedNonBlankString("albumId") ?: return null
    return LibrarySelectedItem.ServerAlbum(
        profileId, albumId, saved.savedString("name").orEmpty(), saved.savedString("artist").orEmpty(),
        saved.savedString("coverUrl"), saved.savedNumber("songCount")?.toInt()?.coerceAtLeast(0) ?: 0
    )
}

private fun restoreServerArtistItem(saved: Map<String, Any?>): LibrarySelectedItem? {
    val profileId = saved.savedNonBlankString("profileId") ?: return null
    val artistId = saved.savedNonBlankString("artistId") ?: return null
    return LibrarySelectedItem.ServerArtist(
        profileId, artistId, saved.savedString("name").orEmpty(), saved.savedString("coverUrl"),
        saved.savedNumber("albumCount")?.toInt()?.coerceAtLeast(0) ?: 0, saved.savedString("fallbackCoverUrl")
    )
}

private fun restoreNeteaseArtistAlbumItem(saved: Map<String, Any?>): LibrarySelectedItem? {
    val artist = saved.savedNeteaseArtist() ?: return null
    val album = restoreAlbumSummary(saved.savedMap("album")) ?: return null
    return LibrarySelectedItem.NeteaseArtistAlbum(artist, album)
}

private fun restoreBiliUploaderItem(saved: Map<String, Any?>): LibrarySelectedItem? {
    val mid = saved.savedNumber("mid")?.toLong()?.takeIf { it > 0 } ?: return null
    val name = saved.savedNonBlankString("name") ?: return null
    return LibrarySelectedItem.BiliUploader(BiliUploaderSummary(mid, name, saved.savedString("avatar").orEmpty()))
}

private fun restoreYouTubeCreatorItem(saved: Map<String, Any?>): LibrarySelectedItem? {
    val browseId = saved.savedNonBlankString("browseId") ?: return null
    val title = saved.savedNonBlankString("title") ?: return null
    return LibrarySelectedItem.YouTubeCreator(
        browseId = browseId,
        title = title,
        subtitle = saved.savedString("subtitle").orEmpty(),
        coverUrl = saved.savedString("coverUrl").orEmpty()
    )
}

private fun Map<String, Any?>.savedNeteaseArtist(): NeteaseArtistSummary? {
    val id = savedNumber("artistId")?.toLong()?.takeIf { it > 0L } ?: return null
    val name = savedNonBlankString("artistName") ?: return null
    return NeteaseArtistSummary(id = id, name = name)
}
