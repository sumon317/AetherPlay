package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.local.EqualizerSettingsEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistMediaCrossRef
import com.example.data.local.PlaylistWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    val videosFlow: Flow<List<MediaItemEntity>> = mediaDao.observeVideos()
    val audioFlow: Flow<List<MediaItemEntity>> = mediaDao.observeAudio()
    val recentFlow: Flow<List<MediaItemEntity>> = mediaDao.observeAllByRecent()
    val likedFlow: Flow<List<MediaItemEntity>> = mediaDao.observeLikedItems()
    val continueWatchingFlow: Flow<List<MediaItemEntity>> = mediaDao.observeContinueWatching()
    val playlistsFlow: Flow<List<PlaylistWithItems>> = mediaDao.observePlaylistsWithItems()
    val equalizerSettingsFlow: Flow<EqualizerSettingsEntity?> = mediaDao.observeEqualizerSettings()

    fun observePlaylist(playlistId: Long): Flow<PlaylistWithItems?> =
        mediaDao.observePlaylistById(playlistId)

    suspend fun getMediaByUri(uri: String): MediaItemEntity? = mediaDao.getMediaByUri(uri)

    suspend fun toggleLike(uri: String, isLiked: Boolean) {
        mediaDao.setLiked(uri, isLiked)
    }

    suspend fun updatePlaybackProgress(uri: String, positionMs: Long) {
        mediaDao.updatePlaybackProgress(uri, positionMs, System.currentTimeMillis())
    }

    suspend fun attachExternalSubtitle(mediaUri: String, subtitleUri: String?) {
        runCatching {
            if (subtitleUri != null) {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(subtitleUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        mediaDao.updateExternalSubtitle(mediaUri, subtitleUri)
    }

    suspend fun deleteMedia(uri: String) {
        mediaDao.deleteMediaByUri(uri)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return mediaDao.insertPlaylist(PlaylistEntity(name = name.trim(), description = description.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, name: String, description: String = "") {
        mediaDao.updatePlaylist(playlistId, name.trim(), description.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        mediaDao.deletePlaylist(playlistId)
    }

    suspend fun addToPlaylist(playlistId: Long, mediaUri: String) {
        mediaDao.addItemToPlaylist(PlaylistMediaCrossRef(playlistId = playlistId, mediaUri = mediaUri))
    }

    suspend fun removeFromPlaylist(playlistId: Long, mediaUri: String) {
        mediaDao.removeItemFromPlaylist(playlistId, mediaUri)
    }

    suspend fun getEqualizerSettings(): EqualizerSettingsEntity? = mediaDao.getEqualizerSettings()

    suspend fun saveEqualizerSettings(settings: EqualizerSettingsEntity) {
        mediaDao.saveEqualizerSettings(settings)
    }

    suspend fun importDocumentUris(uris: List<Uri>): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val imported = mutableListOf<MediaItemEntity>()
        for (uri in uris) {
            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val item = extractMediaMetadata(resolver, uri, folderLabel = "Imported")
            if (item != null) {
                mediaDao.upsertMedia(item)
                imported += item
            }
        }
        imported
    }

    suspend fun importSingleExternalUri(uri: Uri): MediaItemEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val existing = mediaDao.getMediaByUri(uri.toString())
        if (existing != null) return@withContext existing

        val isHttp = uri.scheme?.lowercase(Locale.ROOT) in listOf("http", "https")
        val item = if (isHttp) {
            createStreamEntity(uri.toString(), null)
        } else {
            extractMediaMetadata(resolver, uri, folderLabel = "External")
        }
        if (item != null) {
            mediaDao.upsertMedia(item)
        }
        item
    }

    suspend fun importStreamUrl(url: String, customTitle: String?, isVideo: Boolean): MediaItemEntity =
        withContext(Dispatchers.IO) {
            val item = createStreamEntity(url.trim(), customTitle?.takeIf { it.isNotBlank() }, isVideo)
            mediaDao.upsertMedia(item)
            item
        }

    suspend fun scanFolderTree(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootFolderName = queryDisplayName(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId))
            ?: "Media Folder"

        val found = mutableListOf<MediaItemEntity>()
        scanDirectoryRecursive(resolver, treeUri, rootDocId, rootFolderName, found, depth = 0)
        if (found.isNotEmpty()) {
            mediaDao.insertMediaIfAbsent(found)
        }
        found.size
    }

    private fun scanDirectoryRecursive(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        folderName: String,
        output: MutableList<MediaItemEntity>,
        depth: Int
    ) {
        if (depth > 4) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Media" else "Media"
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanDirectoryRecursive(resolver, treeUri, docId, name, output, depth + 1)
                    } else if (isSupportedMediaFile(name, mime)) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val entity = extractMediaMetadata(
                            resolver = resolver,
                            uri = fileUri,
                            folderLabel = folderName,
                            fallbackName = name,
                            fallbackMime = mime,
                            fallbackSize = size
                        )
                        if (entity != null) output += entity
                    }
                }
            }
        }
    }

    private fun extractMediaMetadata(
        resolver: ContentResolver,
        uri: Uri,
        folderLabel: String,
        fallbackName: String? = null,
        fallbackMime: String? = null,
        fallbackSize: Long = 0L
    ): MediaItemEntity? {
        val rawName = fallbackName ?: queryDisplayName(resolver, uri) ?: uri.lastPathSegment ?: "Untitled Media"
        val mime = fallbackMime?.takeIf { it.isNotBlank() } ?: resolver.getType(uri) ?: guessMimeFromName(rawName)
        if (!isSupportedMediaFile(rawName, mime)) return null

        val isVideo = isVideoMedia(rawName, mime)
        val sizeBytes = if (fallbackSize > 0L) fallbackSize else queryFileSize(resolver, uri)

        var title = rawName.substringBeforeLast('.')
        var artist = if (isVideo) folderLabel else "Unknown Artist"
        var album = if (isVideo) folderLabel else "Local Audio"
        var durationMs = 0L
        var width = 0
        var height = 0

        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                durationMs = it
            }
            if (isVideo) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?.let {
                    width = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?.let {
                    height = it
                }
            } else {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                    title = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                    artist = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                    album = it
                }
            }
        }
        runCatching { retriever.release() }

        return MediaItemEntity(
            uri = uri.toString(),
            title = title.ifBlank { rawName },
            artist = artist,
            album = album,
            durationMs = durationMs,
            mimeType = mime,
            isVideo = isVideo,
            width = width,
            height = height,
            fileSizeBytes = sizeBytes,
            folderName = folderLabel,
            isStream = false
        )
    }

    private fun createStreamEntity(url: String, customTitle: String?, forceVideo: Boolean = true): MediaItemEntity {
        val cleanName = customTitle ?: Uri.parse(url).lastPathSegment?.substringBefore('?')?.ifBlank { null } ?: "Network Stream"
        val lower = url.lowercase(Locale.ROOT)
        val isAudio = !forceVideo || lower.endsWith(".mp3") || lower.endsWith(".aac") ||
            lower.endsWith(".flac") || lower.endsWith(".ogg") || lower.endsWith(".opus") || lower.endsWith(".wav")
        val mime = when {
            lower.contains(".m3u8") -> "application/x-mpegURL"
            lower.contains(".mpd") -> "application/dash+xml"
            isAudio -> "audio/*"
            else -> "video/*"
        }
        return MediaItemEntity(
            uri = url,
            title = cleanName,
            artist = if (isAudio) "Network Audio" else "Live / Network Stream",
            album = "Streams",
            mimeType = mime,
            isVideo = !isAudio,
            folderName = "Streams",
            isStream = true
        )
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryFileSize(resolver: ContentResolver, uri: Uri): Long {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c: Cursor ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "mov", "avi", "flv", "ts", "m2ts", "mts",
            "3gp", "wmv", "m4v", "mpg", "mpeg", "vob", "ogv", "m3u8", "mpd"
        )
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "aac", "m4a", "ogg", "opus", "wav", "alac",
            "wma", "aiff", "amr", "ac3", "eac3", "dts", "mid", "midi"
        )

        fun isSupportedMediaFile(fileName: String, mimeType: String): Boolean {
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            if (lowerMime.startsWith("video/") || lowerMime.startsWith("audio/")) return true
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS
        }

        fun isVideoMedia(fileName: String, mimeType: String): Boolean {
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            if (lowerMime.startsWith("video/")) return true
            if (lowerMime.startsWith("audio/")) return false
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return ext in VIDEO_EXTENSIONS
        }

        fun guessMimeFromName(fileName: String): String {
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when (ext) {
                "mkv" -> "video/x-matroska"
                "mp4", "m4v" -> "video/mp4"
                "webm" -> "video/webm"
                "avi" -> "video/x-msvideo"
                "mov" -> "video/quicktime"
                "flv" -> "video/x-flv"
                "ts", "m2ts" -> "video/mp2t"
                "mp3" -> "audio/mpeg"
                "flac" -> "audio/flac"
                "aac", "m4a" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                "m3u8" -> "application/x-mpegURL"
                "mpd" -> "application/dash+xml"
                else -> if (ext in AUDIO_EXTENSIONS) "audio/*" else "video/*"
            }
        }
    }
}
