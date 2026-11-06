package moe.ouom.neriplayer.ui.screen.tab.library

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
 * File: moe.ouom.neriplayer.ui.screen.tab.library/LibraryScreenLocalContent
 * Created: 2025/8/8
 */

import moe.ouom.neriplayer.ui.tv.tvReturnFocus
import moe.ouom.neriplayer.ui.tv.tvRowStart
import moe.ouom.neriplayer.ui.component.common.NeriSearchFieldGlass
import moe.ouom.neriplayer.ui.component.common.rememberTvTextFieldIme
import moe.ouom.neriplayer.ui.component.common.CleanSearchField
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.neriSearchFieldColors
import moe.ouom.neriplayer.ui.theme.neriSearchFieldShape
import androidx.compose.ui.graphics.Shape
import moe.ouom.neriplayer.ui.theme.neriRoundedShape
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import moe.ouom.neriplayer.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.playlist.favorite.FAVORITE_SOURCE_NETEASE_ARTIST
import moe.ouom.neriplayer.data.playlist.favorite.FAVORITE_SOURCE_BILI_ARTIST
import moe.ouom.neriplayer.data.playlist.favorite.FAVORITE_SOURCE_YOUTUBE_ARTIST
import moe.ouom.neriplayer.data.model.playlist.FavoritePlaylist
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.local.playlist.system.LocalFilesPlaylist
import moe.ouom.neriplayer.data.model.playlist.LocalArtistSummary
import moe.ouom.neriplayer.data.model.playlist.LocalPlaylist
import moe.ouom.neriplayer.data.local.playlist.artist.buildLocalArtistSummaries
import moe.ouom.neriplayer.data.local.playlist.LocalPlaylistRepository
import moe.ouom.neriplayer.data.local.playlist.system.SystemLocalPlaylists
import moe.ouom.neriplayer.data.local.media.displayArtist
import moe.ouom.neriplayer.data.local.media.displayName
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.util.rememberPlaylistDisplayCoverUrl
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.ui.tv.rememberTvMenuFirstItemFocus
import moe.ouom.neriplayer.ui.tv.rememberTvRowMenuKeyState
import moe.ouom.neriplayer.ui.tv.tvRowMenuKeyPress
import moe.ouom.neriplayer.ui.tv.TvRowMenuKeyEffect
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.haptic.HapticTextButton
import moe.ouom.neriplayer.ui.screen.playlist.reorder.LocalPlaylistReorderableItem
import moe.ouom.neriplayer.ui.screen.playlist.reorder.localPlaylistDetectReorder
import moe.ouom.neriplayer.ui.screen.playlist.reorder.localPlaylistReorderable
import moe.ouom.neriplayer.ui.screen.playlist.reorder.rememberLocalPlaylistReorderState
import moe.ouom.neriplayer.ui.screen.tab.home.ContinueStyleCardSpacingDp
import moe.ouom.neriplayer.ui.screen.tab.home.ContinueStyleHorizontalInsetDp
import moe.ouom.neriplayer.ui.screen.tab.home.resolveContinueStyleColumns
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsButton
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsDialogContent
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import moe.ouom.neriplayer.ui.screen.tab.settings.miuix.MiuixSettingsTextField
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import org.burnoutcrew.reorderable.ItemPosition
import moe.ouom.neriplayer.R as AppR
import moe.ouom.neriplayer.ui.component.playlist.PlaylistNameError
import moe.ouom.neriplayer.ui.i18n.asString
@Composable
internal fun LocalPlaylistList(
    playlists: List<LocalPlaylist>,
    listState: LazyListState,
    onCreate: (String) -> Unit,
    onClick: (LocalPlaylist) -> Unit,
    onArtistClick: (LocalArtistSummary) -> Unit,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onDelete: (List<Long>) -> Unit = {},
    onReorder: (List<Long>) -> Unit = {},
    offlineMode: Boolean
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    var selectedLocalCategory by rememberSaveable {
        mutableIntStateOf(LOCAL_CATEGORY_PLAYLIST)
    }
    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var localArtistSortMode by rememberSaveable {
        mutableStateOf(readLocalArtistSortMode(context))
    }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<PlaylistNameError?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val defaultPlaylistName = composeResources.getString(CoreCommonR.string.library_create_playlist_default)
    val maxNameLength = LocalPlaylistRepository.MAX_PLAYLIST_NAME_LENGTH
    val autoShowKeyboard by AppContainer.settingsRepo.autoShowKeyboardFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val displayedLocalArtists = if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
        val localArtists by produceState<List<LocalArtistSummary>?>(
            initialValue = null,
            key1 = playlists,
            key2 = context
        ) {
            value = withContext(Dispatchers.Default) {
                buildLocalArtistSummaries(playlists, context)
            }
        }
        val artistSnapshot = localArtists
        val artistQuery = localSearchQuery
        val artistSortMode = localArtistSortMode
        val displayedArtists by produceState<List<LocalArtistSummary>?>(
            initialValue = null,
            key1 = artistSnapshot,
            key2 = artistQuery,
            key3 = artistSortMode
        ) {
            value = withContext(Dispatchers.Default) {
                artistSnapshot?.let { artists ->
                    sortLocalArtists(filterLocalArtists(artists, artistQuery), artistSortMode)
                }
            }
        }
        displayedArtists
    } else {
        emptyList()
    }
    val editablePlaylists = remember(playlists, context) {
        playlists.filterNot { SystemLocalPlaylists.isSystemPlaylist(it, context) }
    }
    val reorderablePlaylists = remember(editablePlaylists) {
        mutableStateListOf<LocalPlaylist>().apply {
            addAll(editablePlaylists)
        }
    }

    val reorderState = rememberLocalPlaylistReorderState(
        listState = listState,
        onMove = { from: ItemPosition, to: ItemPosition ->
            if (!selectionMode) return@rememberLocalPlaylistReorderState
            val fromId = from.key as? Long ?: return@rememberLocalPlaylistReorderState
            val toId = to.key as? Long ?: return@rememberLocalPlaylistReorderState
            val fromIdx = reorderablePlaylists.indexOfFirst { it.id == fromId }
            val toIdx = reorderablePlaylists.indexOfFirst { it.id == toId }
            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                reorderablePlaylists.add(toIdx, reorderablePlaylists.removeAt(fromIdx))
            }
        },
        canDragOver = { target, _ ->
            selectionMode && target.key is Long
        },
        onDragEnd = { _, _ ->
            if (selectionMode) {
                onReorder(reorderablePlaylists.map { it.id })
            }
        }
    )

    LaunchedEffect(showDialog) {
        if (showDialog && autoShowKeyboard) focusRequester.requestFocus()
    }

    fun exitSelection() {
        reorderState.endDrag()
        selectionMode = false
        selectedIds = emptySet()
        showDeleteSelectedConfirm = false
    }

    fun toggleSelection(playlistId: Long) {
        selectedIds =
            if (selectedIds.contains(playlistId)) selectedIds - playlistId else selectedIds + playlistId
    }

    fun deleteSelected() {
        if (selectedIds.isEmpty()) return
        showDeleteSelectedConfirm = true
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    LaunchedEffect(editablePlaylists) {
        val validIds = editablePlaylists.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
        if (selectionMode && editablePlaylists.isEmpty()) {
            exitSelection()
        }
    }

    fun tryCreate(): Boolean {
        val trimmedInput = newName.trim().take(maxNameLength)
        val finalName = trimmedInput.ifBlank { defaultPlaylistName }.take(maxNameLength)

        if (FavoritesPlaylist.matches(finalName, context)) {
            nameError = PlaylistNameError.FAVORITES_RESERVED
            return false
        }
        if (LocalFilesPlaylist.matches(finalName, context)) {
            nameError = PlaylistNameError.LOCAL_FILES_RESERVED
            return false
        }
        if (playlists.any { it.name.equals(finalName, ignoreCase = true) }) {
            nameError = PlaylistNameError.DUPLICATE
            return false
        }

        onCreate(finalName)
        showDialog = false
        newName = ""
        nameError = null
        return true
    }

    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val favoritesPlaylist = playlists.firstOrNull { FavoritesPlaylist.isSystemPlaylist(it, context) }
    val localFilesPlaylist = playlists.firstOrNull { LocalFilesPlaylist.isSystemPlaylist(it, context) }

    val showingLocalPlaylists = selectedLocalCategory == LOCAL_CATEGORY_PLAYLIST
    val displayedFavoritesPlaylist = favoritesPlaylist
        ?.takeIf { playlist ->
            showingLocalPlaylists && playlist.matchesLocalPlaylistSearch(localSearchQuery, context)
        }
    val displayedLocalFilesPlaylist = localFilesPlaylist
        ?.takeIf { playlist ->
            showingLocalPlaylists && playlist.matchesLocalPlaylistSearch(localSearchQuery, context)
        }
    val displayedPlaylists = if (showingLocalPlaylists) {
        reorderablePlaylists.filter { playlist -> playlist.matchesLocalPlaylistSearch(localSearchQuery, context) }
    } else {
        emptyList()
    }
    val hasPlaylistSearchMatches =
        displayedFavoritesPlaylist != null ||
            displayedPlaylists.isNotEmpty() ||
            displayedLocalFilesPlaylist != null
    SideEffect {
        val playlistStartIndex = 1 +
            (if (selectionMode) 1 else 0) +
            (if (localSearchQuery.isBlank()) 1 else 0) +
            (if (displayedFavoritesPlaylist != null) 1 else 0)
        reorderState.updatePresentedOrder { key ->
            displayedPlaylists.indexOfFirst { it.id == key }
                .takeIf { it >= 0 }?.plus(playlistStartIndex) ?: -1
        }
    }
    val windowWidthDp = currentWindowWidthDp()
    val isTelevision = LocalIsTelevision.current
    // 电视：卡片大小 / 间距都按首页「继续播放」那一档来。
    // 左右多留 ContinueStyleHorizontalInsetDp，卡片左右边缘就和继续播放那一行对齐；
    // 列数用同一个公式反算，单格宽度自然和继续播放卡片完全相等。
    val localGridSidePadding = pageSidePaddingOr(8.dp) +
        (if (isTelevision) ContinueStyleHorizontalInsetDp else 0.dp)
    // 电视：这 8dp 不能进 LazyColumn 的 contentPadding —— 那会把「歌单/歌手」二级标签栏
    // （fillMaxWidth）一起缩窄 16dp，和网易云等分页对不上。改成只加在网格行上，
    // 卡片左缘、列数、单格宽度全部不变。
    val localListSidePadding = if (isTelevision) 0.dp else localGridSidePadding
    val localGridRowSidePadding = if (isTelevision) ContinueStyleHorizontalInsetDp else 0.dp
    val localGridSpacing = if (isTelevision) ContinueStyleCardSpacingDp else 10.dp
    val localArtistColumnCount = remember(windowWidthDp, isTelevision, localGridSidePadding) {
        if (isTelevision) {
            resolveContinueStyleColumns((windowWidthDp - localGridSidePadding * 2).value)
                .coerceAtLeast(1)
        } else {
            ((windowWidthDp.value - 16f + 10f) / 130f).toInt().coerceAtLeast(1)
        }
    }
    val localArtistRows = remember(displayedLocalArtists, localArtistColumnCount) {
        displayedLocalArtists.orEmpty().chunked(localArtistColumnCount)
    }

    val cardShape = neriRoundedShape(12.dp)
    LazyColumn(
        state = reorderState.listState,
        contentPadding = PaddingValues(
            start = localListSidePadding,
            end = localListSidePadding,
            top = 8.dp,
            bottom = 8.dp + miniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .localPlaylistReorderable(reorderState)
    ) {
        item(key = "local_library_header") {
            LocalLibraryHeaderContent(
                selectedLocalCategory = selectedLocalCategory,
                selectionMode = selectionMode,
                searchQuery = localSearchQuery,
                onSearchQueryChange = { localSearchQuery = it },
                artistSortMode = localArtistSortMode,
                onArtistSortModeChange = { sortMode ->
                    localArtistSortMode = sortMode
                    persistLocalArtistSortMode(context, sortMode)
                },
                onPlaylistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_PLAYLIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_PLAYLIST
                    }
                },
                onArtistSelected = {
                    if (selectedLocalCategory != LOCAL_CATEGORY_ARTIST) {
                        exitSelection()
                        selectedLocalCategory = LOCAL_CATEGORY_ARTIST
                    }
                }
            )
        }

        if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
            if (displayedLocalArtists?.isEmpty() == true) {
                item(key = "local_artist_empty") {
                    LibrarySearchEmptyCard(
                        titleResId = if (localSearchQuery.isBlank()) {
                            CoreCommonR.string.library_local_artist_empty
                        } else {
                            CoreCommonR.string.library_local_search_empty
                        },
                        hintResId = if (localSearchQuery.isBlank()) {
                            CoreCommonR.string.library_local_artist_hint
                        } else {
                            CoreCommonR.string.library_local_search_empty_hint
                        },
                        iconIsArtist = true
                    )
                }
            }
            items(
                items = localArtistRows,
                key = { row -> row.joinToString(separator = "|") { artist -> artist.stableKey } }
            ) { rowArtists ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(localGridSpacing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = localGridRowSidePadding, vertical = 5.dp)
                ) {
                    rowArtists.forEachIndexed { column, artist ->
                        Box(modifier = Modifier.weight(1f)) {
                            LocalArtistGridCard(
                                artist = artist,
                                onClick = { onArtistClick(artist) },
                                offlineMode = offlineMode,
                                atRowStart = column == 0
                            )
                        }
                    }
                    repeat(localArtistColumnCount - rowArtists.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
                if (selectionMode) {
                    item(key = "local_playlist_selection_header") {
                        val allSelected =
                            selectedIds.size == displayedPlaylists.size && displayedPlaylists.isNotEmpty()
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(cardShape)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                pluralStringResource(
                                    AppR.plurals.common_selected_count,
                                    selectedIds.size,
                                    selectedIds.size
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            HapticIconButton(onClick = { exitSelection() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(CoreCommonR.string.action_exit_multi_select)
                                )
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HapticTextButton(
                                    onClick = {
                                        selectedIds = if (allSelected) {
                                            emptySet()
                                        } else {
                                            displayedPlaylists.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (allSelected) {
                                            stringResource(CoreCommonR.string.action_deselect_all)
                                        } else {
                                            stringResource(CoreCommonR.string.action_select_all)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                HapticTextButton(
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = { deleteSelected() }
                                ) {
                                    Text(stringResource(CoreCommonR.string.common_delete_selected))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (localSearchQuery.isBlank()) {
            item(key = "local_playlist_create") {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .animateItem()
                    .clip(cardShape)
                    .tvRowStart()
                    .clickable(enabled = !selectionMode) { showDialog = true }
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(CoreCommonR.string.library_create_new)) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            if (showDialog) {
                MiuixSettingsDialog(
                    onDismissRequest = {
                        showDialog = false
                        newName = ""
                        nameError = null
                    },
                    title = { Text(stringResource(CoreCommonR.string.playlist_create)) },
                    text = {
                        MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                            MiuixSettingsTextField(
                                value = newName,
                                onValueChange = {
                                    newName = it.take(maxNameLength)
                                    if (nameError != null) nameError = null
                                },
                                placeholder = { Text(stringResource(CoreCommonR.string.playlist_enter_name)) },
                                singleLine = true,
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { tryCreate() })
                            )
                            if (nameError != null) {
                                Text(
                                    text = nameError?.message()?.asString().orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        MiuixSettingsButton(
                            onClick = { tryCreate() },
                            enabled = newName.trim().isNotBlank()
                        ) {
                            Text(stringResource(CoreCommonR.string.action_create))
                        }
                    },
                    dismissButton = {
                        MiuixSettingsTextButton(
                            onClick = {
                                showDialog = false
                                newName = ""
                                nameError = null
                            }
                        ) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                    }
                )
            }

            if (showDeleteSelectedConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteSelectedConfirm = false },
                    title = { Text(stringResource(AppR.string.dialog_confirm_delete)) },
                    text = {
                        Text(
                            pluralStringResource(
                                AppR.plurals.library_delete_selected_confirm,
                                selectedIds.size,
                                selectedIds.size
                            )
                        )
                    },
                    confirmButton = {
                        HapticTextButton(
                            onClick = {
                                val idsToDelete = selectedIds.toList()
                                exitSelection()
                                onDelete(idsToDelete)
                            }
                        ) { Text(stringResource(CoreCommonR.string.action_delete)) }
                    },
                    dismissButton = {
                        HapticTextButton(
                            onClick = { showDeleteSelectedConfirm = false }
                        ) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                    }
                )
            }
            }
        }

        if (localSearchQuery.isNotBlank() && !hasPlaylistSearchMatches) {
            item(key = "local_playlist_search_empty") {
                LibrarySearchEmptyCard(
                    titleResId = CoreCommonR.string.library_local_search_empty,
                    hintResId = CoreCommonR.string.library_local_search_empty_hint,
                    iconIsArtist = false
                )
            }
        }

        if (isTelevision) {
            item(key = "local_playlist_grid") {
                val tvCards = ArrayList<LocalPlaylist>().apply {
                    displayedFavoritesPlaylist?.let { add(it) }
                    displayedLocalFilesPlaylist?.let { add(it) }
                    addAll(displayedPlaylists)
                }
                Column(Modifier.fillMaxWidth()) {
                    // 注意：chunked 的 index 是**行号**，不是列号。只有每行的第 0 个卡片才是「行首」，
                    // 写成 `column == 0` 会把整行都标成行首 —— 表现是行内按左直接跳到侧栏、走不到左边那张。
                    tvCards.chunked(localArtistColumnCount).forEach { rowCards ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(localGridSpacing),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = localGridRowSidePadding, vertical = 5.dp)
                        ) {
                            rowCards.forEachIndexed { column, pl ->
                                Box(modifier = Modifier.weight(1f)) {
                                    LocalPlaylistGridCard(
                                        playlist = pl,
                                        onClick = { if (!selectionMode) onClick(pl) },
                                        onRename = { name -> onRename(pl.id, name) },
                                        onDelete = { onDelete(listOf(pl.id)) },
                                        offlineMode = offlineMode,
                                        atRowStart = column == 0
                                    )
                                }
                            }
                            repeat(localArtistColumnCount - rowCards.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
        displayedFavoritesPlaylist?.let { system ->
            item(key = "local_playlist_favorites") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(cardShape)
                    .tvBringIntoViewOnFocus()
                    .combinedClickable(
                        onClick = {
                            if (!selectionMode) onClick(system)
                        }
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(AppR.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(neriRoundedShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

            items(
                items = displayedPlaylists,
                key = { it.id }
            ) { pl ->
            LocalPlaylistReorderableItem(state = reorderState, key = pl.id) { isDragging ->
                val rowScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.02f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "local-playlist-row-scale"
                )
                val systemPlaylist = SystemLocalPlaylists.resolve(pl.id, pl.name, context)
                val displayName = systemPlaylist?.currentName ?: pl.name
                val isSystemPlaylist = systemPlaylist != null
                val isSelected = selectionMode && selectedIds.contains(pl.id)
                val rowContainerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }

                var showMenu by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var renameText by remember { mutableStateOf(pl.name.take(maxNameLength)) }
                // 电视：遥控器菜单键直接弹出本行菜单（三个点不好聚焦）
                val menuKeyState = rememberTvRowMenuKeyState()

                if (selectionMode && showMenu) showMenu = false

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = rowContainerColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .graphicsLayer {
                            scaleX = rowScale
                            scaleY = rowScale
                        }
                        .clip(cardShape)
                        .tvBringIntoViewOnFocus()
                        .tvRowMenuKeyPress(menuKeyState)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(pl.id)
                                } else {
                                    onClick(pl)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode && !isSystemPlaylist) {
                                    selectionMode = true
                                    selectedIds = setOf(pl.id)
                                }
                            }
                        )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(AppR.plurals.library_song_count, pl.songs.size, pl.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (!isSystemPlaylist) toggleSelection(pl.id)
                                        },
                                        enabled = !isSystemPlaylist
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = pl,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(neriRoundedShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (selectionMode && !isSystemPlaylist) {
                                Box(
                                    modifier = Modifier
                                        .localPlaylistDetectReorder(reorderState, pl.id)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = stringResource(CoreCommonR.string.common_drag_handle),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (!selectionMode && !isSystemPlaylist) {
                                Box {
                                    TvRowMenuKeyEffect(state = menuKeyState) { showMenu = true }
                                    val firstMenuFocus = rememberTvMenuFirstItemFocus(showMenu)
                                    HapticIconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = stringResource(CoreCommonR.string.common_more_options)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(CoreCommonR.string.action_rename)) },
                                            modifier = Modifier.focusRequester(firstMenuFocus),
                                            onClick = {
                                                showMenu = false
                                                renameText = pl.name.take(maxNameLength)
                                                showRenameDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(CoreCommonR.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (showRenameDialog) {
                    MiuixSettingsDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text(stringResource(CoreCommonR.string.action_rename)) },
                        text = {
                            MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                                MiuixSettingsTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it.take(maxNameLength) },
                                    placeholder = { Text(displayName) },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            MiuixSettingsButton(
                                onClick = {
                                    val trimmed = renameText.trim().take(maxNameLength)
                                    if (trimmed.isNotBlank()) {
                                        onRename(pl.id, trimmed)
                                        showRenameDialog = false
                                    }
                                },
                                enabled = renameText.trim().isNotBlank()
                            ) { Text(stringResource(CoreCommonR.string.action_confirm)) }
                        },
                        dismissButton = {
                            MiuixSettingsTextButton(
                                onClick = { showRenameDialog = false }
                            ) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                        }
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(CoreCommonR.string.action_delete)) },
                        text = {
                            Text(stringResource(CoreCommonR.string.library_delete_playlist_confirm, displayName))
                        },
                        confirmButton = {
                            HapticTextButton(
                                onClick = {
                                    val playlistId = pl.id
                                    showDeleteDialog = false
                                    onDelete(listOf(playlistId))
                                }
                            ) { Text(stringResource(CoreCommonR.string.action_delete)) }
                        },
                        dismissButton = {
                            HapticTextButton(
                                onClick = { showDeleteDialog = false }
                            ) { Text(stringResource(CoreCommonR.string.action_cancel)) }
                        }
                    )
                }
            }
        }

        displayedLocalFilesPlaylist?.let { system ->
            item(key = "local_playlist_local_files") {
                val displayName = SystemLocalPlaylists.resolve(system.id, system.name, context)?.currentName ?: system.name
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(cardShape)
                    .tvBringIntoViewOnFocus()
                    .combinedClickable(
                        onClick = {
                            if (!selectionMode) onClick(system)
                        }
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                pluralStringResource(AppR.plurals.library_song_count, system.songs.size, system.songs.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        leadingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectionMode) {
                                    Spacer(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                val cover = rememberPlaylistDisplayCoverUrl(
                                    playlist = system,
                                    resolveLocalFallback = true
                                )
                                if (!cover.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = fastScrollableImageRequest(
                                            context = context,
                                            data = cover,
                                            sizePx = 160,
                                            offlineMode = offlineMode
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(neriRoundedShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
        }
    }
    }
}

/**
 * 电视上的歌单小卡：和「歌手」标签里的歌手卡片同一套版式（正方形封面 + 名称 + 数量）。
 * 原来是一整行的 ListItem，跟歌手页放一起看着完全不是同一种东西。
 * 菜单键（遥控器）打开重命名/删除，和原来的三点菜单一致。
 */
@Composable
private fun LocalPlaylistGridCard(
    playlist: LocalPlaylist,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    offlineMode: Boolean,
    /** 是否本行第 0 列：按左直接交回侧栏，不往左上跑 */
    atRowStart: Boolean = false
) {
    val context = LocalContext.current
    val systemPlaylist = SystemLocalPlaylists.resolve(playlist.id, playlist.name, context)
    val displayName = systemPlaylist?.currentName ?: playlist.name
    val isSystemPlaylist = systemPlaylist != null
    val maxNameLength = LocalPlaylistRepository.MAX_PLAYLIST_NAME_LENGTH

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name.take(maxNameLength)) }
    val menuKeyState = rememberTvRowMenuKeyState()
    TvRowMenuKeyEffect(state = menuKeyState) { if (!isSystemPlaylist) showMenu = true }
    val firstMenuFocus = rememberTvMenuFirstItemFocus(showMenu)
    // 和首页「继续播放」卡片同一套焦点框：8dp 圆角 + 外扩 4dp（卡片尺寸不变）
    val tvRingIndication = rememberTvFocusRingIndication(8.dp, outset = 4.dp)
        ?: LocalIndication.current

    Column(
        modifier = Modifier
            .tvBringIntoViewOnFocus()
            .tvRowStart(atRowStart)
            .tvRowMenuKeyPress(menuKeyState)
            // 电视「返回来源」：进这个歌单，退出时光标回到这张卡
            .tvReturnFocus("local:${playlist.id}")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRingIndication,
                onClick = onClick
            )
            // 裁剪放在 clickable 之后，否则会把外扩的焦点环一起切掉
            .clip(neriRoundedShape(12.dp))
    ) {
        val cover = rememberPlaylistDisplayCoverUrl(
            playlist = playlist,
            resolveLocalFallback = true
        )
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = fastScrollableImageRequest(
                    context = context,
                    data = cover,
                    sizePx = 384,
                    offlineMode = offlineMode
                ),
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(neriRoundedShape(12.dp))
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(neriRoundedShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = pluralStringResource(
                        AppR.plurals.library_song_count,
                        playlist.songs.size,
                        playlist.songs.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (!isSystemPlaylist) {
                Box {
                    HapticIconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(CoreCommonR.string.common_more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(CoreCommonR.string.action_rename)) },
                            modifier = Modifier.focusRequester(firstMenuFocus),
                            onClick = {
                                showMenu = false
                                renameText = playlist.name.take(maxNameLength)
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(CoreCommonR.string.action_delete)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        MiuixSettingsDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(CoreCommonR.string.action_rename)) },
            text = {
                MiuixSettingsDialogContent(verticalSpacing = 12.dp) {
                    MiuixSettingsTextField(
                        value = renameText,
                        onValueChange = { renameText = it.take(maxNameLength) },
                        placeholder = { Text(displayName) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                MiuixSettingsButton(
                    onClick = {
                        val trimmed = renameText.trim().take(maxNameLength)
                        if (trimmed.isNotBlank()) {
                            onRename(trimmed)
                            showRenameDialog = false
                        }
                    },
                    enabled = renameText.trim().isNotBlank()
                ) { Text(stringResource(CoreCommonR.string.action_confirm)) }
            },
            dismissButton = {
                MiuixSettingsTextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(CoreCommonR.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(CoreCommonR.string.action_delete)) },
            text = {
                Text(stringResource(CoreCommonR.string.library_delete_playlist_confirm, displayName))
            },
            confirmButton = {
                HapticTextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text(stringResource(CoreCommonR.string.action_delete)) }
            },
            dismissButton = {
                HapticTextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(CoreCommonR.string.action_cancel))
                }
            }
        )
    }
}

@Composable
internal fun LocalLibraryHeaderContent(
    selectedLocalCategory: Int,
    selectionMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    artistSortMode: LocalArtistSortMode,
    onArtistSortModeChange: (LocalArtistSortMode) -> Unit,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LocalCategoryTabs(
            selectedCategory = selectedLocalCategory,
            onPlaylistSelected = onPlaylistSelected,
            onArtistSelected = onArtistSelected
        )
        if (!selectionMode) {
            if (selectedLocalCategory == LOCAL_CATEGORY_ARTIST) {
                LocalArtistSearchAndSortRow(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    sortMode = artistSortMode,
                    onSortModeChange = onArtistSortModeChange
                )
            } else {
                LibraryInlineSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderResId = CoreCommonR.string.library_local_playlist_search_hint
                )
            }
        }
    }
}

@Composable
internal fun LocalArtistSearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: LocalArtistSortMode,
    onSortModeChange: (LocalArtistSortMode) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // 右边还有一个排序按钮（三个点）：只放行右键，左键留在原地
    // （左键放行会穿到页面最左边外面，焦点飞走）
    val artistSearchIme = rememberTvTextFieldIme(swallowLeft = true, swallowRight = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (NeriDesign.isClean) {
            CleanSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(CoreCommonR.string.library_local_artist_search_hint),
                // 右边还有排序三点：右键要能过去（和下方 Material 分支一致）
                swallowRight = false,
                modifier = Modifier.weight(1f)
            )
        } else NeriSearchFieldGlass(
            modifier = Modifier.weight(1f),
            shape = neriSearchFieldShape(LibrarySearchFieldShape)
        ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            readOnly = artistSearchIme.readOnly,
            modifier = Modifier
                .fillMaxWidth()
                .then(artistSearchIme.modifier),
            placeholder = { Text(stringResource(CoreCommonR.string.library_local_artist_search_hint)) },
            singleLine = true,
            shape = neriSearchFieldShape(LibrarySearchFieldShape),

            colors = neriSearchFieldColors(),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    HapticIconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(CoreCommonR.string.action_clear)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        }
        Box {
            HapticIconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(CoreCommonR.string.library_local_artist_sort)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.SONG_COUNT,
                    text = stringResource(CoreCommonR.string.library_local_artist_sort_count),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.SONG_COUNT)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.RECENT_ADDED,
                    text = stringResource(CoreCommonR.string.library_local_artist_sort_recent),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.RECENT_ADDED)
                        menuExpanded = false
                    }
                )
                LocalArtistSortMenuItem(
                    selected = sortMode == LocalArtistSortMode.NAME,
                    text = stringResource(CoreCommonR.string.library_local_artist_sort_name),
                    onClick = {
                        onSortModeChange(LocalArtistSortMode.NAME)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun LocalArtistSortMenuItem(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text)
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(CoreCommonR.string.common_selected)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        },
        onClick = onClick
    )
}

@Composable
internal fun LocalCategoryTabs(
    selectedCategory: Int,
    onPlaylistSelected: () -> Unit,
    onArtistSelected: () -> Unit
) {
    LibrarySecondaryTabs(
        labels = listOf(
            stringResource(CoreCommonR.string.library_favorite_tab_playlists),
            stringResource(CoreCommonR.string.library_favorite_tab_artists)
        ),
        selectedIndex = selectedCategory,
        onSelected = { index ->
            if (index == LOCAL_CATEGORY_ARTIST) onArtistSelected() else onPlaylistSelected()
        }
    )
}

@Composable
internal fun LibraryInlineSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderResId: Int
) {
    // 电视上不放搜索框（遥控器输入文字太费劲，用户要求去掉）
    if (LocalIsTelevision.current) return
    if (NeriDesign.isClean) {
        CleanSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(placeholderResId),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
        return
    }
    NeriSearchFieldGlass(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = neriSearchFieldShape(LibrarySearchFieldShape)
    ) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(placeholderResId)) },
        singleLine = true,
        shape = neriSearchFieldShape(LibrarySearchFieldShape),

        colors = neriSearchFieldColors(),
        trailingIcon = {
            if (query.isNotEmpty()) {
                HapticIconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(CoreCommonR.string.action_clear)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
    }
}

@Composable
internal fun LibrarySearchEmptyCard(
    titleResId: Int,
    hintResId: Int,
    iconIsArtist: Boolean
) {
    Card(
        shape = neriRoundedShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(neriRoundedShape(12.dp))
    ) {
        ListItem(
            headlineContent = { Text(stringResource(titleResId)) },
            supportingContent = {
                Text(
                    stringResource(hintResId),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = if (iconIsArtist) {
                        Icons.Filled.AccountCircle
                    } else {
                        Icons.AutoMirrored.Filled.QueueMusic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
        )
    }
}

internal fun filterLocalArtists(
    artists: List<LocalArtistSummary>,
    query: String
): List<LocalArtistSummary> {
    if (query.isBlank()) return artists
    return artists.filter { artist -> artist.matchesLocalArtistSearch(query) }
}

internal fun sortLocalArtists(
    artists: List<LocalArtistSummary>,
    sortMode: LocalArtistSortMode
): List<LocalArtistSummary> {
    return when (sortMode) {
        LocalArtistSortMode.SONG_COUNT -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.songs.size
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.RECENT_ADDED -> artists.sortedWith(
            compareByDescending<LocalArtistSummary> { artist ->
                artist.coverSong?.addedAt ?: 0L
            }.thenBy { artist ->
                artist.name.lowercase()
            }
        )
        LocalArtistSortMode.NAME -> artists.sortedBy { artist ->
            artist.name.lowercase()
        }
    }
}

internal fun LocalArtistSummary.matchesLocalArtistSearch(query: String): Boolean {
    return queryMatches(query, name) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

internal fun LocalPlaylist.matchesLocalPlaylistSearch(query: String, context: Context): Boolean {
    if (query.isBlank()) return true
    val displayName = SystemLocalPlaylists.resolve(id, name, context)?.currentName ?: name
    return queryMatches(query, id, name, displayName, songs.size) ||
        songs.any { song ->
            queryMatches(
                query,
                song.displayName(),
                song.displayArtist(),
                song.album,
                song.localFileName
            )
        }
}

internal fun filterFavoritePlaylists(
    favorites: List<FavoritePlaylist>,
    query: String
): List<FavoritePlaylist> {
    if (query.isBlank()) return favorites
    return favorites.filter { favorite -> favorite.matchesFavoriteSearch(query) }
}

internal fun FavoritePlaylist.matchesFavoriteSearch(query: String): Boolean {
    return queryMatches(
        query,
        id,
        name,
        subtitle,
        source,
        browseId,
        playlistId,
        trackCount,
        favoriteSourceSearchAliases(source)
    ) || songs.any { song ->
        queryMatches(
            query,
            song.displayName(),
            song.displayArtist(),
            song.album,
            song.localFileName
        )
    }
}

internal fun favoriteSourceSearchAliases(source: String): List<String> {
    return when (source) {
        "youtubeMusic" -> listOf("YouTube", "YouTube Music")
        "neteaseAlbum" -> listOf("Netease Album", "网易云专辑", "专辑")
        "netease" -> listOf("Netease", "网易云", "歌单")
        "bili" -> listOf("Bilibili", "哔哩哔哩", "B站")
        FAVORITE_SOURCE_NETEASE_ARTIST -> listOf("Netease", "网易云", "Artist", "歌手")
        FAVORITE_SOURCE_BILI_ARTIST -> listOf("Bilibili", "哔哩哔哩", "B站", "UP主", "歌手")
        FAVORITE_SOURCE_YOUTUBE_ARTIST -> listOf("YouTube", "YouTube Music", "Artist", "歌手", "频道")
        else -> listOf(source)
    }
}

internal fun queryMatches(query: String, vararg values: Any?): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return true
    return values.any { value ->
        when (value) {
            null -> false
            is Iterable<*> -> value.any { item ->
                item?.toString()?.contains(normalizedQuery, ignoreCase = true) == true
            }
            else -> value.toString().contains(normalizedQuery, ignoreCase = true)
        }
    }
}
