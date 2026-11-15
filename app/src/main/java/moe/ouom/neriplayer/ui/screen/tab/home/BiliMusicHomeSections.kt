package moe.ouom.neriplayer.ui.screen.tab.home

import moe.ouom.neriplayer.ui.i18n.asString
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.rememberTvDeferredImage
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvExitLeftTo
import moe.ouom.neriplayer.ui.tv.tvHoldLeft
import moe.ouom.neriplayer.ui.tv.tvFocusRing
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.tvStripClamp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicHotVideo
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicPlaybackSource
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicRankSong
import moe.ouom.neriplayer.data.model.bilibili.music.BiliMusicRankType
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableBiliVideoItem
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableHotVideoItems
import moe.ouom.neriplayer.data.model.bilibili.music.toPlayableRankVideoItems
import moe.ouom.neriplayer.platform.bilibili.api.image.buildBiliThumbnailUrl
import moe.ouom.neriplayer.ui.component.common.NeriChoiceChip
import moe.ouom.neriplayer.ui.viewmodel.playlist.BiliVideoItem
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeBiliMusicRankState
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeSectionState
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.cleanCornerRadiusFor
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.util.format.formatDurationSec
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest

/**
 * 首页「Bilibili 音乐榜」区块。
 *
 * 该区块只负责展示与回调，请求、缓存与竞态控制都在 [moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel] 中，
 * 避免在 Compose 重组中重复请求。
 */
