package moe.ouom.neriplayer.ui.component.lyrics

import moe.ouom.neriplayer.data.model.lyrics.LyricEntry
import moe.ouom.neriplayer.lyrics.parser.hasWordTimedEntries
import moe.ouom.neriplayer.lyrics.parser.isNeteaseYrc
import moe.ouom.neriplayer.lyrics.parser.isTtmlLyrics
import moe.ouom.neriplayer.lyrics.parser.matchTranslationsToLineIndices
import moe.ouom.neriplayer.lyrics.parser.parseNeteaseLrc
import moe.ouom.neriplayer.lyrics.parser.parseNeteaseLyricsAuto
import moe.ouom.neriplayer.lyrics.parser.prepareBilingualLyricsForDisplay
import moe.ouom.neriplayer.lyrics.parser.withoutLyricCredits

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import moe.ouom.neriplayer.data.settings.lyrics.scaledLyricFontSize
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import kotlin.math.abs
import kotlin.math.max

private const val TranslationAlignmentToleranceMs = 450L
private const val FocusedLyricVisualCompensationRatio = 0.42f
private val FocusedLyricMaskSafePadding = 24.dp

@Composable
fun AdvancedLyricsView(
    lyrics: List<LyricEntry>,
    currentTimeMs: Long,
    modifier: Modifier = Modifier,
    textColor: Color,
    lyricFontScale: Float,
    translationFontScale: Float = lyricFontScale,
    baseFontSizeSp: Float = 18f,
    lyricOffsetMs: Long = 0L,
    rawLyrics: String? = null,
    rawTranslatedLyrics: String? = null,
    translatedLyrics: List<LyricEntry>? = null,
    showLyricTranslation: Boolean = true,
    showPhoneticAsTranslation: Boolean = false,
    lyricBlurEnabled: Boolean = true,
    lyricBlurAmount: Float = 2.5f,
    isPlaying: Boolean = false,
    animateViewportScroll: Boolean = false,
    userScrollEnabled: Boolean = true,
    playbackSpeed: Float = 1f,
    lowPowerRendering: Boolean = false,
    useAdditiveBlend: Boolean = !lowPowerRendering,
    offset: Dp = 48.dp,
    keepAliveZone: Dp = 108.dp,
    playedLyricViewportFraction: Float = 0.30f,
    topFadeLength: Dp = 112.dp,
    bottomFadeLength: Dp = 196.dp,
    bottomContentInset: Dp = 0.dp,
    onLyricLongClick: ((LyricEntry) -> Unit)? = null,
    onSeekTo: (Long) -> Unit = {}
) {
    val effectiveTranslatedLyrics = translatedLyrics.orEmpty()
    val syncedLyrics = rememberAdvancedSyncedLyrics(
        AdvancedLyricsPreparationInput(
            rawLyrics = rawLyrics,
            rawTranslatedLyrics = rawTranslatedLyrics,
            lyrics = lyrics,
            translatedLyrics = effectiveTranslatedLyrics,
            showPhoneticAsTranslation = showPhoneticAsTranslation
        )
    )
    if (syncedLyrics.lines.isEmpty()) {
        return
    }

    val normalFontSize = scaledLyricFontSize(baseFontSizeSp, lyricFontScale).sp
    val accompanimentFontSize = scaledLyricFontSize(baseFontSizeSp * 0.62f, lyricFontScale).sp
    val normalTextStyle = remember(normalFontSize) {
        TextStyle(
            fontSize = normalFontSize,
            fontWeight = FontWeight.Bold,
            textMotion = TextMotion.Animated,
            lineHeight = (normalFontSize.value * 1.18f).sp
        )
    }
    val accompanimentTextStyle = remember(accompanimentFontSize) {
        TextStyle(
            fontSize = accompanimentFontSize,
            fontWeight = FontWeight.SemiBold,
            textMotion = TextMotion.Animated,
            lineHeight = (accompanimentFontSize.value * 1.12f).sp
        )
    }
    val baseTranslationTextStyle = LocalTextStyle.current
    val translationTextStyle = remember(
        baseTranslationTextStyle,
        baseFontSizeSp,
        translationFontScale
    ) {
        val translationFontSize = scaledLyricFontSize(baseFontSizeSp * 0.70f, translationFontScale).sp
        baseTranslationTextStyle.copy(
            fontSize = translationFontSize,
            lineHeight = (translationFontSize.value * 1.12f).sp
        )
    }
    val blurDelta = (lyricBlurAmount * 0.45f).coerceIn(0f, 4f)
    val safeCurrentPosition = (currentTimeMs + lyricOffsetMs)
        .coerceAtLeast(0L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
    val renderPositionProvider = rememberInterpolatedPlaybackPositionProvider(
        currentTimeMs = safeCurrentPosition,
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        frameIntervalNanos = if (lowPowerRendering) {
            InterpolatedPlaybackLowPowerFrameIntervalNanos
        } else {
            InterpolatedPlaybackDefaultFrameIntervalNanos
        }
    )

    // 播放位置还没送到时先不组合列表：进页面首帧的 position 常常还是 0，
    // 这时候把列表建出来只能落在第一行，等真实位置到了再校正 —— 那就是「跳一下」。
    if (isPlaying && safeCurrentPosition <= 0L) {
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val focusedLineVisualCompensation = with(density) {
            normalTextStyle.lineHeight.toDp() * FocusedLyricVisualCompensationRatio
        }
        val effectiveOffset = resolvePlayedLyricViewportOffset(
            viewportHeight = maxHeight,
            keepAliveZone = keepAliveZone,
            minimumOffset = offset,
            playedLyricViewportFraction = playedLyricViewportFraction,
            focusedLineVisualCompensation = focusedLineVisualCompensation,
            topFadeLength = topFadeLength
        )
        // 列表一开始就落在「当前唱到的那一句」上。
        //
        // 两个坑：
        // 1) 不能用 rememberLazyListState 的 initialFirstVisibleItemScrollOffset 定位 ——
        //    那个参数在首次测量时会被当成「往上回填上一项」，负值落不到目标位置。
        // 2) 更隐蔽的是 rememberLazyListState 本身：它是 rememberSaveable，同一进程里
        //    再次进入播放页时会从注册表把上一次（甚至上一首）的滚动位置恢复回来 ——
        //    落点就成了「歌词里某个说不清的地方」。
        // 所以这里按歌词实例新建一个干净的 state，落点交给 requestScrollToItem：
        // 它在 SideEffect 里调用，早于布局，首次测量就带着正确位置。
        val listState = remember(syncedLyrics) { LazyListState() }
        val initialAlignmentState = remember(syncedLyrics) { mutableStateOf(false) }
        SideEffect {
            if (!initialAlignmentState.value && syncedLyrics.lines.isNotEmpty()) {
                val targetIndex = syncedLyrics
                    .getCurrentFirstHighlightLineIndexByTime(safeCurrentPosition.toInt())
                    .coerceIn(0, syncedLyrics.lines.lastIndex)
                // 落点 = 把当前行对到 content 起点。
                //
                // 不要传 -(effectiveOffset + keepAliveZone)（那是 KaraokeLyricsView 内部
                // targetScrollOffset 的写法，但它自己也要靠 realign 事后纠正）：
                // LazyColumn 的 contentPadding.top 已经等于 offset + keepAliveZone，
                // 再传这个负偏移会让当前行落在 content 起点**下方**整整 496px，
                // 首帧画出去之后 KaraokeLyricsView 才发现差了 496px、一把 scrollBy 拉回去
                // —— 肉眼看到的就是「进页面歌词跳一下」。
                listState.requestScrollToItem(targetIndex, 0)
                initialAlignmentState.value = true
            }
        }

        CompositionLocalProvider(LocalTextStyle provides translationTextStyle) {
            KaraokeLyricsView(
                listState = listState,
                lyrics = syncedLyrics,
                currentPosition = { safeCurrentPosition.toInt() },
                renderCurrentPosition = renderPositionProvider,
                onLineClicked = { line -> onSeekTo(line.start.toLong()) },
                onLinePressed = { line ->
                    val entry = resolvePressedLyricEntry(line, lyrics)
                    if (onLyricLongClick != null) {
                        onLyricLongClick(entry)
                    } else {
                        onSeekTo(line.start.toLong())
                    }
                },
                // 电视上没有触摸：歌词行不挂点击/长按，也就不会成为方向键的停留点
                linesInteractive = !LocalIsTelevision.current,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        // 电视：加色混合要把整个歌词区先离屏渲染再 Plus 混合，是最贵的一档
                        if (useAdditiveBlend && !LocalIsTelevision.current) {
                            Modifier.graphicsLayer {
                                blendMode = BlendMode.Plus
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                        } else {
                            Modifier
                        }
                    ),
                normalLineTextStyle = normalTextStyle,
                accompanimentLineTextStyle = accompanimentTextStyle,
                textColor = textColor,
                showTranslation = showLyricTranslation,
                showPhonetic = false,
                useBlurEffect = lyricBlurEnabled,
                animateViewportScroll = animateViewportScroll,
                userScrollEnabled = userScrollEnabled,
                offset = effectiveOffset,
                keepAliveZone = keepAliveZone,
                bottomContentInset = bottomContentInset,
                blurDelta = if (lowPowerRendering) blurDelta * 0.55f else blurDelta,
                topFadeLength = topFadeLength,
                bottomFadeLength = bottomFadeLength,
                focusedLineScale = 1.015f,
                unfocusedLineScale = 0.965f,
                activeLineAlpha = 1f,
                inactiveLineAlpha = 0.28f,
                blendMode = resolveAdvancedLyricsBlendMode(useAdditiveBlend)
            )
        }
    }
}

internal fun resolveAdvancedLyricsBlendMode(useAdditiveBlend: Boolean): BlendMode {
    return if (useAdditiveBlend) BlendMode.Plus else BlendMode.SrcOver
}

private fun resolvePressedLyricEntry(
    line: ISyncedLine,
    lyrics: List<LyricEntry>
): LyricEntry {
    val startTimeMs = line.start.toLong()
    lyrics.firstOrNull { it.startTimeMs == startTimeMs }?.let { return it }
    lyrics.minByOrNull { abs(it.startTimeMs - startTimeMs) }
        ?.takeIf { abs(it.startTimeMs - startTimeMs) <= TranslationAlignmentToleranceMs }
        ?.let { return it }

    return LyricEntry(
        text = line.plainText(),
        startTimeMs = line.start.toLong(),
        endTimeMs = line.end.toLong()
    )
}

private fun ISyncedLine.plainText(): String {
    return when (this) {
        is KaraokeLine -> syllables.joinToString(separator = "") { it.content }
        is SyncedLine -> content
        else -> ""
    }
}

internal fun resolvePlayedLyricViewportOffset(
    viewportHeight: Dp,
    keepAliveZone: Dp,
    minimumOffset: Dp,
    playedLyricViewportFraction: Float,
    focusedLineVisualCompensation: Dp,
    topFadeLength: Dp
): Dp {
    val effectivePlayedLyricViewportFraction = playedLyricViewportFraction.coerceIn(0.18f, 0.46f)
    val desiredPlayedLyricSpace = viewportHeight * effectivePlayedLyricViewportFraction
    val minimumVisiblePlayedLyricSpace = topFadeLength +
        FocusedLyricMaskSafePadding +
        keepAliveZone
    val resolvedPlayedLyricSpace = if (desiredPlayedLyricSpace > minimumVisiblePlayedLyricSpace) {
        desiredPlayedLyricSpace
    } else {
        minimumVisiblePlayedLyricSpace
    }
    return (
        resolvedPlayedLyricSpace + focusedLineVisualCompensation - keepAliveZone
        ).coerceAtLeast(minimumOffset)
}

internal fun buildAdvancedSyncedLyrics(
    rawLyrics: String?,
    rawTranslatedLyrics: String?,
    lyrics: List<LyricEntry>,
    translatedLyrics: List<LyricEntry>,
    showPhoneticAsTranslation: Boolean = false
): SyncedLyrics {
    val baseLyrics = resolveAdvancedBaseSyncedLyrics(
        rawLyrics = rawLyrics,
        lyrics = lyrics
    )
    val translatedBase = when {
        rawTranslatedLyrics != null -> baseLyrics.withoutEmbeddedTranslations().attachTranslations(
            parseNeteaseLyricsAuto(rawTranslatedLyrics), replaceExisting = true
        )
        showPhoneticAsTranslation -> {
            val phoneticBase = baseLyrics.withoutEmbeddedTranslations()
            if (translatedLyrics.isNotEmpty()) {
                phoneticBase.attachTranslations(translatedLyrics, replaceExisting = true)
            } else {
                phoneticBase.withPhoneticsAsTranslations()
            }
        }
        else -> baseLyrics.attachTranslations(translatedLyrics)
    }
    return if (rawLyrics != null && (isTtmlLyrics(rawLyrics) || isNeteaseYrc(rawLyrics))) {
        translatedBase
    } else {
        translatedBase.withBilingualDisplayLines()
    }
}

fun buildPhoneticLyricEntries(
    rawLyrics: String?,
    lyrics: List<LyricEntry>
): List<LyricEntry> {
    return resolveAdvancedBaseSyncedLyrics(
        rawLyrics = rawLyrics,
        lyrics = lyrics
    ).lines.mapNotNull { line ->
        val phonetic = line.phoneticText()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        LyricEntry(
            text = phonetic,
            startTimeMs = line.start.toLong(),
            endTimeMs = line.end.toLong()
        )
    }
}

private fun resolveAdvancedBaseSyncedLyrics(
    rawLyrics: String?,
    lyrics: List<LyricEntry>
): SyncedLyrics {
    val shouldPreferParsedLyrics = lyrics.hasWordTimedEntries() &&
        (rawLyrics.isNullOrBlank() || !isNeteaseYrc(rawLyrics))
    return when {
        shouldPreferParsedLyrics -> lyrics.toSyncedLyrics()
        else -> parseRawLyrics(rawLyrics).takeIf { it.lines.isNotEmpty() }
            ?: lyrics.toSyncedLyrics()
    }
}

private fun parseRawLyrics(rawLyrics: String?): SyncedLyrics {
    if (rawLyrics.isNullOrBlank()) {
        return SyncedLyrics(emptyList())
    }
    return runCatching {
        if (isTtmlLyrics(rawLyrics) || isNeteaseYrc(rawLyrics)) {
            val parsed = AutoParser().parse(rawLyrics)
            parsed.copy(lines = parsed.lines.withoutLyricCredits { it.plainText() })
        } else {
            parseNeteaseLrc(rawLyrics).toSyncedLyrics()
        }
    }
        .getOrDefault(SyncedLyrics(emptyList()))
}

private fun List<LyricEntry>.toSyncedLyrics(): SyncedLyrics {
    if (isEmpty()) {
        return SyncedLyrics(emptyList())
    }
    return SyncedLyrics(lines = map { it.toSyncedLine() })
}

private fun SyncedLyrics.withBilingualDisplayLines(): SyncedLyrics {
    val displayed = mutableListOf<ISyncedLine>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        if (line !is SyncedLine) {
            displayed += line
            index++
            continue
        }
        val ordinaryLines = buildList {
            while (index < lines.size) {
                val ordinaryLine = lines[index] as? SyncedLine ?: break
                add(LyricEntry(
                    text = ordinaryLine.content,
                    startTimeMs = ordinaryLine.start.toLong(),
                    endTimeMs = ordinaryLine.end.toLong(),
                    translation = ordinaryLine.translation
                ))
                index++
            }
        }
        // 先按原始行数完成翻译配对，再合并显示，避免同时间戳的多条译文丢失
        displayed += prepareBilingualLyricsForDisplay(ordinaryLines).map { it.toSyncedLine() }
    }
    return copy(lines = displayed)
}

