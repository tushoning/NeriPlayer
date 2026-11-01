package moe.ouom.neriplayer.ui.screen.tab.explore

import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import moe.ouom.neriplayer.ui.tv.tvStripItemFocus
import moe.ouom.neriplayer.ui.tv.tvStripClamp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.tvGridClampHorizontal
import moe.ouom.neriplayer.ui.tv.rememberTvGridRowNavigation
import moe.ouom.neriplayer.ui.tv.tvGridRowNav
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvGridColumnsForCardWidth
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.ui.screen.tab.home.PlaylistCard
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import moe.ouom.neriplayer.R as AppR

internal data class NeteaseBrowseState(
    val selectedTag: String,
    val playlists: List<PlaylistSummary>,
    val loading: Boolean,
    val error: String?
)

internal data class YouTubeBrowseState(
    val playlists: List<YouTubeMusicPlaylist>,
    val loading: Boolean,
    val error: String?
)

internal val ExploreBrowseContentMaxWidth = 1240.dp

internal fun Modifier.exploreCenteredMaxWidth(maxWidth: Dp): Modifier = this
    .fillMaxSize()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)
    .fillMaxSize()

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NeteaseDefaultContent(
    gridState: LazyGridState,
    browse: NeteaseBrowseState,
    tagKeys: List<String>,
    tagLabels: List<String>,
    favoriteKeys: Set<String>,
    onTagSelected: (String) -> Unit,
    onPlay: (PlaylistSummary) -> Unit,
    tagChipSelectedAlpha: Float,
    tagChipUnselectedAlpha: Float,
    tagChipBorderAlpha: Float,
    isTabletLayout: Boolean = false
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isTelevision = LocalIsTelevision.current
    val gridHorizontalPadding = pageSidePaddingOr(if (isTabletLayout) 56.dp else 16.dp)
    val gridMinCellSize = if (isTabletLayout) 170.dp else 150.dp
    val gridSpacing = if (isTabletLayout) 16.dp else 12.dp
    // 电视上按首页「雷达歌单」那一档卡片宽度（172dp）反算列数，
    // 否则 Adaptive 会把单元格撑到 200dp，比首页卡片大一圈
    val tvColumns = if (isTelevision) {
        tvGridColumnsForCardWidth(
            availableWidth = currentWindowWidthDp() - gridHorizontalPadding * 2,
            spacing = 12.dp
        )
    } else {
        0
    }
    val tagListState = rememberLazyListState()
    // 电视：上下键自己按行搬焦点，见 TvGridRowNavigation
    val gridRowNav = rememberTvGridRowNavigation(gridState)
    val showTagStartFade by remember(tagListState) {
        derivedStateOf { tagListState.canScrollBackward }
    }
    val showTagEndFade by remember(tagListState) {
        derivedStateOf { tagListState.canScrollForward }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = if (isTelevision) GridCells.Fixed(tvColumns) else GridCells.Adaptive(gridMinCellSize),
        verticalArrangement = Arrangement.spacedBy(gridSpacing),
        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
        contentPadding = PaddingValues(
            start = pageSidePaddingOr(gridHorizontalPadding),
            end = pageSidePaddingOr(gridHorizontalPadding),
            top = 16.dp,
            bottom = 16.dp + miniPlayerHeight
        ),
        modifier = Modifier.exploreCenteredMaxWidth(ExploreBrowseContentMaxWidth)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                val displayKeys = tagKeys
                val displayLabels = tagLabels
                LazyRow(
                    state = tagListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .exploreHorizontalEdgeFade(
                            showStartFade = showTagStartFade,
                            showEndFade = showTagEndFade
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayKeys) { index, tagKey ->
                        val selected = (browse.selectedTag == tagKey)
                        ExploreTagChip(
                            label = displayLabels[index],
                            selected = selected,
                            onClick = { if (!selected) onTagSelected(tagKey) },
                            selectedAlpha = tagChipSelectedAlpha,
                            unselectedAlpha = tagChipUnselectedAlpha,
                            borderAlpha = tagChipBorderAlpha,
                            modifier = Modifier
                                // 电视：焦点走到哪滚到哪，且走到尽头不再往右跑
                                // （不夹住的话会翻到分页器的下一页，画面整块左移）
                                .tvStripItemFocus(tagListState, index)
                                .tvStripClamp(tagListState, index, displayKeys.lastIndex)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (browse.loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (browse.playlists.isNotEmpty()) {
            itemsIndexed(
                items = browse.playlists,
                key = { _, playlist -> playlist.id }
            ) { _, playlist ->
                // 电视：卡片整体缩小 10%（格位不变、居中），焦点导航仍按格位算
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PlaylistCard(
                        playlist = playlist,
                        isFavorite = favoriteKeys.contains("netease:${playlist.id}"),
                        onClick = { onPlay(playlist) },
                        // 电视：最左/最右一列再按方向键就停住，别让焦点跑出网格；
                        // 上下键按「同一列」逐行移动（视口外那一行还没组合，Compose 自己搜会掉到第一个卡片）
                        modifier = Modifier.fillMaxWidth(if (isTelevision) 0.9f else 1f)
                            .tvGridRowNav(gridRowNav, playlist.id)
                            .tvGridClampHorizontal(gridState, playlist.id)
                    )
                }
            }
        } else if (browse.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (browse.error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(browse.error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun ExploreTagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedAlpha: Float,
    unselectedAlpha: Float,
    borderAlpha: Float,
    icon: ImageVector? = null,
    chipHeight: Dp = 32.dp,
    /** 电视（遥控器）关掉 48dp 最小触控尺寸，标签行才压得下和搜索框配平的高度 */
    enforceMinInteractiveSize: Boolean = true,
    modifier: Modifier = Modifier
) {
    ExploreGlassPillSurface(
        fallbackColor = tagContainerColor(selected, selectedAlpha, unselectedAlpha),
        tintColor = tagTintColor(selected),
        contentColor = tagContentColor(selected),
        border = BorderStroke(1.dp, tagBorderColor(selected, borderAlpha)),
        onClick = onClick,
        enforceMinInteractiveSize = enforceMinInteractiveSize,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .height(chipHeight)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExploreTagIcon(icon)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun tagContainerColor(selected: Boolean, selectedAlpha: Float, unselectedAlpha: Float): Color {
    val colors = MaterialTheme.colorScheme
    return if (selected) colors.secondaryContainer.copy(alpha = selectedAlpha)
    else colors.surface.copy(alpha = unselectedAlpha)
}

@Composable
private fun tagTintColor(selected: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return if (selected) colors.secondaryContainer else colors.surface
}

@Composable
private fun tagContentColor(selected: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return if (selected) colors.onSecondaryContainer else colors.onSurface
}

@Composable
private fun tagBorderColor(selected: Boolean, borderAlpha: Float): Color {
    val colors = MaterialTheme.colorScheme
    return if (selected) colors.secondary.copy(alpha = borderAlpha)
    else colors.outline.copy(alpha = borderAlpha)
}

@Composable
private fun ExploreTagIcon(icon: ImageVector?) {
    if (icon == null) return
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
}

@Composable
internal fun YouTubeMusicExploreContent(
    browse: YouTubeBrowseState,
    onRetry: () -> Unit,
    onClick: (YouTubeMusicPlaylist) -> Unit,
    offlineMode: Boolean,
    gridState: LazyGridState,
    isTabletLayout: Boolean = false
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val gridHorizontalPadding = if (isTabletLayout) 56.dp else 16.dp
    val gridMinCellSize = if (isTabletLayout) 156.dp else 120.dp
    val gridSpacing = if (isTabletLayout) 14.dp else 10.dp
    when {
        browse.loading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) { CircularProgressIndicator() }
        }
        browse.error != null -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        browse.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    HapticTextButton(onClick = onRetry) {
                        Text(stringResource(CoreCommonR.string.action_retry))
                    }
                }
            }
        }
        browse.playlists.isEmpty() -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerHeight),
                Alignment.Center
            ) {
                Text(
                    stringResource(AppR.string.explore_tag_youtube_music),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        else -> {
            val gridRowNav = rememberTvGridRowNavigation(gridState)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(gridMinCellSize),
                contentPadding = PaddingValues(
                    start = pageSidePaddingOr(gridHorizontalPadding), end = pageSidePaddingOr(gridHorizontalPadding),
                    top = 8.dp,
                    bottom = 16.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.exploreCenteredMaxWidth(ExploreBrowseContentMaxWidth)
            ) {
                items(
                    items = browse.playlists,
                    key = { it.browseId }
                ) { playlist ->
                    YtMusicExploreCard(
                        playlist = playlist,
                        onClick = { onClick(playlist) },
                        offlineMode = offlineMode,
                        modifier = Modifier
                            .tvGridRowNav(gridRowNav, playlist.browseId)
                            .tvGridClampHorizontal(gridState, playlist.browseId)
                    )
                }
            }
        }
    }
}

@Composable
private fun YtMusicExploreCard(
    playlist: YouTubeMusicPlaylist,
    onClick: () -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(neriRoundedShape(12.dp))
            .tvBringIntoViewOnFocus()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = fastScrollableImageRequest(
                context = context,
                data = playlist.coverUrl,
                sizePx = 384,
                offlineMode = offlineMode
            ),
            contentDescription = playlist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(neriRoundedShape(12.dp))
        )
        Column(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                text = playlist.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            YouTubeBrowseCardSubtitle(playlist.subtitle)
        }
    }
}

@Composable
private fun YouTubeBrowseCardSubtitle(subtitle: String) {
    if (subtitle.isBlank()) return
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}
