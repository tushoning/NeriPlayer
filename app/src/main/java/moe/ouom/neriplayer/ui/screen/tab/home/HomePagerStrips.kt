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
 * File: moe.ouom.neriplayer.ui.screen.tab.home/HomePagerStrips
 * Created: 2026/11/15
 */

import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import android.content.ClipData
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.local.media.displayAlbum
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.util.rememberSongDisplayCoverUrl
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import kotlin.math.ceil
import kotlin.math.min
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.ui.tv.rememberTvMenuFirstItemFocus
import moe.ouom.neriplayer.ui.tv.rememberTvDeferredImage
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.TvPagerPageMemory
import moe.ouom.neriplayer.ui.tv.TvCardStripHorizontalPadding
import moe.ouom.neriplayer.ui.tv.tvCardStripMetrics
import moe.ouom.neriplayer.ui.tv.tvReturnFocus
import moe.ouom.neriplayer.ui.tv.TvTuning
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import moe.ouom.neriplayer.ui.tv.tvRowStart

/** 分页横条里每张卡的 FocusRequester 登记表（按 item index） */
internal class TvPagerFocus {
    private val requesters = HashMap<Int, FocusRequester>()
    fun requester(index: Int): FocusRequester =
        requesters.getOrPut(index) { FocusRequester() }
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
internal fun PagedCardStrip(
    itemCount: Int,
    itemKey: (Int) -> Any,
    targetCardWidth: Dp,
    /**
     * 这条横条的稳定标识，用来记住「上次停在第几页」。
     *
     * 传空串 = 不记忆（每次从第 0 页开始）。
     */
    stripKey: String = "",
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
    val shownCount = if (stagedLimit < itemCount) stagedLimit else itemCount
    val spacing = 12.dp
    val horizontalPadding = TvCardStripHorizontalPadding
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
        // 「一排几张 / 每张多宽」和探索页网格共用同一套算式（见 tvCardStripMetrics），
        // 两边卡片必须一样大。
        val metrics = tvCardStripMetrics(
            availableWidth = maxWidth - horizontalPadding * 2,
            targetCardWidth = targetCardWidth,
            spacing = spacing
        )
        val perPage = metrics.perPage
        val slotWidth = metrics.slotWidth
        val cardWidth = metrics.cardWidth
        // 页数按**真实条目数**算，不能用分帧限流后的 shownCount：
        // 首帧 shownCount 只有 2，页数会退化成 1，initialPage 拿到的记忆页码会被
        // PagerState 直接夹到 0 —— 从第 2 页往后的卡片返回时就不存在，焦点只能落到别处。
        val pageCount = ceil(itemCount / perPage.toFloat()).toInt().coerceAtLeast(1)
        // 首帧就摆到上次停留的那一页：卡片在同一帧里就位，`TvReturnFocus` 才能
        // 在 layout 阶段把它抢回来，用户看不到「先亮在上一行、再跳回来」。
        // 之后翻页会让 currentPage 变化，但那只是状态更新，不会重读这里。
        val initialPage = remember(stripKey, pageCount) {
            (TvPagerPageMemory.page(stripKey) ?: 0).coerceIn(0, pageCount - 1)
        }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { pageCount }
        )
        val focus = remember { TvPagerFocus() }
        // 分帧限流 + 页码记录：
        //  1. 先让首帧那套「只有两张卡」的廉价组合跑完，再补满内容；
        //  2. 页码由 initialPage 负责恢复，这里只在「记忆页码和当前页不一致」时兜一下
        //     （比如条目数变化导致 initialPage 被夹住）；
        //  3. 之后才开始记录当前页，避免把恢复前的值写回记忆。
        LaunchedEffect(stripKey, itemCount) {
            withFrameNanos { }
            stagedLimit = Int.MAX_VALUE
            val saved = TvPagerPageMemory.page(stripKey)
            if (saved != null && saved != pagerState.currentPage && saved in 0 until pageCount) {
                runCatching { pagerState.scrollToPage(saved) }
            }
            snapshotFlow { pagerState.currentPage }
                .collect { TvPagerPageMemory.remember(stripKey, it) }
        }
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
                    // 限流期间**当前页必须照常渲染**：从二级页返回时分页器第一帧就在
                    // 记忆的那一页上，若这一页被限流成 Spacer，目标卡根本不存在，
                    // 归还焦点就无处可落（表现：焦点先亮在上一行第一张卡上）。
                    // 限流放开后不再读 currentPage，避免每页都订阅页码。
                    val keepCurrentPage = stagedLimit < itemCount && page == pagerState.currentPage
                    if (index < shownCount || (index < itemCount && keepCurrentPage)) {
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
internal fun tvPagerItemModifier(
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
        // 整条里的第一张 = 第 0 列：按左直接回侧栏本页标签。
        // 不声明的话，几何左搜索会把它右边那些「左边没邻居」的控件（下面区块的分类芯片）
        // 当成候选，光标就往下跳到另一个区块上了。
        .tvRowStart(index == 0)
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

@Composable
internal fun SongRowMini(
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

@Composable
internal fun ResponsiveSongPagerList(
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

