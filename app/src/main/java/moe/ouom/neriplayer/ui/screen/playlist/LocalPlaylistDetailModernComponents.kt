package moe.ouom.neriplayer.ui.screen.playlist

import moe.ouom.neriplayer.ui.haptic.HapticFeedbackEffect
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvFocusRing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassOverscrollBackdrop
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassOverscrollBackdrop
import moe.ouom.neriplayer.ui.effect.glass.drawAdvancedGlassOverscrollBackdrop
import moe.ouom.neriplayer.ui.haptic.HapticFilledIconButton
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.util.format.formatPlayCount
import moe.ouom.neriplayer.data.model.artwork.CoverArtColorSample
import moe.ouom.neriplayer.util.media.CoverArtColorCache
import moe.ouom.neriplayer.util.media.normalizeCoverArtColorCacheKey
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import moe.ouom.neriplayer.common.search.SearchTextMatcher
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.roundToInt

internal const val PLAYLIST_HEADER_KEY = "header"
internal const val PLAYLIST_ACTIONS_KEY = "playlist_actions"
internal const val LOCAL_PLAYLIST_HEADER_KEY = PLAYLIST_HEADER_KEY
internal const val LOCAL_PLAYLIST_ACTIONS_KEY = PLAYLIST_ACTIONS_KEY
internal const val LOCAL_PLAYLIST_METADATA_PROCESSING_KEY = "metadata_processing_card"
internal val PlaylistModernHeroHeight = 122.dp
internal val PlaylistModernHeroSearchHeight = 190.dp
internal val PlaylistModernHeroSearchFieldOffset = 106.dp
private const val PlaylistHeroLightFallbackSeedArgb = 0xFF5F6875.toInt()
private const val PlaylistHeroDarkFallbackSeedArgb = 0xFF303846.toInt()

internal fun shouldRequestPlaylistSearchFocus(
    showSearch: Boolean,
    selectionMode: Boolean,
    autoShowKeyboard: Boolean
): Boolean {
    return showSearch && !selectionMode && autoShowKeyboard
}

internal fun shouldTransferPlaylistSearchFocus(
    showSearch: Boolean,
    selectionMode: Boolean,
    searchFieldComposed: Boolean,
    searchInputFocused: Boolean,
    searchQuery: String
): Boolean {
    return showSearch &&
        !selectionMode &&
        searchFieldComposed &&
        (searchInputFocused || searchQuery.isNotBlank())
}

internal fun shouldShowPlaylistSearch(
    showSearch: Boolean,
    selectionMode: Boolean
): Boolean {
    return showSearch && !selectionMode
}

internal fun resolvePlaylistSearchFieldOffsetPx(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    expandedOffsetPx: Int
): Int {
    if (firstVisibleItemIndex > 0) return 0
    return (expandedOffsetPx - firstVisibleItemScrollOffsetPx).coerceAtLeast(0)
}

internal fun resolvePlaylistHeroFallbackSeedArgb(isDarkTheme: Boolean): Int {
    return if (isDarkTheme) {
        PlaylistHeroDarkFallbackSeedArgb
    } else {
        PlaylistHeroLightFallbackSeedArgb
    }
}

internal fun shouldComposePlaylistSearchSlot(
    searchVisible: Boolean,
    visibilityProgress: Float
): Boolean {
    return searchVisible || visibilityProgress > 0.001f
}

@Composable
internal fun playlistModernSearchVisibilityProgress(
    searchVisible: Boolean,
    label: String
): Float {
    val progress by animateFloatAsState(
        targetValue = if (searchVisible) 1f else 0f,
        // 调用端统一应用缓动曲线，避免弹簧进度再次缓动后在中段突然加速
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = label
    )
    return progress.coerceIn(0f, 1f)
}

internal fun resolvePlaylistEasedProgress(progress: Float): Float {
    return FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
}

internal fun resolvePlaylistDockedSearchSlotProgress(
    searchVisibilityProgress: Float,
    dockedRevealProgress: Float
): Float {
    return searchVisibilityProgress.coerceIn(0f, 1f) *
        resolvePlaylistEasedProgress(dockedRevealProgress)
}

internal fun resolvePlaylistHeaderSearchAlpha(
    searchVisibilityProgress: Float,
    chromeCollapseProgress: Float
): Float {
    return resolvePlaylistEasedProgress(searchVisibilityProgress) *
        (1f - resolvePlaylistEasedProgress(chromeCollapseProgress))
}

internal fun resolvePlaylistSearchDockedProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    expandedOffsetPx: Int
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (expandedOffsetPx <= 0) return 0f
    return (firstVisibleItemScrollOffsetPx.toFloat() / expandedOffsetPx)
        .coerceIn(0f, 1f)
}

internal fun resolvePlaylistDockedSearchRevealProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    revealDistancePx: Int
): Float {
    if (firstVisibleItemIndex <= 1) return 0f
    if (firstVisibleItemIndex > 2) return 1f
    if (revealDistancePx <= 0) return 1f
    return (firstVisibleItemScrollOffsetPx.toFloat() / revealDistancePx)
        .coerceIn(0f, 1f)
}

internal fun resolvePlaylistDockedSearchGlassColor(
    playlistColor: Color,
    isDarkSurface: Boolean
): Color {
    val playlistLuminance = playlistColor.luminance()
    return when {
        isDarkSurface && playlistLuminance > 0.58f -> Color.White.copy(alpha = 0.14f)
        isDarkSurface -> Color.Black.copy(alpha = 0.28f)
        else -> Color.White.copy(alpha = if (playlistLuminance < 0.34f) 0.42f else 0.50f)
    }
}

@Composable
internal fun playlistModernDockedSearchGlassColor(
    playlistColor: Color
): Color {
    return resolvePlaylistDockedSearchGlassColor(
        playlistColor = playlistColor,
        isDarkSurface = playlistModernUsesDarkSurface()
    )
}

internal fun resolvePlaylistSearchListTopPaddingPx(
    searchVisible: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    revealDistancePx: Int,
    dockedSlotHeightPx: Int
): Int {
    if (!searchVisible) return 0
    val safeDockedSlotHeight = dockedSlotHeightPx.coerceAtLeast(0)
    if (firstVisibleItemIndex <= 1) return 0
    if (firstVisibleItemIndex > 2) return safeDockedSlotHeight
    if (revealDistancePx <= 0) return safeDockedSlotHeight
    val progress = (firstVisibleItemScrollOffsetPx.toFloat() / revealDistancePx)
        .coerceIn(0f, 1f)
    return (safeDockedSlotHeight * progress).toInt()
}

internal fun resolvePlaylistSearchInputSyncValue(
    inputValue: TextFieldValue,
    lastSynchronizedQuery: String,
    query: String
): TextFieldValue? {
    return when {
        inputValue.composition == null && inputValue.text == lastSynchronizedQuery -> TextFieldValue(
            text = query,
            selection = TextRange(query.length)
        )
        inputValue.composition == null &&
            query.isBlank() &&
            inputValue.text.isBlank() -> TextFieldValue(
            text = query,
            selection = TextRange.Zero
        )
        else -> null
    }
}

@Stable
internal class PlaylistSearchUiState(
    val visible: MutableState<Boolean>,
    val query: MutableState<String>
)

@Composable
internal fun rememberPlaylistSearchUiState(): PlaylistSearchUiState {
    val visible = rememberSaveable { mutableStateOf(false) }
    val query = rememberSaveable { mutableStateOf("") }
    return remember(visible, query) { PlaylistSearchUiState(visible, query) }
}

