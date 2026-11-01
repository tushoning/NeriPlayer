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
 * File: moe.ouom.neriplayer.ui.component.playlist/PlaylistChromeTopBar
 * Created: 2026/10/31
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistTopBarCoverBackdrop
import moe.ouom.neriplayer.ui.screen.playlist.interpolatePlaylistColor
import moe.ouom.neriplayer.ui.screen.playlist.playlistModernCollapsedTopBarContentColor
import moe.ouom.neriplayer.ui.screen.playlist.rememberPlaylistModernHeroBackgroundColor
import moe.ouom.neriplayer.ui.screen.playlist.resolvePlaylistChromeCollapseProgress
import moe.ouom.neriplayer.ui.screen.playlist.resolvePlaylistEasedProgress
import moe.ouom.neriplayer.ui.screen.playlist.resolvePlaylistSolidTopBarContentColor
import moe.ouom.neriplayer.ui.screen.playlist.tvPlaylistTopBarShape
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr

/**
 * 二级页顶栏（返回按钮那一行）—— 全 app 唯一一份。
 *
 * 行为完全取自网易云歌单页，别的页面不要再各写一遍 TopAppBar：
 * - 背景就是头图最上面那一段（和下面头图同一块裁剪，接得上）
 * - 页面往上滚 → 背景跟着淡出（淡出进度 = 头图被滚走的比例）
 * - 标题/返回/右侧按钮颜色随之从「头图上的可读色」渐变到「折叠后的标题色」
 * - 电视上上边缘带圆角（`tvPlaylistTopBarShape`），并垫一层头图取色的底色抗锯齿
 *
 * @param heroHeight 下面头图的高度，必须和传给头图组件的一致
 * @param listState 内容列表，用来算「头图滚走多少」
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistChromeTopBar(
    title: String,
    coverUrl: String?,
    offlineMode: Boolean,
    heroHeight: Dp,
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val collapseProgress by remember(listState, density, heroHeight) {
        derivedStateOf {
            resolvePlaylistChromeCollapseProgress(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffsetPx = listState.firstVisibleItemScrollOffset,
                expandedHeroHeightPx = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }?.size ?: with(density) {
                    heroHeight.roundToPx()
                }
            )
        }
    }
    val visualProgress = resolvePlaylistEasedProgress(collapseProgress)
    val chromeColor = rememberPlaylistModernHeroBackgroundColor(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    )
    val contentColor = interpolatePlaylistColor(
        start = resolvePlaylistSolidTopBarContentColor(chromeColor),
        end = playlistModernCollapsedTopBarContentColor(),
        fraction = visualProgress
    )
    val resolvedBackDescription = backContentDescription
        ?: stringResource(CoreCommonR.string.action_back)

    Box(
        modifier = modifier
            .padding(horizontal = pageSidePaddingOr(0.dp))
            .tvPlaylistTopBarShape(useHeroColor = false)
    ) {
        PlaylistTopBarCoverBackdrop(
            coverUrl = coverUrl,
            offlineMode = offlineMode,
            heroHeight = heroHeight,
            alpha = 1f - collapseProgress
        )
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                HapticIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = resolvedBackDescription
                    )
                }
            },
            actions = actions,
            windowInsets = WindowInsets.statusBars,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = contentColor,
                navigationIconContentColor = contentColor,
                actionIconContentColor = contentColor
            )
        )
    }
}
