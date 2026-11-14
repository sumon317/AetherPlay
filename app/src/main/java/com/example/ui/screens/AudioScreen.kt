package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.MonoMetricsStyle
import com.example.ui.viewmodel.AudioFilterTab
import com.example.ui.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    uiState: LibraryUiState,
    currentPlayingUri: String?,
    onSearchChange: (String) -> Unit,
    onSelectFilter: (AudioFilterTab) -> Unit,
    onPlayAudio: (List<MediaItemEntity>, Int) -> Unit,
    onShufflePlay: (List<MediaItemEntity>) -> Unit,
    onToggleLike: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onDeleteAudio: (MediaItemEntity) -> Unit,
    onPickAudioFiles: () -> Unit,
    onPickFolderTree: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Audio Studio", style = MaterialTheme.typography.headlineMedium)
                        if (uiState.audioTracks.isNotEmpty()) {
                            Text(
                                text = "${uiState.audioTracks.size} tracks • Hi-Res Ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("audio_eq_button")
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Open Equalizer")
                    }
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("audio_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search audio"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showImportMenu = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("audio_import_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Import audio")
                }
                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Pick Audio Files (MP3, FLAC, OPUS...)") },
                        leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onPickAudioFiles()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan Music Folder") },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            showImportMenu = false
                            onPickFolderTree()
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
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search songs, artists, albums...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(AudioFilterTab.entries) { tab ->
                        FilterChip(
                            selected = uiState.audioFilter == tab,
                            onClick = { onSelectFilter(tab) },
                            label = { Text(tab.label) }
                        )
                    }
                }
                if (uiState.audioTracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { onShufflePlay(uiState.audioTracks) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("audio_shuffle_all")
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle")
                    }
                }
            }

            if (uiState.audioTracks.isEmpty()) {
                AudioEmptyState(
                    isLikedFilter = uiState.audioFilter == AudioFilterTab.LIKED,
                    onPickAudioFiles = onPickAudioFiles,
                    onPickFolderTree = onPickFolderTree,
                    onOpenEqualizer = onOpenEqualizer
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.audioTracks, key = { _, item -> item.uri }) { index, track ->
                        AudioTrackRow(
                            track = track,
                            isCurrent = track.uri == currentPlayingUri,
                            onPlay = { onPlayAudio(uiState.audioTracks, index) },
                            onToggleLike = { onToggleLike(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onDelete = { onDeleteAudio(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(
    track: MediaItemEntity,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("audio_track_row")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnail(
                uri = track.uri,
                isVideo = false,
                isStream = track.isStream,
                contentDescription = track.title,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (track.durationMs > 0L) {
                Text(
                    text = formatDuration(track.durationMs),
                    style = MonoMetricsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like track",
                    tint = if (track.isLiked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Track options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            onAddToPlaylist()
                            menuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Library") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            onDelete()
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioEmptyState(
    isLikedFilter: Boolean,
    onPickAudioFiles: () -> Unit,
    onPickFolderTree: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_audio_artwork_fallback),
                        contentDescription = "Audio studio artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                )
                            )
                    )
                }
                Text(
                    text = if (isLikedFilter) "No Liked Songs Yet" else "Hi-Fi Audio Library",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (isLikedFilter) {
                        "Tap the heart icon on any song or music track to collect your favorites here."
                    } else {
                        "Import MP3, FLAC, AAC, Opus, Vorbis, WAV, or ALAC files with seamless background playback and a 5-band studio equalizer."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onPickAudioFiles,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("empty_pick_audio_button")
                ) {
                    Icon(Icons.Rounded.Audiotrack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Audio Files")
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
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Folder")
                    }
                    FilledTonalButton(
                        onClick = onOpenEqualizer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Equalizer")
                    }
                }
            }
        }
    }
}