@Composable
internal fun rememberPlaylistSearchInputState(
    query: String,
    onQueryChange: (String) -> Unit,
    delayMillis: Long = 80L
): MutableState<TextFieldValue> {
    val inputState = remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }
    val lastSynchronizedQueryState = remember { mutableStateOf(query) }
    LaunchedEffect(query, block = syncPlaylistSearchInput(inputState, lastSynchronizedQueryState, query))
    LaunchedEffect(
        inputState.value,
        block = debouncePlaylistSearchQuery(inputState, query, delayMillis, onQueryChange)
    )
    return inputState
}

private fun syncPlaylistSearchInput(
    inputState: MutableState<TextFieldValue>,
    lastSynchronizedQueryState: MutableState<String>,
    query: String
): suspend CoroutineScope.() -> Unit = {
    resolvePlaylistSearchInputSyncValue(
        inputValue = inputState.value,
        lastSynchronizedQuery = lastSynchronizedQueryState.value,
        query = query
    )?.let { synchronizedValue ->
        inputState.value = synchronizedValue
    }
    lastSynchronizedQueryState.value = query
}

private fun debouncePlaylistSearchQuery(
    inputState: MutableState<TextFieldValue>,
    query: String,
    delayMillis: Long,
    onQueryChange: (String) -> Unit
): suspend CoroutineScope.() -> Unit = {
    val pendingQuery = inputState.value.text
    if (pendingQuery != query) {
        delay(delayMillis.milliseconds)
        if (inputState.value.text == pendingQuery) {
            onQueryChange(pendingQuery)
        }
    }
}

internal fun shouldBuildPlaylistSearchIndex(
    searchVisible: Boolean,
    query: String
): Boolean = searchVisible || query.isNotBlank()

@Composable
private fun playlistSearchLocaleTags(): String {
    val configuration = Configuration(LocalConfiguration.current)
    configuration.setLocales(LocalResources.current.configuration.locales)
    return configuration.locales.toLanguageTags()
}

@Composable
internal fun rememberPlaylistSearchContext(): Context {
    val context = LocalContext.current
    val localeTags = playlistSearchLocaleTags()
    return remember(context, localeTags) {
        val configuration = Configuration().apply {
            setLocales(LocaleList.forLanguageTags(localeTags))
        }
        context.createConfigurationContext(configuration)
    }
}

private class PlaylistSearchIndexState<T>(
    val owner: Any,
    val index: SearchTextMatcher.Index<T>
)

private class PlaylistSearchResultState<T>(
    val index: PlaylistSearchIndexState<T>,
    val query: String,
    val items: List<T>
)

@Composable
internal fun <T> rememberPlaylistSearchResults(
    query: String,
    items: List<T>,
    tokens: (T) -> Iterable<Any?>,
    buildIndex: Boolean = true
): List<T> {
    if (!buildIndex) return items

    val localeTags = playlistSearchLocaleTags()
    val indexOwner = remember(items, localeTags) { Any() }
    val preparedIndex by produceState<PlaylistSearchIndexState<T>?>(null, indexOwner) {
        value = withContext(Dispatchers.Default) {
            PlaylistSearchIndexState(indexOwner, SearchTextMatcher.index(items, tokens))
        }
    }
    val activeIndex = preparedIndex?.takeIf { it.owner === indexOwner }
    val displayedItems by produceState<PlaylistSearchResultState<T>?>(
        initialValue = null,
        key1 = activeIndex,
        key2 = query
    ) {
        value = activeIndex?.let { index ->
            PlaylistSearchResultState(index, query, rankPlaylistSearchResults(items, index.index, query))
        }
    }
    if (query.isBlank()) return items
    return displayedItems?.takeIf { it.index === activeIndex && it.query == query }?.items.orEmpty()
}

private suspend fun <T> rankPlaylistSearchResults(
    items: List<T>,
    index: SearchTextMatcher.Index<T>?,
    query: String
): List<T> = if (index == null) items else withContext(Dispatchers.Default) { index.filterAndRank(query) }

private val PlaylistHeroCoverSize = 88.dp
private val PlaylistHeroCoverCornerRadius = 14.dp
private val PlaylistHeroSearchTopPadding = 22.dp
private val PlaylistDockedSearchTopPadding = 10.dp
private val PlaylistDockedSearchBottomPadding = 8.dp
private val PlaylistSearchFieldMinHeight = 60.dp
internal val PlaylistModernDockedSearchSlotHeight =
    PlaylistDockedSearchTopPadding +
        PlaylistSearchFieldMinHeight +
        PlaylistDockedSearchBottomPadding
private val PlaylistActionBarHeight = 44.dp
private val PlaylistCompactActionButtonSize = 40.dp
private val PlaylistSearchFieldShape = RoundedCornerShape(18.dp)
private val PlaylistActionSheetCornerRadius = 28.dp
private val PlaylistActionSheetShape = RoundedCornerShape(
    topStart = PlaylistActionSheetCornerRadius,
    topEnd = PlaylistActionSheetCornerRadius,
    bottomEnd = 0.dp,
    bottomStart = 0.dp
)
private const val PlaylistSheetLightAlpha = 0.18f
private const val PlaylistSheetDarkAlpha = 0.24f

/**
 * 头图往下多铺出来的那一截。
 *
 * 「全部播放」那一行自己是不透明底色，它上边缘只有 28dp 圆角缺口是空的 ——
 * 头图要正好盖住这块缺口，缺口里看到的才是头图，而不是补上去的纯色。
 *
 * 只给「圆角 + 4dp」：
 * - 少一点（< 28dp）缺口就只铺一半，露出两条纯色三角；
 * - 多一点（曾给过 90dp）图会从这一行下面穿出来，压到第一首歌行上，
 *   看起来就像这一栏"半透明"。
 * 这一行的实际高度约 59dp，32dp 只会落在它自己的范围里。
 */
internal val PlaylistActionSheetBleed = PlaylistActionSheetCornerRadius + 4.dp

/** 顶栏（返回那一行）高度，头图要往上铺这么多才能和顶栏接上 */
internal val PlaylistChromeTopBarHeight = 64.dp

/**
 * 顶栏实际占的高度：TopAppBar 自身 64dp + 状态栏内边距。
 *
 * 头图往上铺多少、顶栏那张背景图按多高来裁，都要用同一个值，
 * 否则两段图缩放不一样，接缝处会错开一截。
 */
@Composable
internal fun playlistTopBarBleed(): Dp =
    PlaylistChromeTopBarHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** 顶栏（返回那一行）/「全部播放」那一行共用的上边缘圆角 */
internal val PlaylistChromeCornerRadius = PlaylistActionSheetCornerRadius

/**
 * 歌单详情页顶栏在电视上的造型：上边缘和「全部播放」那一行一样的圆角。
 *
 * 手机上是整屏通栏，套圆角反而像贴了个卡片，所以只在电视上生效。
 */
