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
 * File: moe.ouom.neriplayer.ui.screen.tab.library/LibraryScreenRemotePlaylistContent
 * Created: 2025/8/8
 */

import moe.ouom.neriplayer.ui.component.common.NeriSearchFieldGlass
import moe.ouom.neriplayer.ui.component.common.rememberTvTextFieldIme
import moe.ouom.neriplayer.ui.theme.neriSearchFieldColors
import moe.ouom.neriplayer.ui.theme.neriSearchFieldShape
import moe.ouom.neriplayer.ui.component.common.CleanSegmentedTabs
import moe.ouom.neriplayer.ui.theme.NeriDesign
import androidx.compose.ui.graphics.Shape
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvClampRowEnds
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.tvFocusGate
import moe.ouom.neriplayer.ui.tv.TvChipHeight
import moe.ouom.neriplayer.ui.tv.TvChipHorizontalPadding
import moe.ouom.neriplayer.ui.tv.tvChipLabelStyle
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.playlist.favorite.FavoritePlaylistRepository
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylistKind
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.favoriteId
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import moe.ouom.neriplayer.R as AppR
@Composable
internal fun LibraryMainTabs(
    tabs: List<LibraryTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenRecent: () -> Unit = {},
    /** 电视上再点一次当前标签 = 刷新这一页（下拉刷新在电视上会误触，已经关掉） */
    onRefreshCurrentTab: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // 内容区按左/右键时不许跳到这一栏上来
            .tvFocusGate()
            .padding(horizontal = pageSidePaddingOr(16.dp))
    ) {
        // 整栏按内容宽度排布。
        // 这里**不能**用 PrimaryScrollableTabRow：它内部会强行 fillMaxWidth，
        // 把紧跟其后的文字项挤出屏幕（表现为"这一栏变得特别宽、后面的不见了"）。
        val row: @Composable () -> Unit = {
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                // 标签之间留 8dp：原来只靠各自的内边距，整排看着挤成一团
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 「最近播放」是一个独立页面，不是分页里的一页，所以放在标签前、
                // 和「播放统计」一样当纯文字入口用。
                // 电视：这一栏两端按左/右就停住，不许跳出这一行
                val lastLabelIndex = tabs.size + 1
                LibraryTabLabel(
                    label = stringResource(CoreCommonR.string.library_recent_played),
                    selected = false,
                    onClick = onOpenRecent,
                    modifier = Modifier.tvClampRowEnds(0, lastLabelIndex)
                )
                tabs.forEachIndexed { index, tab ->
                    LibraryTabLabel(
                        label = stringResource(tab.labelResId),
                        selected = selectedTabIndex == index,
                        onClick = {
                            if (selectedTabIndex == index) onRefreshCurrentTab()
                            else onTabSelected(index)
                        },
                        modifier = Modifier.tvClampRowEnds(index + 1, lastLabelIndex)
                    )
                }
                // 播放统计：原来在页面顶部单独占一条工具栏，现在并进这一栏做成纯文字，
                // 和主标签同一行，省一条屏幕高度，也不再多一个图标按钮。
                LibraryTabLabel(
                    label = stringResource(AppR.string.stats_title),
                    selected = false,
                    onClick = onOpenStats,
                    modifier = Modifier.tvClampRowEnds(lastLabelIndex, lastLabelIndex)
                )
            }
        }

        if (NeriDesign.isClean) {
            row()
        } else AdvancedGlassSurface(
            role = AdvancedGlassRole.ScreenTopTab,
            modifier = Modifier
                .wrapContentWidth()
                .clip(LibraryPrimaryTabShape),
            shape = LibraryPrimaryTabShape
        ) {
            row()
        }
    }
}