private fun LyricEntry.toSyncedLine(): ISyncedLine {
    val syllables = words.orEmpty().mapIndexedNotNull { index, word ->
        val content = extractWordContent(index)
        if (content.isEmpty()) {
            null
        } else {
            KaraokeSyllable(
                content = content,
                start = word.startTimeMs.toIntSafely(),
                end = max(word.endTimeMs.toIntSafely(), word.startTimeMs.toIntSafely())
            )
        }
    }

    if (syllables.isEmpty()) {
        return SyncedLine(
            content = text,
            translation = translation,
            start = startTimeMs.toIntSafely(),
            end = endTimeMs.toIntSafely()
        )
    }

    return KaraokeLine.MainKaraokeLine(
        syllables = syllables,
        translation = translation,
        alignment = KaraokeAlignment.Unspecified,
        start = startTimeMs.toIntSafely(),
        end = max(endTimeMs.toIntSafely(), syllables.last().end)
    )
}

private fun LyricEntry.extractWordContent(index: Int): String {
    val safeWords = words.orEmpty()
    if (safeWords.isEmpty()) {
        return text
    }

    var cursor = 0
    safeWords.forEachIndexed { currentIndex, word ->
        val requestedLength = word.charCount.coerceAtLeast(0)
        val isLast = currentIndex == safeWords.lastIndex
        val endExclusive = when {
            isLast -> text.length
            requestedLength == 0 -> cursor
            else -> (cursor + requestedLength).coerceAtMost(text.length)
        }
        if (currentIndex == index) {
            return text.substring(cursor.coerceAtMost(text.length), endExclusive)
        }
        cursor = endExclusive
    }
    return ""
}