internal fun LazyGridScope.addBiliMusicRankSection(
    registerKey: (String) -> String,
    state: HomeBiliMusicRankState,
    onTypeSelected: (BiliMusicRankType) -> Unit,
    onMoreClick: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    onRetry: () -> Unit,
    offlineMode: Boolean
) {
    val hasContent = state.songs.isNotEmpty()
    if (!hasContent && !state.loading && state.error == null) return

    // 标题行和卡片横条放在**同一个 item** 里。
    //
    // 分成两个 item 时，焦点站在 chip 上按「下」，下面那个 item 如果还没进组合
    // （懒加载网格只组合视口附近的 item），方向搜索就找不到任何候选 → 焦点原地不动 →
    // 页面也不再滚（滚动是由焦点 bring-into-view 触发的）→ 长按一直卡在 chip 上；
    // 反过来从卡片往上也一样。合成一个 item 之后 chip 和卡片永远同时存在。
    item(
        key = registerKey("home:biliMusic:rank:section"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        // 「查看全部」按左的落点：本区块自己的「原创榜」芯片
        val originalChipRequester = remember { FocusRequester() }
        BiliMusicSectionLayout(
            header = {
                BiliMusicSectionHeader(
                    icon = Icons.Outlined.MusicNote,
                    title = stringResource(CoreCommonR.string.home_bili_music_rank),
                    onMoreClick = onMoreClick,
                    typeSelector = {
                        BiliMusicRankTypeSelector(
                            selected = state.type,
                            onTypeSelected = onTypeSelected,
                            originalChipRequester = originalChipRequester
                        )
                    },
                    moreLeftTarget = originalChipRequester
                )
            },
            body = {
                when {
                    hasContent -> BiliMusicRankStrip(
                        state = state,
                        onPlay = onPlay,
                        offlineMode = offlineMode
                    )

                    state.loading -> BiliMusicSectionLoading(
                        text = stringResource(CoreCommonR.string.home_loading)
                    )

                    else -> BiliMusicSectionError(
                        detail = state.error?.asString().orEmpty(),
                        onRetry = onRetry
                    )
                }
            }
        )
    }
}

/**
 * 首页「Bilibili 音乐热门」区块（音乐分区视频榜）。
 *
 * 与榜单区块同构：同样只做展示与回调，数据由 [moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel] 提供，
 * 因此两个区块的卡片尺寸、缩略图裁剪方案与排版层级完全一致。
 */
internal fun LazyGridScope.addBiliMusicHotSection(
    registerKey: (String) -> String,
    state: HomeSectionState<BiliMusicHotVideo>,
    onMoreClick: () -> Unit,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    onRetry: () -> Unit,
    offlineMode: Boolean
) {
    val hasContent = state.items.isNotEmpty()
    if (!hasContent && !state.loading && state.error == null) return

    // 同榜单区：标题行和横条合成一个 item，避免跨 item 时焦点搜不到候选而卡住。
    item(
        key = registerKey("home:biliMusic:hot:section"),
        span = { GridItemSpan(maxLineSpan) }
    ) {
        BiliMusicSectionLayout(
            header = {
                BiliMusicSectionHeader(
                    icon = Icons.Outlined.LibraryMusic,
                    title = stringResource(CoreCommonR.string.home_bili_music_hot),
                    onMoreClick = onMoreClick
                )
            },
            body = {
                when {
                    hasContent -> BiliMusicHotStrip(
                        videos = state.items,
                        onPlay = onPlay,
                        offlineMode = offlineMode
                    )

                    state.loading -> BiliMusicSectionLoading(
                        text = stringResource(CoreCommonR.string.home_loading)
                    )

                    else -> BiliMusicSectionError(
                        detail = state.error?.asString().orEmpty(),
                        onRetry = onRetry
                    )
                }
            }
        )
    }
}

/**
 * 区块布局：**组合顺序是「卡片行 → 标题行」，视觉顺序仍是「标题行在上」**。
 *
 * 为什么不能用 Column：侧栏按右进内容区走的是 `FocusRequester.requestFocus()`，
 * 它按**组合顺序**深度优先找第一个可聚焦节点。标题行里的「查看全部」排在卡片行前面时，
 * 只要这个区块成了网格里第一个已组合的 item（页面上方没有「继续播放」、且已滚到它），
 * 「查看全部」就会是落点 —— 光标从标签栏按右直接落到那个角落按钮上。
 *
 * 把卡片行放前面组合，落点就变成第一张卡；标题行照旧摆在上方。
 *
 * ⚠️ **别把它「简化」回 Column**：一旦退回，上面那个落点问题立刻复发，而且只在
 * 「页面上方没有「继续播放」、并且已经滚到本区块」时才出现 —— 平时测不出来。
 * 同理，`content` 里每个槽位都包了 `Box`：横条内部是 `LazyRow + Spacer` 两个节点，
 * 平铺会让 `measurables` 多一个，摆放下标整体错位（标题行会整块消失）。
 */
@Composable
private fun BiliMusicSectionLayout(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    Layout(
        content = {
            // 两个槽位各包一层 Box：横条内部是 LazyRow + Spacer 两个节点，
            // 直接平铺会让 measurables 多出一个，摆放时下标全错（标题行会整块消失）。
            Box(Modifier.fillMaxWidth()) { body() }
            Box(Modifier.fillMaxWidth()) { header() }
        }
    ) { measurables, constraints ->
        // measurables 的顺序 = 组合顺序 = [body, header]
        val bodyPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
        val headerPlaceable = measurables[1].measure(constraints.copy(minHeight = 0))
        layout(constraints.maxWidth, headerPlaceable.height + bodyPlaceable.height) {
            headerPlaceable.place(0, 0)
            bodyPlaceable.place(0, headerPlaceable.height)
        }
    }
}

/**
 * 两个 B 站音乐区块共用的标题行：图标 + 标题 + 「查看全部」，榜单区块额外挂一个榜单类型选择器。
 */
@Composable
private fun BiliMusicSectionHeader(
    icon: ImageVector,
    title: String,
    onMoreClick: () -> Unit,
    typeSelector: (@Composable () -> Unit)? = null,
    /**
     * 电视：「查看全部」按左的落点。
     *
     * 传了就固定去那里（音乐榜 → 自己的「原创榜」芯片）；
     * 不传 = 按左原地不动（音乐热门那个左边没有任何属于它的邻居）。
     *
     * 两种都不能交给几何搜索：左键在内容区根节点就被接管了，
     * 那里的 `moveFocus` 会按矩形位置选点，落点会跑到上一区块的卡片上。
     * 也不能用 `tvClampLeft` —— item 级监听收不到左键。
     */
    moreLeftTarget: FocusRequester? = null
) {
    val moreButtonModifier = if (moreLeftTarget != null) {
        Modifier.tvExitLeftTo(moreLeftTarget)
    } else {
        Modifier.tvHoldLeft()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        if (NeriDesign.isClean) {
            // clean style carries hierarchy by weight alone; the accent stays reserved for actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp, bottom = 4.dp)
                )
                // 用 HapticTextButton：电视上它自带焦点环（原生 TextButton 没有，
                // 光标落上来就"像是丢了"）。按左去哪由 moreButtonModifier 决定。
                HapticTextButton(
                    onClick = onMoreClick,
                    modifier = moreButtonModifier
                ) {
                    Text(text = stringResource(CoreCommonR.string.home_bili_music_view_all))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                // 用 HapticTextButton：电视上它自带焦点环（原生 TextButton 没有，
                // 光标落上来就"像是丢了"）。按左去哪由 moreButtonModifier 决定。
                HapticTextButton(
                    onClick = onMoreClick,
                    modifier = moreButtonModifier
                ) {
                    Text(text = stringResource(CoreCommonR.string.home_bili_music_view_all))
                }
            }
        }
        typeSelector?.invoke()
    }
}

/**
 * 榜单类型切换（热门榜 / 原创榜）：切换后由 ViewModel 清空旧榜数据并按新类型重新加载，避免串期。
 */