@Composable
internal fun Modifier.tvPlaylistTopBarShape(useHeroColor: Boolean = true): Modifier {
    if (!LocalIsTelevision.current) return this
    val shape = RoundedCornerShape(
        topStart = PlaylistChromeCornerRadius,
        topEnd = PlaylistChromeCornerRadius,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )
    // 和「全部播放」面板同一套：先用 `background(color, shape)` 画一块**抗锯齿**的圆角底，
    // 再把照片 clip 到同一个形状盖上去。
    // 这样看到的轮廓是那块底的边（drawPath 填充，抗锯齿），照片自己那条 clip 出来的
    // 硬边被压在底下、只露出不到一个像素，圆角就是平滑的。
    // （纯 clip 时看到的正是照片那条不抗锯齿的边，所以是一排台阶。）
    // 网易云歌单页顶栏在取色 Provider 之外，底色本来就是 surface；
    // 本地歌单整页在 Provider 里，若跟着用头图色，滚到底会变成一条彩色条，
    // 和网易云对不上。所以这里允许显式要求「跟网易云一样用 surface」。
    val baseColor = if (useHeroColor) {
        LocalPlaylistHeroVisualColors.current
            .backgroundOr(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.surface
    }
    return background(baseColor, shape).clip(shape)
}

internal val LOCAL_PLAYLIST_FIXED_ITEM_KEYS = setOf(
    LOCAL_PLAYLIST_HEADER_KEY,
    LOCAL_PLAYLIST_ACTIONS_KEY,
    LOCAL_PLAYLIST_METADATA_PROCESSING_KEY
)

internal fun resolveLocalPlaylistPlayingItemIndex(
    songIndex: Int,
    metadataProcessingVisible: Boolean
): Int {
    return resolveLocalPlaylistSongListIndex(songIndex, metadataProcessingVisible)
}

internal fun resolveLocalPlaylistSongListIndex(
    songIndex: Int,
    metadataProcessingVisible: Boolean
): Int {
    return resolvePlaylistSongItemIndex(
        songIndex = songIndex,
        fixedItemCount = 2 + if (metadataProcessingVisible) 1 else 0
    )
}

internal fun resolvePlaylistSongItemIndex(
    songIndex: Int,
    fixedItemCount: Int = 2
): Int {
    if (songIndex < 0) return -1
    return songIndex + fixedItemCount.coerceAtLeast(0)
}

internal fun resolvePlaylistPlaybackStartIndex(
    songCount: Int,
    shuffleEnabled: Boolean,
    randomIndex: Int
): Int {
    if (songCount <= 0) return -1
    return if (shuffleEnabled) randomIndex.coerceIn(0, songCount - 1) else 0
}

internal fun shouldEnableLocalPlaylistQuickExport(songCount: Int): Boolean {
    return songCount > 0
}

internal fun localPlaylistRepeatModeLabelRes(repeatMode: Int): Int {
    return playlistRepeatModeLabelRes(repeatMode)
}

internal fun playlistRepeatModeLabelRes(repeatMode: Int): Int {
    return when (repeatMode) {
        Player.REPEAT_MODE_ALL -> CoreCommonR.string.playlist_mode_repeat_all
        Player.REPEAT_MODE_ONE -> CoreCommonR.string.playlist_mode_repeat_one
        else -> CoreCommonR.string.playlist_mode_repeat_off
    }
}

internal fun resolvePlaylistHeroBackgroundArgb(
    coverColorArgb: Int?,
    fallbackArgb: Int,
    isDarkTheme: Boolean
): Int {
    val source = coverColorArgb ?: fallbackArgb
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(source, hsl)
    val targetSaturation = (hsl[1] * 0.52f).coerceIn(0.12f, 0.42f)
    val targetLightness = if (isDarkTheme) 0.20f else 0.36f
    val tonal = ColorUtils.HSLToColor(floatArrayOf(hsl[0], targetSaturation, targetLightness))
    return ColorUtils.blendARGB(
        tonal,
        0xFF000000.toInt(),
        if (isDarkTheme) 0.18f else 0.08f
    )
}

internal fun resolvePlaylistHeroAccentArgb(
    coverColorArgb: Int?,
    fallbackArgb: Int,
    isDarkTheme: Boolean
): Int {
    val source = coverColorArgb ?: fallbackArgb
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(source, hsl)
    val targetSaturation = hsl[1].coerceIn(0.34f, 0.78f)
    val targetLightness = if (isDarkTheme) 0.62f else 0.48f
    return ColorUtils.HSLToColor(floatArrayOf(hsl[0], targetSaturation, targetLightness))
}

internal fun resolvePlaylistDetailCoverUrl(
    headerCoverUrl: String?,
    fallbackCoverUrl: String?
): String? {
    return normalizePlaylistDetailCoverUrl(headerCoverUrl)
        ?: normalizePlaylistDetailCoverUrl(fallbackCoverUrl)
}

private fun normalizePlaylistDetailCoverUrl(coverUrl: String?): String? {
    return coverUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
}

internal fun resolvePlaylistChromeCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    expandedHeroHeightPx: Int
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (expandedHeroHeightPx <= 0) return 0f
    return (firstVisibleItemScrollOffsetPx.toFloat() / expandedHeroHeightPx)
        .coerceIn(0f, 1f)
}

internal fun interpolatePlaylistDp(
    start: Dp,
    end: Dp,
    fraction: Float
): Dp {
    val progress = fraction.coerceIn(0f, 1f)
    return start + (end - start) * progress
}

internal fun interpolatePlaylistColor(
    start: Color,
    end: Color,
    fraction: Float
): Color {
    return Color(
        ColorUtils.blendARGB(
            start.toArgb(),
            end.toArgb(),
            fraction.coerceIn(0f, 1f)
        )
    )
}

private data class PlaylistHeroVisualColors(
    val background: Color,
    val accent: Color,
    val readableAccent: Color,
    val controlContent: Color
)

private fun PlaylistHeroVisualColors?.accentOr(fallback: Color): Color = this?.accent ?: fallback

private fun PlaylistHeroVisualColors?.readableAccentOr(fallback: Color): Color =
    this?.readableAccent ?: fallback

private fun PlaylistHeroVisualColors?.backgroundOr(fallback: Color): Color = this?.background ?: fallback

private fun PlaylistHeroVisualColors?.controlContentOr(fallback: Color): Color =
    this?.controlContent ?: fallback

private fun Modifier.focusRequesterIfPresent(focusRequester: FocusRequester?): Modifier =
    if (focusRequester == null) this else focusRequester(focusRequester)

private data class PlaylistSearchGlassStyle(
    val fallbackColor: Color,
    val tintColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val focusedBorderColor: Color,
    val unfocusedBorderColor: Color
)

private val LocalPlaylistHeroVisualColors = staticCompositionLocalOf<PlaylistHeroVisualColors?> {
    null
}

@Composable
private fun rememberResolvedPlaylistHeroVisualColors(
    coverUrl: String?,
    offlineMode: Boolean
): PlaylistHeroVisualColors {
    val context = LocalContext.current
    val isDarkTheme = playlistModernUsesDarkSurface()
    val normalizedCoverModel = normalizeLocalPlaylistHeaderCoverModel(coverUrl)
    val colorCacheKey = normalizeCoverArtColorCacheKey(normalizedCoverModel)
        ?: normalizedCoverModel
    val fallbackArgb = resolvePlaylistHeroFallbackSeedArgb(isDarkTheme)
    val cachedColorSample = remember(colorCacheKey) {
        CoverArtColorCache.peek(normalizedCoverModel)
    }
    val colorSampleState = remember {
        mutableStateOf(cachedColorSample)
    }
    val hasCoverModel = !coverUrl.isNullOrBlank()
    LaunchedEffect(
        context,
        colorCacheKey,
        offlineMode,
        block = loadPlaylistHeroColorSample(
            context = context,
            coverModel = normalizedCoverModel,
            hasCoverModel = hasCoverModel,
            offlineMode = offlineMode,
            colorSampleState = colorSampleState
        )
    )
    val coverColorArgb = resolvePlaylistHeroCoverColorArgb(
        hasCoverModel = hasCoverModel,
        cachedSample = cachedColorSample,
        loadedSample = colorSampleState
    )
    val backgroundColor by animateColorAsState(
        targetValue = Color(
            resolvePlaylistHeroBackgroundArgb(
                coverColorArgb = coverColorArgb,
                fallbackArgb = fallbackArgb,
                isDarkTheme = isDarkTheme
            )
        ),
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "playlist-hero-background"
    )
    val accentColor by animateColorAsState(
        targetValue = Color(
            resolvePlaylistHeroAccentArgb(
                coverColorArgb = coverColorArgb,
                fallbackArgb = fallbackArgb,
                isDarkTheme = isDarkTheme
            )
        ),
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "playlist-hero-accent"
    )
    val readableAccentColor by animateColorAsState(
        targetValue = resolveReadablePlaylistAccentColor(accentColor, isDarkTheme),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "playlist-readable-accent"
    )
    return PlaylistHeroVisualColors(
        background = backgroundColor,
        accent = accentColor,
        readableAccent = readableAccentColor,
        controlContent = playlistHeroControlContentColor(isDarkTheme)
    )
}

