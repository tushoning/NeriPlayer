/*
 * NeriPlayer TV fork - update check session.
 *
 * This file is part of NeriPlayer (GPL-3.0-or-later).
 */
package moe.ouom.neriplayer.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.BuildConfig
import java.time.LocalDate

/**
 * 「检查更新」弹窗当前要展示的内容。
 *
 * `null`（[UpdateCheckController.dialogState] 的初始值）表示不显示弹窗。
 */
internal sealed interface UpdateCheckUiState {
    /** 正在请求 GitHub Release；从设置页点进来时立刻是这个状态。 */
    data object Checking : UpdateCheckUiState

    /** 检查成功，但没有比当前安装版本更新的 Release。 */
    data object UpToDate : UpdateCheckUiState

    /** 请求失败（无网络、镜像全挂、超时）。 */
    data object Failed : UpdateCheckUiState

    /** 查到新版本，可以下载。 */
    data class Available(val latest: AppUpdateRepository.Latest) : UpdateCheckUiState
}

/** [UpdateCheckController.checkOnLaunch] 的结果，用来决定启动时要不要提示用户。 */
internal enum class LaunchUpdateCheckResult {
    /** 查到新版本，弹窗已经自己顶上来了 */
    Updated,

    /** 确认已是最新版本 */
    UpToDate,

    /** 请求失败，静默处理 */
    Failed,

    /** 今天已经查过，或已经有别的弹窗在用，直接跳过 */
    Skipped
}

/**
 * 更新检查的全局会话。
 *
 * 设置页点「版本」和启动时的一天一次检查都走这里，弹窗由 `NeriApp` 统一挂载
 * （设置页自己不再持有弹窗状态，否则离开设置页窗口就没了）。
 *
 * 一次检查同一时刻只会有一个：用自增 [token] 丢弃过期结果，不用 `Job.cancel()` ——
 * 取消会让 `runCatching` 把 `CancellationException` 一起吞掉，旧协程会继续往下跑。
 */
internal object UpdateCheckController {

    private const val PREFS_NAME = "app_update_check"
    private const val KEY_LAST_CHECK_DAY = "last_check_epoch_day"

    private val _dialogState = MutableStateFlow<UpdateCheckUiState?>(null)
    val dialogState: StateFlow<UpdateCheckUiState?> = _dialogState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 每次发起检查自增，结果回来时对不上就丢弃 */
    private var token = 0

    /**
     * 设置页点「版本」：立刻把弹窗顶出来显示「更新检查中」，再在后台请求。
     */
    fun checkFromSettings() {
        val myToken = ++token
        _dialogState.value = UpdateCheckUiState.Checking
        scope.launch {
            val found = runCatching { AppUpdateRepository.fetchLatest() }.getOrNull()
            if (myToken != token) return@launch
            // 检查期间用户可能已经点了「关闭」，那就别再把窗口顶出来
            if (_dialogState.value == null) return@launch
            _dialogState.value = found.toUiState()
        }
    }

    /**
     * 启动时调用：同一自然日只查一次。
     *
     * 只有查到新版本才自己弹窗；其余情况把结果交回调用方（去 Toast 或静默）。
     * 「今天查过了」的标记在发请求之前就写下去 —— 网络失败也占掉今天的额度，
     * 否则开机没网时会每次冷启动都重试一遍。
     */
    suspend fun checkOnLaunch(context: Context): LaunchUpdateCheckResult {
        if (_dialogState.value != null) return LaunchUpdateCheckResult.Skipped
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong(KEY_LAST_CHECK_DAY, Long.MIN_VALUE) == today) {
            return LaunchUpdateCheckResult.Skipped
        }
        prefs.edit().putLong(KEY_LAST_CHECK_DAY, today).apply()

        val myToken = ++token
        val found = runCatching { AppUpdateRepository.fetchLatest() }.getOrNull()
        if (myToken != token) return LaunchUpdateCheckResult.Skipped
        return when (val state = found.toUiState()) {
            is UpdateCheckUiState.Available -> {
                _dialogState.value = state
                LaunchUpdateCheckResult.Updated
            }
            UpdateCheckUiState.UpToDate -> LaunchUpdateCheckResult.UpToDate
            else -> LaunchUpdateCheckResult.Failed
        }
    }

    fun dismiss() {
        _dialogState.value = null
    }

    private fun AppUpdateRepository.Latest?.toUiState(): UpdateCheckUiState = when {
        this == null -> UpdateCheckUiState.Failed
        versionCode <= BuildConfig.VERSION_CODE -> UpdateCheckUiState.UpToDate
        else -> UpdateCheckUiState.Available(this)
    }
}
