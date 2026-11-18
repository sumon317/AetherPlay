package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatResolution
import com.example.ui.theme.MonoMetricsStyle
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.MediaFolderSummary
import com.example.ui.viewmodel.MediaSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    uiState: LibraryUiState,
    isGridMode: Boolean,
    hasAllFilesPermission: Boolean,
    currentPlayingUri: String?,
    onSearchChange: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onCloseFolder: () -> Unit,
    onToggleFolderView: () -> Unit,
    onSelectSort: (MediaSortOrder) -> Unit,
    onToggleGridMode: () -> Unit,
    onPlayVideo: (List<MediaItemEntity>, Int) -> Unit,
    onToggleLike: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onDeleteVideo: (MediaItemEntity) -> Unit,
    onPickPhotoPickerVideos: () -> Unit,
    onPickDocumentFiles: () -> Unit,
    onPickFolderTree: () -> Unit,
    onOpenStreamDialog: () -> Unit,
    onRequestAllFilesPermission: () -> Unit,
    onRescanDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    val showingFolderDetail = uiState.isVideoFolderView && uiState.openedVideoFolder != null
    val showingFoldersList = uiState.isVideoFolderView &&
        uiState.openedVideoFolder == null &&
        uiState.searchQuery.isBlank()

    BackHandler(enabled = showingFolderDetail) {
        onCloseFolder()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (showingFolderDetail) {
                        IconButton(
                            onClick = onCloseFolder,
                            modifier = Modifier.testTag("videos_folder_back")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to folders")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (showingFolderDetail) uiState.openedVideoFolder.orEmpty() else "Videos",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when {
                            showingFolderDetail -> "${uiState.videos.size} ${if (uiState.videos.size == 1) "video" else "videos"}"
                            showingFoldersList && uiState.videoFolderSummaries.isNotEmpty() ->
                                "${uiState.videoFolderSummaries.size} ${if (uiState.videoFolderSummaries.size == 1) "folder" else "folders"} • ${uiState.videos.size} titles"
                            uiState.videos.isNotEmpty() -> "${uiState.videos.size} titles • Hardware Ready"
                            else -> null
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleFolderView,
                        modifier = Modifier.testTag("videos_folder_view_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVideoFolderView) Icons.Rounded.VideoLibrary else Icons.Rounded.Folder,
                            contentDescription = if (uiState.isVideoFolderView) "Show all videos flat" else "Group by folder"
                        )
                    }
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("videos_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search videos"
                        )
                    }
                    IconButton(
                        onClick = onToggleGridMode,
                        modifier = Modifier.testTag("videos_layout_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle grid or list"
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort videos")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            MediaSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        onSelectSort(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (hasAllFilesPermission) "Rescan Device Storage" else "Allow All Files Access") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = {
                                    showSortMenu = false
                                    if (hasAllFilesPermission) onRescanDevice() else onRequestAllFilesPermission()
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenStreamDialog,
                        modifier = Modifier.testTag("open_stream_button")
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = "Open stream URL")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showImportMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("videos_import_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Import media")
                }
                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Pick Videos (Photo Picker)") },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onPickPhotoPickerVideos()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Any Format (MKV, AVI, TS...)") },
                        leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onPickDocumentFiles()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan Media Folder") },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onPickFolderTree()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Stream URL (HLS / DASH)") },
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onOpenStreamDialog()
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }

            if (!hasAllFilesPermission && uiState.videoFolderSummaries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onRequestAllFilesPermission)
                        .testTag("videos_permission_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable All Files Access", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Tap to scan all device video & audio folders automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search movies, clips, folders...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("videos_search_field")
                )
            }

            if (!uiState.isVideoFolderView && uiState.videoFolders.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.videoFolders) { folder ->
                        FilterChip(
                            selected = folder == uiState.selectedFolder,
                            onClick = { onSelectFolder(folder) },
                            label = { Text(folder) }
                        )
                    }
                }
            }

            if (showingFoldersList) {
                if (uiState.videoFolderSummaries.isEmpty()) {
                    VideoEmptyHeroState(
                        hasAllFilesPermission = hasAllFilesPermission,
                        onRequestAllFilesPermission = onRequestAllFilesPermission,
                        onRescanDevice = onRescanDevice,
                        onPickPhotoPickerVideos = onPickPhotoPickerVideos,
                        onPickDocumentFiles = onPickDocumentFiles,
                        onPickFolderTree = onPickFolderTree,
                        onOpenStreamDialog = onOpenStreamDialog
                    )
                } else if (isGridMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.continueWatching.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ContinueWatchingSection(
                                    items = uiState.continueWatching,
                                    onPlay = { item ->
                                        val idx = uiState.videos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                        onPlayVideo(uiState.videos, idx)
                                    }
                                )
                            }
                        }
                        gridItems(uiState.videoFolderSummaries, key = { "vfolder_${it.folderName}" }) { folder ->
                            VideoFolderGridCard(
                                folder = folder,
                                onClick = { onOpenFolder(folder.folderName) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                ContinueWatchingSection(
                                    items = uiState.continueWatching,
                                    onPlay = { item ->
                                        val idx = uiState.videos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                        onPlayVideo(uiState.videos, idx)
                                    }
                                )
                            }
                        }
                        items(uiState.videoFolderSummaries, key = { "vfolder_${it.folderName}" }) { folder ->
                            VideoFolderListRow(
                                folder = folder,
                                onClick = { onOpenFolder(folder.folderName) }
                            )
                        }
                    }
                }
            } else if (uiState.videos.isEmpty()) {
                VideoEmptyHeroState(
                    hasAllFilesPermission = hasAllFilesPermission,
                    onRequestAllFilesPermission = onRequestAllFilesPermission,
                    onRescanDevice = onRescanDevice,
                    onPickPhotoPickerVideos = onPickPhotoPickerVideos,
                    onPickDocumentFiles = onPickDocumentFiles,
                    onPickFolderTree = onPickFolderTree,
                    onOpenStreamDialog = onOpenStreamDialog
                )
            } else if (isGridMode) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 172.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!showingFolderDetail && uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueWatchingSection(
                                items = uiState.continueWatching,
                                onPlay = { item ->
                                    val idx = uiState.videos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                    onPlayVideo(uiState.videos, idx)
                                }
                            )
                        }
                    }
                    itemsIndexed(uiState.videos, key = { _, item -> item.uri }) { index, video ->
                        VideoGridCard(
                            video = video,
                            isPlayingNow = video.uri == currentPlayingUri,
                            onPlay = { onPlayVideo(uiState.videos, index) },
                            onToggleLike = { onToggleLike(video) },
                            onAddToPlaylist = { onAddToPlaylist(video) },
                            onDelete = { onDeleteVideo(video) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!showingFolderDetail && uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
                        item {
                            ContinueWatchingSection(
                                items = uiState.continueWatching,
                                onPlay = { item ->
                                    val idx = uiState.videos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                    onPlayVideo(uiState.videos, idx)
                                }
                            )
                        }
                    }
                    itemsIndexed(uiState.videos, key = { _, item -> item.uri }) { index, video ->
                        VideoListRow(
                            video = video,
                            isPlayingNow = video.uri == currentPlayingUri,
                            onPlay = { onPlayVideo(uiState.videos, index) },
                            onToggleLike = { onToggleLike(video) },
                            onAddToPlaylist = { onAddToPlaylist(video) },
                            onDelete = { onDeleteVideo(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoFolderListRow(
    folder: MediaFolderSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_folder_row_${folder.folderName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 10f)
            ) {
                if (folder.previewUri != null) {
                    MediaThumbnail(
                        uri = folder.previewUri,
                        isVideo = true,
                        isStream = false,
                        contentDescription = folder.folderName,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val sizeLabel = formatFileSize(folder.totalSizeBytes)
                val countLabel = "${folder.itemCount} ${if (folder.itemCount == 1) "video" else "videos"}"
                Text(
                    text = listOf(countLabel, sizeLabel).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${folder.itemCount}",
                    style = MonoMetricsStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoFolderGridCard(
    folder: MediaFolderSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_folder_grid_${folder.folderName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                if (folder.previewUri != null) {
                    MediaThumbnail(
                        uri = folder.previewUri,
                        isVideo = true,
                        isStream = false,
                        contentDescription = folder.folderName,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${folder.itemCount} videos",
                        style = MonoMetricsStyle,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeLabel = formatFileSize(folder.totalSizeBytes)
                if (sizeLabel.isNotBlank()) {
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    items: List<MediaItemEntity>,
    onPlay: (MediaItemEntity) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { "cw_${it.uri}" }) { item ->
                val progress = if (item.durationMs > 0L) {
                    (item.lastPlayedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Card(
                    modifier = Modifier
                        .width(210.dp)
                        .clickable { onPlay(item) }
                        .testTag("continue_watching_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            MediaThumbnail(
                                uri = item.uri,
                                isVideo = true,
                                isStream = item.isStream,
                                contentDescription = item.title,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: MediaItemEntity,
    isPlayingNow: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val resBadge = formatResolution(video.width, video.height)
    val progress = if (video.durationMs > 0L && video.lastPlayedPositionMs > 0L) {
        (video.lastPlayedPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("video_grid_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingNow) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                MediaThumbnail(
                    uri = video.uri,
                    isVideo = true,
                    isStream = video.isStream,
                    contentDescription = video.title,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxSize()
                )
                if (resBadge.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = resBadge,
                            style = MonoMetricsStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (video.durationMs > 0L) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            style = MonoMetricsStyle,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(video.folderName, formatFileSize(video.fileSizeBytes))
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More actions")
                    }
                    VideoActionMenu(
                        expanded = menuOpen,
                        isLiked = video.isLiked,
                        onDismiss = { menuOpen = false },
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoListRow(
    video: MediaItemEntity,
    isPlayingNow: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val resBadge = formatResolution(video.width, video.height)
    val progress = if (video.durationMs > 0L && video.lastPlayedPositionMs > 0L) {
        (video.lastPlayedPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("video_list_row"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingNow) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(126.dp)
                    .aspectRatio(16f / 9f)
            ) {
                MediaThumbnail(
                    uri = video.uri,
                    isVideo = true,
                    isStream = video.isStream,
                    contentDescription = video.title,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize()
                )
                if (video.durationMs > 0L) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.74f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            style = MonoMetricsStyle,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resBadge.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = resBadge,
                                style = MonoMetricsStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = listOf(video.folderName, formatFileSize(video.fileSizeBytes))
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (video.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isLiked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                }
                VideoActionMenu(
                    expanded = menuOpen,
                    isLiked = video.isLiked,
                    onDismiss = { menuOpen = false },
                    onToggleLike = onToggleLike,
                    onAddToPlaylist = onAddToPlaylist,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun VideoActionMenu(
    expanded: Boolean,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isLiked) "Remove from Liked" else "Like Video") },
            leadingIcon = {
                Icon(
                    if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null
                )
            },
            onClick = {
                onToggleLike()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
            onClick = {
                onAddToPlaylist()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Remove from Library") },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}

@Composable
private fun VideoEmptyHeroState(
    hasAllFilesPermission: Boolean,
    onRequestAllFilesPermission: () -> Unit,
    onRescanDevice: () -> Unit,
    onPickPhotoPickerVideos: () -> Unit,
    onPickDocumentFiles: () -> Unit,
    onPickFolderTree: () -> Unit,
    onOpenStreamDialog: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_video_empty_hero),
                            contentDescription = "AetherPlay Cinema Studio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Cinema Engine Ready",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "MKV, MP4, HEVC, AV1, WebM, AVI, FLV, HLS & DASH with hardware decoding and gesture controls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = if (hasAllFilesPermission) onRescanDevice else onRequestAllFilesPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("empty_scan_all_videos_button")
                        ) {
                            Icon(
                                if (hasAllFilesPermission) Icons.Rounded.Refresh else Icons.Rounded.FolderOpen,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (hasAllFilesPermission) "Scan Device Video Folders" else "Allow All Files & Scan Device")
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = onPickPhotoPickerVideos,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("empty_pick_videos_button")
                            ) {
                                Icon(Icons.Rounded.Movie, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = onPickFolderTree,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("empty_scan_folder_button")
                            ) {
                                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Folder", maxLines = 1)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onPickDocumentFiles,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("empty_open_files_button")
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Any Format", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenStreamDialog,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("empty_stream_button")
                            ) {
                                Icon(Icons.Rounded.Link, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stream URL", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