private fun SyncedLyrics.withoutEmbeddedTranslations(): SyncedLyrics =
    copy(lines = lines.map(ISyncedLine::withoutEmbeddedTranslation))

private fun ISyncedLine.withoutEmbeddedTranslation(): ISyncedLine = when (this) {
    is KaraokeLine.MainKaraokeLine -> copy(translation = null,
        accompanimentLines = accompanimentLines?.map { it.copy(translation = null) })
    is KaraokeLine.AccompanimentKaraokeLine -> copy(translation = null)
    is SyncedLine -> copy(translation = null)
    else -> this
}

private fun SyncedLyrics.attachTranslations(
    translations: List<LyricEntry>,
    replaceExisting: Boolean = false
): SyncedLyrics {
    if (lines.isEmpty() || translations.isEmpty()) {
        return this
    }

    val baseLyricEntries = lines.map { line ->
        LyricEntry(
            text = line.plainText(),
            startTimeMs = line.start.toLong(),
            endTimeMs = line.end.toLong()
        )
    }
    val translationMatchesByIndex = matchTranslationsToLineIndices(
        lines = baseLyricEntries,
        translations = translations,
        toleranceMs = TranslationAlignmentToleranceMs
    )

    val updatedLines = lines.mapIndexed { index, line ->
        val matchedTranslation = translationMatchesByIndex[index]?.text

        when {
            matchedTranslation.isNullOrBlank() -> line
            line is KaraokeLine.MainKaraokeLine && (replaceExisting || line.translation.isNullOrBlank()) ->
                line.copy(translation = matchedTranslation)
            line is SyncedLine && (replaceExisting || line.translation.isNullOrBlank()) ->
                line.copy(translation = matchedTranslation)
            else -> line
        }
    }

    return copy(lines = updatedLines)
}

