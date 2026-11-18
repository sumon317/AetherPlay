package com.example.data.repository

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.data.local.EqualizerSettingsEntity
import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistMediaCrossRef
import com.example.data.local.PlaylistWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
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

    suspend fun scanAllDeviceMedia(): Int = withContext(Dispatchers.IO) {
        if (!hasAllFilesPermission(context)) return@withContext 0
        val resolver = context.contentResolver
        val seenPaths = HashSet<String>()
        val collected = mutableListOf<MediaItemEntity>()

        // 1. Fast query via Android MediaStore (Video + Audio)
        queryMediaStore(
            resolver = resolver,
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            isVideo = true,
            seenPaths = seenPaths,
            output = collected
        )
        queryMediaStore(
            resolver = resolver,
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            isVideo = false,
            seenPaths = seenPaths,
            output = collected
        )

        // 2. Direct filesystem walk for formats MediaStore might skip (MKV, FLV, TS, OPUS, AC3, etc.)
        val roots = LinkedHashSet<File>()
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory()?.let { if (it.exists() && it.canRead()) roots += it }
        context.getExternalFilesDirs(null).orEmpty().forEach { appDir ->
            var parent = appDir
            // Climb out of /Android/data/<pkg>/files to volume root if accessible
            repeat(4) { parent = parent?.parentFile }
            if (parent != null && parent.exists() && parent.canRead()) {
                roots += parent
            }
        }
        roots.forEach { root ->
            walkStorageDirectory(
                dir = root,
                seenPaths = seenPaths,
                output = collected,
                depth = 0
            )
        }

        if (collected.isNotEmpty()) {
            mediaDao.insertMediaIfAbsent(collected)
        }
        collected.size
    }

    private fun queryMediaStore(
        resolver: ContentResolver,
        contentUri: Uri,
        isVideo: Boolean,
        seenPaths: MutableSet<String>,
        output: MutableList<MediaItemEntity>
    ) {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.TITLE)
            add(MediaStore.MediaColumns.DURATION)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            }
            if (isVideo) {
                add(MediaStore.Video.VideoColumns.WIDTH)
                add(MediaStore.Video.VideoColumns.HEIGHT)
            } else {
                add(MediaStore.Audio.AudioColumns.ARTIST)
                add(MediaStore.Audio.AudioColumns.ALBUM)
            }
        }.toTypedArray()

        runCatching {
            resolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
                val durCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                } else -1
                val widthCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.VideoColumns.WIDTH) else -1
                val heightCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT) else -1
                val artistCol = if (!isVideo) cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST) else -1
                val albumCol = if (!isVideo) cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM) else -1

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val file = filePath?.let { File(it) }?.takeIf { it.exists() && it.canRead() }
                    val canonical = file?.canonicalPath
                    if (canonical != null && !seenPaths.add(canonical)) continue

                    val displayName = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                        ?: file?.name
                        ?: "Media"
                    val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else null)
                        ?.takeIf { it.isNotBlank() }
                        ?: guessMimeFromName(displayName)
                    if (!isSupportedMediaFile(displayName, mime)) continue

                    val rawTitle = (if (titleCol >= 0) cursor.getString(titleCol) else null)
                        ?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val durationMs = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else (file?.length() ?: 0L)
                    if (sizeBytes == 0L && file != null && file.length() == 0L) continue

                    val folderName = file?.parentFile?.name?.takeIf { it.isNotBlank() }
                        ?: (if (bucketCol >= 0) cursor.getString(bucketCol) else null)?.takeIf { it.isNotBlank() }
                        ?: "Internal Storage"

                    val itemUri = if (file != null) {
                        Uri.fromFile(file).toString()
                    } else {
                        ContentUris.withAppendedId(contentUri, id).toString()
                    }

                    val width = if (widthCol >= 0) cursor.getInt(widthCol).coerceAtLeast(0) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol).coerceAtLeast(0) else 0
                    val artist = if (artistCol >= 0) {
                        cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    } else folderName
                    val album = if (albumCol >= 0) {
                        cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: folderName
                    } else folderName

                    output += MediaItemEntity(
                        uri = itemUri,
                        title = rawTitle.ifBlank { displayName },
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        mimeType = mime,
                        isVideo = isVideo,
                        width = width,
                        height = height,
                        fileSizeBytes = sizeBytes,
                        folderName = folderName,
                        isStream = false
                    )
                }
            }
        }
    }

    private fun walkStorageDirectory(
        dir: File,
        seenPaths: MutableSet<String>,
        output: MutableList<MediaItemEntity>,
        depth: Int
    ) {
        if (depth > 8 || !dir.isDirectory || !dir.canRead()) return
        val dirName = dir.name
        if (dirName.startsWith(".") || dirName.equals("Android", ignoreCase = true)) return

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                walkStorageDirectory(child, seenPaths, output, depth + 1)
            } else if (child.isFile && child.length() > 0L) {
                val name = child.name
                if (name.startsWith(".")) continue
                val mime = guessMimeFromName(name)
                if (!isSupportedMediaFile(name, mime)) continue
                val canonical = runCatching { child.canonicalPath }.getOrNull() ?: child.absolutePath
                if (!seenPaths.add(canonical)) continue

                val isVideo = isVideoMedia(name, mime)
                val folderName = dirName.ifBlank { "Internal Storage" }
                val fileUri = Uri.fromFile(child)
                var title = name.substringBeforeLast('.')
                var artist = if (isVideo) folderName else "Unknown Artist"
                var album = if (isVideo) folderName else folderName
                var durationMs = 0L
                var width = 0
                var height = 0

                val retriever = MediaMetadataRetriever()
                runCatching {
                    retriever.setDataSource(child.absolutePath)
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

                output += MediaItemEntity(
                    uri = fileUri.toString(),
                    title = title.ifBlank { name },
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    mimeType = mime,
                    isVideo = isVideo,
                    width = width,
                    height = height,
                    fileSizeBytes = child.length(),
                    folderName = folderName,
                    isStream = false
                )
            }
        }
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
        fun hasAllFilesPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

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
