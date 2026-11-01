package moe.ouom.neriplayer.ui.screen.tab.settings.component.playback

import moe.ouom.neriplayer.ui.screen.tab.settings.component.AutoSettingSpecListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.tv.tvEdgeScroll
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.settings.AutoSettingsSchema
import moe.ouom.neriplayer.data.settings.SettingsRepository
import moe.ouom.neriplayer.data.model.youtube.playback.YouTubePlaybackSourcePreference
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsChoiceRow
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsTextButton

@Composable
internal fun YouTubePlaybackSourceSetting(
    repository: SettingsRepository,
    highlightTargetId: String? = null,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val source by repository.youtubePlaybackSourceFlow.collectAsState(
        initial = YouTubePlaybackSourcePreference.Automatic
    )
    var showDialog by remember { mutableStateOf(false) }

    AutoSettingSpecListItem(
        setting = AutoSettingsSchema.playback.youtubePlaybackSource,
        supportingContent = { Text(youTubePlaybackSourceLabel(source)) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        highlightTargetId = highlightTargetId,
        highlightPulse = highlightPulse,
        onHighlightFinished = onHighlightFinished,
        onClick = { showDialog = true }
    )

    if (!showDialog) return

    MiuixSettingsDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(stringResource(CoreCommonR.string.settings_youtube_playback_source)) },
        text = {
            // 六个选项在电视上一屏放不下：内容要能滚，否则最后一项只露个边、
            // 光标一按就跳到下面那个「关闭」上。滚动容器自己接管「已经无处可去」的上下键。
            val scrollState = rememberScrollState()
            val maxContentHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
            androidx.compose.foundation.layout.Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(scrollState)
                    .tvEdgeScroll(scrollState)
            ) {
                YouTubePlaybackSourcePreference.entries.forEach { option ->
                    MiuixSettingsChoiceRow(
                        title = youTubePlaybackSourceLabel(option),
                        subtitle = youTubePlaybackSourceDescription(option),
                        selected = option == source,
                        onClick = {
                            scope.launch { repository.setYouTubePlaybackSource(option) }
                            showDialog = false
                        }
                    )
                }
            }
        },
        confirmButton = {
            MiuixSettingsTextButton(
                onClick = { showDialog = false },
                text = { Text(stringResource(CoreCommonR.string.action_close)) }
            )
        }
    )
}

@Composable
private fun youTubePlaybackSourceLabel(
    source: YouTubePlaybackSourcePreference
): String = stringResource(
    when (source) {
        YouTubePlaybackSourcePreference.Automatic ->
            CoreCommonR.string.settings_youtube_playback_source_automatic
        YouTubePlaybackSourcePreference.VisionOs ->
            CoreCommonR.string.settings_youtube_playback_source_visionos
        YouTubePlaybackSourcePreference.AndroidVr ->
            CoreCommonR.string.settings_youtube_playback_source_android_vr
        YouTubePlaybackSourcePreference.WebRemix ->
            CoreCommonR.string.settings_youtube_playback_source_web_remix
        YouTubePlaybackSourcePreference.TvHtml5 ->
            CoreCommonR.string.settings_youtube_playback_source_tv_html5
        YouTubePlaybackSourcePreference.WebCreator ->
            CoreCommonR.string.settings_youtube_playback_source_web_creator
    }
)

@Composable
private fun youTubePlaybackSourceDescription(
    source: YouTubePlaybackSourcePreference
): String = stringResource(
    when (source) {
        YouTubePlaybackSourcePreference.Automatic ->
            CoreCommonR.string.settings_youtube_playback_source_automatic_desc
        YouTubePlaybackSourcePreference.VisionOs ->
            CoreCommonR.string.settings_youtube_playback_source_visionos_desc
        YouTubePlaybackSourcePreference.AndroidVr ->
            CoreCommonR.string.settings_youtube_playback_source_android_vr_desc
        YouTubePlaybackSourcePreference.WebRemix ->
            CoreCommonR.string.settings_youtube_playback_source_web_remix_desc
        YouTubePlaybackSourcePreference.TvHtml5 ->
            CoreCommonR.string.settings_youtube_playback_source_tv_html5_desc
        YouTubePlaybackSourcePreference.WebCreator ->
            CoreCommonR.string.settings_youtube_playback_source_web_creator_desc
    }
)
