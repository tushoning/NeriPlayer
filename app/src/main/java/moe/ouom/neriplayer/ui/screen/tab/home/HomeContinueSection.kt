package moe.ouom.neriplayer.ui.screen.tab.home

import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.model.download.DownloadedSong
import moe.ouom.neriplayer.core.download.policy.toPlaybackSongItem
import moe.ouom.neriplayer.data.model.playlist.LocalPlaylist
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.local.playlist.system.LocalFilesPlaylist
import moe.ouom.neriplayer.data.local.playlist.system.SystemLocalPlaylists
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.playlist.usage.PlaylistUsageRepository
import moe.ouom.neriplayer.data.playlist.usage.resolveLocalPlaylistUsageId
import moe.ouom.neriplayer.data.model.stats.UsageEntry
import moe.ouom.neriplayer.ui.util.rememberPlaylistDisplayCoverUrl
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.cleanCornerRadiusFor
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.tv.tvClampLeft
import moe.ouom.neriplayer.ui.tv.tvClampRight
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import kotlin.math.ceil
import moe.ouom.neriplayer.R as AppR

private const val HomeContinueHorizontalPaddingDp = 8f
private const val HomeContinueCardSpacingDp = 12f
private const val HomeContinueCardMaxWidthDp = 140f
internal const val HomeContinueEntryLimit = 12
internal const val HomeLocalPlaylistCoverCandidateLimit = 24
private const val HomeDownloadedCoverSourceLimit = 64
private const val HomeContinueThreeSlotWidthDp = 300f
private const val HomeContinueTabletWidthDp = 600f

private class ContinuePageModel(
    val items: List<UsageEntry>,
    val page: Int,
    val cardsPerPage: Int,
    val cardWidth: Dp,
    val localPlaylistLookup: Map<Long, LocalPlaylist>,
    val localFilesCoverCandidates: List<SongItem>,
    val offlineMode: Boolean,
    val onClick: (UsageEntry) -> Unit
)

private class ContinueCardModel(
    val entry: UsageEntry,
    val localPlaylist: LocalPlaylist?,
    val localFilesCoverCandidates: List<SongItem>,
    val offlineMode: Boolean,
    val modifier: Modifier,
    onEntryClick: (UsageEntry) -> Unit
) {
    val onClick: () -> Unit = { onEntryClick(entry) }
    val onRemove: () -> Unit = { removeHomeContinueEntry(entry) }
}

internal fun shouldShowHomeContinueSection(
    showContinueCard: Boolean,
    usageLoaded: Boolean,
    hasUsage: Boolean
): Boolean = showContinueCard && (!usageLoaded || hasUsage)

internal fun resolveHomeContinuePagerPage(savedPage: Int, pageCount: Int): Int {
    return savedPage.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
}

internal fun shouldResolveHomeContinueLocalCoverFallback(
    persistedCoverUrl: String?,
    localPlaylist: LocalPlaylist?
): Boolean = localPlaylist != null && shouldValidateHomeContinueCoverReference(persistedCoverUrl)

internal fun shouldValidateHomeContinueCoverReference(
    persistedCoverUrl: String?
): Boolean {
    val normalized = persistedCoverUrl?.trim().orEmpty()
    return normalized.isEmpty() ||
        (!normalized.startsWith("http://", ignoreCase = true) &&
            !normalized.startsWith("https://", ignoreCase = true))
}

internal fun homeLocalFilesCoverCandidates(
    downloadedSongs: List<DownloadedSong>
): List<SongItem> {
    return downloadedSongs.asSequence()
        .take(HomeDownloadedCoverSourceLimit)
        .filter(DownloadedSong::hasHomeCover)
        .take(HomeLocalPlaylistCoverCandidateLimit)
        .map(DownloadedSong::toPlaybackSongItem)
        .toList()
}

private fun DownloadedSong.hasHomeCover(): Boolean {
    if (customCoverUrl.hasHomeCoverValue()) return true
    if (coverPath.hasHomeCoverValue()) return true
    return coverUrl.hasHomeCoverValue()
}

private fun String?.hasHomeCoverValue(): Boolean = this?.trim()?.isNotEmpty() == true

