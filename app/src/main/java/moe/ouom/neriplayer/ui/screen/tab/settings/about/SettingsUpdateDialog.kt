/*
 * NeriPlayer TV fork - update dialog.
 *
 * This file is part of NeriPlayer (GPL-3.0-or-later).
 */
package moe.ouom.neriplayer.ui.screen.tab.settings.about

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.update.AppUpdateRepository
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import java.io.File
import moe.ouom.neriplayer.common.R as CoreCommonR

/**
 * 「有新版本」菜单。
 *
 * 用项目里现成的 [DensityScaledAlertDialog]（Clean / Material 两套主题都吃），不另造弹窗。
 * 按钮只有两个：左边「取消」，右边「更新」→ 下载中显示百分比 → 下载完变「安装」。
 */
@Composable
internal fun SettingsUpdateDialog(
    latest: AppUpdateRepository.Latest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var apk by remember { mutableStateOf<File?>(null) }

    val downloadedFile = apk
    val downloadingThis = downloading

    DensityScaledAlertDialog(
        // 下载中不许点外面关掉，否则进度和文件都白费
        onDismissRequest = { if (!downloadingThis) onDismiss() },
        title = {
            Text(stringResource(CoreCommonR.string.settings_update_available, latest.tag))
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloadingThis) {
                Text(stringResource(CoreCommonR.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloadingThis,
                onClick = {
                    when {
                        downloadedFile != null -> AppUpdateRepository.install(context, downloadedFile)
                        downloadingThis -> Unit
                        else -> {
                            downloading = true
                            progress = 0
                            scope.launch {
                                val result = AppUpdateRepository.download(context, latest.tag) { percent ->
                                    progress = percent
                                }
                                downloading = false
                                if (result != null) {
                                    apk = result
                                } else {
                                    AppFeedback.showToast(
                                        context = context,
                                        message = context.getString(
                                            CoreCommonR.string.settings_update_download_failed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) {
                Text(
                    when {
                        downloadedFile != null ->
                            stringResource(CoreCommonR.string.settings_update_action_install)
                        downloadingThis -> "$progress%"
                        else -> stringResource(CoreCommonR.string.settings_update_action_update)
                    }
                )
            }
        }
    )
}
