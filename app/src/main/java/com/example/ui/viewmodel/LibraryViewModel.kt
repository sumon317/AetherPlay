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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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

data class MediaFolder(
    val key: String,
    val name: String,
    val pathLabel: String,
    val items: List<MediaItemEntity>,
    val totalSizeBytes: Long,
    val totalDurationMs: Long,
    val previewUri: String?,
    val isVideoFolder: Boolean
)

data class LibraryUiState(
    val videos: List<MediaItemEntity> = emptyList(),
    val allVideosCount: Int = 0,
    val audioTracks: List<MediaItemEntity> = emptyList(),
    val allAudioCount: Int = 0,
    val likedItems: List<MediaItemEntity> = emptyList(),
    val continueWatching: List<MediaItemEntity> = emptyList(),
    val playlists: List<PlaylistWithItems> = emptyList(),
    val videoFolders: List<MediaFolder> = emptyList(),
    val selectedVideoFolderKey: String? = null,
    val audioFolders: List<MediaFolder> = emptyList(),
    val selectedAudioFolderKey: String? = null,
    val searchQuery: String = "",
    val sortOrder: MediaSortOrder = MediaSortOrder.RECENT,
    val audioFilter: AudioFilterTab = AudioFilterTab.ALL,
    val isScanning: Boolean = false,
    val statusBanner: String? = null
) {
    val activeVideoFolder: MediaFolder?
        get() = selectedVideoFolderKey?.let { key -> videoFolders.firstOrNull { it.key == key } }

    val activeAudioFolder: MediaFolder?
        get() = selectedAudioFolderKey?.let { key -> audioFolders.firstOrNull { it.key == key } }
}

class LibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val playerController: PlayerControllerHolder
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedVideoFolderKey = MutableStateFlow<String?>(null)
    private val selectedAudioFolderKey = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(MediaSortOrder.RECENT)
    private val audioFilter = MutableStateFlow(AudioFilterTab.ALL)
    private val isScanning = MutableStateFlow(false)
    private val statusBanner = MutableStateFlow<String?>(null)

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val libraryInputs = combine(
        mediaRepository.videosFlow,
        mediaRepository.audioFlow,
        mediaRepository.likedFlow,
        mediaRepository.continueWatchingFlow,
        mediaRepository.playlistsFlow
    ) { videos, audio, liked, recent, playlists ->
        LibraryDataBundle(videos, audio, liked, recent, playlists)
    }

    private val filterInputs = combine<Any?, FilterBundle>(
        searchQuery,
        selectedVideoFolderKey,
        selectedAudioFolderKey,
        sortOrder,
        audioFilter,
        isScanning,
        statusBanner
    ) { arr ->
        FilterBundle(
            query = arr[0] as String,
            videoFolderKey = arr[1] as String?,
            audioFolderKey = arr[2] as String?,
            sort = arr[3] as MediaSortOrder,
            audioTab = arr[4] as AudioFilterTab,
            scanning = arr[5] as Boolean,
            banner = arr[6] as String?
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryInputs,
        filterInputs,
        settingsRepository.preferencesFlow
    ) { data, filters, prefs ->
        val visibleVideos = filterByMinDuration(data.videos, prefs.minVideoDurationSeconds)
        val visibleAudio = filterByMinDuration(data.audio, prefs.minAudioDurationSeconds)
        val visibleLiked = data.liked.filter { item ->
            val minSec = if (item.isVideo) prefs.minVideoDurationSeconds else prefs.minAudioDurationSeconds
            passesMinDuration(item, minSec)
        }
        val visibleContinueWatching = data.continueWatching.filter { item ->
            item.isVideo && item.lastPlayedPositionMs > 0L && passesMinDuration(item, prefs.minVideoDurationSeconds)
        }

        val allVideoFolders = buildFolders(visibleVideos, isVideo = true, sortOrder = filters.sort)
        val activeVideoKey = filters.videoFolderKey?.takeIf { key -> allVideoFolders.any { it.key == key } }

        val videoBase = if (activeVideoKey != null) {
            allVideoFolders.firstOrNull { it.key == activeVideoKey }?.items.orEmpty()
        } else {
            visibleVideos
        }

        val filteredVideos = videoBase
            .filter {
                filters.query.isBlank() ||
                    it.title.contains(filters.query, ignoreCase = true) ||
                    it.folderName.contains(filters.query, ignoreCase = true)
            }
            .sortWithOrder(filters.sort)

        val allAudioFolders = buildFolders(visibleAudio, isVideo = false, sortOrder = filters.sort)
        val activeAudioKey = filters.audioFolderKey?.takeIf { key -> allAudioFolders.any { it.key == key } }

        val baseAudio = when (filters.audioTab) {
            AudioFilterTab.ALL -> {
                if (activeAudioKey != null) {
                    allAudioFolders.firstOrNull { it.key == activeAudioKey }?.items.orEmpty()
                } else {
                    visibleAudio
                }
            }
            AudioFilterTab.LIKED -> visibleLiked.filter { !it.isVideo }
            AudioFilterTab.RECENT -> data.continueWatching.filter {
                !it.isVideo && passesMinDuration(it, prefs.minAudioDurationSeconds)
            }
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
            allVideosCount = visibleVideos.size,
            audioTracks = filteredAudio,
            allAudioCount = visibleAudio.size,
            likedItems = visibleLiked,
            continueWatching = visibleContinueWatching,
            playlists = data.playlists,
            videoFolders = allVideoFolders,
            selectedVideoFolderKey = activeVideoKey,
            audioFolders = allAudioFolders,
            selectedAudioFolderKey = activeAudioKey,
            searchQuery = filters.query,
            sortOrder = filters.sort,
            audioFilter = filters.audioTab,
            isScanning = filters.scanning,
            statusBanner = filters.banner
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun observePlaylist(playlistId: Long): StateFlow<PlaylistWithItems?> =
        mediaRepository.observePlaylist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedVideoFolderKey(folderKey: String?) {
        selectedVideoFolderKey.value = folderKey
    }

    fun setSelectedAudioFolderKey(folderKey: String?) {
        selectedAudioFolderKey.value = folderKey
    }

    fun setSortOrder(order: MediaSortOrder) {
        sortOrder.value = order
    }

    fun setAudioFilter(tab: AudioFilterTab) {
        audioFilter.value = tab
        if (tab != AudioFilterTab.ALL) {
            selectedAudioFolderKey.value = null
        }
    }

    fun toggleVideoGridMode() {
        viewModelScope.launch {
            settingsRepository.toggleVideoGridMode()
        }
    }

    fun toggleVideoFolderView() {
        viewModelScope.launch {
            val next = settingsRepository.toggleVideoFolderView()
            if (!next) selectedVideoFolderKey.value = null
        }
    }

    fun toggleAudioFolderView() {
        viewModelScope.launch {
            val next = settingsRepository.toggleAudioFolderView()
            if (!next) selectedAudioFolderKey.value = null
        }
    }

    fun clearStatusBanner() {
        statusBanner.value = null
    }

    fun scanDeviceMedia(showSummaryBanner: Boolean = false) {
        if (isScanning.value) return
        viewModelScope.launch {
            isScanning.value = true
            val result = runCatching { mediaRepository.scanDeviceMedia() }.getOrNull()
            isScanning.value = false
            if (result != null) {
                if (showSummaryBanner || result.newlyAdded > 0) {
                    statusBanner.value = when {
                        result.newlyAdded > 0 ->
                            "Found ${result.newlyAdded} new media ${if (result.newlyAdded == 1) "file" else "files"}"
                        result.totalScanned > 0 ->
                            "Library up to date (${result.totalScanned} files scanned)"
                        else ->
                            "No audio or video files found on device"
                    }
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
        val trimmed = url.trim()
        if (!com.example.player.MediaSafety.isValidStreamUrl(trimmed)) {
            statusBanner.value = "Invalid stream URL"
            return
        }
        viewModelScope.launch {
            val item = mediaRepository.importStreamUrl(trimmed, title, isVideo) ?: run {
                statusBanner.value = "Unsupported stream URL"
                return@launch
            }
            playerController.playSingle(item, resumeSavedPosition = false)
            if (item.isVideo) onOpenVideo() else onOpenAudio()
        }
    }

    fun openExternalUri(uri: Uri, onOpenVideo: () -> Unit, onOpenAudio: () -> Unit) {
        viewModelScope.launch {
            val item = mediaRepository.importSingleExternalUri(uri) ?: run {
                statusBanner.value = "Unable to open media URI"
                return@launch
            }
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

    private data class LibraryDataBundle(
        val videos: List<MediaItemEntity>,
        val audio: List<MediaItemEntity>,
        val liked: List<MediaItemEntity>,
        val continueWatching: List<MediaItemEntity>,
        val playlists: List<PlaylistWithItems>
    )

    private data class FilterBundle(
        val query: String,
        val videoFolderKey: String?,
        val audioFolderKey: String?,
        val sort: MediaSortOrder,
        val audioTab: AudioFilterTab,
        val scanning: Boolean,
        val banner: String?
    )

    companion object {
        private data class FolderInfo(
            val key: String,
            val name: String,
            val pathLabel: String
        )

        internal fun buildFolders(
            items: List<MediaItemEntity>,
            isVideo: Boolean,
            sortOrder: MediaSortOrder
        ): List<MediaFolder> {
            if (items.isEmpty()) return emptyList()

            val grouped = LinkedHashMap<String, MutableList<MediaItemEntity>>()
            val namesByKey = HashMap<String, String>()
            val pathsByKey = HashMap<String, String>()

            for (item in items) {
                val info = resolveFolderInfo(item)
                grouped.getOrPut(info.key) { mutableListOf() }.add(item)
                namesByKey.putIfAbsent(info.key, info.name)
                pathsByKey.putIfAbsent(info.key, info.pathLabel)
            }

            return grouped.entries.map { (key, folderItems) ->
                val sortedItems = folderItems.sortWithOrder(sortOrder)
                MediaFolder(
                    key = key,
                    name = namesByKey[key] ?: "Media",
                    pathLabel = pathsByKey[key] ?: "Local Storage",
                    items = sortedItems,
                    totalSizeBytes = sortedItems.sumOf { it.fileSizeBytes },
                    totalDurationMs = sortedItems.sumOf { it.durationMs },
                    previewUri = sortedItems.firstOrNull { !it.isStream }?.uri ?: sortedItems.firstOrNull()?.uri,
                    isVideoFolder = isVideo
                )
            }.sortedWith(compareBy<MediaFolder> { it.name.lowercase() }.thenBy { it.pathLabel.lowercase() })
        }

        private fun resolveFolderInfo(item: MediaItemEntity): FolderInfo {
            if (item.uri.startsWith("file://")) {
                val rawPath = item.uri.removePrefix("file://").substringBefore('?').substringBefore('#')
                val filePath = if ('%' in rawPath) {
                    runCatching { java.net.URI(item.uri).path }.getOrNull() ?: rawPath
                } else {
                    rawPath
                }
                val lastSlash = filePath.lastIndexOf('/')
                if (lastSlash > 0) {
                    val abs = filePath.substring(0, lastSlash)
                    val name = abs.substringAfterLast('/').ifBlank { "Internal Storage" }
                    val prettyPath = formatStoragePath(abs)
                    return FolderInfo(key = abs, name = name, pathLabel = prettyPath)
                }
            }
            val label = item.folderName.ifBlank { if (item.isStream) "Streams" else "Imported" }
            val pathLabel = if (item.isStream) "Network Streams" else "Device / $label"
            return FolderInfo(
                key = "virtual:${label.lowercase()}",
                name = label,
                pathLabel = pathLabel
            )
        }

        internal fun formatStoragePath(absPath: String): String {
            val normalized = absPath.replace('\\', '/')
            return when {
                normalized == "/storage/emulated/0" -> "Internal Storage"
                normalized.startsWith("/storage/emulated/0/") ->
                    "Internal Storage/" + normalized.removePrefix("/storage/emulated/0/")
                normalized.startsWith("/storage/") -> {
                    val rest = normalized.removePrefix("/storage/")
                    val slashIdx = rest.indexOf('/')
                    if (slashIdx > 0) "SD Card/" + rest.substring(slashIdx + 1) else "SD Card"
                }
                else -> normalized
            }
        }

        internal fun passesMinDuration(item: MediaItemEntity, minSeconds: Int): Boolean {
            if (minSeconds <= 0 || item.isStream || item.durationMs <= 0L) return true
            return item.durationMs >= minSeconds * 1000L
        }

        internal fun filterByMinDuration(items: List<MediaItemEntity>, minSeconds: Int): List<MediaItemEntity> {
            if (minSeconds <= 0) return items
            return items.filter { passesMinDuration(it, minSeconds) }
        }

        internal fun List<MediaItemEntity>.sortWithOrder(order: MediaSortOrder): List<MediaItemEntity> = when (order) {
            MediaSortOrder.RECENT -> sortedByDescending { it.addedAt }
            MediaSortOrder.NAME_ASC -> sortedBy { it.title.lowercase() }
            MediaSortOrder.DURATION_DESC -> sortedByDescending { it.durationMs }
            MediaSortOrder.SIZE_DESC -> sortedByDescending { it.fileSizeBytes }
        }

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