/** 这一栏里的单个文字标签，选中时高亮。宽度由文字决定，不拉伸。 */
@Composable
private fun LibraryTabLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isTelevision = LocalIsTelevision.current
    // 全局焦点环圆角 16dp，这排标签是 20dp 胶囊（高度 30dp 时等于半圆）—— 不跟着改四角就对不上。
    // 传 20dp 后绘制时会按短边一半再收一次，正好贴着标签边框内侧。
    val tvRingIndication = rememberTvFocusRingIndication(20.dp) ?: LocalIndication.current
    Box(
        modifier = modifier
            .then(if (isTelevision) Modifier.height(TvChipHeight) else Modifier)
            .clip(LibraryPrimaryTabShape)
            .background(
                if (selected) colors.primary.copy(alpha = 0.18f) else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication
            ) {
                context.performHapticFeedback()
                onClick()
            }
            // 电视上和探索页的来源标签用同一套尺寸（30dp 高、18sp），
            // 原来 22sp + 上下 10dp 内边距，比探索页那排高出一头
            .padding(
                horizontal = if (isTelevision) TvChipHorizontalPadding else 16.dp,
                vertical = if (isTelevision) 0.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (isTelevision) {
                tvChipLabelStyle()
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
internal fun YouTubeMusicPlaylistList(
    playlists: List<YouTubeMusicPlaylist>,
    error: String?,
    listState: LazyListState,
    onClick: (YouTubeMusicPlaylist) -> Unit,
    onRetry: () -> Unit,
    offlineMode: Boolean
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var menuPlaylist by remember { mutableStateOf<YouTubeMusicPlaylist?>(null) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        AppFeedback.show(
            context = context,
            message = composeResources.getString(CoreCommonR.string.toast_copied)
        )
    }

    val cardShape = neriRoundedShape(12.dp)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = pageSidePaddingOr(8.dp),
            end = pageSidePaddingOr(8.dp),
            top = 8.dp,
            bottom = 8.dp + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (playlists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = error ?: stringResource(CoreCommonR.string.library_youtube_music_empty),
                                color = if (error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                }
                            )
                        },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(CoreCommonR.string.library_youtube_music_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (error != null) {
                                    HapticTextButton(onClick = onRetry) {
                                        Text(text = stringResource(CoreCommonR.string.action_retry))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = CoreCommonR.drawable.ic_youtube),
                                contentDescription = stringResource(CoreCommonR.string.common_youtube),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = playlists,
            key = { it.browseId }
        ) { playlist ->
            val playlistFavoriteId = remember(playlist.playlistId, playlist.browseId) {
                playlist.favoriteId()
            }
            val isFavorite = remember(favorites, playlistFavoriteId) {
                favoriteRepo.isFavorite(playlistFavoriteId, "youtubeMusic")
            }
            // 电视：这一行的菜单原本只能长按打开，遥控器没有长按，改成菜单键
            val menuKeyState = rememberTvRowMenuKeyState()
            TvRowMenuKeyEffect(state = menuKeyState) { menuPlaylist = playlist }
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .tvBringIntoViewOnFocus()
                    .tvRowMenuKeyPress(menuKeyState)
                    .combinedClickable(
                        onClick = { onClick(playlist) },
                        onLongClick = { menuPlaylist = playlist }
                    )
            ) {
                ListItem(
                    headlineContent = { Text(playlist.title) },
                    supportingContent = {
                        val trackCountText = playlist.trackCount
                            .takeIf { it > 0 }
                            ?.let { count ->
                                pluralStringResource(
                                    AppR.plurals.library_song_count,
                                    count,
                                    count
                                )
                            }
                        val subtitleText = playlist.subtitle.ifBlank {
                            stringResource(CoreCommonR.string.library_youtube_music_hint)
                        }
                        Text(
                            text = listOfNotNull(subtitleText, trackCountText)
                                .distinct()
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        if (playlist.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = offlineCachedImageRequest(
                                    context = context,
                                    data = playlist.coverUrl,
                                    sizePx = 192,
                                    allowHardware = false,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(neriRoundedShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = menuPlaylist?.browseId == playlist.browseId,
                    onDismissRequest = { menuPlaylist = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(CoreCommonR.string.library_youtube_music_open_playlist)) },
                        onClick = {
                            menuPlaylist = null
                            onClick(playlist)
                        }
                    )
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
                        onClick = {
                            menuPlaylist = null
                            val toastMessage = if (isFavorite) {
                                composeResources.getString(CoreCommonR.string.home_unfavorited)
                            } else {
                                composeResources.getString(CoreCommonR.string.favorite_success)
                            }
                            scope.launch {
                                if (isFavorite) {
                                    favoriteRepo.removeFavorite(playlistFavoriteId, "youtubeMusic")
                                } else {
                                    favoriteRepo.addFavorite(
                                        id = playlistFavoriteId,
                                        name = playlist.title,
                                        coverUrl = playlist.coverUrl,
                                        trackCount = playlist.trackCount,
                                        source = "youtubeMusic",
                                        browseId = playlist.browseId,
                                        playlistId = playlist.playlistId,
                                        subtitle = playlist.subtitle,
                                        songs = emptyList()
                                    )
                                }
                                AppFeedback.show(
                                    context = context,
                                    message = toastMessage
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(CoreCommonR.string.library_youtube_music_copy_browse_id)) },
                        onClick = {
                            copyToClipboard("ytmusic_browse_id", playlist.browseId)
                            menuPlaylist = null
                        }
                    )
                    if (playlist.playlistId.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(CoreCommonR.string.library_youtube_music_copy_playlist_id)) },
                            onClick = {
                                copyToClipboard("ytmusic_playlist_id", playlist.playlistId)
                                menuPlaylist = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BiliPlaylistList(
    playlists: List<BiliPlaylist>,
    error: String?,
    listState: LazyListState,
    onClick: (BiliPlaylist) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val createdLabel = stringResource(CoreCommonR.string.library_bili_created_favorite)
    val collectedLabel = stringResource(CoreCommonR.string.library_bili_collected_favorite)
    val collectionLabel = stringResource(CoreCommonR.string.library_bili_collection)
    val seriesLabel = stringResource(CoreCommonR.string.library_bili_series)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredPlaylists = remember(
        playlists,
        searchQuery,
        createdLabel,
        collectedLabel,
        collectionLabel,
        seriesLabel
    ) {
        filterBiliPlaylists(
            playlists = playlists,
            query = searchQuery,
            createdLabel = createdLabel,
            collectedLabel = collectedLabel,
            collectionLabel = collectionLabel,
            seriesLabel = seriesLabel
        )
    }

    val cardShape = neriRoundedShape(12.dp)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = pageSidePaddingOr(8.dp), end = pageSidePaddingOr(8.dp), top = 8.dp, bottom = 8.dp + miniPlayerHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "bili_playlist_search") {
            val searchIme = rememberTvTextFieldIme()
            NeriSearchFieldGlass(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = neriSearchFieldShape(LibrarySearchFieldShape)
            ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                readOnly = searchIme.readOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(searchIme.modifier),
                placeholder = { Text(stringResource(CoreCommonR.string.library_bili_search_hint)) },
                singleLine = true,
                shape = neriSearchFieldShape(LibrarySearchFieldShape),

                colors = neriSearchFieldColors()
            )
            }
        }
        if (playlists.isNotEmpty() && filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(CoreCommonR.string.library_bili_search_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(CoreCommonR.string.library_bili_search_empty_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        } else if (filteredPlaylists.isEmpty()) {
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = error ?: stringResource(CoreCommonR.string.library_bili_empty),
                                color = if (error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                }
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(CoreCommonR.string.library_bili_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    )
                }
            }
        }
        items(
            items = filteredPlaylists,
            key = { "${it.kind}:${it.mediaId}" }
        ) { pl ->
            val kindLabel = when (pl.kind) {
                BiliPlaylistKind.CREATED_FAVORITE -> stringResource(CoreCommonR.string.library_bili_created_favorite)
                BiliPlaylistKind.COLLECTED_FAVORITE -> stringResource(CoreCommonR.string.library_bili_collected_favorite)
                BiliPlaylistKind.COLLECTION -> stringResource(CoreCommonR.string.library_bili_collection)
                BiliPlaylistKind.SERIES -> stringResource(CoreCommonR.string.library_bili_series)
            }
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .tvBringIntoViewOnFocus()
                    .clickable { onClick(pl) }
            ) {
                ListItem(
                    headlineContent = { Text(pl.title) },
                    supportingContent = {
                        val countText = pluralStringResource(AppR.plurals.library_video_count, pl.count, pl.count)
                        Text(
                            listOf(kindLabel, pl.subtitle, countText)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    leadingContent = {
                        if (pl.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = offlineCachedImageRequest(
                                    context = context,
                                    data = pl.coverUrl,
                                    sizePx = 192,
                                    allowHardware = false,
                                    offlineMode = offlineMode
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(neriRoundedShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