@Composable
private fun BiliMusicRankTypeSelector(
    selected: BiliMusicRankType,
    onTypeSelected: (BiliMusicRankType) -> Unit,
    /** 电视：「原创榜」这一颗的焦点请求器，供标题行的「查看全部」按左时落到它上面 */
    originalChipRequester: FocusRequester? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            // 和下面的卡片横条对齐：横条 contentPadding 是 8dp，第一张卡片的可聚焦节点
            // 左边缘就落在 8dp。以前简洁风格是 0dp、Material 是 30dp（对齐标题文字），
            // 和卡片左边缘都不齐。
            start = 8.dp,
            bottom = 2.dp
        )
    ) {
        BiliMusicRankType.values().forEach { type ->
            val label = stringResource(type.labelRes)
            // 电视：M3 的 chip 只有很细的焦点边框，电视上几乎看不出来 —— 补一圈和卡片一致的焦点环，
            // 否则光标跳上来时看着就像"丢了"。原创榜那颗额外记一个焦点请求器，
            // 让标题行的「查看全部」按左能精确落回它身上。
            var chipModifier = if (LocalIsTelevision.current) {
                Modifier.tvFocusRing(MaterialTheme.colorScheme.primary)
            } else {
                Modifier
            }
            if (type == BiliMusicRankType.ORIGINAL && originalChipRequester != null) {
                chipModifier = chipModifier.focusRequester(originalChipRequester)
            }
            if (NeriDesign.isClean) {
                NeriChoiceChip(
                    selected = selected == type,
                    onClick = { onTypeSelected(type) },
                    label = label,
                    modifier = chipModifier
                )
            } else {
                FilterChip(
                    selected = selected == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(text = label) },
                    modifier = chipModifier
                )
            }
        }
    }
}

/**
 * 两个 B 站音乐区块共用的横条：LazyRow + 统一内边距 + 两端夹边。
 *
 * 以前榜单和热门各写一份，内容完全一样（只差数据类型），改一次要改两处。
 * 卡片渲染由调用方通过 [item] 提供，这里只负责列表骨架。
 */
