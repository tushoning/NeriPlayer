package moe.ouom.neriplayer.ui.component.playback

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
 * File: moe.ouom.neriplayer.ui.component/SleepTimerDialog
 * Created: 2026/1/6
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.playback.SleepTimerMode
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledModalBottomSheet
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsSlider
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.R as AppR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit
) {
    val timerState by PlayerManager.sleepTimerManager.timerState.collectAsStateWithLifecycle()
    val sleepTimerFinishCurrentOnExpiry by AppContainer.settingsRepo
        .sleepTimerFinishCurrentOnExpiryFlow
        .collectAsStateWithLifecycle(initialValue = false)
    var sliderValue by remember { mutableFloatStateOf(30f) }
    // 一打开就全展开；小白条由内容顶部自绘（自带那条会变成焦点落点）
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 版式和「三点菜单 → 歌词字体大小」一致：底部弹出、标题居中在最上方，
    // 内容一栏从上往下排，没有底部按钮栏（关闭靠返回键 / 点外面）。
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
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
            Text(
                text = stringResource(CoreCommonR.string.sleep_timer_title),
                style = if (NeriDesign.isClean) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 当前状态显示
                if (timerState.isActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (NeriDesign.isClean) NeriDesign.tokens.cardShape else CardDefaults.shape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (NeriDesign.isClean) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(CoreCommonR.string.sleep_timer_running),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            when (timerState.mode) {
                                SleepTimerMode.COUNTDOWN,
                                SleepTimerMode.COUNTDOWN_FINISH_CURRENT -> {
                                    Text(
                                        text = PlayerManager.sleepTimerManager.formatRemainingTime(),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    if (timerState.mode == SleepTimerMode.COUNTDOWN_FINISH_CURRENT) {
                                        Text(
                                            text = stringResource(
                                                CoreCommonR.string.sleep_timer_countdown_finish_current
                                            ),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                SleepTimerMode.FINISH_CURRENT -> {
                                    Text(
                                        text = stringResource(CoreCommonR.string.sleep_timer_finish_current),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                SleepTimerMode.FINISH_PLAYLIST -> {
                                    Text(
                                        text = stringResource(CoreCommonR.string.sleep_timer_finish_playlist),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        PlayerManager.sleepTimerManager.cancel()
                        onDismiss()
                    }) {
                        Text(stringResource(CoreCommonR.string.sleep_timer_cancel))
                    }
                }

                // 倒计时滑块
                Text(
                    text = stringResource(CoreCommonR.string.sleep_timer_countdown),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (NeriDesign.isClean) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Text(
                    text = pluralStringResource(
                        AppR.plurals.sleep_timer_minutes,
                        sliderValue.toInt(),
                        sliderValue.toInt()
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // 和播放页「倍速与音调」用同一条滑条：简洁风格是白圆点 + 细轨，
                // 换主题风格时外观自动跟着变（见 MiuixSettingsSlider 里的两套实现）
                MiuixSettingsSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = ((it / 5f).toInt() * 5f).toFloat() },
                    valueRange = 5f..120f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        PlayerManager.sleepTimerManager.startCountdown(
                            minutes = sliderValue.toInt(),
                            finishCurrentOnExpiry = sleepTimerFinishCurrentOnExpiry
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(CoreCommonR.string.sleep_timer_start_countdown))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 其他模式
                Text(
                    text = stringResource(CoreCommonR.string.sleep_timer_other_modes),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (NeriDesign.isClean) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                OutlinedButton(
                    onClick = {
                        PlayerManager.sleepTimerManager.startFinishCurrent()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(CoreCommonR.string.sleep_timer_finish_current))
                }

                OutlinedButton(
                    onClick = {
                        PlayerManager.sleepTimerManager.startFinishPlaylist()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(CoreCommonR.string.sleep_timer_finish_playlist))
                }
            }
        }
    }
}