private fun loadPlaylistHeroColorSample(
    context: Context,
    coverModel: String,
    hasCoverModel: Boolean,
    offlineMode: Boolean,
    colorSampleState: MutableState<CoverArtColorSample?>
): suspend CoroutineScope.() -> Unit = {
    colorSampleState.value = if (hasCoverModel) {
        CoverArtColorCache.getOrLoad(
            context = context,
            coverUrl = coverModel,
            offlineMode = offlineMode
        )
    } else {
        null
    }
}

internal fun resolvePlaylistHeroCoverColorArgb(
    hasCoverModel: Boolean,
    cachedSample: CoverArtColorSample?,
    loadedSample: State<CoverArtColorSample?>
): Int? = if (hasCoverModel) (cachedSample ?: loadedSample.value)?.baseColorArgb else null

internal fun playlistHeroControlContentColor(isDarkTheme: Boolean): Color = if (isDarkTheme) {
    Color.White.copy(alpha = 0.94f)
} else {
    Color(0xFF191712)
}

@Composable
private fun rememberPlaylistHeroVisualColors(
    coverUrl: String?,
    offlineMode: Boolean
): PlaylistHeroVisualColors {
    return LocalPlaylistHeroVisualColors.current
        ?: rememberResolvedPlaylistHeroVisualColors(
            coverUrl = coverUrl,
            offlineMode = offlineMode
        )
}

@Composable
internal fun PlaylistModernVisualColorsProvider(
    coverUrl: String?,
    offlineMode: Boolean,
    content: @Composable () -> Unit
) {
    val visualColors = LocalPlaylistHeroVisualColors.current
        ?: rememberResolvedPlaylistHeroVisualColors(
            coverUrl = coverUrl,
            offlineMode = offlineMode
        )
    val overscrollOffset = remember { mutableFloatStateOf(0f) }
    val overscrollBackdrop = remember(overscrollOffset, visualColors.background) {
        AdvancedGlassOverscrollBackdrop(
            color = visualColors.background,
            offsetY = overscrollOffset
        )
    }
    Box(
        modifier = Modifier
            .drawAdvancedGlassOverscrollBackdrop(overscrollBackdrop)
    ) {
        CompositionLocalProvider(
            LocalPlaylistHeroVisualColors provides visualColors,
            LocalAdvancedGlassOverscrollBackdrop provides overscrollBackdrop
        ) {
            content()
        }
    }
}

@Composable
internal fun rememberPlaylistModernHeroBackgroundColor(
    coverUrl: String?,
    offlineMode: Boolean
): Color {
    return rememberPlaylistHeroVisualColors(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    ).background
}

