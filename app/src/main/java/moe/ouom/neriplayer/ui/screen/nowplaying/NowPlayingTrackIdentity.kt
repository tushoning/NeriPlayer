package moe.ouom.neriplayer.ui.screen.nowplaying

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.ui.component.playback.NowPlayingSongTitle
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication

internal data class NowPlayingTrackDisplay(
    val name: String?,
    val artist: String?
)

internal fun resolveNowPlayingTrackDisplay(song: SongItem?): NowPlayingTrackDisplay =
    NowPlayingTrackDisplay(song?.displayName(), song?.displayArtist())

internal data class NowPlayingTrackIdentityPresentation(
    val titleStyle: TextStyle,
    val artistStyle: TextStyle,
    val horizontalAlignment: Alignment.Horizontal,
    val modifier: Modifier
)

internal enum class NowPlayingTrackIdentityLayout {
    Expanded,
    Compact,
    LargeCover
}

/** 歌手名焦点环：单行文字做成胶囊，和顶部标签栏一致（半径 = 节点高 / 2） */
private val ArtistFocusRingRadius = 16.dp
/**
 * 环画在节点内侧，靠内边距给文字留气口。
 * 左右对称、各 10dp —— 不再像之前那样左右各塞 12dp 把框拉成窄扁长条。
 */
private val ArtistFocusHorizontalPadding = 10.dp
/** 上下内边距。导出给播放页的「歌名/歌手名固定高度槽位」算高度用 */
internal val NowPlayingArtistFocusVerticalPadding = 4.dp

internal fun resolveNowPlayingTrackIdentityPresentation(
    layout: NowPlayingTrackIdentityLayout,
    typography: Typography,
    isTelevision: Boolean = false
): NowPlayingTrackIdentityPresentation = when (layout) {
    // 电视：和整页歌词的歌名/歌手名同号（titleLarge / bodyLarge），两行居中
    NowPlayingTrackIdentityLayout.Compact -> if (isTelevision) {
        NowPlayingTrackIdentityPresentation(
            typography.titleLarge,
            typography.bodyLarge,
            Alignment.CenterHorizontally,
            Modifier
        )
    } else {
        NowPlayingTrackIdentityPresentation(
            typography.titleLarge, typography.bodyMedium, Alignment.Start, Modifier
        )
    }
    NowPlayingTrackIdentityLayout.Expanded -> if (isTelevision) {
        NowPlayingTrackIdentityPresentation(
            typography.titleLarge,
            typography.bodyLarge,
            Alignment.CenterHorizontally,
            Modifier
        )
    } else {
        NowPlayingTrackIdentityPresentation(
            typography.headlineSmall, typography.bodyLarge, Alignment.CenterHorizontally, Modifier
        )
    }
    NowPlayingTrackIdentityLayout.LargeCover -> NowPlayingTrackIdentityPresentation(
        typography.headlineMedium,
        typography.titleMedium.copy(fontWeight = FontWeight.Normal),
        Alignment.Start,
        Modifier.fillMaxWidth()
    )
}

internal class NowPlayingTrackIdentityOwner {
    private data class CopyContext(val clipboard: Clipboard, val scope: CoroutineScope)

    private var copyContext: CopyContext? = null
    var showNameMenu by mutableStateOf(false)
        private set
    var showArtistMenu by mutableStateOf(false)
        private set

    val openArtistMenuAction: () -> Unit = { openArtistMenu() }
    val closeArtistMenuAction: () -> Unit = { closeArtistMenu() }

    fun bindCopyContext(clipboard: Clipboard, scope: CoroutineScope) {
        copyContext = CopyContext(clipboard, scope)
    }

    fun openNameMenu() { showNameMenu = true }
    fun closeNameMenu() { showNameMenu = false }
    fun openArtistMenu() { showArtistMenu = true }
    fun closeArtistMenu() { showArtistMenu = false }

    fun copyName(text: String?) {
        copyContext?.let { copyNowPlayingText(text, it.clipboard, it.scope) }
        closeNameMenu()
    }

    fun copyArtist(text: String?) {
        copyContext?.let { copyNowPlayingText(text, it.clipboard, it.scope) }
        closeArtistMenu()
    }

