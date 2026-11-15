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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.update.AppUpdateRepository
import moe.ouom.neriplayer.update.UpdateCheckController
import moe.ouom.neriplayer.update.UpdateCheckUiState
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog
import moe.ouom.neriplayer.ui.feedback.AppFeedback
import java.io.File
import moe.ouom.neriplayer.common.R as CoreCommonR

/**
 * 「检查更新」弹窗，设置页点版本和启动时的一天一次检查共用。
 *
 * 用项目里现成的 [DensityScaledAlertDialog]（Clean / Material 两套主题都吃），不另造弹窗。
 * 固定两个按钮：左边「关闭」，右边一颗随 [state] 变化的主按钮：
 * - 检查中 → 「检查更新」置灰
 * - 暂无新版本 / 检查失败 → 「检查更新」，点了重新查
 * - 有新版本 → 「下载更新」→ 百分比 → 下载完变「安装」
 */
@Composable
internal fun UpdateCheckDialog(
    state: UpdateCheckUiState,
    onCheckAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest: AppUpdateRepository.Latest? =
        (state as? UpdateCheckUiState.Available)?.latest

    // 下载状态按版本号分开记：重新检查到另一个版本时不能沿用上一个的进度和文件
    var downloading by remember(latest?.tag) { mutableStateOf(false) }
    var progress by remember(latest?.tag) { mutableIntStateOf(0) }
    var apk by remember(latest?.tag) { mutableStateOf<File?>(null) }

    val downloadedFile = apk
    val downloadingThis = downloading
    val checking = state == UpdateCheckUiState.Checking

    DensityScaledAlertDialog(
        // 下载中不许点外面关掉，否则进度和文件都白费
        onDismissRequest = { if (!downloadingThis) onDismiss() },
        title = { Text(state.titleText(latest)) },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloadingThis) {
                Text(stringResource(CoreCommonR.string.settings_update_action_close))
            }
        },
        confirmButton = {
            if (latest == null) {
                TextButton(enabled = !checking, onClick = onCheckAgain) {
                    Text(stringResource(CoreCommonR.string.settings_update_action_check))
                }
            } else {
                TextButton(
                    enabled = !downloadingThis,
                    onClick = {
                        when {
                            downloadedFile != null ->
                                AppUpdateRepository.install(context, downloadedFile)
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
                            else -> stringResource(CoreCommonR.string.settings_update_action_download)
                        }
                    )
                }
            }
        }
    )
}

/**
 * 全局更新弹窗宿主，挂在 `NeriApp` 根部。
 *
 * 设置页点「版本」和启动时的一天一次检查都只改 [UpdateCheckController.dialogState]，
 * 弹窗本身由这里统一渲染 —— 所以离开设置页、切换标签都不会把窗口弄丢。
 *
 * @param blocked 启动期其它全局弹窗（USB 后台权限、流量风险）**正在显示**时先让位，
 *   避免两个窗口同时抢焦点；它们处理完自然恢复显示。
 *   注意别传 `NeriApp` 的 `canShowGlobalDialogs` —— 那个值在 `MainActivity` 里算的是
 *   「外部启动弹窗可用 && 当前有全局弹窗」，常态是 false，传进来这个窗口就永远不出现。
 */
@Composable
internal fun UpdateCheckDialogHost(blocked: Boolean) {
    val state by UpdateCheckController.dialogState.collectAsStateWithLifecycle()
    val current = state
    if (current == null || blocked) return
    UpdateCheckDialog(
        state = current,
        onCheckAgain = UpdateCheckController::checkFromSettings,
        onDismiss = UpdateCheckController::dismiss
    )
}

@Composable
private fun UpdateCheckUiState.titleText(latest: AppUpdateRepository.Latest?): String = when (this) {
    UpdateCheckUiState.Checking -> stringResource(CoreCommonR.string.settings_update_checking)
    UpdateCheckUiState.UpToDate -> stringResource(CoreCommonR.string.settings_update_none)
    UpdateCheckUiState.Failed -> stringResource(CoreCommonR.string.settings_update_check_failed)
    is UpdateCheckUiState.Available -> stringResource(
        CoreCommonR.string.settings_update_available,
        latest?.tag.orEmpty()
    )
}