private fun resolveReadablePlaylistAccentColor(
    accentColor: Color,
    isDarkTheme: Boolean
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(accentColor.toArgb(), hsl)
    hsl[1] = hsl[1].coerceIn(0.36f, 0.82f)
    hsl[2] = if (isDarkTheme) {
        hsl[2].coerceAtLeast(0.66f)
    } else {
        hsl[2].coerceAtMost(0.38f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
private fun resolvePlaylistSearchGlassStyle(
    glassColor: Color?,
    playlistColor: Color?,
    isDarkSurface: Boolean,
    progress: Float,
    fallbackContentColor: Color,
    fallbackAccentColor: Color
): PlaylistSearchGlassStyle {
    if (glassColor == null) {
        return PlaylistSearchGlassStyle(
            fallbackColor = interpolatePlaylistColor(
                start = Color.White.copy(alpha = 0.16f),
                end = playlistModernSheetFallbackColor(hasCustomBackground = false),
                fraction = progress
            ),
            tintColor = interpolatePlaylistColor(
                start = Color.White.copy(alpha = 0.28f),
                end = playlistModernSheetTintColor(hasCustomBackground = false),
                fraction = progress
            ),
            contentColor = fallbackContentColor,
            accentColor = fallbackAccentColor,
            focusedBorderColor = interpolatePlaylistColor(
                start = Color.White.copy(alpha = 0.42f),
                end = fallbackAccentColor.copy(alpha = 0.58f),
                fraction = progress
            ),
            unfocusedBorderColor = interpolatePlaylistColor(
                start = Color.White.copy(alpha = 0.18f),
                end = fallbackContentColor.copy(alpha = 0.20f),
                fraction = progress
            )
        )
    }

    val usesDarkContent = glassColor.luminance() > 0.48f
    val contentColor = if (usesDarkContent) {
        Color(0xFF22252D)
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val playlistTint = playlistColor?.copy(alpha = if (isDarkSurface) 0.16f else 0.10f)
        ?: glassColor
    return PlaylistSearchGlassStyle(
        fallbackColor = glassColor,
        tintColor = interpolatePlaylistColor(
            start = glassColor,
            end = playlistTint,
            fraction = 0.22f
        ),
        contentColor = contentColor,
        accentColor = contentColor.copy(alpha = 0.92f),
        focusedBorderColor = contentColor.copy(alpha = if (usesDarkContent) 0.20f else 0.18f),
        unfocusedBorderColor = contentColor.copy(alpha = if (usesDarkContent) 0.12f else 0.10f)
    )
}

@Composable
internal fun PlaylistModernStableSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester?,
    dockedProgress: Float,
    modifier: Modifier = Modifier,
    glassColor: Color? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    inputState: MutableState<TextFieldValue>? = null
) {
    val resolvedInputState = inputState ?: rememberPlaylistSearchInputState(
        query = query,
        onQueryChange = onQueryChange
    )
    val progress = dockedProgress.coerceIn(0f, 1f)
    val visualColors = LocalPlaylistHeroVisualColors.current
    val fallbackContentColor = interpolatePlaylistColor(
        start = Color.White,
        end = playlistModernSheetContentColor(),
        fraction = progress
    )
    val fallbackAccentColor = interpolatePlaylistColor(
        start = visualColors.accentOr(MaterialTheme.colorScheme.primary),
        end = visualColors.readableAccentOr(MaterialTheme.colorScheme.primary),
        fraction = progress
    )
    val glassStyle = resolvePlaylistSearchGlassStyle(
        glassColor = glassColor,
        playlistColor = visualColors?.background,
        isDarkSurface = playlistModernUsesDarkSurface(),
        progress = progress,
        fallbackContentColor = fallbackContentColor,
        fallbackAccentColor = fallbackAccentColor
    )
    val contentColor = glassStyle.contentColor
    val accentColor = glassStyle.accentColor
    val fieldModifier = modifier
        .fillMaxWidth()
        .padding(
            start = interpolatePlaylistDp(20.dp, 16.dp, progress),
            top = interpolatePlaylistDp(0.dp, PlaylistDockedSearchTopPadding, progress),
            end = interpolatePlaylistDp(20.dp, 16.dp, progress),
            bottom = interpolatePlaylistDp(0.dp, PlaylistDockedSearchBottomPadding, progress)
        )
    val searchFieldContent: @Composable () -> Unit = {
        OutlinedTextField(
            value = resolvedInputState.value,
            onValueChange = { resolvedInputState.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaylistSearchFieldMinHeight)
                .focusRequesterIfPresent(focusRequester)
                .onFocusChanged { state -> onFocusChanged?.invoke(state.isFocused) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = PlaylistSearchFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor.copy(alpha = 0.94f),
                cursorColor = accentColor,
                focusedBorderColor = glassStyle.focusedBorderColor,
                unfocusedBorderColor = glassStyle.unfocusedBorderColor,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedLeadingIconColor = contentColor.copy(alpha = 0.86f),
                unfocusedLeadingIconColor = contentColor.copy(alpha = 0.74f),
                focusedPlaceholderColor = contentColor.copy(alpha = 0.70f),
                unfocusedPlaceholderColor = contentColor.copy(alpha = 0.62f)
            )
        )
    }
    AdvancedGlassSurface(
        role = AdvancedGlassRole.SemanticCard,
        modifier = fieldModifier,
        shape = PlaylistSearchFieldShape,
        fallbackColor = glassStyle.fallbackColor,
        tintColor = glassStyle.tintColor
    ) {
        searchFieldContent()
    }
}

@Composable
private fun playlistModernSheetFallbackColor(hasCustomBackground: Boolean): Color {
    return if (playlistModernUsesDarkSurface()) {
        if (hasCustomBackground) {
            Color.Black.copy(alpha = PlaylistSheetDarkAlpha)
        } else {
            playlistModernListContainerColor()
        }
    } else {
        Color.White.copy(alpha = PlaylistSheetLightAlpha)
    }
}

@Composable
private fun playlistModernSheetTintColor(hasCustomBackground: Boolean): Color {
    return if (playlistModernUsesDarkSurface()) {
        if (hasCustomBackground) {
            Color.Black
        } else {
            playlistModernListContainerColor()
        }
    } else {
        Color.White
    }
}

@Composable
private fun playlistModernSheetContentColor(): Color {
    return if (playlistModernUsesDarkSurface()) {
        Color.White.copy(alpha = 0.92f)
    } else {
        Color(0xFF191712)
    }
}

@Composable
internal fun playlistModernExpandedTopBarColor(
    playlistColor: Color
): Color {
    return resolvePlaylistTranslucentTopBarColor(
        playlistColor = playlistColor,
        collapseProgress = 0f
    )
}

@Composable
internal fun playlistModernCollapsedTopBarColor(
    playlistColor: Color? = null
): Color {
    return playlistColor?.let {
        resolvePlaylistTranslucentTopBarColor(
            playlistColor = it,
            collapseProgress = 1f
        )
    } ?: Color.Transparent
}

@Composable
internal fun playlistModernCollapsedTopBarContentColor(
    playlistColor: Color? = null
): Color {
    if (playlistColor == null) return playlistModernListPrimaryContentColor()
    return resolvePlaylistSolidTopBarContentColor(playlistColor)
}

internal fun resolvePlaylistSolidTopBarContentColor(
    playlistColor: Color
): Color {
    return if (playlistColor.luminance() > 0.48f) {
        Color(0xFF17191F)
    } else {
        Color.White.copy(alpha = 0.95f)
    }
}

internal fun resolvePlaylistTranslucentTopBarColor(
    playlistColor: Color,
    collapseProgress: Float
): Color {
    val progress = collapseProgress.coerceIn(0f, 1f)
    val alpha = 1f - progress
    return playlistColor.copy(alpha = alpha)
}

internal fun resolvePlaylistSelectionTopBarColor(
    playlistColor: Color,
    collapseProgress: Float
): Color {
    val progress = collapseProgress.coerceIn(0f, 1f)
    return if (progress < 1f) playlistColor else Color.Transparent
}

internal fun resolvePlaylistSelectionTopBarContentColor(
    playlistColor: Color,
    collapsedContentColor: Color,
    collapseProgress: Float
): Color {
    val progress = collapseProgress.coerceIn(0f, 1f)
    return if (progress < 1f) {
        resolvePlaylistSolidTopBarContentColor(playlistColor)
    } else {
        collapsedContentColor
    }
}

@Composable
internal fun playlistModernListPrimaryContentColor(): Color {
    return if (playlistModernUsesDarkSurface()) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color(0xFF17191F)
    }
}

@Composable
internal fun playlistModernListSecondaryContentColor(): Color {
    return if (playlistModernUsesDarkSurface()) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color(0xFF4C505B)
    }
}

@Composable
internal fun playlistModernListTertiaryContentColor(): Color {
    return if (playlistModernUsesDarkSurface()) {
        Color.White.copy(alpha = 0.62f)
    } else {
        Color(0xFF5E6270)
    }
}

@Composable
private fun playlistModernListContainerColor(): Color {
    return MaterialTheme.colorScheme.background
}

@Composable
private fun playlistModernUsesDarkSurface(): Boolean {
    return ColorUtils.calculateLuminance(
        playlistModernListContainerColor().toArgb()
    ) < 0.5
}

/**
 * 把一个「比容器高」的图层塞进容器里。
 *
 * **不能用 `BoxScope.align()`**：实测它在条链路上不生效（会被落成居中），
 * 头图/顶栏两张背景图就是因此错开的 —— 日志里 top/bottom 跟任何 align 都对不上，
 * 只跟「居中」对得上。
 *
 * 这里自己算：**尺寸报成容器大小**（父布局怎么摆偏移都是 0，与 align 无关），
 * 图层再用**显式的 top 偏移**落位，越界部分画到容器外面去。
 *
 * 注意 `measurable.measure(constraints)` 必须**原样传约束**：
 * 高度是外层 `requiredHeight` 定的，一旦改掉它，图片就按原图比例量成 1736 高（=宽），
 * 越界范围直接翻几倍。
 *
 * 用 top 偏移（而不是「贴底」）是故意的：top 偏移只跟容器的**上边界**和 dp 常量有关，
 * 和容器实测高度、和行内容有没有被字撑高都没关系 —— 换缩放/换字体也不会错开。
 *
 * @param topPx 图层顶边相对容器顶边的偏移（px），可以给负数往容器上方画
 * @param containerHeightPx 容器高度，只用来「把自身尺寸报成容器大小」
 */
private fun Modifier.placeLayerInContainer(
    topPx: Int,
    containerHeightPx: Int
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(constraints.maxWidth, containerHeightPx) {
        placeable.place(0, topPx)
    }
}

@Composable
internal fun PlaylistModernHeroHeader(
    displayName: String,
    coverUrl: String?,
    subtitle: String,
    offlineMode: Boolean,
    height: Dp,
    coverContentDescription: String = displayName,
    actionsRevealProgress: Float = 1f,
    /** 背景图往下多铺多少（铺进「全部播放」那一行上边缘的圆角缺口） */
    backgroundBleed: Dp = 0.dp,
    /** 背景图往上多铺多少（铺到顶栏下面，顶栏自己的背景也换成这张图） */
    topBleed: Dp = 0.dp,
    actions: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val normalizedCoverModel = normalizeLocalPlaylistHeaderCoverModel(coverUrl)
    val visualColors = rememberPlaylistHeroVisualColors(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .onGloballyPositioned { coords ->
                // 排查用，别删：头图顶边应该正好等于顶栏的 y + h。
                android.util.Log.i(
                    "NERI_TV_H",
                    "hero y=${coords.positionInWindow().y.toInt()} h=${coords.size.height} " +
                        "param=${with(density) { height.toPx() }.toInt()} " +
                        "w=${coords.size.width}"
                )
            }
    ) {
        // 底色垫在最下面：内容（标题 + 搜索框）撑得比 height 高时，图片只按 height 算，
        // 底下多出来的那一截露出来的就是这块底色，而不是透明/花屏。
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(visualColors.background)
        )
        // 背景层。用 matchParentSize 套一层，内层再用 requiredHeight 撑到「头图 + 多铺出来的部分」，
        // 这样它不影响头图自身高度，又能在父布局不裁剪的情况下画到自己范围之外。
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val containerHeightPx = constraints.maxHeight
            if (!coverUrl.isNullOrBlank()) {
                // 把「顶栏 + 头图 + 圆角缺口」当成一整块来裁这张图：
                // 顶栏里画的是这一块最上面那一段，头图里画的是它下面那一段。
                // 两处必须用**同一个**高度来裁，否则缩放不一样，接缝处就成两张图了 ——
                // 这里只能用 height 参数，不能拿实测高度（实测含搜索框，会比 height 高一截）。
                //
                // 对齐方式：**贴底**（底边 = 头图下边界 + backgroundBleed），
                // 靠 `placeLayerInContainer` 自己算，不走 `align()`（见这个函数的注释）。
                // 底边要盖满「全部播放」那一行的圆角缺口；顶边由块高推出来，正好落在顶栏上边缘。
                val backgroundHeight = topBleed + height + backgroundBleed
                // 顶边 = 头图顶边 - topBleed = 顶栏上边缘：和顶栏那张图的顶边是同一条线，
                // 谁也不用去测对方的高度。
                val topPx = -with(density) { topBleed.toPx() }.roundToInt()
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = coverUrl,
                        sizePx = 720,
                        allowHardware = false,
                        offlineMode = offlineMode
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(backgroundHeight)
                        .placeLayerInContainer(
                            topPx = topPx,
                            containerHeightPx = containerHeightPx
                        )
                        .onGloballyPositioned { coords ->
                            // 排查用，别删：底边应该 = 头图下边界 + backgroundBleed，
                            // 顶边应该和顶栏那张图同一个 y（logcat -s NERI_TV_H）。
                            val bounds = coords.boundsInWindow()
                            android.util.Log.i(
                                "NERI_TV_H",
                                "heroPhoto top=${bounds.top.toInt()} " +
                                    "bottom=${bounds.bottom.toInt()} h=${coords.size.height} " +
                                    "block=${with(density) { backgroundHeight.toPx() }.toInt()}"
                            )
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(backgroundHeight)
                        .placeLayerInContainer(
                            topPx = topPx,
                            containerHeightPx = containerHeightPx
                        )
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.18f),
                                0.5f to Color.Black.copy(alpha = 0.52f),
                                1f to Color.Black.copy(alpha = 0.82f)
                            )
                        )
                )
            }
        }
        Column(
            modifier = Modifier
                // 竖向居中：封面图 / 标题这一块整体居中，封面中心就落在头图中心。
                // 用「铺满高度 + 内容居中」而不是 `align(Center)`，同样是为了绕开 align。
                .fillMaxHeight()
                .wrapContentHeight(Alignment.CenterVertically)
                .fillMaxWidth()
                .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = normalizedCoverModel,
                        sizePx = 320,
                        allowHardware = false,
                        crossfade = true,
                        offlineMode = offlineMode
                    ),
                    contentDescription = coverContentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PlaylistHeroCoverSize)
                        .clip(RoundedCornerShape(PlaylistHeroCoverCornerRadius))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.22f),
                                offset = Offset(1f, 1f),
                                blurRadius = 3f
                            )
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CompositionLocalProvider(LocalPlaylistHeroVisualColors provides visualColors) {
                if (actions != null) {
                    Box(
                        modifier = Modifier
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                // 完整测量输入框，只展开槽位高度，避免首帧撑满自适应头部
                                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                                val revealedHeight = (placeable.height * actionsRevealProgress.coerceIn(0f, 1f)).roundToInt()
                                layout(placeable.width, constraints.constrainHeight(revealedHeight)) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .padding(top = PlaylistHeroSearchTopPadding)
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

/**
 * 顶栏（返回那一行）的背景：直接画头图。
 *
 * 高度按「顶栏 + 头图 + 圆角缺口」整块算，顶栏里显示的就是这块图最上面那一段，
 * 和下面头图里显示的那一段接得上，看起来是一张图从顶上铺下来。
 *
 * 两边各自锚一条**确定的边**，谁也不靠位移猜：
 * - 这张（顶栏里）：贴顶栏上边缘，高度 = 整块；
 * - 头图里那张：贴「头图下边界 + 圆角那一截」，高度相同。
 * 而顶栏高度 == 头图要往上铺的那一截（都是 `64dp + 状态栏`），
 * 所以两张图的 top/bottom 会落在同一处。
 *
 * @param heroHeight 下面头图的高度，必须和传给 [PlaylistModernHeroHeader] 的一致
 * @param alpha 跟着顶栏一起淡出（页面往上滚时顶栏本来会渐变到透明）
 */
@Composable
internal fun BoxScope.PlaylistTopBarCoverBackdrop(
    coverUrl: String?,
    offlineMode: Boolean,
    heroHeight: Dp,
    alpha: Float = 1f
) {
    if (coverUrl.isNullOrBlank() || alpha <= 0.01f) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val topBleed = playlistTopBarBleed()
    val totalHeight = topBleed + heroHeight + PlaylistActionSheetBleed
    BoxWithConstraints(
        // 必须 matchParentSize：以前用 fillMaxSize，Column 里的顶栏被撑到整屏高，
        // 下面「全部播放」和歌曲列表全被挤到屏幕外面去了。
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { coords ->
                // 排查用，别删：顶栏的窗口 y 和高度必须和头图接上（logcat -s NERI_TV_H）。
                android.util.Log.i(
                    "NERI_TV_H",
                    "bar y=${coords.positionInWindow().y.toInt()} h=${coords.size.height} " +
                        "bleed=${with(density) { topBleed.toPx() }.toInt()} " +
                        "block=${with(density) { totalHeight.toPx() }.toInt()} " +
                        "w=${coords.size.width}"
                )
                // 头图那张是按「顶栏上边缘」定位的，前提是顶栏实际高度 == playlistTopBarBleed()。
                // 两者不一致（换了顶栏实现 / 主题改了高度）就会错开，这里主动报警。
                val barHeightPx = coords.size.height
                val topBleedPx = with(density) { topBleed.toPx() }.roundToInt()
                if (kotlin.math.abs(barHeightPx - topBleedPx) > 1) {
                    android.util.Log.w(
                        "NERI_TV_H",
                        "顶栏高度 ${barHeightPx}px 与 playlistTopBarBleed() ${topBleedPx}px 不一致，" +
                            "头图会错开 —— 需要让 PlaylistChromeTopBarHeight 跟随顶栏实际高度"
                    )
                }
            }
            .graphicsLayer { this.alpha = alpha }
    ) {
        val containerHeightPx = constraints.maxHeight
        AsyncImage(
            model = offlineCachedImageRequest(
                context = context,
                data = coverUrl,
                sizePx = 720,
                allowHardware = false,
                offlineMode = offlineMode
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(totalHeight)
                .placeLayerInContainer(
                    topPx = 0,
                    containerHeightPx = containerHeightPx
                )
                .onGloballyPositioned { coords ->
                    // 排查用，别删：这张图的 top/bottom 必须和头图那张完全一致。
                    val bounds = coords.boundsInWindow()
                    android.util.Log.i(
                        "NERI_TV_H",
                        "barPhoto top=${bounds.top.toInt()} bottom=${bounds.bottom.toInt()} " +
                            "h=${coords.size.height} alpha=$alpha"
                    )
                }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(totalHeight)
                .placeLayerInContainer(
                    topPx = 0,
                    containerHeightPx = containerHeightPx
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f),
                        0.5f to Color.Black.copy(alpha = 0.52f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )
    }
}

@Composable
internal fun LocalPlaylistHeroHeader(
    displayName: String,
    headerCover: String?,
    totalDurationText: String,
    songCount: Int,
    playCount: Long,
    offlineMode: Boolean,
    height: Dp,
    actionsRevealProgress: Float = 1f,
    backgroundBleed: Dp = 0.dp,
    topBleed: Dp = 0.dp,
    actions: (@Composable () -> Unit)? = null
) {
    PlaylistModernHeroHeader(
        displayName = displayName,
        coverUrl = headerCover,
        subtitle = stringResource(
            CoreCommonR.string.local_playlist_total_duration,
            totalDurationText,
            songCount,
            formatPlayCount(LocalContext.current, playCount)
        ),
        offlineMode = offlineMode,
        height = height,
        actionsRevealProgress = actionsRevealProgress,
        backgroundBleed = backgroundBleed,
        topBleed = topBleed,
        actions = actions
    )
}

@Composable
internal fun PlaylistModernHeroSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    inputState: MutableState<TextFieldValue>? = null
) {
    val resolvedInputState = inputState ?: rememberPlaylistSearchInputState(
        query = query,
        onQueryChange = onQueryChange
    )
    val visualColors = LocalPlaylistHeroVisualColors.current
    val accentColor = visualColors.accentOr(MaterialTheme.colorScheme.primary)
    AdvancedGlassSurface(
        role = AdvancedGlassRole.SemanticCard,
        modifier = modifier.fillMaxWidth(),
        shape = PlaylistSearchFieldShape,
        fallbackColor = Color.White.copy(alpha = 0.16f),
        tintColor = Color.White.copy(alpha = 0.28f)
    ) {
        OutlinedTextField(
            value = resolvedInputState.value,
            onValueChange = { resolvedInputState.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaylistSearchFieldMinHeight)
                .focusRequesterIfPresent(focusRequester)
                .onFocusChanged { state -> onFocusChanged?.invoke(state.isFocused) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = PlaylistSearchFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.94f),
                cursorColor = accentColor,
                focusedBorderColor = Color.White.copy(alpha = 0.42f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedLeadingIconColor = Color.White.copy(alpha = 0.86f),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.74f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.70f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.62f)
            )
        )
    }
}

@Composable
internal fun PlaylistModernDockedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    inputState: MutableState<TextFieldValue>? = null
) {
    val resolvedInputState = inputState ?: rememberPlaylistSearchInputState(
        query = query,
        onQueryChange = onQueryChange
    )
    val visualColors = LocalPlaylistHeroVisualColors.current
    val glassColor = playlistModernDockedSearchGlassColor(
        playlistColor = visualColors.backgroundOr(MaterialTheme.colorScheme.primary)
    )
    val glassStyle = resolvePlaylistSearchGlassStyle(
        glassColor = glassColor,
        playlistColor = visualColors?.background,
        isDarkSurface = playlistModernUsesDarkSurface(),
        progress = 1f,
        fallbackContentColor = playlistModernSheetContentColor(),
        fallbackAccentColor = visualColors.readableAccentOr(MaterialTheme.colorScheme.primary)
    )
    val contentColor = glassStyle.contentColor
    val accentColor = glassStyle.accentColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                top = PlaylistDockedSearchTopPadding,
                end = 16.dp,
                bottom = PlaylistDockedSearchBottomPadding
            )
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.SemanticCard,
            modifier = Modifier.fillMaxWidth(),
            shape = PlaylistSearchFieldShape,
            fallbackColor = glassStyle.fallbackColor,
            tintColor = glassStyle.tintColor
        ) {
            OutlinedTextField(
                value = resolvedInputState.value,
                onValueChange = { resolvedInputState.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlaylistSearchFieldMinHeight)
                    .focusRequesterIfPresent(focusRequester)
                    .onFocusChanged { state -> onFocusChanged?.invoke(state.isFocused) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = PlaylistSearchFieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor.copy(alpha = 0.92f),
                    cursorColor = accentColor,
                    focusedBorderColor = glassStyle.focusedBorderColor,
                    unfocusedBorderColor = glassStyle.unfocusedBorderColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedLeadingIconColor = contentColor.copy(alpha = 0.84f),
                    unfocusedLeadingIconColor = contentColor.copy(alpha = 0.68f),
                    focusedPlaceholderColor = contentColor.copy(alpha = 0.58f),
                    unfocusedPlaceholderColor = contentColor.copy(alpha = 0.50f)
                )
            )
        }
    }
}

