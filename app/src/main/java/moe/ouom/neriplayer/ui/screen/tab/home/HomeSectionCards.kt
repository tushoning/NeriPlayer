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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomeSectionCards
 * Created: 2026/11/15
 */

import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.cleanCornerRadiusFor
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.playlist.favorite.FavoritePlaylistRepository
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.ui.tv.rememberTvDeferredImage
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvReturnFocus
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus

@Composable
internal fun SectionLoadingState(text: String) {
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
internal fun SectionErrorState(detail: String) {
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

/**
 * 「为你推荐 / 我的推荐」这类歌单区块：整页翻页的卡片横条。
 */
@Composable
internal fun PlaylistSectionStrip(
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
        targetCardWidth = cardWidth,
        // 记住这条横条停在第几页：从歌单详情返回时页码还在，焦点才能落回原卡
        stripKey = sectionKey
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
internal fun RadarPlaylistStrip(
    playlists: List<PlaylistSummary>,
    favoriteKeys: Set<String>,
    listState: LazyListState,
    onClick: (PlaylistSummary) -> Unit,
    onShowSnackbar: (String) -> Unit,
    offlineMode: Boolean,
    /** 这条横条的稳定标识，用来记住停在第几页（见 PagedCardStrip） */
    stripKey: String = "home:netease:radar-playlist"
) {
    // 电视上改成整页翻页（listState 现在不再用于滚动，保留参数以免改动宿主）
    val cardWidth = if (currentWindowWidthDp() >= 600.dp) 172.dp else 148.dp
    PagedCardStrip(
        itemCount = playlists.size,
        itemKey = { index -> homeNeteaseRadarPlaylistScrollKey(playlists[index].id) },
        targetCardWidth = cardWidth,
        stripKey = stripKey
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
internal fun RadarPlaylistCard(
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

