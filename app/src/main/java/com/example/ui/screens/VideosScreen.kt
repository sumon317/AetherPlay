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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.example.ui.viewmodel.MediaFolder
import com.example.ui.viewmodel.MediaSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    uiState: LibraryUiState,
    isGridMode: Boolean,
    isFolderView: Boolean,
    hasStoragePermission: Boolean,
    currentPlayingUri: String?,
    onSearchChange: (String) -> Unit,
    onSelectFolderKey: (String?) -> Unit,
    onSelectSort: (MediaSortOrder) -> Unit,
    onToggleGridMode: () -> Unit,
    onToggleFolderView: () -> Unit,
    onRescanDevice: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onPlayVideo: (List<MediaItemEntity>, Int) -> Unit,
    onToggleLike: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onDeleteVideo: (MediaItemEntity) -> Unit,
    onPickPhotoPickerVideos: () -> Unit,
    onPickDocumentFiles: () -> Unit,
    onPickFolderTree: () -> Unit,
    onOpenStreamDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    val activeFolder = uiState.activeVideoFolder
    val isShowingFolderList = isFolderView && activeFolder == null && uiState.searchQuery.isBlank()

    BackHandler(enabled = activeFolder != null) {
        onSelectFolderKey(null)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                ),
                navigationIcon = {
                    if (activeFolder != null) {
                        IconButton(
                            onClick = { onSelectFolderKey(null) },
                            modifier = Modifier.testTag("videos_folder_back")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to folders")
                        }
                    }
                },
                title = {
                    if (activeFolder != null) {
                        Text(
                            text = activeFolder.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("videos_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search videos"
                        )
                    }
                    if (activeFolder == null) {
                        IconButton(
                            onClick = onToggleFolderView,
                            modifier = Modifier.testTag("videos_folder_view_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFolderView) Icons.Rounded.VideoLibrary else Icons.Rounded.Folder,
                                contentDescription = if (isFolderView) "Switch to all videos" else "Switch to folder view",
                                tint = if (isFolderView) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleGridMode,
                        modifier = Modifier.testTag("videos_layout_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGridMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = if (isGridMode) "Switch to list view" else "Switch to grid view",
                            tint = if (isGridMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
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
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("videos_more_button")
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rescan Device Storage") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (hasStoragePermission) onRescanDevice() else onRequestStoragePermission()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open Stream URL") },
                                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenStreamDialog()
                                }
                            )
                        }
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
                        text = { Text("Rescan Device Storage") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            if (hasStoragePermission) onRescanDevice() else onRequestStoragePermission()
                        }
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!hasStoragePermission) {
                StoragePermissionBanner(
                    onGrantClick = onRequestStoragePermission,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            if (activeFolder != null) "Search in ${activeFolder.name}..."
                            else "Search movies, clips, folders..."
                        )
                    },
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

            if (isShowingFolderList) {
                if (uiState.videoFolders.isEmpty()) {
                    VideoEmptyHeroState(
                        hasStoragePermission = hasStoragePermission,
                        onRequestStoragePermission = onRequestStoragePermission,
                        onRescanDevice = onRescanDevice,
                        onPickPhotoPickerVideos = onPickPhotoPickerVideos,
                        onPickDocumentFiles = onPickDocumentFiles,
                        onPickFolderTree = onPickFolderTree,
                        onOpenStreamDialog = onOpenStreamDialog
                    )
                } else if (isGridMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
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
                        items(uiState.videoFolders, key = { it.key }) { folder ->
                            VideoFolderGridCard(
                                folder = folder,
                                onClick = { onSelectFolderKey(folder.key) }
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
                        items(uiState.videoFolders, key = { it.key }) { folder ->
                            VideoFolderListRow(
                                folder = folder,
                                onClick = { onSelectFolderKey(folder.key) }
                            )
                        }
                    }
                }
            } else if (uiState.videos.isEmpty()) {
                VideoEmptyHeroState(
                    hasStoragePermission = hasStoragePermission,
                    onRequestStoragePermission = onRequestStoragePermission,
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
                    if (activeFolder == null && uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
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
                    if (activeFolder == null && uiState.continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
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
fun StoragePermissionBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Allow All Files Access",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Grant permission to scan all device folders automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onGrantClick) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun VideoFolderListRow(
    folder: MediaFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_folder_row"),
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
                val preview = folder.previewUri
                if (preview != null) {
                    MediaThumbnail(
                        uri = preview,
                        isVideo = true,
                        isStream = preview.startsWith("http"),
                        contentDescription = folder.name,
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = folder.pathLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val sizeStr = formatFileSize(folder.totalSizeBytes)
                val countStr = "${folder.items.size} ${if (folder.items.size == 1) "video" else "videos"}"
                Text(
                    text = listOf(countStr, sizeStr).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MonoMetricsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VideoFolderGridCard(
    folder: MediaFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_folder_grid_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                val preview = folder.previewUri
                if (preview != null) {
                    MediaThumbnail(
                        uri = preview,
                        isVideo = true,
                        isStream = preview.startsWith("http"),
                        contentDescription = folder.name,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${folder.items.size}",
                            style = MonoMetricsStyle,
                            color = Color.White
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        "${folder.items.size} ${if (folder.items.size == 1) "video" else "videos"}",
                        formatFileSize(folder.totalSizeBytes)
                    ).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .aspectRatio(16f / 9f)
                ) {
                    MediaThumbnail(
                        uri = video.uri,
                        isVideo = true,
                        isStream = video.isStream,
                        contentDescription = video.title,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    if (video.durationMs > 0L) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.74f),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = formatDuration(video.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                val sizeStr = formatFileSize(video.fileSizeBytes)
                val metaText = when {
                    sizeStr.isNotEmpty() -> sizeStr
                    video.isStream -> "Stream"
                    else -> video.folderName
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    if (metaText.isNotBlank()) {
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (video.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (video.isLiked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(20.dp)
                            )
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
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
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
                            onClick = if (hasStoragePermission) onRescanDevice else onRequestStoragePermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("empty_scan_device_videos_button")
                        ) {
                            Icon(
                                imageVector = if (hasStoragePermission) Icons.Rounded.Refresh else Icons.Rounded.FolderOpen,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (hasStoragePermission) "Scan Device Videos" else "Allow All Files & Scan")
                        }
                        FilledTonalButton(
                            onClick = onPickPhotoPickerVideos,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("empty_pick_videos_button")
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Videos from Gallery")
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                Text("Scan Folder", maxLines = 1)
                            }
                            FilledTonalButton(
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
                        }
                        OutlinedButton(
                            onClick = onOpenStreamDialog,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("empty_stream_button")
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Network Stream URL")
                        }
                    }
                }
            }
        }
    }
}