@Composable
internal fun PlaylistModernDockedSearchSlot(
    revealProgress: Float,
    coverUrl: String?,
    offlineMode: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester?,
    dockedProgress: Float,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    inputState: MutableState<TextFieldValue>? = null
) {
    val slotProgress = revealProgress.coerceIn(0f, 1f)
    val slotAlpha = resolvePlaylistEasedProgress(slotProgress)
    if (!shouldComposePlaylistSearchSlot(searchVisible = false, visibilityProgress = slotProgress)) {
        return
    }
    val density = LocalDensity.current
    PlaylistModernVisualColorsProvider(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    ) {
        val visualColors = LocalPlaylistHeroVisualColors.current
        val glassColor = playlistModernDockedSearchGlassColor(
            playlistColor = visualColors.backgroundOr(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(
                    interpolatePlaylistDp(
                        start = 0.dp,
                        end = PlaylistModernDockedSearchSlotHeight,
                        fraction = slotProgress
                    )
                )
                .clipToBounds()
                .graphicsLayer {
                    alpha = slotAlpha
                }
        ) {
            PlaylistModernStableSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                focusRequester = focusRequester,
                onFocusChanged = onFocusChanged,
                dockedProgress = dockedProgress,
                glassColor = glassColor,
                modifier = Modifier.graphicsLayer {
                    translationY = with(density) {
                        ((1f - slotAlpha) * -8.dp.toPx())
                    }
                },
                inputState = inputState
            )
        }
    }
}

