package moe.ouom.neriplayer.ui.screen.nowplaying

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
 * File: moe.ouom.neriplayer.ui.screen.nowplaying/NowPlayingSpeedPitchSheet
 * Created: 2026/11/02
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import moe.ouom.neriplayer.ui.theme.NeriDesign
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledModalBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.playback.MAX_PLAYBACK_PITCH
import moe.ouom.neriplayer.data.model.playback.MIN_PLAYBACK_PITCH
import moe.ouom.neriplayer.data.model.playback.pitchToSemitoneOffset
import moe.ouom.neriplayer.data.model.playback.semitoneOffsetToPitch
import moe.ouom.neriplayer.ui.screen.tab.settings.audio.formatSemitones
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsSlider
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.tv.rememberTvInitialFocus
import moe.ouom.neriplayer.ui.tv.tvInitialFocus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 播放页左下工具栏的「倍速与音调」图标。
 *
 * 直接用 Material 官方的 `Outlined.Speed`（镂空圆环 + 一条斜指针），
 * 和旁边 `Outlined.Timer` / `Outlined.QueueMusic` 是同一套图标、同样的线宽和比例。
 */
internal val NowPlayingSpeedPitchIcon: ImageVector
    get() = Icons.Outlined.Speed

/** 播放速度可选的快捷档位 */
private val SpeedQuickValues = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** 音调可选的快捷档位（半音） */
private val PitchQuickSemitones = listOf(-3f, -1f, 0f, 1f, 3f)

private const val MinSpeed = 0.25f
private const val MaxSpeed = 3f
private const val SpeedStep = 0.25f

private const val MinSemitone = -12f
private const val MaxSemitone = 12f

private fun formatSpeedLabel(speed: Float): String =
    String.format(Locale.US, "%.2fX", speed)

private fun formatPitchLabel(semitones: Float): String = when {
    semitones > 0.0001f -> String.format(Locale.US, "+%.1f", semitones)
    semitones < -0.0001f -> String.format(Locale.US, "%.1f", semitones)
    else -> "0.00"
}

