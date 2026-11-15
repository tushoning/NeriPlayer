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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomeYouTubeMusicSections
 * Created: 2026/11/15
 */

import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.graphics.vector.ImageVector
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.platform.youtube.api.transport.buildYouTubeMusicMediaUri
import moe.ouom.neriplayer.platform.youtube.api.transport.stableYouTubeMusicId
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicHomeShelf
import moe.ouom.neriplayer.data.model.youtube.music.YouTubeMusicHomeItem
import moe.ouom.neriplayer.platform.youtube.api.parser.YouTubeMusicParser
import java.util.Locale

internal data class ClassifiedYouTubeMusicShelves(
    val guessYouLike: YouTubeMusicHomeShelf?,
    val dailyDiscover: YouTubeMusicHomeShelf?,
    val remaining: List<YouTubeMusicHomeShelf>
)

internal val YouTubeMusicGuessYouLikeKeywords = listOf(
    "猜你喜欢",
    "guess you like",
    "recommended for you"
)

internal val YouTubeMusicDailyDiscoverKeywords = listOf(
    "每日发现",
    "daily discover",
    "discover daily"
)

internal val YouTubeMusicSongShelfKeywords = listOf(
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

internal fun classifyYouTubeMusicShelves(
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

internal fun YouTubeMusicHomeShelf.shouldRenderAsSongShelf(): Boolean {
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

internal fun YouTubeMusicHomeShelf.hasRenderablePlaylistItems(): Boolean {
    return items.any { it.isPlaylistItem() }
}

internal fun YouTubeMusicHomeItem.isPlaylistItem(): Boolean {
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

internal fun String.matchesYouTubeMusicShelfKeywords(keywords: List<String>): Boolean {
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

internal fun LazyGridScope.addYouTubeMusicSongShelfSection(
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