    fun artistCopyAction(text: String?): () -> Unit = { copyArtist(text) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NowPlayingTrackIdentity(
    display: NowPlayingTrackDisplay,
    visible: Boolean,
    marqueeEnabled: Boolean,
    titleColor: Color,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onArtistClick: () -> Unit,
    layout: NowPlayingTrackIdentityLayout = NowPlayingTrackIdentityLayout.Expanded
) {
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val owner = remember { NowPlayingTrackIdentityOwner() }
    owner.bindCopyContext(clipboard, clipboardScope)
    val presentation = resolveNowPlayingTrackIdentityPresentation(
        layout, MaterialTheme.typography, LocalIsTelevision.current
    )
    AnimatedVisibility(
        visible = visible,
        modifier = presentation.modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 400, delayMillis = 150),
            initialOffsetY = { it / 4 }
        ) + fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 150))
    ) {
        Column(horizontalAlignment = presentation.horizontalAlignment) {
            NowPlayingTrackTitle(
                display.name, marqueeEnabled, titleColor, owner, presentation.titleStyle
            )
            NowPlayingTrackArtist(
                display.artist, sharedTransitionScope, animatedVisibilityScope,
                onArtistClick, owner, presentation.artistStyle
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingTrackTitle(
    displayName: String?,
    marqueeEnabled: Boolean,
    titleColor: Color,
    owner: NowPlayingTrackIdentityOwner,
    titleStyle: TextStyle
) {
    // 电视：歌名只是展示，不该成为可聚焦节点（遥控器方向键经过它就会停一下）
    val tvNoFocus = if (LocalIsTelevision.current) {
        Modifier.focusProperties { canFocus = false }
    } else {
        Modifier
    }
    BoxWithConstraints {
        NowPlayingSongTitle(
            text = displayName.orEmpty(),
            marqueeEnabled = marqueeEnabled,
            style = titleStyle,
            color = titleColor,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(8.dp))
                .then(tvNoFocus)
                .combinedClickable(onClick = {}, onLongClick = owner::openNameMenu)
        )
        DropdownMenu(
            expanded = owner.showNameMenu,
            onDismissRequest = owner::closeNameMenu
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(CoreCommonR.string.action_copy_song_name)) },
                onClick = { owner.copyName(displayName) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun NowPlayingTrackArtist(
    displayArtist: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onArtistClick: () -> Unit,
    owner: NowPlayingTrackIdentityOwner,
    artistStyle: TextStyle
) {
    // 歌手名的焦点要保留（点了能进歌手页），歌名才是纯展示
    //
    // 电视：焦点环原来传 4dp 圆角 + 左右各 12dp 的横向 padding，画出来是个窄扁方框，
    // 和简洁风格（顶部标签栏那种胶囊）完全不搭。现在节点改成紧凑的胶囊：
    // 半径 16dp、上下 4dp / 左右 10dp，节点高约 32dp，宽度就是文字宽 + 20dp。
    val artistRing = rememberTvFocusRingIndication(
        radius = ArtistFocusRingRadius,
        cleanRadiusBoost = 0.dp
    )
    val artistClickable = if (artistRing != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = artistRing,
            onClick = onArtistClick,
            onLongClick = owner.openArtistMenuAction
        )
    } else {
        Modifier.combinedClickable(
            onClick = onArtistClick,
            onLongClick = owner.openArtistMenuAction
        )
    }
    Box {
        Text(
            text = displayArtist.orEmpty(),
            style = artistStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .nowPlayingArtistSharedModifier(sharedTransitionScope, animatedVisibilityScope)
                .clip(RoundedCornerShape(ArtistFocusRingRadius))
                .then(artistClickable)
                .padding(
                    horizontal = ArtistFocusHorizontalPadding,
                    vertical = NowPlayingArtistFocusVerticalPadding
                )
        )
        NowPlayingArtistCopyMenu(displayArtist, owner)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.nowPlayingArtistSharedModifier(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    this@nowPlayingArtistSharedModifier.sharedElement(
        rememberSharedContentState(key = NowPlayingLyricsSharedTransitionElement.ARTIST.key),
        boundsTransform = LocalNowPlayingLyricsBoundsTransform.current,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@Composable
private fun NowPlayingArtistCopyMenu(
    displayArtist: String?,
    owner: NowPlayingTrackIdentityOwner
) {
    DropdownMenu(
        expanded = owner.showArtistMenu,
        onDismissRequest = owner.closeArtistMenuAction
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(CoreCommonR.string.action_copy_artist)) },
            onClick = owner.artistCopyAction(displayArtist)
        )
    }
}

private fun copyNowPlayingText(text: String?, clipboard: Clipboard, scope: CoroutineScope) {
    if (text == null) return
    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text))) }
}