@Composable
internal fun PlaylistModernActionSheet(
    coverUrl: String?,
    offlineMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = PlaylistActionSheetShape,
    cornerGapHeight: Dp = PlaylistActionSheetCornerRadius,
    hasCustomBackground: Boolean = false,
    /**
     * 头图铺到了这一行上边缘的圆角缺口里。
     *
     * 生效时这一行必须是**不透明的纯色底**（不再是玻璃底）：
     * 头图就从这一行下面走过，半透明的底会把图透上来，
     * 看起来就是"全部播放这一栏是半透明的"。
     * 图只在圆角那两块缺口里露出来。
     */
    overHeroImage: Boolean = false,
    content: @Composable () -> Unit
) {
    val visualColors = rememberPlaylistHeroVisualColors(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    )
    val fallbackColor = playlistModernSheetFallbackColor(hasCustomBackground)
    val tintColor = playlistModernSheetTintColor(hasCustomBackground)
    val glassEnabled = hasCustomBackground || !playlistModernUsesDarkSurface()
    CompositionLocalProvider(LocalPlaylistHeroVisualColors provides visualColors) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    android.util.Log.i(
                        "NERI_TV_H",
                        "actions y=${coords.positionInWindow().y.toInt()} h=${coords.size.height}"
                    )
                }
        ) {
            if (overHeroImage) {
                // 不透明底 + 圆角：先铺一块抗锯齿的圆角底，再把内容裁进同一个形状。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(playlistModernListContainerColor(), shape)
                        .clip(shape)
                ) {
                    content()
                }
            } else {
                PlaylistActionSheetCornerGapLayer(
                    shape = shape,
                    color = visualColors.background,
                    gapHeight = cornerGapHeight
                )
                AdvancedGlassSurface(
                    role = AdvancedGlassRole.PlaylistSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = shape,
                    fallbackColor = fallbackColor,
                    tintColor = tintColor,
                    enabled = glassEnabled
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PlaylistActionSheetCornerGapLayer(
    shape: Shape,
    color: Color,
    gapHeight: Dp
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawWithCache {
                val cornerGapHeightPx = gapHeight.toPx().coerceIn(0f, size.height)
                val gapPath = Path().apply {
                    addRect(Rect(0f, 0f, size.width, cornerGapHeightPx))
                }
                val sheetPath = Path().apply {
                    addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
                }
                val cornerGapPath = Path.combine(
                    operation = PathOperation.Difference,
                    path1 = gapPath,
                    path2 = sheetPath
                )
                onDrawBehind {
                    drawPath(
                        path = cornerGapPath,
                        color = color
                    )
                }
            }
    )
}

@Composable
internal fun PlaylistModernListItemSurface(
    coverUrl: String?,
    offlineMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val visualColors = rememberPlaylistHeroVisualColors(coverUrl, offlineMode)
    CompositionLocalProvider(LocalPlaylistHeroVisualColors provides visualColors) {
        Box(
            modifier = modifier.fillMaxWidth(),
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaylistModernPlaybackActions(
    songCount: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    modifier: Modifier = Modifier,
    onPlayInOrder: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onExportToLocalPlaylist: () -> Unit,
    /** B 站视频这类没有「导出到本地歌单」概念的内容把这一颗按钮藏起来 */
    showExport: Boolean = true
) {
    val canUseSongs = songCount > 0
    val visualColors = LocalPlaylistHeroVisualColors.current
    val accentColor = visualColors.readableAccentOr(MaterialTheme.colorScheme.primary)
    val onAccentColor = resolvePlaylistContentColor(accentColor)
    val controlContentColor = visualColors.controlContentOr(MaterialTheme.colorScheme.onSurface)
    val playLabel = stringResource(playlistPlayLabelRes(shuffleEnabled))
    val onPlay = if (shuffleEnabled) onShufflePlay else onPlayInOrder

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 8.dp, end = 22.dp, bottom = 8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HapticFilledIconButton(
                onClick = onPlay,
                enabled = canUseSongs,
                modifier = Modifier.size(PlaylistActionBarHeight),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                ),
                // 节点 44dp、可见圆 40dp（圆心重合）→ 环内收 1dp 后再算上基础 1dp，
                // 正好是一圈直径 40dp 的圆，和圆边严丝合缝。
                ringInset = 1.dp
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = playLabel,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = playLabel,
                style = MaterialTheme.typography.titleMedium,
                color = controlContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCompactIconButton(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = stringResource(playlistShuffleModeLabelRes(shuffleEnabled)),
                enabled = canUseSongs,
                active = shuffleEnabled,
                onClick = onToggleShuffle
            )
            PlaylistCompactIconButton(
                imageVector = playlistRepeatModeIcon(repeatMode),
                contentDescription = stringResource(playlistRepeatModeLabelRes(repeatMode)),
                active = repeatMode != Player.REPEAT_MODE_OFF,
                onClick = onCycleRepeatMode
            )
            if (showExport) {
                PlaylistCompactIconButton(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = stringResource(CoreCommonR.string.playlist_export_to_local),
                    enabled = shouldEnableLocalPlaylistQuickExport(songCount),
                    onClick = onExportToLocalPlaylist,
                )
            }
        }
    }
}

@Composable
private fun PlaylistCompactIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val visualColors = LocalPlaylistHeroVisualColors.current
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = visualColors.compactIconContainerColor(colorScheme, active)
    val contentColor = visualColors.compactIconContentColor(colorScheme, enabled, active)

    HapticIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(PlaylistCompactActionButtonSize)
            .clip(neriRoundedShape(22.dp))
            .background(containerColor)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun PlaylistHeroVisualColors?.compactIconContainerColor(
    colorScheme: ColorScheme,
    active: Boolean
): Color = when {
    this == null && active -> colorScheme.primary.copy(alpha = 0.18f)
    this == null -> colorScheme.onSurface.copy(alpha = 0.09f)
    active -> accent.copy(alpha = 0.24f)
    else -> controlContent.copy(alpha = 0.09f)
}

private fun PlaylistHeroVisualColors?.compactIconContentColor(
    colorScheme: ColorScheme,
    enabled: Boolean,
    active: Boolean
): Color {
    if (this == null) {
        return when {
            !enabled -> colorScheme.onSurface.copy(alpha = 0.32f)
            active -> colorScheme.primary
            else -> colorScheme.onSurfaceVariant
        }
    }
    return when {
        !enabled -> controlContent.copy(alpha = 0.32f)
        active -> readableAccent
        else -> controlContent.copy(alpha = 0.82f)
    }
}

internal fun playlistPlayLabelRes(shuffleEnabled: Boolean): Int = if (shuffleEnabled) {
    CoreCommonR.string.player_shuffle_play
} else {
    CoreCommonR.string.player_play_all
}

internal fun playlistShuffleModeLabelRes(shuffleEnabled: Boolean): Int = if (shuffleEnabled) {
    CoreCommonR.string.playlist_mode_shuffle
} else {
    CoreCommonR.string.playlist_mode_order
}

internal fun playlistRepeatModeIcon(repeatMode: Int): ImageVector =
    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Outlined.Repeat

private fun resolvePlaylistContentColor(backgroundColor: Color): Color {
    return if (ColorUtils.calculateLuminance(backgroundColor.toArgb()) > 0.48) {
        Color.Black
    } else {
        Color.White
    }
}

@Composable
internal fun LocalPlaylistPlaybackActions(
    songCount: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    modifier: Modifier = Modifier,
    onPlayInOrder: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onExportToLocalPlaylist: () -> Unit
) {
    PlaylistModernPlaybackActions(
        songCount = songCount,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        modifier = modifier,
        onPlayInOrder = onPlayInOrder,
        onShufflePlay = onShufflePlay,
        onToggleShuffle = onToggleShuffle,
        onCycleRepeatMode = onCycleRepeatMode,
        onExportToLocalPlaylist = onExportToLocalPlaylist
    )
}