private fun SyncedLyrics.withPhoneticsAsTranslations(): SyncedLyrics {
    if (lines.isEmpty()) {
        return this
    }

    return copy(
        lines = lines.map { line ->
            val phonetic = line.phoneticText()?.takeIf { it.isNotBlank() } ?: return@map line
            when (line) {
                is KaraokeLine.MainKaraokeLine -> line.copy(
                    translation = phonetic,
                    accompanimentLines = line.accompanimentLines?.map { accompaniment ->
                        val accompanimentPhonetic = accompaniment.phoneticText()
                        accompaniment.copy(
                            translation = accompanimentPhonetic.takeUnless { it.isNullOrBlank() }
                                ?: accompaniment.translation
                        )
                    }
                )
                is KaraokeLine.AccompanimentKaraokeLine -> line.copy(translation = phonetic)
                is SyncedLine -> line
                else -> line
            }
        }
    )
}

private fun ISyncedLine.phoneticText(): String? {
    return when (this) {
        is KaraokeLine -> linePhoneticText()
        else -> null
    }
}

private fun KaraokeLine.linePhoneticText(): String? {
    phonetic?.takeIf { it.isNotBlank() }?.let { return it }
    return syllables.joinToString(separator = " ") { it.phonetic.orEmpty() }
        .trim()
        .takeIf { it.isNotEmpty() }
}

private fun Long.toIntSafely(): Int {
    return coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
