package moe.ouom.neriplayer.ui.screen.artist

import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.local.media.displayAlbum
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.ui.util.rememberSongDisplayCoverUrl
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.component.playlist.NeriSongRow
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.util.format.formatDuration
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import moe.ouom.neriplayer.R as AppR

@Composable
internal fun ArtistSongRow(
    index: Int,
    song: SongItem,
    onClick: () -> Unit,
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    val displayArtist = song.displayArtist().takeIf { it.isNotBlank() }
    val displayAlbum = song.displayAlbum(context)
        .replace("Netease", "")
        .takeIf { it.isNotBlank() }
    // 电视：左右各补 16dp，和歌单页歌曲行一层层加上来的边距对齐
    val horizontalPadding = if (LocalIsTelevision.current) 16.dp else 0.dp
    NeriSongRow(
        index = index,
        song = song,
        onClick = onClick,
        subtitleContent = {
            ArtistSongSubtitle(
                artist = displayArtist,
                album = displayAlbum
            )
        },
        offlineMode = offlineMode,
        horizontalPadding = horizontalPadding,
        onPlayNext = { PlayerManager.addToQueueNext(song) },
        onAddToQueueEnd = { PlayerManager.addToQueueEnd(song) }
    )
}

@Composable
private fun ArtistSongSubtitle(
    artist: String?,
    album: String?
) {
    if (artist == null && album == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (artist != null) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (album != null) {
            Text(
                text = if (artist == null) album else " · $album",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
internal fun ArtistAlbumRow(
    album: AlbumSummary,
    onClick: () -> Unit,
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberTvFocusRingIndication(12.dp, insetHorizontal = 25.dp)
                    ?: LocalIndication.current,
                onClick = onClick
            )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = offlineCachedImageRequest(
                    context = context,
                    data = album.picUrl,
                    offlineMode = offlineMode
                ),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(neriRoundedShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(AppR.plurals.count_songs_format, album.size, album.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 88.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
internal fun LoadingBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.width(12.dp))
        Text(stringResource(CoreCommonR.string.playlist_loading_content))
    }
}

@Composable
internal fun ErrorBlock(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(CoreCommonR.string.action_retry))
        }
    }
}

@Composable
internal fun EmptyBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LoadMoreButton(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(CoreCommonR.string.artist_load_more))
        }
    }
}
