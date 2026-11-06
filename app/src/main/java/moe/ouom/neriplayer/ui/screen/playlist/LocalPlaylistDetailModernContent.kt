package moe.ouom.neriplayer.ui.screen.playlist

import moe.ouom.neriplayer.R as AppR

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
 * File: moe.ouom.neriplayer.ui.screen.playlist/LocalPlaylistDetailScreen
 * Updated: 2026/3/23
 */

import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import moe.ouom.neriplayer.ui.tv.rememberProgressiveListReveal
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import moe.ouom.neriplayer.data.identity.identity
import moe.ouom.neriplayer.data.identity.stableKey
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.tvScrollToTopOnUp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.download.GlobalDownloadManager
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.local.playlist.system.LocalFilesPlaylist
import moe.ouom.neriplayer.data.local.media.LocalMediaSupport
import moe.ouom.neriplayer.data.local.playlist.launchLocalPlaylistMutation
import moe.ouom.neriplayer.data.local.playlist.runLocalPlaylistMutationSafely
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.data.local.media.isLocalSong
import moe.ouom.neriplayer.data.model.stableKey
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import moe.ouom.neriplayer.ui.component.download.BatchDownloadManagerSheet
import moe.ouom.neriplayer.ui.component.download.DownloadedSongDeleteProgressCard
import moe.ouom.neriplayer.ui.component.download.isDownloadedSongDeletionRunning
import moe.ouom.neriplayer.ui.component.download.shouldShowDownloadedSongDeleteProgress
import moe.ouom.neriplayer.ui.component.playlist.NeriSongRow
import moe.ouom.neriplayer.ui.component.playlist.PlaylistChromeTopBar
import moe.ouom.neriplayer.ui.component.playlist.PlaylistExportSheet
import moe.ouom.neriplayer.ui.component.playlist.PlaylistSelectionActions
import moe.ouom.neriplayer.ui.screen.playlist.curation.LocalCurationEntryAction
import moe.ouom.neriplayer.ui.screen.playlist.curation.LocalCurationSheet
import moe.ouom.neriplayer.ui.screen.playlist.insert.PlaylistInsertDialog
import moe.ouom.neriplayer.ui.screen.playlist.insert.PlaylistInsertPreview
import moe.ouom.neriplayer.ui.screen.playlist.insert.isPlaylistInsertPreviewCurrent
import moe.ouom.neriplayer.ui.screen.playlist.reorder.LocalPlaylistReorderableItem
import moe.ouom.neriplayer.ui.screen.playlist.reorder.localPlaylistDetectReorder
import moe.ouom.neriplayer.ui.screen.playlist.reorder.localPlaylistReorderable
import moe.ouom.neriplayer.ui.component.playlist.showPlaylistBatchExportAddedResult
import moe.ouom.neriplayer.ui.component.playlist.showPlaylistBatchExportCreatedResult
import moe.ouom.neriplayer.ui.component.playlist.showPlaylistDeleteResultGlobally
import moe.ouom.neriplayer.ui.component.local.LocalSongDetailsDialog
import moe.ouom.neriplayer.ui.component.local.LocalSongSyncConfirmDialog
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.ui.screen.playback.shouldConfirmFavoriteSync
import moe.ouom.neriplayer.ui.component.download.SongDownloadSubtitle
import moe.ouom.neriplayer.ui.feedback.NeriSnackbarHost
import moe.ouom.neriplayer.ui.feedback.showNeriSnackbar
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.SongIdentity
import moe.ouom.neriplayer.ui.haptic.HapticFloatingActionButton
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.util.format.formatDuration
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import kotlin.time.Duration.Companion.milliseconds