@Composable
internal fun ContinueSection(
    items: List<UsageEntry>,
    localPlaylistLookup: Map<Long, LocalPlaylist>,
    localFilesCoverCandidates: List<SongItem>,
    onClick: (UsageEntry) -> Unit,
    savedPage: Int,
    onPageChanged: (Int) -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardsPerPage = remember(maxWidth) {
            resolveHomeContinueCardsPerPage(maxWidth.value)
        }
        val cardWidth = remember(maxWidth, cardsPerPage) {
            resolveHomeContinueCardWidthDp(
                containerWidthDp = maxWidth.value,
                cardsPerPage = cardsPerPage
            ).dp
        }
        val pageCount = remember(items.size, cardsPerPage) {
            ceil(items.size / cardsPerPage.toFloat()).toInt().coerceAtLeast(1)
        }
        val initialPage = remember(savedPage, pageCount) {
            resolveHomeContinuePagerPage(savedPage, pageCount)
        }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { pageCount }
        )
        LaunchedEffect(pagerState.currentPage) {
            onPageChanged(pagerState.currentPage)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) { page ->
            ContinuePage(
                model = ContinuePageModel(
                    items = items,
                    page = page,
                    cardsPerPage = cardsPerPage,
                    cardWidth = cardWidth,
                    localPlaylistLookup = localPlaylistLookup,
                    localFilesCoverCandidates = localFilesCoverCandidates,
                    offlineMode = offlineMode,
                    onClick = onClick
                ),
            )
        }
    }
}

@Composable
private fun ContinuePage(model: ContinuePageModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // vertical 4dp 给外扩的焦点环留空间，不留会被翻页容器裁掉
            .padding(
                horizontal = HomeContinueHorizontalPaddingDp.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(HomeContinueCardSpacingDp.dp)
    ) {
        repeat(model.cardsPerPage) { slot ->
            ContinuePageSlot(model, slot)
        }
    }
}

@Composable
private fun ContinuePageSlot(model: ContinuePageModel, slot: Int) {
    val entry = model.items.getOrNull(model.page * model.cardsPerPage + slot)
    if (entry == null) {
        Spacer(Modifier.width(model.cardWidth))
        return
    }
    val absoluteIndex = model.page * model.cardsPerPage + slot
    ContinueSlot(
        ContinueCardModel(
            entry = entry,
            localPlaylist = resolveHomeContinuePlaylist(entry, model.localPlaylistLookup, LocalContext.current),
            localFilesCoverCandidates = model.localFilesCoverCandidates,
            offlineMode = model.offlineMode,
            modifier = Modifier.width(model.cardWidth)
                // 电视：整条第一张再按左、最后一张再按右就停住。
                // 不夹住的话方向搜索会一路往上跑到根节点，把顶部标签栏当成候选，
                // 表现就是「首页第一个卡片按左跳回首页标签」。
                .then(if (absoluteIndex == 0) Modifier.tvClampLeft() else Modifier)
                .then(
                    if (absoluteIndex == model.items.lastIndex) Modifier.tvClampRight()
                    else Modifier
                ),
            onEntryClick = model.onClick
        )
    )
}

@Composable
private fun ContinueSlot(model: ContinueCardModel) {
    ContinueCard(
        entry = model.entry,
        localPlaylist = model.localPlaylist,
        localFilesCoverCandidates = model.localFilesCoverCandidates,
        onClick = model.onClick,
        onRemove = model.onRemove,
        offlineMode = model.offlineMode,
        modifier = model.modifier
    )
}

/**
 * 「我喜欢的音乐」固定占第一个格子，网易云「我喜欢的音乐」紧随其后占第二个。
 *
 * 其余的卡片由仓库按打开时间排（最近打开的在前），这里只把这两张提到最前面：
 * 它们出现就一定在前两位（顺序恒为 本地我喜欢 → 网易云我喜欢），没出现过就维持仓库原有的顺序。
 */
internal fun pinHomeContinueFavorites(
    items: List<UsageEntry>,
    context: Context
): List<UsageEntry> {
    if (items.size < 2) return items
    val favoritesIndex = items.indexOfFirst { entry ->
        entry.source == PlaylistUsageRepository.SOURCE_LOCAL &&
            SystemLocalPlaylists.resolve(entry.id, entry.name, context)?.id ==
            FavoritesPlaylist.SYSTEM_ID
    }
    // 网易云「我喜欢的音乐」是账号自带歌单，id 因人而异，只能靠名字认
    val neteaseLikedIndex = items.indexOfFirst(::isNeteaseLikedPlaylistEntry)
    val pinnedIndexes = listOf(favoritesIndex, neteaseLikedIndex)
        .filter { it >= 0 }
        .distinct()
    if (pinnedIndexes.isEmpty()) return items
    if (pinnedIndexes.size == 1 && pinnedIndexes.first() == 0) return items
    return buildList {
        pinnedIndexes.forEach { index -> add(items[index]) }
        items.forEachIndexed { index, entry ->
            if (index !in pinnedIndexes) add(entry)
        }
    }
}

