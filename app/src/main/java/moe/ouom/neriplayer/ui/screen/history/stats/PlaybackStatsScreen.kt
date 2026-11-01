package moe.ouom.neriplayer.ui.screen.history.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ClearAll
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.stats.PlaybackStatsSort
import moe.ouom.neriplayer.data.model.stats.PlaybackStatsPeriod
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.R as AppR
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr

internal enum class StatsSortMode(@param:StringRes val labelRes: Int) {
    PLAY_COUNT(AppR.string.stats_sort_play_count),
    LISTEN_TIME(AppR.string.stats_sort_listen_time),
    RECENT(AppR.string.stats_sort_recent),
    FIRST_PLAYED(AppR.string.stats_sort_first_played)
}

private val TvStatsHeaderHeight = 56.dp

@Composable
fun PlaybackStatsScreen(
    onBack: () -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    offlineMode: Boolean = false
) {
    val mini = LocalMiniPlayerHeight.current
    var selectedPeriod by remember { mutableStateOf(PlaybackStatsPeriod.ALL) }
    var sortMode by remember { mutableStateOf(StatsSortMode.PLAY_COUNT) }
    var showClearDialog by remember { mutableStateOf(false) }
    val queryDay by rememberStatsQueryDay()
    val query = rememberPlaybackStatsQuery(selectedPeriod, PlaybackStatsSort.valueOf(sortMode.name), queryDay.nowMillis)
    val queryDayKey = queryDay.key.takeUnless { selectedPeriod == PlaybackStatsPeriod.ALL }
    val pageRequestState = remember(query, queryDayKey) { mutableStateOf(StatsPageRequest()) }
    var pageRequest by pageRequestState
    val pageState by rememberStatsPage(query, pageRequestState)
    val tablet = usePlaybackStatsTabletLayout(currentWindowWidthDp())
    // 电视：进页面先把光标放到返回按钮上，跟「最近播放」一致，
    // 否则光标直接掉进内容里，返回按钮反而成了很难碰到的位置
    val backFocus = remember { FocusRequester() }
    val isTelevision = LocalIsTelevision.current
    LaunchedEffect(Unit) {
        if (!isTelevision) return@LaunchedEffect
        repeat(6) {
            runCatching { backFocus.requestFocus() }
            withFrameNanos { }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(AppR.string.stats_clear_title)) },
            text = { Text(stringResource(AppR.string.stats_clear_message)) },
            confirmButton = {
                HapticTextButton(onClick = {
                    AppContainer.playbackStatsRepo.clearAll()
                    showClearDialog = false
                }) {
                    Text(stringResource(CoreCommonR.string.action_confirm))
                }
            },
            dismissButton = {
                HapticTextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(CoreCommonR.string.action_cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                PlaybackStatsTopBar(
                    sortMode = sortMode,
                    onSortModeChange = { sortMode = it },
                    onBack = onBack,
                    onClearRequest = { showClearDialog = true },
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it },
                    // 电视上把周期按钮收进标题行，内容区就不再重复一条
                    inlinePeriodSelector = LocalIsTelevision.current,
                    backFocusRequester = backFocus
                )
            }
        ) { innerPadding ->
            PlaybackStatsContent(
                state = if (!pageState.failed && (pageState.loadedQuery != query || pageState.loadedRequest != pageRequest))
                    pageState.copy(loading = true) else pageState,
                request = pageRequest,
                selectedPeriod = selectedPeriod,
                sortMode = sortMode,
                onPeriodSelected = { selectedPeriod = it },
                onPageRequest = { pageRequest = it },
                onSongClick = onSongClick,
                offlineMode = offlineMode,
                miniPlayerHeight = mini,
                tablet = tablet,
                showPeriodSelector = !LocalIsTelevision.current,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackStatsTopBar(
    sortMode: StatsSortMode,
    onSortModeChange: (StatsSortMode) -> Unit,
    onBack: () -> Unit,
    onClearRequest: () -> Unit,
    selectedPeriod: PlaybackStatsPeriod = PlaybackStatsPeriod.ALL,
    onPeriodSelected: (PlaybackStatsPeriod) -> Unit = {},
    inlinePeriodSelector: Boolean = false,
    backFocusRequester: FocusRequester? = null
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val sortButton: @Composable () -> Unit = {
        Box {
            HapticIconButton(onClick = { showSortMenu = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(sortMode.labelRes)
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                StatsSortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(stringResource(mode.labelRes)) },
                        onClick = { onSortModeChange(mode); showSortMenu = false }
                    )
                }
            }
        }
    }
    val clearButton: @Composable () -> Unit = {
        HapticIconButton(onClick = onClearRequest) {
            Icon(
                Icons.Filled.ClearAll,
                contentDescription = stringResource(AppR.string.stats_clear_title)
            )
        }
    }

    if (inlinePeriodSelector) {
        // 电视上不用 TopAppBar：周期按钮要和标题排在同一行，省一条屏幕高度。
        // 整行用显式的 Row 排版，右侧两个图标按钮紧挨着，中间不留任何可聚焦的缝隙。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvStatsHeaderHeight)
                .padding(start = pageSidePaddingOr(4.dp), end = pageSidePaddingOr(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HapticIconButton(
                onClick = onBack,
                modifier = backFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreCommonR.string.action_back)
                )
            }
            Text(
                text = stringResource(AppR.string.stats_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Spacer(Modifier.width(14.dp))
            StatsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = onPeriodSelected,
                modifier = Modifier.wrapContentWidth()
            )
            Spacer(Modifier.weight(1f))
            sortButton()
            clearButton()
        }
        return
    }

    TopAppBar(
        modifier = Modifier.padding(horizontal = pageSidePaddingOr(0.dp)),
        title = { Text(stringResource(AppR.string.stats_title)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        navigationIcon = {
            HapticIconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreCommonR.string.action_back)
                )
            }
        },
        actions = {
            sortButton()
            clearButton()
        }
    )
}