/**
 * 播放页「倍速与音调」面板。
 *
 * 版式和「三点菜单 → 歌词字体大小」一致：从底部弹出、标题居中在最上方、
 * 下面一条条滑条，没有底部按钮栏（改完即生效，按返回键收起）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingSpeedPitchSheet(onDismiss: () -> Unit) {
    // skipPartiallyExpanded：一打开就是全展开，不会先露半截再手动拉；
    // dragHandle = null：不用自带那条（它被包了 clickable，会变成焦点落点），改由内容顶部自绘
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    DensityScaledModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (NeriDesign.isClean) NeriDesign.tokens.pageHorizontalPadding else 24.dp,
                    vertical = 16.dp
                )
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部小白条：自己画一条，不接 BottomSheet 的 dragHandle ——
            // 自带的那条被包了一层 clickable，在电视上会变成一个可聚焦的落点。
            Box(
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(ringHandleColor(), CircleShape)
            )
            Text(
                text = "倍速与音调",
                style = if (NeriDesign.isClean) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                }
            )
            Spacer(Modifier.height(12.dp))
            NowPlayingSpeedPitchContent()
        }
    }
}

@Composable
internal fun NowPlayingSpeedPitchContent(modifier: Modifier = Modifier) {
    val settings by PlayerManager.audioEffectsSettingsFlow.collectAsStateWithLifecycle()
    val soundState by PlayerManager.playbackSoundStateFlow.collectAsStateWithLifecycle()

    val linked = settings.speedPitchLinked
    val appliedSpeed = if (settings.speedEnabled) soundState.speed else settings.storedSpeed
    val appliedPitch = if (settings.speedEnabled) soundState.pitch else settings.storedPitch

    // 滑条自己维护「手上的值」，只认整数半音 / 0.25 档位。
    // 不直接拿播放器状态回读：状态里存的 pitch 是 2^(n/12) 的浮点，反算回来会带误差，
    // 拖动时看起来就不是整步进了。
    var localSpeed by remember { mutableStateOf<Float?>(null) }
    var localSemitone by remember { mutableStateOf<Float?>(null) }

    // 外部（设置页 / 切换歌曲恢复）改了值，就把手上的值丢掉，跟着走
    LaunchedEffect(appliedSpeed) { localSpeed = null }
    LaunchedEffect(appliedPitch) { localSemitone = null }

    val speed = localSpeed ?: appliedSpeed
    val semitones = (localSemitone ?: pitchToSemitoneOffset(appliedPitch)).roundToInt().toFloat()

    // 勾了「变调跟随倍速」之后，音调跟着速度走，两个值必须一起下发
    val applySpeed: (Float) -> Unit = { raw ->
        val snapped = ((raw / SpeedStep).roundToInt() * SpeedStep)
            .coerceIn(MinSpeed, MaxSpeed)
        localSpeed = snapped
        if (linked) {
            localSemitone = snapped.coerceIn(MinSemitone, MaxSemitone)
            PlayerManager.setPlaybackSpeedAndPitch(
                snapped,
                snapped.coerceIn(MIN_PLAYBACK_PITCH, MAX_PLAYBACK_PITCH)
            )
        } else {
            PlayerManager.setPlaybackSpeed(snapped)
        }
    }
    // 音调严格按 1 半音步进：不管滑条给什么连续值，这里先取整再下发
    val applyPitch: (Float) -> Unit = { raw ->
        val snapped = raw.roundToInt().toFloat().coerceIn(MinSemitone, MaxSemitone)
        localSemitone = snapped
        PlayerManager.setPlaybackPitch(semitoneOffsetToPitch(snapped))
    }

    // 电视：对话框是独立窗口，打开时系统不会把光标交给里面的控件，得主动给一次
    val initialFocus = rememberTvInitialFocus()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SpeedPitchSection(title = "播放速度", valueText = formatSpeedLabel(speed)) {
            MiuixSettingsSlider(
                value = speed.coerceIn(MinSpeed, MaxSpeed),
                onValueChange = applySpeed,
                modifier = Modifier.tvInitialFocus(initialFocus),
                valueRange = MinSpeed..MaxSpeed,
                // 0.25 ~ 3.00 每 0.25 一档，中间 10 个刻度
                steps = 10
            )
            QuickValueRow(
                values = SpeedQuickValues,
                label = ::formatSpeedLabel,
                isSelected = { abs(speed - it) < 0.005f },
                onSelect = applySpeed
            )
        }

        SpeedPitchSection(
            title = "音调",
            valueText = formatSemitones(semitones) + if (linked) "（跟随倍速）" else ""
        ) {
            MiuixSettingsSlider(
                value = semitones.coerceIn(MinSemitone, MaxSemitone),
                onValueChange = applyPitch,
                enabled = !linked,
                valueRange = MinSemitone..MaxSemitone,
                // -12 ~ +12 每 1 一个刻度，中间 23 个
                steps = 23
            )
            if (!linked) {
                QuickValueRow(
                    values = PitchQuickSemitones,
                    label = ::formatPitchLabel,
                    isSelected = { abs(semitones - it) < 0.25f },
                    onSelect = applyPitch
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "变调跟随倍速",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "音调跟着播放速度一起变化",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = linked,
                onCheckedChange = { enabled ->
                    PlayerManager.updateAudioEffects {
                        it.copy(speedPitchLinked = enabled)
                    }
                    PlayerManager.setPlaybackPitch(
                        if (enabled) {
                            speed.coerceIn(MIN_PLAYBACK_PITCH, MAX_PLAYBACK_PITCH)
                        } else {
                            1f
                        }
                    )
                    localSemitone = null
                }
            )
        }
    }
}

@Composable
private fun SpeedPitchSection(
    title: String,
    valueText: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickValueRow(
    values: List<Float>,
    label: (Float) -> String,
    isSelected: (Float) -> Boolean,
    onSelect: (Float) -> Unit
) {
    // 对话框比整屏窄：固定等分会把字挤扁，改成按内容宽度排、放不下自动换行
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            QuickValueChip(
                text = label(value),
                selected = isSelected(value),
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun QuickValueChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = neriRoundedShape(999.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) colors.primary else colors.surfaceContainerHighest,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

/** 面板顶部小白条的颜色：跟着主题走，不抢注意力 */
@Composable
private fun ringHandleColor() = MaterialTheme.colorScheme.onSurfaceVariant
