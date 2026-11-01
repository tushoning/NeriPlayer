package moe.ouom.neriplayer.ui.component.playlist

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
 * File: moe.ouom.neriplayer.ui.component.playlist/NeriSongRow
 * Created: 2026/10/31
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.feedback.showNeriSnackbar
import moe.ouom.neriplayer.ui.screen.playlist.PlayingIndicator
import moe.ouom.neriplayer.ui.screen.playlist.hapticRowAction
import moe.ouom.neriplayer.ui.screen.playlist.playlistModernListPrimaryContentColor
import moe.ouom.neriplayer.ui.screen.playlist.playlistModernListSecondaryContentColor
import moe.ouom.neriplayer.ui.screen.playlist.playlistModernListTertiaryContentColor
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.rememberTvMenuFirstItemFocus
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import moe.ouom.neriplayer.ui.util.copyMessageRes
import moe.ouom.neriplayer.ui.util.copyPlainTextSafely
import moe.ouom.neriplayer.util.format.formatDuration
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

/**
 * 全 app 唯一一份歌曲行（电视版）。
 *
 * 规格基准 = 网易云歌单页的 `SongRow`（就是带新头图的那一版）：
 * - 行内边距 16dp / 12dp
 * - 序号块 48dp、封面 48dp 圆角 10dp、序号与封面之间不留额外空隙
 * - 标题 titleMedium 单行 + 副标题 bodySmall 单行
 * - 右侧顺序固定：时长（或播放动效）→ 三点菜单
 * - 焦点环 12dp 圆角 + 左右各内收 25dp
 *
 * 各页面只传数据与回调；页面特有的东西走插槽，不要各自再抄一份行布局：
 * - [subtitleContent] 自定义副标题（本地/歌手页要下载标记、分段富文本）
 * - [coverContent] 自定义封面（本地歌单要带下载态的特殊封面请求）
 * - [trailingContent] 自定义尾部（本地歌单的拖拽手柄）
 * - [extraMenuTop] / [extraMenuBottom] 页面专属菜单项（打开详情、分享、下载、跳过管理…）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NeriSongRow(
    index: Int,
    song: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = song.displayName(),
    subtitle: String = "",
    subtitleContent: (@Composable () -> Unit)? = null,
    titleMaxLines: Int = 1,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showCover: Boolean = true,
    coverWidth: Dp = 48.dp,
    coverHeight: Dp = 48.dp,
    coverCornerRadius: Dp = 10.dp,
    coverUrl: String? = null,
    coverContent: (@Composable () -> Unit)? = null,
    offlineMode: Boolean = false,
    isCurrentSong: Boolean = false,
    animatePlayingIndicator: Boolean = true,
    durationText: String? = formatDuration(song.durationMs),
    selectionMode: Boolean = false,
    selected: Boolean = false,
    leadingWidth: Dp = 48.dp,
    indexTextStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold
    ),
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 12.dp,
    onToggleSelect: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    rowContainerColor: Color = Color.Transparent,
    trailingContent: (@Composable () -> Unit)? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: ((SongItem, Boolean) -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueueEnd: (() -> Unit)? = null,
    copyInfoText: String? = "${song.displayName()}-${song.displayArtist()}",
    extraMenuTop: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    extraMenuBottom: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val isTelevision = LocalIsTelevision.current
    val tvRowMenuState = rememberTvRowMenuKeyState()
    val hasMenu = onPlayNext != null ||
        onAddToQueueEnd != null ||
        onFavoriteToggle != null ||
        copyInfoText != null ||
        extraMenuTop != null ||
        extraMenuBottom != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvBringIntoViewOnFocus()
            .tvRowMenuKeyPress(
                state = tvRowMenuState,
                enabled = isTelevision && !selectionMode && hasMenu
            )
            .clip(neriRoundedShape(12.dp))
            .background(rowContainerColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberTvFocusRingIndication(12.dp, insetHorizontal = 25.dp)
                    ?: LocalIndication.current,
                onClick = hapticRowAction(
                    context,
                    if (selectionMode) onToggleSelect else onClick
                ),
                onLongClick = onLongClick?.let { hapticRowAction(context, it) },
                enabled = enabled
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号 / 多选框
        Box(
            modifier = Modifier.width(leadingWidth),
            contentAlignment = Alignment.Center
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelect() }
                )
            } else {
                Text(
                    text = index.toString(),
                    style = indexTextStyle,
                    color = playlistModernListTertiaryContentColor(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 封面（可用插槽完全接管）
        if (coverContent != null) {
            coverContent()
            Spacer(Modifier.width(12.dp))
        } else {
            val effectiveCover = coverUrl ?: song.coverUrl
            if (showCover && !effectiveCover.isNullOrBlank()) {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = effectiveCover,
                        offlineMode = offlineMode
                    ),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(coverWidth)
                        .height(coverHeight)
                        .clip(neriRoundedShape(coverCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
            }
        }

        // 标题 / 副标题
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle,
                color = playlistModernListPrimaryContentColor()
            )
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = playlistModernListSecondaryContentColor()
                )
            }
        }

        // 右侧：时长 / 播放动效
        if (trailingContent != null) {
            trailingContent()
        } else {
            if (isCurrentSong) {
                PlayingIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    animate = animatePlayingIndicator
                )
            } else if (durationText != null) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = playlistModernListSecondaryContentColor(),
                    maxLines = 1
                )
            }
        }

        // 三点菜单（多选时不显示）
        if (!selectionMode && hasMenu) {
            NeriSongRowMenu(
                song = song,
                tvMenuKeyState = tvRowMenuState,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                onPlayNext = onPlayNext,
                onAddToQueueEnd = onAddToQueueEnd,
                copyInfoText = copyInfoText,
                extraMenuTop = extraMenuTop,
                extraMenuBottom = extraMenuBottom,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun NeriSongRowMenu(
    song: SongItem,
    tvMenuKeyState: TvRowMenuKeyState,
    isFavorite: Boolean,
    onFavoriteToggle: ((SongItem, Boolean) -> Unit)?,
    onPlayNext: (() -> Unit)?,
    onAddToQueueEnd: (() -> Unit)?,
    copyInfoText: String?,
    extraMenuTop: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
    extraMenuBottom: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
    snackbarHostState: SnackbarHostState?
) {
    val clipboard = LocalClipboard.current
    val composeResources = LocalResources.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    TvRowMenuKeyEffect(state = tvMenuKeyState) { expanded = true }
    val firstItemFocus = rememberTvMenuFirstItemFocus(expanded)
    val close = { expanded = false }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(CoreCommonR.string.cd_more_actions),
                tint = playlistModernListSecondaryContentColor()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = close
        ) {
            extraMenuTop?.invoke(this, close)

            if (onPlayNext != null) {
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
                        close()
                        onPlayNext()
                    }
                )
            }
            if (onAddToQueueEnd != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(CoreCommonR.string.playlist_add_to_end)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        close()
                        onAddToQueueEnd()
                    }
                )
            }
            if (onFavoriteToggle != null) {
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
                        close()
                        onFavoriteToggle(song, isFavorite)
                    }
                )
            }

            extraMenuBottom?.invoke(this, close)

            if (copyInfoText != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(CoreCommonR.string.action_copy_song_info)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        close()
                        scope.launch {
                            val result = clipboard.copyPlainTextSafely("text", copyInfoText)
                            snackbarHostState?.showNeriSnackbar(
                                composeResources.getString(result.copyMessageRes())
                            )
                        }
                    }
                )
            }
        }
    }
}