/**
 * 网易云「<用户名>喜欢的音乐」—— 在这 app 里显示成「xx喜欢的音乐」。
 *
 * 判定三条：
 * 1. 必须是**网易云**歌单：本地那个「我喜欢的音乐」已经在第一格钉死了，
 *    这一条就是为了把它排除掉（它名字里也带这几个字）；
 * 2. 名字以「喜欢的音乐」结尾（用户名在前面）；
 * 3. 名字总字节数不超过 58：超过的多半是用户自己建、名字里恰好带这几个字的歌单。
 */
private fun isNeteaseLikedPlaylistEntry(entry: UsageEntry): Boolean {
    if (!entry.source.equals("netease", ignoreCase = true)) return false
    val name = entry.name.trim()
    if (!name.endsWith("喜欢的音乐")) return false
    return name.toByteArray(Charsets.UTF_8).size <= 58
}

internal fun resolveHomeContinuePlaylist(    entry: UsageEntry,
    lookup: Map<Long, LocalPlaylist>,
    context: Context? = null
): LocalPlaylist? {
    if (entry.source != PlaylistUsageRepository.SOURCE_LOCAL) return null
    return lookup[entry.id] ?: lookup[resolveLocalPlaylistUsageId(entry, context)]
}

private fun removeHomeContinueEntry(entry: UsageEntry) {
    AppContainer.launchBackgroundIo {
        AppContainer.playlistUsageRepo.removeEntry(entry.id, entry.source, entry.subtype)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(
    entry: UsageEntry,
    localPlaylist: LocalPlaylist?,
    localFilesCoverCandidates: List<SongItem>,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    offlineMode: Boolean,
    modifier: Modifier = Modifier
) {
    val displayName = rememberContinueDisplayName(entry)
    val coverUrl = rememberContinueCoverUrl(entry, localPlaylist, localFilesCoverCandidates)
    ContinueCardSurface(entry, displayName, coverUrl, onClick, onRemove, offlineMode, modifier)
}

@Composable
private fun rememberContinueDisplayName(entry: UsageEntry): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(entry.id, entry.name, entry.source, configuration) {
        resolveContinueDisplayName(entry, context)
    }
}

private fun resolveContinueDisplayName(entry: UsageEntry, context: Context): String {
    return resolvedSystemPlaylistName(entry, context) ?: entry.name
}

private fun resolvedSystemPlaylistName(entry: UsageEntry, context: Context): String? {
    return SystemLocalPlaylists.resolve(entry.id, entry.name, context)?.currentName
}

@Composable
private fun rememberContinueCoverUrl(
    entry: UsageEntry,
    localPlaylist: LocalPlaylist?,
    localFilesCoverCandidates: List<SongItem>
): String? {
    val resolved = if (localPlaylist == null) {
        null
    } else {
        rememberContinueLocalCover(entry, localPlaylist, localFilesCoverCandidates)
    }
    return selectContinueCoverUrl(resolved, entry.picUrl)
}

internal fun selectContinueCoverUrl(resolved: String?, persisted: String?): String? {
    val usableResolved = resolved?.takeIf(String::isNotBlank)
    return usableResolved ?: persisted?.takeIf(String::isNotBlank)
}

@Composable
private fun rememberContinueLocalCover(
    entry: UsageEntry,
    localPlaylist: LocalPlaylist,
    localFilesCoverCandidates: List<SongItem>
): String? {
    if (!shouldValidateHomeContinueCoverReference(entry.picUrl)) return null
    return rememberPlaylistDisplayCoverUrl(
        playlist = localPlaylist,
        additionalCoverCandidates = coverCandidatesForPlaylist(localPlaylist, localFilesCoverCandidates),
        preferredCoverUrl = entry.picUrl
    )
}

private fun coverCandidatesForPlaylist(
    playlist: LocalPlaylist,
    localFilesCoverCandidates: List<SongItem>
): List<SongItem> {
    return if (playlist.id == LocalFilesPlaylist.SYSTEM_ID) localFilesCoverCandidates else emptyList()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCardSurface(
    entry: UsageEntry,
    displayName: String,
    coverUrl: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    offlineMode: Boolean,
    modifier: Modifier
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }
    // 卡片圆角 8dp；环外扩 4dp 画在卡片外侧，卡片尺寸不变、环也不压封面。
    // 简洁风格不额外放大圆角：环要和卡片圆角同心，否则看着又圆又扁
    // 环半径跟着卡片圆角走：卡片是 neriRoundedShape(8.dp)，简洁风格下会映射成 12dp，
    // 两种风格的环都要和卡片同心（固定 12 的话，切回 Material 就比 8dp 的卡片圆一圈）
    val cardCorner = if (NeriDesign.isClean) cleanCornerRadiusFor(8.dp) else 8.dp
    val tvRingIndication = rememberTvFocusRingIndication(
        radius = cardCorner,
        outset = 4.dp,
        cleanRadiusBoost = 0.dp
    ) ?: LocalIndication.current
    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication,
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showMenu = true
                }
            )
            // 圆角裁剪放在 clickable 之后，否则会把外扩的焦点环一起切掉
            .clip(neriRoundedShape(8.dp))
    ) {
        ContinueCover(displayName, coverUrl, offlineMode)
        ContinueCardDetails(displayName, entry.trackCount)
        ContinueRemoveMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onRemove = {
                showMenu = false
                onRemove()
            }
        )
    }
}

