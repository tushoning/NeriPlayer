package moe.ouom.neriplayer.ui.screen.tab.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision

/**
 * Set while a pull is in progress. The collapsing top bar sits outside the refresh gesture in
 * the nested-scroll chain, so it must step aside or it would swallow the upward drag that
 * pushes the indicator back and cancels the refresh.
 */
@Stable
internal class LibraryPullGate {
    var pulling by mutableStateOf(false)
}

internal val LocalLibraryPullGate = staticCompositionLocalOf<LibraryPullGate?> { null }

/**
 * Pull-to-refresh for remote library pages, replacing the old toolbar refresh button.
 * [enabled] only gates the gesture, so toggling it never remounts the list underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPullToRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()
    val gate = LocalLibraryPullGate.current
    // 电视上没有手指下拉这个动作，方向键挪焦点时反而会误触（焦点一往下就刷新）。
    // 电视上的刷新入口是「在当前标签上再按一次确认键」，见 LibraryMainTabs。
    val gestureEnabled = enabled && !LocalIsTelevision.current
    val latestRefreshing by rememberUpdatedState(refreshing)
    LaunchedEffect(state, gate) {
        snapshotFlow { state.distanceFraction > 0f && !latestRefreshing }.collect { gate?.pulling = it }
    }
    DisposableEffect(gate) {
        onDispose { gate?.pulling = false }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = refreshing,
                state = state,
                enabled = gestureEnabled,
                onRefresh = onRefresh
            )
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