@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    DelicateCoroutinesApi::class
)
internal fun LocalPlaylistDetailModernContent(
    contentScope: LocalPlaylistDetailModernContentScope
) {
    with(contentScope) {
        // 分帧组合：首帧只组合页头，下一帧再填歌曲行，把进页面的峰值拆成两半。
        // 实测进页面那一帧原本要 99ms 重组 + 110ms 测量，远超 16.7ms 的帧预算。
        val revealCount = rememberProgressiveListReveal()
        val currentContentScope by rememberUpdatedState(contentScope)
        SideEffect {
            reorderState.updatePresentedOrder { key ->
                val songIndex = displayedSongs.indexOfFirst { it.stableKey() == key }
                if (songIndex < 0) -1 else resolveLocalPlaylistSongListIndex(
                    songIndex, metadataProcessingVisible
                )
            }
        }
        val deleteProgress by vm.downloadedSongDeleteProgress.collectAsStateWithLifecycle()
        val deleteFailureDismissed by
        vm.downloadedSongDeleteFailureDismissed.collectAsStateWithLifecycle()
        var deletingSongCount by remember(playlistId) { mutableIntStateOf(0) }
        var showInsertDialog by remember(playlistId) { mutableStateOf(false) }
        var insertInProgress by remember(playlistId) { mutableStateOf(false) }
        var curationSongs by remember(playlistId) { mutableStateOf<List<SongItem>?>(null) }
        val curatableTabSongs = remember(tabSongs) { tabSongs.filter { it.isLocalSong() } }
        val selectedDownloadableSongs = remember(selectedSongsForAction) {
            playlistDownloadCandidates(selectedSongsForAction)
        }
        LaunchedEffect(selectionMode, canReorderCurrentSongs) {
            if (!selectionMode || !canReorderCurrentSongs) showInsertDialog = false
        }

        suspend fun scrollToInsertedSongs(newOrder: List<SongIdentity>, movedKeys: Set<String>) {
            val ready = withTimeoutOrNull(PLAYLIST_INSERT_SCROLL_SYNC_TIMEOUT_MS) {
                snapshotFlow {
                    val current = currentContentScope
                    current.playlistId == playlistId &&
                        current.playlist.songs.map { it.identity() } == newOrder &&
                        current.localSongs.map { it.identity() } == newOrder
                }.first { it }
            }
            if (ready != true) return
            // 等新顺序完成布局，再按稳定身份定位，搜索过滤和固定头部都会改变列表索引
            withFrameNanos { }
            val current = currentContentScope
            if (current.playlistId != playlistId ||
                !current.canReorderCurrentSongs || current.blockSync ||
                repo.playlists.value.firstOrNull { it.id == playlistId }
                    ?.songs?.map { it.identity() } != newOrder
            ) return
            val songIndex = current.displayedSongs.indexOfFirst { it.stableKey() in movedKeys }
            if (songIndex >= 0) {
                current.reorderState.listState.animateScrollToItem(
                    resolveLocalPlaylistSongListIndex(
                        songIndex = songIndex,
                        metadataProcessingVisible = current.metadataProcessingVisible
                    )
                )
            }
        }

        fun showInsertSuccess(
            preview: PlaylistInsertPreview,
            originalOrder: List<SongIdentity>,
            newOrder: List<SongIdentity>
        ) {
            scope.launch feedback@{
                val scrollJob = launch {
                    scrollToInsertedSongs(newOrder, preview.movedKeys.toSet())
                }
                // 每次成功提示都对应独立撤销快照，不能按相同文案去重
                snackbarHostState.currentSnackbarData?.dismiss()
                val action = snackbarHostState.showSnackbar(
                    message = composeResources.getString(
                        AppR.string.playlist_insert_success,
                        preview.startPosition
                    ),
                    actionLabel = composeResources.getString(CoreCommonR.string.playlist_batch_export_undo),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                if (action != SnackbarResult.ActionPerformed) return@feedback
                scrollJob.cancel()
                if (insertInProgress || blockSync || currentContentScope.playlistId != playlistId) {
                    snackbarHostState.showNeriSnackbar(
                        composeResources.getString(AppR.string.playlist_insert_undo_stale)
                    )
                    return@feedback
                }
                insertInProgress = true
                val undo = try {
                    runLocalPlaylistMutationSafely("undoPlaylistInsert") {
                        repo.reorderSongs(
                            playlistId = playlistId,
                            newOrder = originalOrder,
                            expectedOrder = newOrder
                        )
                    }
                } finally {
                    insertInProgress = false
                }
                val undoMessage = when {
                    undo.isFailure -> AppR.string.playlist_insert_undo_failed
                    undo.getOrNull() != true -> AppR.string.playlist_insert_undo_stale
                    else -> AppR.string.playlist_insert_undone
                }
                snackbarHostState.showNeriSnackbar(composeResources.getString(undoMessage))
            }
        }

        val deletionInProgress = deletingSongCount > 0 ||
                isDownloadedSongDeletionRunning(deleteProgress)
        PlaylistModernVisualColorsProvider(
            coverUrl = headerCover,
            offlineMode = offlineMode
        ) {
            val playlistChromeColor = rememberPlaylistModernHeroBackgroundColor(
                coverUrl = headerCover,
                offlineMode = offlineMode
            )
            val density = LocalDensity.current
            val searchVisible = shouldShowPlaylistSearch(
                showSearch = showSearch,
                selectionMode = selectionMode
            )
            val searchVisibilityProgress = playlistModernSearchVisibilityProgress(
                searchVisible = searchVisible,
                label = "playlist-search-visibility"
            )
            val searchSlotProgress = searchVisibilityProgress.coerceIn(0f, 1f)
            val searchVisibilityEased = FastOutSlowInEasing.transform(searchSlotProgress)
            val searchDockedRevealProgress by remember(
                reorderState.listState,
                density
            ) {
                derivedStateOf {
                    resolvePlaylistDockedSearchRevealProgress(
                        firstVisibleItemIndex = reorderState.listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffsetPx =
                            reorderState.listState.firstVisibleItemScrollOffset,
                        revealDistancePx = with(density) {
                            PlaylistModernDockedSearchSlotHeight.roundToPx()
                        }
                    )
                }
            }
            val searchDockedVisualProgress = FastOutSlowInEasing.transform(
                searchDockedRevealProgress
            )
            val dockedSearchProgress = resolvePlaylistDockedSearchSlotProgress(
                searchVisibilityProgress = searchSlotProgress,
                dockedRevealProgress = searchDockedRevealProgress
            )
            val searchSlotVisible = shouldComposePlaylistSearchSlot(
                searchVisible = searchVisible,
                visibilityProgress = dockedSearchProgress
            )
            val searchSlotHeight = interpolatePlaylistDp(
                start = 0.dp,
                end = PlaylistModernDockedSearchSlotHeight,
                fraction = dockedSearchProgress
            )
            val searchSlotAlpha = FastOutSlowInEasing.transform(dockedSearchProgress)
            val playlistHeroHeight = interpolatePlaylistDp(
                start = PlaylistModernHeroHeight,
                end = PlaylistModernHeroSearchHeight,
                fraction = searchVisibilityEased
            )
            val playlistChromeCollapseProgress by remember(
                reorderState.listState,
                density,
                playlistHeroHeight
            ) {
                derivedStateOf {
                    resolvePlaylistChromeCollapseProgress(
                        firstVisibleItemIndex = reorderState.listState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffsetPx =
                            reorderState.listState.firstVisibleItemScrollOffset,
                        expandedHeroHeightPx = reorderState.listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == 0 }?.size ?: with(density) {
                            playlistHeroHeight.roundToPx()
                        }
                    )
                }
            }
            val playlistChromeVisualProgress =
                FastOutSlowInEasing.transform(playlistChromeCollapseProgress)
            val headerSearchAlpha = resolvePlaylistHeaderSearchAlpha(
                searchVisibilityProgress = searchSlotProgress,
                chromeCollapseProgress = playlistChromeCollapseProgress
            )
            val headerSearchVisible = shouldComposePlaylistSearchSlot(
                searchVisible = searchVisible,
                visibilityProgress = headerSearchAlpha
            )
            val playlistTopBarColor = resolvePlaylistTranslucentTopBarColor(
                playlistColor = playlistChromeColor,
                collapseProgress = playlistChromeVisualProgress
            )
            val playlistTopBarContentColor = interpolatePlaylistColor(
                start = resolvePlaylistSolidTopBarContentColor(playlistChromeColor),
                end = playlistModernCollapsedTopBarContentColor(),
                fraction = playlistChromeVisualProgress
            )
            val playlistSelectionTopBarColor = resolvePlaylistSelectionTopBarColor(
                playlistColor = playlistChromeColor,
                collapseProgress = playlistChromeCollapseProgress
            )
            val playlistSelectionTopBarContentColor = resolvePlaylistSelectionTopBarContentColor(
                playlistColor = playlistChromeColor,
                collapsedContentColor = playlistModernCollapsedTopBarContentColor(),
                collapseProgress = playlistChromeCollapseProgress
            )
            val dockedSearchGlassColor = playlistModernDockedSearchGlassColor(
                playlistColor = playlistChromeColor
            )
            val searchFieldFocusInHeader =
                headerSearchVisible && searchDockedRevealProgress < 0.5f
            val searchFieldComposed = headerSearchVisible || searchSlotVisible
            LaunchedEffect(
                showSearch,
                selectionMode,
                autoShowKeyboard,
                searchFieldComposed,
                searchFieldFocusInHeader
            ) {
                if (!searchFieldComposed) return@LaunchedEffect
                val shouldAutoFocus = shouldRequestPlaylistSearchFocus(
                    showSearch,
                    selectionMode,
                    autoShowKeyboard
                )
                val shouldTransferFocus = shouldTransferPlaylistSearchFocus(
                    showSearch = showSearch,
                    selectionMode = selectionMode,
                    searchFieldComposed = searchFieldComposed,
                    searchInputFocused = headerSearchFocused || dockedSearchFocused,
                    searchQuery = searchQuery
                )
                if (!shouldAutoFocus && !shouldTransferFocus) return@LaunchedEffect
                if (shouldAutoFocus) delay(120.milliseconds)
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    NeriSnackbarHost(
                        hostState = snackbarHostState,
                        bottomPadding = LocalMiniPlayerHeight.current
                    )
                },
                topBar = {
                    if (!selectionMode) {
                        val displayName = when {
                            isFavorites -> stringResource(CoreCommonR.string.favorite_my_music)
                            isLocalFilesPlaylist -> stringResource(CoreCommonR.string.local_files)
                            else -> playlist.name
                        }
                        PlaylistChromeTopBar(
                            title = displayName,
                            coverUrl = headerCover,
                            offlineMode = offlineMode,
                            heroHeight = playlistHeroHeight,
                            listState = reorderState.listState,
                            onBack = onBack,
                            actions = {
                                HapticIconButton(onClick = {
                                    val openingSearch = !showSearch
                                    if (!openingSearch) {
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                    showSearch = openingSearch
                                }) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = stringResource(CoreCommonR.string.cd_search_songs)
                                    )
                                }

                                if (hasDownloadManagerEntry) {
                                    HapticIconButton(
                                        onClick = { showDownloadManager = true }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Download,
                                            contentDescription = stringResource(CoreCommonR.string.cd_download_manager)
                                        )
                                    }
                                }

                                if (
                                    isLocalFilesPlaylist &&
                                    selectedLocalFilesTab == LocalFilesSongTab.MANUALLY_ADDED
                                ) {
                                    HapticIconButton(onClick = {
                                        showLocalScanModeDialog = true
                                    }, enabled = !scanPreviewState.isScanning) {
                                        Icon(
                                            Icons.Outlined.LibraryMusic,
                                            contentDescription = stringResource(CoreCommonR.string.download_scan_local)
                                        )
                                    }
                                }
                                if (curatableTabSongs.isNotEmpty()) {
                                    LocalCurationEntryAction(
                                        onClick = { curationSongs = curatableTabSongs }
                                    )
                                }
                                if (isFavorites) {
                                    HapticIconButton(
                                        onClick = { requestNeteaseSync() },
                                        enabled = !syncInProgress
                                    ) {
                                        if (syncInProgress) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Sync,
                                                contentDescription = stringResource(CoreCommonR.string.local_playlist_sync_netease_liked)
                                            )
                                        }
                                    }
                                }

                                if (!isSystemPlaylist) {
                                    HapticIconButton(onClick = {
                                        renameText =
                                            playlistNameFieldValue(playlist.name, maxNameLength)
                                        renameError = null
                                        showRename = true
                                    }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(CoreCommonR.string.local_playlist_rename)
                                        )
                                    }
                                    HapticIconButton(onClick = {
                                        showDeletePlaylistConfirm = true
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(CoreCommonR.string.local_playlist_delete)
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        val displayedSongKeys = displayedSongs.map { it.stableKey() }.toSet()
                        val allSelected = areDisplayedSongKeysSelected(
                            selectedKeys = selectedKeysState.value,
                            displayedKeys = displayedSongKeys
                        )
                        TopAppBar(
                            modifier = Modifier
                                .padding(horizontal = pageSidePaddingOr(0.dp))
                                .tvPlaylistTopBarShape(),
                            title = {
                                Text(
                                    pluralStringResource(
                                        AppR.plurals.common_selected_count,
                                        selectedKeysState.value.size,
                                        selectedKeysState.value.size
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                HapticIconButton(onClick = { exitSelectionMode() }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(CoreCommonR.string.cd_exit_select)
                                    )
                                }
                            },
                            actions = {
                                if (selectedDownloadableSongs.isNotEmpty()) {
                                    HapticIconButton(
                                        onClick = {
                                            val selectedSongs = selectedDownloadableSongs
                                            if (selectedSongs.isNotEmpty()) {
                                                showDownloadManager = true
                                                exitSelectionMode()
                                                GlobalDownloadManager.startBatchDownload(
                                                    context,
                                                    selectedSongs
                                                )
                                            }
                                        },
                                        enabled = !insertInProgress
                                    ) {
                                        Icon(
                                            Icons.Outlined.Download,
                                            contentDescription = stringResource(CoreCommonR.string.cd_download_selected)
                                        )
                                    }
                                }
                                HapticIconButton(
                                    onClick = {
                                        if (selectedKeysState.value.isNotEmpty()) {
                                            showDeleteMultiConfirm = true
                                        }
                                    },
                                    enabled = selectedKeysState.value.isNotEmpty() &&
                                            !insertInProgress &&
                                            !(isLocalFilesPlaylist &&
                                                    selectedLocalFilesTab == LocalFilesSongTab.DOWNLOADED &&
                                                    deletionInProgress)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(CoreCommonR.string.common_delete_selected)
                                    )
                                }
                                PlaylistSelectionActions(
                                    allSelected = allSelected,
                                    onToggleSelectAll = {
                                        selectedKeysState.value = toggleDisplayedSongSelection(
                                            selectedKeys = selectedKeysState.value,
                                            displayedKeys = displayedSongKeys
                                        )
                                    },
                                    canExport = selectedKeysState.value.isNotEmpty(),
                                    onExport = { showExportSheet = true },
                                    canSync = selectedKeysState.value.isNotEmpty() && !syncInProgress,
                                    onSync = { openNeteaseRemotePlaylistPicker() },
                                    canInsert = canReorderCurrentSongs &&
                                            selectedKeysState.value.isNotEmpty() &&
                                            !blockSync && !insertInProgress,
                                    onInsert = if (canReorderCurrentSongs) {
                                        { showInsertDialog = true }
                                    } else {
                                        null
                                    },
                                    canCurateLocalAssets = selectedSongsForAction.any { it.isLocalSong() },
                                    onCurateLocalAssets = if (curatableTabSongs.isNotEmpty()) {
                                        {
                                            curationSongs = selectedSongsForAction.filter { it.isLocalSong() }
                                            exitSelectionMode()
                                        }
                                    } else {
                                        null
                                    }
                                )
                            },
                            windowInsets = WindowInsets.statusBars,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = playlistSelectionTopBarColor,
                                scrolledContainerColor = playlistSelectionTopBarColor,
                                titleContentColor = playlistSelectionTopBarContentColor,
                                navigationIconContentColor = playlistSelectionTopBarContentColor,
                                actionIconContentColor = playlistSelectionTopBarContentColor
                            )
                        )
                    }
                }
            ) { padding ->
                val miniPlayerHeight = LocalMiniPlayerHeight.current
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    if (isLocalFilesPlaylist && shouldShowDownloadedSongDeleteProgress(
                            deleteProgress, deletingSongCount, deleteFailureDismissed
                        )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(playlistTopBarColor)
                        ) {
                            DownloadedSongDeleteProgressCard(
                                progress = deleteProgress,
                                failureDismissed = deleteFailureDismissed,
                                onDismissFailure = vm::dismissDownloadedSongDeleteFailure,
                                requestedSongCount = deletingSongCount,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    if (searchSlotVisible) {
                        PlaylistModernVisualColorsProvider(
                            coverUrl = headerCover,
                            offlineMode = offlineMode
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(searchSlotHeight)
                                    .clipToBounds()
                                    .graphicsLayer {
                                        alpha = searchSlotAlpha
                                    }
                            ) {
                                PlaylistModernStableSearchField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    placeholder = stringResource(CoreCommonR.string.search_playlist),
                                    inputState = searchInputState,
                                    onFocusChanged = { dockedSearchFocused = it },
                                    focusRequester = if (searchFieldFocusInHeader) {
                                        null
                                    } else {
                                        searchFocusRequester
                                    },
                                    dockedProgress = searchDockedVisualProgress,
                                    glassColor = dockedSearchGlassColor,
                                    modifier = Modifier.graphicsLayer {
                                        translationY = with(density) {
                                            ((1f - searchSlotAlpha) * -8.dp.toPx())
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        key(playlistId) {
                            PlaylistModernVisualColorsProvider(
                                coverUrl = headerCover,
                                offlineMode = offlineMode
                            ) {
                                LazyColumn(
                                    state = reorderState.listState,
                                    contentPadding = PaddingValues(
                                        start = pageSidePaddingOr(0.dp),
                                        end = pageSidePaddingOr(0.dp),
                                        bottom = 24.dp + miniPlayerHeight
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .localPlaylistReorderable(reorderState)
                                ) {
                                    item(
                                        key = headerKey,
                                        contentType = "playlist_header"
                                    ) {
                                        LocalPlaylistHeroHeader(
                                            displayName = headerDisplayName,
                                            headerCover = headerCover,
                                            totalDurationText = totalDurationText,
                                            songCount = tabSongs.size,
                                            playCount = playlistPlayCount,
                                            offlineMode = offlineMode,
                                            height = playlistHeroHeight,
                                            actionsRevealProgress = searchVisibilityEased,
                                            // 头图继续铺到「全部播放」那一行下面
                                            topBleed = playlistTopBarBleed(),
                                            backgroundBleed = PlaylistActionSheetBleed,
                                            actions = if (headerSearchVisible) {
                                                {
                                                    Box(
                                                        modifier = Modifier.graphicsLayer {
                                                            alpha = headerSearchAlpha
                                                        }
                                                    ) {
                                                        PlaylistModernHeroSearchField(
                                                            query = searchQuery,
                                                            onQueryChange = { searchQuery = it },
                                                            placeholder = stringResource(CoreCommonR.string.search_playlist),
                                                            inputState = searchInputState,
                                                            onFocusChanged = {
                                                                headerSearchFocused = it
                                                            },
                                                            focusRequester = if (searchFieldFocusInHeader) {
                                                                searchFocusRequester
                                                            } else {
                                                                null
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }

                                    item(
                                        key = LOCAL_PLAYLIST_ACTIONS_KEY,
                                        contentType = "playlist_actions"
                                    ) {
                                        PlaylistModernActionSheet(
                                            coverUrl = headerCover,
                                            offlineMode = offlineMode,
                                            hasCustomBackground = hasCustomBackground,
                                            overHeroImage = true
                                        ) {
                                            Column {
                                                if (isLocalFilesPlaylist) {
                                                    PrimaryTabRow(
                                                        selectedTabIndex = selectedLocalFilesTabIndex,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp),
                                                        containerColor = Color.Transparent,
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Tab(
                                                            selected = selectedLocalFilesTab == LocalFilesSongTab.MANUALLY_ADDED,
                                                            onClick = {
                                                                selectedLocalFilesTabIndex =
                                                                    LocalFilesSongTab.MANUALLY_ADDED.ordinal
                                                                if (selectionMode) exitSelectionMode()
                                                            },
                                                            text = {
                                                                Text(
                                                                    stringResource(CoreCommonR.string.local_files_manual_added)
                                                                )
                                                            }
                                                        )
                                                        Tab(
                                                            selected = selectedLocalFilesTab == LocalFilesSongTab.DOWNLOADED,
                                                            onClick = {
                                                                selectedLocalFilesTabIndex =
                                                                    LocalFilesSongTab.DOWNLOADED.ordinal
                                                                if (selectionMode) exitSelectionMode()
                                                            },
                                                            text = {
                                                                Text(
                                                                    stringResource(CoreCommonR.string.local_files_downloaded)
                                                                )
                                                            }
                                                        )
                                                    }
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                            alpha = 0.5f
                                                        )
                                                    )
                                                }
                                                LocalPlaylistPlaybackActions(
                                                    modifier = Modifier.tvScrollToTopOnUp(reorderState.listState),
                                                    songCount = tabSongs.size,
                                                    shuffleEnabled = shuffleEnabled,
                                                    repeatMode = repeatMode,
                                                    onPlayInOrder = { playPlaylist(shuffle = false) },
                                                    onShufflePlay = { playPlaylist(shuffle = true) },
                                                    onToggleShuffle = {
                                                        PlayerManager.setShuffle(!shuffleEnabled)
                                                    },
                                                    onCycleRepeatMode = {
                                                        PlayerManager.cycleRepeatMode()
                                                    },
                                                    onExportToLocalPlaylist = {
                                                        showExportAllSheet = true
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (metadataProcessingVisible) {
                                        item(
                                            key = LOCAL_PLAYLIST_METADATA_PROCESSING_KEY,
                                            contentType = "local_metadata_processing"
                                        ) {
                                            PlaylistModernListItemSurface(
                                                coverUrl = headerCover,
                                                offlineMode = offlineMode
                                            ) {
                                                LocalMetadataProcessingCard(
                                                    visibleMetadataProcessingState
                                                )
                                            }
                                        }
                                    }

                                    // 列表
                                    itemsIndexed(
                                        // 分批填充：0 行 → 4 行 → 全量，把首屏组合峰值摊成三份
                                        items = if (revealCount.value >= displayedSongs.size) {
                                            displayedSongs
                                        } else {
                                            displayedSongs.take(revealCount.value)
                                        },
                                        key = { _, song -> song.stableKey() },
                                        contentType = { _, _ -> "local_playlist_song" }
                                    ) { revIndex, song ->
                                        val songKey = remember(song) { song.stableKey() }
                                        LocalPlaylistReorderableItem(
                                            state = reorderState,
                                            key = songKey,
                                            animatePlacement = selectionMode && canReorderCurrentSongs
                                        ) { isDragging ->
                                            val rowScale by animateFloatAsState(
                                                targetValue = if (isDragging) 1.02f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "row-scale"
                                            )
                                            val isSelectedSong =
                                                selectionMode && selectedKeysState.value.contains(
                                                    songKey
                                                )
                                            val isFavoriteSong = favoriteSongLookup.contains(song)
                                            val rowContainerColor = if (isSelectedSong) {
                                                MaterialTheme.colorScheme.secondaryContainer.copy(
                                                    alpha = 0.35f
                                                )
                                            } else {
                                                Color.Transparent
                                            }

                                            PlaylistModernListItemSurface(
                                                coverUrl = headerCover,
                                                offlineMode = offlineMode,
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        scaleX = rowScale; scaleY = rowScale
                                                    }
                                            ) {
                                                val isPlayingSong =
                                                    currentSongLookup.contains(song)
                                                val resolveArtworkFallback =
                                                    shouldResolveLocalPlaylistRowArtworkFallback()
                                                // 和其它歌单页共用同一份行组件
                                                val reorderHandle: (@Composable () -> Unit)? =
                                                    if (selectionMode && canReorderCurrentSongs) {
                                                        {
                                                            Box(
                                                                modifier = Modifier
                                                                    .then(
                                                                        if (insertInProgress) {
                                                                            Modifier
                                                                        } else {
                                                                            Modifier.localPlaylistDetectReorder(
                                                                                reorderState,
                                                                                songKey
                                                                            )
                                                                        }
                                                                    )
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.DragHandle,
                                                                    contentDescription = stringResource(
                                                                        CoreCommonR.string.common_drag_handle
                                                                    ),
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        null
                                                    }
                                                NeriSongRow(
                                                    index = revIndex + 1,
                                                    song = song,
                                                    onClick = {
                                                        if (selectionMode) {
                                                            toggleSelect(songKey)
                                                        } else {
                                                            val pos = queueIndexBySongKey[songKey] ?: -1
                                                            if (pos >= 0) onSongClick(tabSongs, pos)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!selectionMode) {
                                                            selectionMode = true
                                                            selectedKeysState.value = setOf(songKey)
                                                        } else {
                                                            toggleSelect(songKey)
                                                        }
                                                    },
                                                    subtitleContent = {
                                                        SongDownloadSubtitle(
                                                            text = song.displayArtist(),
                                                            downloaded = songKey in downloadedSongKeys,
                                                            color = playlistModernListSecondaryContentColor()
                                                        )
                                                    },
                                                    coverContent = {
                                                        LocalPlaylistSongArtwork(
                                                            song = song,
                                                            offlineMode = offlineMode,
                                                            resolveLocalFallback = resolveArtworkFallback,
                                                            downloadPresenceVersion = downloadPresenceVersion,
                                                            allowEmbeddedCoverFallback = resolveArtworkFallback
                                                        )
                                                    },
                                                    offlineMode = offlineMode,
                                                    isCurrentSong = isPlayingSong,
                                                    animatePlayingIndicator = isPlaying,
                                                    // 多选时右端只留拖拽手柄，不再显示时长
                                                    durationText = if (selectionMode) {
                                                        null
                                                    } else {
                                                        formatDuration(song.durationMs)
                                                    },
                                                    selectionMode = selectionMode,
                                                    selected = isSelectedSong,
                                                    rowContainerColor = rowContainerColor,
                                                    onToggleSelect = { toggleSelect(songKey) },
                                                    isFavorite = isFavoriteSong,
                                                    onFavoriteToggle = { target, favorite ->
                                                        toggleSongFavorite(target, favorite)
                                                    },
                                                    onPlayNext = { PlayerManager.addToQueueNext(song) },
                                                    onAddToQueueEnd = { PlayerManager.addToQueueEnd(song) },
                                                    // 页面专属：本地歌曲的详情与分享
                                                    extraMenuTop = { close ->
                                                        if (song.isLocalSong()) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        stringResource(
                                                                            CoreCommonR.string.local_song_open_details
                                                                        )
                                                                    )
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Info,
                                                                        contentDescription = null
                                                                    )
                                                                },
                                                                onClick = {
                                                                    close()
                                                                    detailSong = song
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        stringResource(
                                                                            CoreCommonR.string.action_share
                                                                        )
                                                                    )
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Share,
                                                                        contentDescription = null
                                                                    )
                                                                },
                                                                onClick = {
                                                                    close()
                                                                    scope.launch {
                                                                        val shared = runCatching {
                                                                            LocalMediaSupport.shareSongFile(
                                                                                context,
                                                                                song
                                                                            )
                                                                        }.getOrElse { false }
                                                                        if (!shared) {
                                                                            snackbarHostState.showNeriSnackbar(
                                                                                composeResources.getString(
                                                                                    CoreCommonR.string.local_song_share_failed
                                                                                )
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    },
                                                    trailingContent = reorderHandle,
                                                    snackbarHostState = snackbarHostState
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (currentIndexInDisplay >= 0) {
                            HapticFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        reorderState.listState.animateScrollToItem(
                                            resolveLocalPlaylistPlayingItemIndex(
                                                songIndex = currentIndexInDisplay,
                                                metadataProcessingVisible = metadataProcessingVisible
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(
                                        bottom = 16.dp + miniPlayerHeight,
                                        end = 16.dp
                                    )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    contentDescription = stringResource(CoreCommonR.string.cd_locate_playing)
                                )
                            }
                        }


                    }
                }

                // 删除歌单二次确认
                if (showDeletePlaylistConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeletePlaylistConfirm = false },
                        title = { Text(stringResource(CoreCommonR.string.local_playlist_delete)) },
                        text = { Text(stringResource(CoreCommonR.string.local_playlist_delete_confirm)) },
                        confirmButton = {
                            HapticTextButton(onClick = {
                                showDeletePlaylistConfirm = false
                                vm.delete { result ->
                                    showPlaylistDeleteResultGlobally(
                                        context = context,
                                        repository = repo,
                                        result = result
                                    )
                                    if (result.getOrNull().orEmpty().isNotEmpty()) {
                                        navigateAfterPlaylistDeleted()
                                    }
                                }
                            }) { Text(stringResource(CoreCommonR.string.action_delete)) }
                        },
                        dismissButton = {
                            HapticTextButton(onClick = {
                                showDeletePlaylistConfirm = false
                            }) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                        }
                    )
                }

                // 多选删除确认
                if (showDeleteMultiConfirm) {
                    val count = selectedKeysState.value.size
                    val deletesDownloadedSongs = isLocalFilesPlaylist &&
                            selectedLocalFilesTab == LocalFilesSongTab.DOWNLOADED
                    AlertDialog(
                        onDismissRequest = { showDeleteMultiConfirm = false },
                        title = {
                            Text(
                                stringResource(
                                    if (deletesDownloadedSongs) {
                                        CoreCommonR.string.local_files_delete_downloaded_title
                                    } else {
                                        CoreCommonR.string.local_playlist_delete_songs
                                    }
                                )
                            )
                        },
                        text = {
                            Text(
                                if (deletesDownloadedSongs) {
                                    pluralStringResource(
                                        CoreCommonR.plurals.local_files_delete_downloaded_confirm,
                                        count,
                                        count
                                    )
                                } else {
                                    pluralStringResource(
                                        AppR.plurals.local_playlist_delete_songs_confirm,
                                        count,
                                        count
                                    )
                                }
                            )
                        },
                        confirmButton = {
                            HapticTextButton(
                                enabled = !deletesDownloadedSongs || !deletionInProgress,
                                onClick = {
                                    if (deletesDownloadedSongs) {
                                        val songsToDelete = selectedDownloadedSongsForAction
                                        deletingSongCount = songsToDelete.size
                                        showDeleteMultiConfirm = false
                                        exitSelectionMode()
                                        vm.deleteDownloadedSongs(songsToDelete) { result ->
                                            deletingSongCount = 0
                                            scope.launch {
                                                val message = when {
                                                    result.physicalCleanupPending -> {
                                                        context.resources.getQuantityString(
                                                            AppR.plurals.local_files_delete_downloaded_cleanup_pending,
                                                            result.deletedCount + result.notDeletedCount,
                                                            result.deletedCount + result.notDeletedCount
                                                        )
                                                    }

                                                    result.deletedCount > 0 && result.notDeletedCount == 0 -> {
                                                        context.resources.getQuantityString(
                                                            AppR.plurals.local_files_delete_downloaded_success,
                                                            result.deletedCount,
                                                            result.deletedCount
                                                        )
                                                    }

                                                    result.deletedCount > 0 -> {
                                                        composeResources.getQuantityString(
                                                            CoreCommonR.plurals.local_files_delete_downloaded_partial,
                                                            result.deletedCount,
                                                            result.deletedCount,
                                                            result.notDeletedCount
                                                        )
                                                    }

                                                    else -> {
                                                        composeResources.getString(
                                                            CoreCommonR.string.local_files_delete_downloaded_failed
                                                        )
                                                    }
                                                }
                                                snackbarHostState.showNeriSnackbar(message)
                                            }
                                        }
                                        return@HapticTextButton
                                    }
                                    val previousSongs = localSongs.toList()
                                    val selectedKeys = selectedKeysState.value
                                    val removeAll = localSongs.isNotEmpty() &&
                                            selectedKeys.size == localSongs.size &&
                                            localSongs.all { it.stableKey() in selectedKeys }
                                    var songsToRemove = emptyList<SongItem>()
                                    val expectedSongs = if (removeAll) {
                                        emptyList()
                                    } else {
                                        songsToRemove = localSongs.filter {
                                            it.stableKey() in selectedKeys
                                        }
                                        val removeIdentities =
                                            songsToRemove.map { it.identity() }.toSet()
                                        localSongs.filterNot { it.identity() in removeIdentities }
                                    }
                                    pendingOrderIdentities = expectedSongs.map { it.identity() }
                                    blockSync = true

                                    localSongs.clear()
                                    localSongs.addAll(expectedSongs)
                                    showDeleteMultiConfirm = false
                                    exitSelectionMode()

                                    if (removeAll) {
                                        vm.clearSongs { result ->
                                            handleLocalSongDeleteResult(previousSongs, result)
                                        }
                                    } else {
                                        vm.removeSongs(songsToRemove) { result ->
                                            handleLocalSongDeleteResult(previousSongs, result)
                                        }
                                    }
                                }) {
                                Text(
                                    stringResource(
                                        CoreCommonR.string.local_playlist_delete_count,
                                        count
                                    )
                                )
                            }
                        },
                        dismissButton = {
                            HapticTextButton(onClick = {
                                showDeleteMultiConfirm = false
                            }) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                        }
                    )
                }

                curationSongs?.let { songs ->
                    LocalCurationSheet(
                        songs = songs,
                        onDismissRequest = { curationSongs = null }
                    )
                }

                // 多选导出
                if (showExportSheet) {
                    PlaylistExportSheet(
                        title = stringResource(CoreCommonR.string.local_playlist_export_to),
                        playlists = allPlaylists.filter {
                            it.id != playlist.id && !LocalFilesPlaylist.isSystemPlaylist(
                                it,
                                context
                            )
                        },
                        selectedCount = selectedKeysState.value.size,
                        onDismissRequest = { showExportSheet = false },
                        onCreateAndExport = { name ->
                            val songs = selectedStoredLocalSongsForExport(
                                storedSongs = tabSongs,
                                selectedKeys = selectedKeysState.value
                            )
                            launchWithLocalSyncWarning(
                                songs = songs,
                                actionLabel = CoreCommonR.string.playlist_add_to
                            ) {
                                scope.launchLocalPlaylistMutation(
                                    operation = "createPlaylistFromLocalPlaylist",
                                    onResult = { result ->
                                        scope.showPlaylistBatchExportCreatedResult(
                                            context = context,
                                            snackbarHostState = snackbarHostState,
                                            repository = repo,
                                            result = result
                                        )
                                    }
                                ) {
                                    repo.createPlaylistWithPreparedSongs(name, songs)
                                }
                                exitSelectionMode()
                            }
                        },
                        onExportToPlaylist = { target ->
                            val songs = selectedStoredLocalSongsForExport(
                                storedSongs = tabSongs,
                                selectedKeys = selectedKeysState.value
                            )
                            launchWithLocalSyncWarning(
                                songs = songs,
                                actionLabel = CoreCommonR.string.playlist_add_to,
                                warnForLocalSync = !FavoritesPlaylist.isSystemPlaylist(target, context) ||
                                    shouldConfirmFavoriteSync(songs, context)
                            ) {
                                scope.launchLocalPlaylistMutation(
                                    operation = "exportSongsFromLocalPlaylist",
                                    onResult = { result ->
                                        scope.showPlaylistBatchExportAddedResult(
                                            context = context,
                                            snackbarHostState = snackbarHostState,
                                            repository = repo,
                                            targetPlaylistId = target.id,
                                            targetPlaylistName = target.name,
                                            result = result
                                        )
                                    }
                                ) {
                                    repo.addPreparedSongsToPlaylistWithResult(target.id, songs)
                                }
                                exitSelectionMode()
                            }
                        }
                    )
                }

                if (showExportAllSheet) {
                    PlaylistExportSheet(
                        title = stringResource(CoreCommonR.string.playlist_export_to_local),
                        playlists = allPlaylists.filter {
                            it.id != playlist.id && !LocalFilesPlaylist.isSystemPlaylist(
                                it,
                                context
                            )
                        },
                        selectedCount = tabSongs.size,
                        onDismissRequest = { showExportAllSheet = false },
                        onCreateAndExport = { name ->
                            val songs = tabSongs
                            launchWithLocalSyncWarning(
                                songs = songs,
                                actionLabel = CoreCommonR.string.playlist_add_to
                            ) {
                                scope.launchLocalPlaylistMutation(
                                    operation = "createPlaylistFromLocalPlaylistAll",
                                    onResult = { result ->
                                        scope.showPlaylistBatchExportCreatedResult(
                                            context = context,
                                            snackbarHostState = snackbarHostState,
                                            repository = repo,
                                            result = result
                                        )
                                    }
                                ) {
                                    repo.createPlaylistWithPreparedSongs(name, songs)
                                }
                                showExportAllSheet = false
                            }
                        },
                        onExportToPlaylist = { target ->
                            val songs = tabSongs
                            launchWithLocalSyncWarning(
                                songs = songs,
                                actionLabel = CoreCommonR.string.playlist_add_to,
                                warnForLocalSync = !FavoritesPlaylist.isSystemPlaylist(target, context) ||
                                    shouldConfirmFavoriteSync(songs, context)
                            ) {
                                scope.launchLocalPlaylistMutation(
                                    operation = "exportAllSongsFromLocalPlaylist",
                                    onResult = { result ->
                                        scope.showPlaylistBatchExportAddedResult(
                                            context = context,
                                            snackbarHostState = snackbarHostState,
                                            repository = repo,
                                            targetPlaylistId = target.id,
                                            targetPlaylistName = target.name,
                                            result = result
                                        )
                                    }
                                ) {
                                    repo.addPreparedSongsToPlaylistWithResult(target.id, songs)
                                }
                                showExportAllSheet = false
                            }
                        }
                    )
                }

                if (showNeteaseRemotePlaylistPicker) {
                    NeteaseRemotePlaylistPickerDialog(
                        playlists = neteaseRemotePlaylists,
                        loading = neteaseRemotePlaylistsLoading,
                        errorMessage = neteaseRemotePlaylistsError,
                        onPlaylistClick = ::selectNeteaseRemotePlaylist,
                        onDismissRequest = ::dismissNeteaseRemotePlaylistPicker
                    )
                }

                pendingNeteaseRemoteSyncConfirm?.let { pending ->
                    val supportedCount = pending.songs.size
                    AlertDialog(
                        onDismissRequest = { pendingNeteaseRemoteSyncConfirm = null },
                        title = {
                            Text(
                                stringResource(
                                    CoreCommonR.string.local_playlist_sync_netease_partial_confirm_title
                                )
                            )
                        },
                        text = {
                            Text(
                                "${
                                    pluralStringResource(
                                        AppR.plurals.local_playlist_sync_netease_partial_confirm_unsupported,
                                        pending.unsupportedCount,
                                        pending.unsupportedCount
                                    )
                                } ${
                                    pluralStringResource(
                                        AppR.plurals.local_playlist_sync_netease_partial_confirm_target,
                                        supportedCount,
                                        supportedCount,
                                        pending.target.name
                                    )
                                }"
                            )
                        },
                        confirmButton = {
                            HapticTextButton(
                                onClick = {
                                    startNeteaseRemotePlaylistSync(
                                        target = pending.target,
                                        songs = pending.songs,
                                        unsupportedCount = pending.unsupportedCount
                                    )
                                }
                            ) {
                                Text(stringResource(CoreCommonR.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            HapticTextButton(
                                onClick = { pendingNeteaseRemoteSyncConfirm = null }
                            ) {
                                Text(stringResource(CoreCommonR.string.action_cancel))
                            }
                        }
                    )
                }

                // 下载管理器
                if (showDownloadManager) {
                    val downloadTasks by GlobalDownloadManager.downloadTasks.collectAsState()
                    BatchDownloadManagerSheet(
                        downloadTasks = downloadTasks,
                        onDismiss = { showDownloadManager = false }
                    )
                }

                detailSong?.let { song ->
                    LocalSongDetailsDialog(
                        song = song,
                        onDismiss = { detailSong = null }
                    )
                }

                if (showNeteaseSyncConfirm) {
                    AlertDialog(
                        onDismissRequest = { showNeteaseSyncConfirm = false },
                        title = { Text(stringResource(CoreCommonR.string.local_playlist_sync_netease_confirm_title)) },
                        text = { Text(stringResource(CoreCommonR.string.local_playlist_sync_netease_confirm_message)) },
                        confirmButton = {
                            HapticTextButton(
                                onClick = {
                                    showNeteaseSyncConfirm = false
                                    openNeteaseSyncPreview()
                                }
                            ) { Text(stringResource(CoreCommonR.string.action_confirm)) }
                        },
                        dismissButton = {
                            HapticTextButton(
                                onClick = { showNeteaseSyncConfirm = false }
                            ) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                        }
                    )
                }

                pendingSyncConfirmAction?.let { action ->
                    LocalSongSyncConfirmDialog(
                        actionLabel = stringResource(pendingSyncConfirmLabel),
                        onConfirm = {
                            pendingSyncConfirmAction = null
                            pendingSyncConfirmLabel = CoreCommonR.string.playlist_add_to
                            action()
                        },
                        onDismiss = {
                            pendingSyncConfirmAction = null
                            pendingSyncConfirmLabel = CoreCommonR.string.playlist_add_to
                        }
                    )
                }

                if (showInsertDialog && selectionMode && canReorderCurrentSongs) {
                    PlaylistInsertDialog(
                        songs = localSongs.toList(),
                        selectedKeys = selectedKeysState.value,
                        offlineMode = offlineMode,
                        onDismiss = { showInsertDialog = false },
                        onConfirm = { preview ->
                            val sourceSongs = localSongs.toList()
                            val currentKeys = sourceSongs.map { it.stableKey() }
                            if (blockSync || insertInProgress || !isPlaylistInsertPreviewCurrent(
                                    preview, currentKeys, selectedKeysState.value
                                )
                            ) {
                                showInsertDialog = false
                                scope.launch {
                                    snackbarHostState.showNeriSnackbar(
                                        composeResources.getString(AppR.string.playlist_insert_stale)
                                    )
                                }
                            } else {
                                val songsByKey = sourceSongs.associateBy { it.stableKey() }
                                val originalOrder = sourceSongs.map { it.identity() }
                                val newOrder = preview.orderedKeys.map { songsByKey.getValue(it).identity() }
                                showInsertDialog = false
                                insertInProgress = true
                                scope.launchLocalPlaylistMutation(
                                    operation = "insertPlaylistSongs",
                                    onResult = { result ->
                                        insertInProgress = false
                                        val message = when {
                                            result.isFailure -> AppR.string.playlist_insert_failed
                                            result.getOrNull() != true -> AppR.string.playlist_insert_stale
                                            else -> null
                                        }
                                        if (message != null) {
                                            scope.launch {
                                                snackbarHostState.showNeriSnackbar(
                                                    composeResources.getString(message)
                                                )
                                            }
                                        } else {
                                            showInsertSuccess(preview, originalOrder, newOrder)
                                        }
                                    }
                                ) {
                                    repo.reorderSongs(
                                        playlistId = playlistId,
                                        newOrder = newOrder,
                                        expectedOrder = originalOrder
                                    )
                                }
                            }
                        }
                    )
                }

                // 多选优先退出
                BackHandler(enabled = selectionMode) { exitSelectionMode() }
            }
        }
    }
}

private const val PLAYLIST_INSERT_SCROLL_SYNC_TIMEOUT_MS = 1_500L