@Composable
private fun ContinueCover(displayName: String, coverUrl: String?, offlineMode: Boolean) {
    AsyncImage(
        model = fastScrollableImageRequest(
            context = LocalContext.current,
            data = coverUrl,
            sizePx = 384,
            offlineMode = offlineMode
        ),
        contentDescription = displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(neriRoundedShape(8.dp))
    )
}

@Composable
private fun ContinueCardDetails(displayName: String, trackCount: Int) {
    Column(modifier = Modifier.padding(6.dp)) {
        Text(
            text = displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = pluralStringResource(AppR.plurals.home_song_count_format, trackCount, trackCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ContinueRemoveMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(CoreCommonR.string.continue_playing_remove)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
            },
            onClick = onRemove
        )
    }
}

internal fun resolveHomeContinueCardsPerPage(containerWidthDp: Float): Int {
    val preferredMinimumSlots = if (containerWidthDp >= HomeContinueThreeSlotWidthDp) 3 else 2
    val availableWidth = (containerWidthDp - HomeContinueHorizontalPaddingDp * 2f)
        .coerceAtLeast(0f)
    val slotsNeededToAvoidSlack = ceil(
        (availableWidth + HomeContinueCardSpacingDp) /
            (HomeContinueCardMaxWidthDp + HomeContinueCardSpacingDp)
    ).toInt()
    val tabletMinimumSlots = if (containerWidthDp >= HomeContinueTabletWidthDp) 4 else 0
    return maxOf(preferredMinimumSlots, tabletMinimumSlots, slotsNeededToAvoidSlack, 1)
}

internal fun resolveHomeContinueCardWidthDp(
    containerWidthDp: Float,
    cardsPerPage: Int
): Float {
    val slots = cardsPerPage.coerceAtLeast(1)
    val availableWidth = containerWidthDp -
        HomeContinueHorizontalPaddingDp * 2f -
        HomeContinueCardSpacingDp * (slots - 1)
    return (availableWidth / slots)
        .coerceAtLeast(0f)
}

/**
 * 「继续播放」那档卡片的排版参数，供别的页面共用（本地歌单 / 本地歌手）。
 *
 * 传**网格自己的内容宽度**（已经扣掉页面左右边距的那一段），返回每行几张。
 * 配合 [ContinueStyleCardSpacingDp] + [ContinueStyleHorizontalInsetDp] 一起用，
 * 算出来的单元格宽度和 `resolveHomeContinueCardWidthDp` 完全相等，
 * 也就是「卡片大小 = 继续播放卡片大小、间距也一样」。
 */
internal fun resolveContinueStyleColumns(contentWidthDp: Float): Int =
    resolveHomeContinueCardsPerPage(contentWidthDp + HomeContinueHorizontalPaddingDp * 2f)

/** 与继续播放卡片一致的卡片间距（dp） */
internal val ContinueStyleCardSpacingDp = HomeContinueCardSpacingDp.dp

/** 与继续播放卡片一致的额外左右留白（dp），让卡片左边缘对得上 */
internal val ContinueStyleHorizontalInsetDp = HomeContinueHorizontalPaddingDp.dp
