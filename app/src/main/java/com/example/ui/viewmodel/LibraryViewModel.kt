package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlaylistWithItems
import com.example.data.local.SettingsRepository
import com.example.data.local.UserPreferences
import com.example.data.repository.MediaRepository
import com.example.player.PlayerControllerHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaSortOrder(val label: String) {
    RECENT("Recently Added"),
    NAME_ASC("Title A–Z"),
    DURATION_DESC("Longest First"),
    SIZE_DESC("Largest Size")
}

enum class AudioFilterTab(val label: String) {
    ALL("All Audio"),
    LIKED("Liked Songs"),
    RECENT("Recently Played")
}

data class MediaFolderSummary(
    val folderName: String,
    val itemCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val previewUri: String?,
    val isVideo: Boolean
)

data class LibraryUiState(
    val videos: List<MediaItemEntity> = emptyList(),
    val audioTracks: List<MediaItemEntity> = emptyList(),
    val likedItems: List<MediaItemEntity> = emptyList(),
    val continueWatching: List<MediaItemEntity> = emptyList(),
    val playlists: List<PlaylistWithItems> = emptyList(),
    val videoFolders: List<String> = listOf("All"),
    val videoFolderSummaries: List<MediaFolderSummary> = emptyList(),
    val audioFolderSummaries: List<MediaFolderSummary> = emptyList(),
    val isVideoFolderView: Boolean = true,
    val isAudioFolderView: Boolean = true,
    val openedVideoFolder: String? = null,
    val openedAudioFolder: String? = null,
    val selectedFolder: String = "All",
    val searchQuery: String = "",
    val sortOrder: MediaSortOrder = MediaSortOrder.RECENT,
    val audioFilter: AudioFilterTab = AudioFilterTab.ALL,
    val isScanning: Boolean = false,
    val statusBanner: String? = null
)

class LibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val playerController: PlayerControllerHolder
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFolder = MutableStateFlow("All")
    private val sortOrder = MutableStateFlow(MediaSortOrder.RECENT)
    private val audioFilter = MutableStateFlow(AudioFilterTab.ALL)
    private val openedVideoFolder = MutableStateFlow<String?>(null)
    private val openedAudioFolder = MutableStateFlow<String?>(null)
    private val isScanning = MutableStateFlow(false)
    private val statusBanner = MutableStateFlow<String?>(null)

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val libraryInputs = combine(
        mediaRepository.videosFlow,
        mediaRepository.audioFlow,
        mediaRepository.likedFlow,
        mediaRepository.continueWatchingFlow,
        mediaRepository.playlistsFlow
    ) { videos, audio, liked, recent, playlists ->
        LibraryDataBundle(videos, audio, liked, recent, playlists)
    }

    private val viewModeInputs = combine(
        settingsRepository.preferencesFlow,
        openedVideoFolder,
        openedAudioFolder
    ) { prefs, openV, openA ->
        FolderViewBundle(
            isVideoFolderView = prefs.videoFolderView,
            isAudioFolderView = prefs.audioFolderView,
            openedVideoFolder = openV,
            openedAudioFolder = openA,
            minVideoDurationSeconds = prefs.minVideoDurationSeconds,
            minAudioDurationSeconds = prefs.minAudioDurationSeconds
        )
    }

    private val filterInputs = combine(
        searchQuery,
        selectedFolder,
        sortOrder,
        audioFilter,
        isScanning,
        statusBanner
    ) { arr ->
        FilterBundle(
            query = arr[0] as String,
            folder = arr[1] as String,
            sort = arr[2] as MediaSortOrder,
            audioTab = arr[3] as AudioFilterTab,
            scanning = arr[4] as Boolean,
            banner = arr[5] as String?
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryInputs,
        filterInputs,
        viewModeInputs
    ) { data, filters, folderMode ->
        val minVideoMs = folderMode.minVideoDurationSeconds * 1000L
        val minAudioMs = folderMode.minAudioDurationSeconds * 1000L

        val eligibleVideos = if (minVideoMs > 0L) {
            data.videos.filter { it.isStream || it.durationMs <= 0L || it.durationMs >= minVideoMs }
        } else {
            data.videos
        }
        val eligibleAudio = if (minAudioMs > 0L) {
            data.audio.filter { it.isStream || it.durationMs <= 0L || it.durationMs >= minAudioMs }
        } else {
            data.audio
        }

        val folders = listOf("All") + eligibleVideos.map { it.folderName }.distinct().sorted()
        val activeFolder = if (filters.folder in folders) filters.folder else "All"

        val validOpenedVideoFolder = folderMode.openedVideoFolder?.takeIf { target ->
            eligibleVideos.any { it.folderName == target }
        }
        val validOpenedAudioFolder = folderMode.openedAudioFolder?.takeIf { target ->
            eligibleAudio.any { it.folderName == target }
        }

        val videoFolderSummaries = eligibleVideos
            .groupBy { it.folderName.ifBlank { "Internal Storage" } }
            .map { (folderName, items) ->
                MediaFolderSummary(
                    folderName = folderName,
                    itemCount = items.size,
                    totalDurationMs = items.sumOf { it.durationMs.coerceAtLeast(0L) },
                    totalSizeBytes = items.sumOf { it.fileSizeBytes.coerceAtLeast(0L) },
                    previewUri = items.firstOrNull()?.uri,
                    isVideo = true
                )
            }
            .filter { summary ->
                filters.query.isBlank() ||
                    summary.folderName.contains(filters.query, ignoreCase = true) ||
                    eligibleVideos.any {
                        it.folderName == summary.folderName &&
                            it.title.contains(filters.query, ignoreCase = true)
                    }
            }
            .sortedBy { it.folderName.lowercase() }

        val audioFolderSummaries = eligibleAudio
            .groupBy { it.folderName.ifBlank { "Internal Storage" } }
            .map { (folderName, items) ->
                MediaFolderSummary(
                    folderName = folderName,
                    itemCount = items.size,
                    totalDurationMs = items.sumOf { it.durationMs.coerceAtLeast(0L) },
                    totalSizeBytes = items.sumOf { it.fileSizeBytes.coerceAtLeast(0L) },
                    previewUri = items.firstOrNull()?.uri,
                    isVideo = false
                )
            }
            .filter { summary ->
                filters.query.isBlank() ||
                    summary.folderName.contains(filters.query, ignoreCase = true) ||
                    eligibleAudio.any {
                        it.folderName == summary.folderName && (
                            it.title.contains(filters.query, ignoreCase = true) ||
                                it.artist.contains(filters.query, ignoreCase = true)
                            )
                    }
            }
            .sortedBy { it.folderName.lowercase() }

        val filteredVideos = eligibleVideos
            .filter { item ->
                when {
                    folderMode.isVideoFolderView && validOpenedVideoFolder != null ->
                        item.folderName == validOpenedVideoFolder
                    folderMode.isVideoFolderView && filters.query.isBlank() ->
                        true
                    else ->
                        activeFolder == "All" || item.folderName == activeFolder
                }
            }
            .filter {
                filters.query.isBlank() ||
                    it.title.contains(filters.query, ignoreCase = true) ||
                    it.folderName.contains(filters.query, ignoreCase = true)
            }
            .sortWithOrder(filters.sort)

        val baseAudio = when (filters.audioTab) {
            AudioFilterTab.ALL -> {
                if (folderMode.isAudioFolderView && validOpenedAudioFolder != null) {
                    eligibleAudio.filter { it.folderName == validOpenedAudioFolder }
                } else {
                    eligibleAudio
                }
            }
            AudioFilterTab.LIKED -> data.liked.filter { !it.isVideo }
            AudioFilterTab.RECENT -> data.continueWatching.filter { !it.isVideo }
        }

        val filteredAudio = baseAudio
            .filter {
                filters.query.isBlank() ||
                    it.title.contains(filters.query, ignoreCase = true) ||
                    it.artist.contains(filters.query, ignoreCase = true) ||
                    it.album.contains(filters.query, ignoreCase = true) ||
                    it.folderName.contains(filters.query, ignoreCase = true)
            }
            .sortWithOrder(filters.sort)

        LibraryUiState(
            videos = filteredVideos,
            audioTracks = filteredAudio,
            likedItems = data.liked,
            continueWatching = data.continueWatching.filter { it.isVideo && it.lastPlayedPositionMs > 0L },
            playlists = data.playlists,
            videoFolders = folders,
            videoFolderSummaries = videoFolderSummaries,
            audioFolderSummaries = audioFolderSummaries,
            isVideoFolderView = folderMode.isVideoFolderView,
            isAudioFolderView = folderMode.isAudioFolderView,
            openedVideoFolder = validOpenedVideoFolder,
            openedAudioFolder = validOpenedAudioFolder,
            selectedFolder = activeFolder,
            searchQuery = filters.query,
            sortOrder = filters.sort,
            audioFilter = filters.audioTab,
            isScanning = filters.scanning,
            statusBanner = filters.banner
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun observePlaylist(playlistId: Long): StateFlow<PlaylistWithItems?> =
        mediaRepository.observePlaylist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedFolder(folder: String) {
        selectedFolder.value = folder
    }

    fun setSortOrder(order: MediaSortOrder) {
        sortOrder.value = order
    }

    fun setAudioFilter(tab: AudioFilterTab) {
        audioFilter.value = tab
    }

    fun openVideoFolder(folderName: String) {
        openedVideoFolder.value = folderName
    }

    fun closeVideoFolder() {
        openedVideoFolder.value = null
    }

    fun toggleVideoFolderView() {
        viewModelScope.launch {
            val next = settingsRepository.toggleVideoFolderView()
            if (!next) openedVideoFolder.value = null
        }
    }

    fun openAudioFolder(folderName: String) {
        openedAudioFolder.value = folderName
    }

    fun closeAudioFolder() {
        openedAudioFolder.value = null
    }

    fun toggleAudioFolderView() {
        viewModelScope.launch {
            val next = settingsRepository.toggleAudioFolderView()
            if (!next) openedAudioFolder.value = null
        }
    }

    fun toggleVideoGridMode() {
        viewModelScope.launch {
            settingsRepository.toggleVideoGridMode()
        }
    }

    fun clearStatusBanner() {
        statusBanner.value = null
    }

    fun scanAllDeviceMedia(silentIfEmpty: Boolean = true) {
        if (isScanning.value) return
        viewModelScope.launch {
            isScanning.value = true
            val count = mediaRepository.scanAllDeviceMedia()
            isScanning.value = false
            if (count > 0 || !silentIfEmpty) {
                statusBanner.value = if (count > 0) {
                    "Scanned device: $count audio & video files ready"
                } else {
                    "No new media files found on device"
                }
            }
        }
    }

    fun importFiles(uris: List<Uri>, autoPlayFirst: Boolean = false, onOpenVideo: () -> Unit = {}, onOpenAudio: () -> Unit = {}) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isScanning.value = true
            val imported = mediaRepository.importDocumentUris(uris)
            isScanning.value = false
            statusBanner.value = if (imported.isEmpty()) {
                "No supported media formats found in selection"
            } else {
                "Imported ${imported.size} media ${if (imported.size == 1) "file" else "files"}"
            }
            if (autoPlayFirst && imported.isNotEmpty()) {
                val first = imported.first()
                playerController.playQueue(imported, 0)
                if (first.isVideo) onOpenVideo() else onOpenAudio()
            }
        }
    }

    fun scanFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            isScanning.value = true
            val count = mediaRepository.scanFolderTree(treeUri)
            isScanning.value = false
            statusBanner.value = if (count > 0) {
                "Scanned and added $count media items"
            } else {
                "No supported video or audio files found in folder"
            }
        }
    }

    fun addAndPlayStream(url: String, title: String?, isVideo: Boolean, onOpenVideo: () -> Unit, onOpenAudio: () -> Unit) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val item = mediaRepository.importStreamUrl(url, title, isVideo)
            if (item == null) {
                statusBanner.value = "Invalid stream URL"
                return@launch
            }
            playerController.playSingle(item, resumeSavedPosition = false)
            if (item.isVideo) onOpenVideo() else onOpenAudio()
        }
    }

    fun openExternalUri(uri: Uri, onOpenVideo: () -> Unit, onOpenAudio: () -> Unit) {
        viewModelScope.launch {
            val item = mediaRepository.importSingleExternalUri(uri) ?: return@launch
            playerController.playSingle(item)
            if (item.isVideo) onOpenVideo() else onOpenAudio()
        }
    }

    fun playMediaQueue(items: List<MediaItemEntity>, index: Int) {
        playerController.playQueue(items, index)
    }

    fun toggleLike(item: MediaItemEntity) {
        viewModelScope.launch {
            mediaRepository.toggleLike(item.uri, !item.isLiked)
        }
    }

    fun deleteMediaItem(uri: String) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(uri)
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.createPlaylist(name, description)
            statusBanner.value = "Created playlist \"$name\""
        }
    }

    fun renamePlaylist(playlistId: Long, name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.renamePlaylist(playlistId, name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            mediaRepository.deletePlaylist(playlistId)
        }
    }

    fun addToPlaylist(playlistId: Long, mediaUri: String, playlistName: String) {
        viewModelScope.launch {
            mediaRepository.addToPlaylist(playlistId, mediaUri)
            statusBanner.value = "Added to $playlistName"
        }
    }

    fun removeFromPlaylist(playlistId: Long, mediaUri: String) {
        viewModelScope.launch {
            mediaRepository.removeFromPlaylist(playlistId, mediaUri)
        }
    }

    fun restoreToPlaylist(playlistId: Long, mediaUri: String) {
        viewModelScope.launch {
            mediaRepository.addToPlaylist(playlistId, mediaUri)
        }
    }

    private fun List<MediaItemEntity>.sortWithOrder(order: MediaSortOrder): List<MediaItemEntity> = when (order) {
        MediaSortOrder.RECENT -> sortedByDescending { it.addedAt }
        MediaSortOrder.NAME_ASC -> sortedBy { it.title.lowercase() }
        MediaSortOrder.DURATION_DESC -> sortedByDescending { it.durationMs }
        MediaSortOrder.SIZE_DESC -> sortedByDescending { it.fileSizeBytes }
    }

    private data class LibraryDataBundle(
        val videos: List<MediaItemEntity>,
        val audio: List<MediaItemEntity>,
        val liked: List<MediaItemEntity>,
        val continueWatching: List<MediaItemEntity>,
        val playlists: List<PlaylistWithItems>
    )

    private data class FolderViewBundle(
        val isVideoFolderView: Boolean,
        val isAudioFolderView: Boolean,
        val openedVideoFolder: String?,
        val openedAudioFolder: String?,
        val minVideoDurationSeconds: Int,
        val minAudioDurationSeconds: Int
    )

    private data class FilterBundle(
        val query: String,
        val folder: String,
        val sort: MediaSortOrder,
        val audioTab: AudioFilterTab,
        val scanning: Boolean,
        val banner: String?
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    mediaRepository = container.mediaRepository,
                    settingsRepository = container.settingsRepository,
                    playerController = container.playerControllerHolder
                )
            }
        }
    }
}
