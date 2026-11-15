package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlaylistWithItems
import com.example.player.MediaSafety

@Composable
fun OpenStreamDialog(
    onDismiss: () -> Unit,
    onConfirmStream: (url: String, title: String?, isVideo: Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(true) }
    val isUrlValid = remember(url) { MediaSafety.isValidStreamUrl(url) }
    val showUrlError = url.isNotBlank() && !isUrlValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Network Stream") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Supports HTTP/HTTPS, RTSP, HLS (.m3u8), DASH (.mpd), MKV, MP4, FLAC, MP3, and Opus streams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/stream.m3u8") },
                    isError = showUrlError,
                    supportingText = if (showUrlError) {
                        { Text("Enter a valid http://, https://, or rtsp:// URL") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_title_input")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isVideo,
                        onClick = { isVideo = true },
                        label = { Text("Video Stream") },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null) }
                    )
                    FilterChip(
                        selected = !isVideo,
                        onClick = { isVideo = false },
                        label = { Text("Audio Stream") },
                        leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = url.trim()
                    if (MediaSafety.isValidStreamUrl(trimmed)) {
                        val safeTitle = MediaSafety.sanitizeDisplayText(title, 100).ifBlank { null }
                        onConfirmStream(trimmed, safeTitle, isVideo)
                        onDismiss()
                    }
                },
                enabled = isUrlValid,
                modifier = Modifier.testTag("stream_play_button")
            ) {
                Text("Play Stream")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    initialDescription: String = "",
    dialogTitle: String = "New Playlist",
    confirmLabel: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("playlist_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    mediaItem: MediaItemEntity,
    playlists: List<PlaylistWithItems>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistWithItems) -> Unit,
    onCreatePlaylist: (name: String) -> Unit
) {
    var showCreateInline by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = mediaItem.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (showCreateInline) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreatePlaylist(newName.trim())
                                newName = ""
                                showCreateInline = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showCreateInline = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Playlist")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists yet. Create one above to organize your videos and tracks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn {
                    items(playlists, key = { it.playlist.id }) { pw ->
                        ListItem(
                            headlineContent = { Text(pw.playlist.name) },
                            supportingContent = { Text("${pw.items.size} items") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onSelectPlaylist(pw)
                                onDismiss()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