@Composable
private fun BiliMusicStrip(
    itemCount: Int,
    key: ((Int) -> Any)? = null,
    item: @Composable (index: Int, modifier: Modifier) -> Unit
) {
    val stripState = rememberLazyListState()
    LazyRow(
        state = stripState,
        modifier = Modifier.fillMaxWidth(),
        // vertical 4dp 给外扩的焦点环留空间（和其它分区的横条一致），否则环会被裁掉
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count = itemCount, key = { index -> key?.invoke(index) ?: index }) { index ->
            item(index, Modifier.tvStripClamp(stripState, index, itemCount - 1))
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}

/**
 * 榜单横向卡片列表，点击卡片按其在可播放条目中的真实下标起播，保证顺序与榜单一致。
 */
@Composable
private fun BiliMusicRankStrip(
    state: HomeBiliMusicRankState,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean
) {
    val playableVideos = remember(state.songs) { state.songs.toPlayableRankVideoItems() }
    BiliMusicStrip(
        itemCount = state.songs.size,
        key = { index ->
            val song = state.songs[index]
            song.musicId.ifBlank { "rank-$index-${song.creationBvid}" }
        }
    ) { index, modifier ->
        val song = state.songs[index]
        BiliMusicVideoCard(
            coverUrl = song.videoCover,
            title = song.title.ifBlank { song.album },
            badge = song.rank.toString(),
            duration = formatDurationSec(song.displayDurationSec),
            subtitle = song.singer.ifBlank { song.playbackUploader },
            footer = song.playbackSourceLabel(),
            offlineMode = offlineMode,
            modifier = modifier,
            onClick = {
                val target = song.toPlayableBiliVideoItem()
                val found = if (target == null) {
                    -1
                } else {
                    playableVideos.indexOfFirst { it.id == target.id && it.bvid == target.bvid }
                }
                if (found >= 0) {
                    onPlay(playableVideos, found)
                }
            }
        )
    }
}

/**
 * 两个 B 站音乐区块共用的卡片：16:9 封面（左上名次、右下时长）、两行标题、第二三行文字。
 *
 * 榜单和热门以前各写一份、逐行重复（连焦点环半径的注释都抄了两遍），
 * 改一次要改两处；现在只在这里维护排版，内容差异由调用方传进来。
 *
 * 不可播放的曲目保持可见但点击无效（由调用方的 onClick 判断）。
 */
@Composable
private fun BiliMusicVideoCard(
    coverUrl: String,
    title: String,
    /** 左上角名次角标 */
    badge: String,
    /** 右下角时长角标 */
    duration: String,
    /** 第二行：歌手 / UP 主 */
    subtitle: String,
    /** 第三行：播放来源 / 分区 + 播放量 */
    footer: String,
    offlineMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 沿用哔哩哔哩收藏夹的缩略图方案：统一走 buildBiliThumbnailUrl（@WxH_1c.webp 裁剪），
    // 保证同一比例与编码，滚动时不会出现尺寸/清晰度参差。
    val thumbnailUrl = remember(coverUrl) {
        buildBiliThumbnailUrl(imageUrl = coverUrl, width = 336, height = 189)
    }
    val coverRequest = remember(context, thumbnailUrl, offlineMode) {
        offlineCachedImageRequest(
            context = context,
            data = thumbnailUrl,
            sizePx = 336,
            allowHardware = false,
            offlineMode = offlineMode
        )
    }
    // 电视：遥控器连续操作期间先不发起新加载（见 rememberTvDeferredImage）
    val deferredCover = rememberTvDeferredImage(key = thumbnailUrl) { coverRequest }
    // 环半径跟卡片圆角绑定，并且保持「环比卡片圆 4dp」这层关系：
    // 卡片 neriRoundedShape(8.dp) → Material 8dp / 简洁 12dp，环就是 12 / 16。
    // 以前写死 8dp，简洁下靠全局 +8 刚好成了 16，切回 Material 却掉回 8，
    // 和简洁时的观感不一致。
    val biliCardCorner = if (NeriDesign.isClean) cleanCornerRadiusFor(8.dp) else 8.dp
    val tvRingIndication = rememberTvFocusRingIndication(
        radius = biliCardCorner + 4.dp,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    Column(
        modifier = modifier
            .width(168.dp)
            // 卡片圆角与封面一致（8dp），焦点环才和封面四角重合；
            // 内边距放在 clickable 之后 —— 环画在节点边界上，靠它和封面之间留间隙。
            .clip(neriRoundedShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication,
                // 不可播放的曲目也保持可聚焦：clickable(enabled = false) 会把这个节点
                // 从焦点树里摘掉，横条里就出现「焦点洞」，遥控器扫到那儿焦点会卡住不动。
                // 能不能播由 onClick 里判断（找不到可播条目就直接返回）。
                enabled = true,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(neriRoundedShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = deferredCover.model,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    onSuccess = { deferredCover.markLoaded() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, neriRoundedShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), neriRoundedShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = footer,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 如实标注该曲目实际会播放的音频来源：官方 MV、榜单授权投稿，或明确告知暂无可播放音频 */
@Composable
internal fun BiliMusicRankSong.playbackSourceLabel(): String =
    when (playbackSource) {
        BiliMusicPlaybackSource.OFFICIAL_MV ->
            stringResource(CoreCommonR.string.home_bili_music_source_mv)

        BiliMusicPlaybackSource.UPLOAD ->
            stringResource(
                CoreCommonR.string.home_bili_music_source_upload,
                playbackUploader.ifBlank { singer }
            )

        BiliMusicPlaybackSource.UNAVAILABLE ->
            stringResource(CoreCommonR.string.home_bili_music_unavailable)
    }

/**
 * 音乐热门横向卡片列表：点击时按 aid + bvid 在可播放条目中定位真实下标，避免索引错位。
 */
@Composable
private fun BiliMusicHotStrip(
    videos: List<BiliMusicHotVideo>,
    onPlay: (List<BiliVideoItem>, Int) -> Unit,
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val playableVideos = remember(videos) { videos.toPlayableHotVideoItems() }
    BiliMusicStrip(
        itemCount = videos.size,
        key = { index ->
            val video = videos[index]
            video.bvid.ifBlank { "hot-$index-${video.aid}" }
        }
    ) { index, modifier ->
        val video = videos[index]
        BiliMusicVideoCard(
            coverUrl = video.coverUrl,
            title = video.title,
            badge = (index + 1).toString(),
            duration = formatDurationSec(video.durationSec),
            subtitle = video.uploader,
            footer = listOf(
                video.tname,
                formatPlayCount(context, video.viewCount)
            ).filter { it.isNotBlank() }.joinToString(" · "),
            offlineMode = offlineMode,
            modifier = modifier,
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

/** 区块加载中占位，高度与卡片区接近，避免加载完成时页面跳动 */
@Composable
private fun BiliMusicSectionLoading(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 区块加载失败提示：错误详情 + 重试按钮，重试由调用方强制重新加载该模块 */
@Composable
private fun BiliMusicSectionError(
    detail: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(CoreCommonR.string.home_bili_music_section_error_prefix, detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        // 同上：原生 TextButton 在电视上没有焦点环
        HapticTextButton(onClick = onRetry) {
            Text(text = stringResource(CoreCommonR.string.recommend_refresh))
        }
    }
}

/** 榜单类型对应的筛选芯片文案 */
private val BiliMusicRankType.labelRes: Int
    get() = when (this) {
        BiliMusicRankType.HOT -> CoreCommonR.string.home_bili_music_rank_hot
        BiliMusicRankType.ORIGINAL -> CoreCommonR.string.home_bili_music_rank_original
    }
