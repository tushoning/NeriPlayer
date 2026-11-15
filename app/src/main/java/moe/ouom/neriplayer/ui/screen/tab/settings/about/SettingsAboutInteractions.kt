package moe.ouom.neriplayer.ui.screen.tab.settings.about

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import moe.ouom.neriplayer.update.UpdateCheckController
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.util.platform.tryStartActivity
import moe.ouom.neriplayer.R as AppR

internal class SettingsAboutVersionTapOwner(
    private val tapCount: MutableIntState,
    private val devModeEnabled: Boolean,
    private val onEnableDevMode: () -> Unit,
    private val onMessage: (Int) -> Unit
) {
    /** @return true 表示这一下被开发者模式的连击逻辑吃掉了（此时不该再去查更新）。 */
    fun onVersionClick(): Boolean {
        // 开发者模式已经开着，7 连击没有意义：点一下就是检查更新
        if (devModeEnabled) return false
        val nextCount = tapCount.intValue + 1
        if (nextCount < 7) {
            tapCount.intValue = nextCount
            return false
        }
        tapCount.intValue = 0
        onEnableDevMode()
        onMessage(AppR.string.debug_mode_opened)
        return true
    }
}

private class SettingsAboutPageActions(
    private val context: Context,
    private val resources: Resources,
    tapCount: MutableIntState,
    devModeEnabled: Boolean,
    onDevModeChange: (Boolean) -> Unit,
    private val onInlineMessageChange: (String) -> Unit,
    private val onShowMessage: (String) -> Unit
) {
    private val versionTap = SettingsAboutVersionTapOwner(
        tapCount = tapCount,
        devModeEnabled = devModeEnabled,
        onEnableDevMode = { onDevModeChange(true) },
        onMessage = { messageRes -> onInlineMessageChange(resources.getString(messageRes)) }
    )

    fun onVersionClick(): Boolean = versionTap.onVersionClick()

    fun onCopyValue(value: String) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("settings_build_value", value))
        val copiedMessage = resources.getString(CoreCommonR.string.toast_copied)
        onInlineMessageChange(copiedMessage)
        onShowMessage(copiedMessage)
    }

    fun onOpenGitHubRepo() {
        if (!context.tryStartActivity(Intent(Intent.ACTION_VIEW, "https://github.com/cwuom/NeriPlayer".toUri()))) {
            onShowMessage(resources.getString(CoreCommonR.string.error_no_app_for_action))
        }
    }
}

@Composable
internal fun SettingsAboutPageContent(
    devModeEnabled: Boolean,
    onDevModeChange: (Boolean) -> Unit,
    onInlineMessageChange: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val tapCount = remember { mutableIntStateOf(0) }
    val actions = SettingsAboutPageActions(
        context = context,
        resources = resources,
        tapCount = tapCount,
        devModeEnabled = devModeEnabled,
        onDevModeChange = onDevModeChange,
        onInlineMessageChange = onInlineMessageChange,
        onShowMessage = onShowMessage
    )
    // 点「版本」= 检查更新：弹窗由 NeriApp 里的 UpdateCheckDialogHost 统一挂载，
    // 这里只负责把会话开起来（离开设置页窗口也不会丢）。
    SettingsAboutContent(
        devModeEnabled = devModeEnabled,
        onVersionClick = {
            // 开发者模式那 7 连击期间不查更新，免得连点七次网络请求
            if (!actions.onVersionClick()) UpdateCheckController.checkFromSettings()
        },
        onCopyValue = actions::onCopyValue,
        onOpenGitHubRepo = actions::onOpenGitHubRepo
    )
}
