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
import com.example.player.MediaSafety
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

data class DeviceScanResult(
    val totalScanned: Int,
    val newlyAdded: Int
)

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

    fun observeMediaByUri(uri: String): Flow<MediaItemEntity?> =
        mediaDao.observeMediaByUri(uri)

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
        if (!MediaSafety.isSafeExternalMediaUri(context, uri)) {
            return@withContext null
        }
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

    suspend fun importStreamUrl(url: String, customTitle: String?, isVideo: Boolean): MediaItemEntity? =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            if (!MediaSafety.isValidStreamUrl(trimmed)) return@withContext null
            val safeTitle = MediaSafety.sanitizeDisplayText(customTitle, 100).ifBlank { null }
            val item = createStreamEntity(trimmed, safeTitle, isVideo)
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

    suspend fun scanDeviceMedia(): DeviceScanResult = withContext(Dispatchers.IO) {
        val allFilesGranted = hasAllFilesPermission(context)
        val hasVideoPerm = allFilesGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED)
        val hasAudioPerm = allFilesGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)

        if (!allFilesGranted && !hasVideoPerm && !hasAudioPerm) {
            return@withContext DeviceScanResult(totalScanned = 0, newlyAdded = 0)
        }

        val existingItems = mediaDao.getAllMediaOnce()
        val existingUris = existingItems.mapTo(HashSet()) { it.uri }

        val mediaStoreEntries = queryMediaStoreEntries(
            includeVideo = allFilesGranted || hasVideoPerm,
            includeAudio = allFilesGranted || hasAudioPerm
        )

        val discovered = mutableListOf<MediaItemEntity>()

        if (allFilesGranted) {
            val byPath = HashMap<String, StoreMediaEntry>(mediaStoreEntries.size * 2)
            for (entry in mediaStoreEntries) {
                val path = entry.filePath ?: continue
                byPath[path] = entry
                runCatching { File(path).canonicalPath }.getOrNull()?.let { canon ->
                    byPath[canon] = entry
                }
            }

            val roots = collectStorageRoots()
            val visitedDirs = HashSet<String>()
            var retrieverBudget = 120

            for (root in roots) {
                val queue = ArrayDeque<Pair<File, Int>>()
                queue.add(root to 0)

                while (queue.isNotEmpty()) {
                    val (dir, depth) = queue.removeFirst()
                    if (depth > 14 || !dir.exists() || !dir.isDirectory || !dir.canRead()) continue
                    val canonDir = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
                    if (!visitedDirs.add(canonDir)) continue

                    val dirName = dir.name
                    if (depth > 0 && shouldSkipDirectory(dirName, dir.absolutePath)) continue

                    val children = dir.listFiles() ?: continue
                    for (child in children) {
                        if (child.isDirectory) {
                            if (!shouldSkipDirectory(child.name, child.absolutePath)) {
                                queue.add(child to depth + 1)
                            }
                        } else if (child.isFile && child.length() > 0L && isSupportedMediaFile(child.name, "")) {
                            val fileUri = Uri.fromFile(child).toString()
                            if (fileUri in existingUris) {
                                discovered += MediaItemEntity(uri = fileUri, title = child.name)
                                continue
                            }

                            val canonFile = runCatching { child.canonicalPath }.getOrNull()
                            val storeMeta = (canonFile?.let { byPath[it] }) ?: byPath[child.absolutePath]
                            val folderName = child.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Internal Storage"
                            val mime = storeMeta?.mimeType?.takeIf { it.isNotBlank() } ?: guessMimeFromName(child.name)
                            val isVideo = storeMeta?.isVideo ?: isVideoMedia(child.name, mime)

                            var title = storeMeta?.title?.takeIf { it.isNotBlank() }
                                ?: child.nameWithoutExtension.ifBlank { child.name }
                            var artist = storeMeta?.artist?.takeIf { it.isNotBlank() }
                                ?: if (isVideo) folderName else "Unknown Artist"
                            var album = storeMeta?.album?.takeIf { it.isNotBlank() }
                                ?: if (isVideo) folderName else "Local Audio"
                            var durationMs = storeMeta?.durationMs ?: 0L
                            var width = storeMeta?.width ?: 0
                            var height = storeMeta?.height ?: 0
                            val sizeBytes = storeMeta?.sizeBytes?.takeIf { it > 0L } ?: child.length()

                            if (durationMs <= 0L && retrieverBudget > 0) {
                                retrieverBudget--
                                val retriever = MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(child.absolutePath)
                                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                        ?.toLongOrNull()?.let { durationMs = it }
                                    if (isVideo) {
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                            ?.toIntOrNull()?.let { width = it }
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                            ?.toIntOrNull()?.let { height = it }
                                    } else {
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                            ?.takeIf { it.isNotBlank() }?.let { title = it }
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                            ?.takeIf { it.isNotBlank() }?.let { artist = it }
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                            ?.takeIf { it.isNotBlank() }?.let { album = it }
                                    }
                                } catch (ce: CancellationException) {
                                    throw ce
                                } catch (_: Exception) {
                                    // Skip corrupt or unreadable metadata
                                } finally {
                                    runCatching { retriever.release() }
                                }
                            }

                            discovered += MediaItemEntity(
                                uri = fileUri,
                                title = title,
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

            // Prune stale file:// entries whose backing file no longer exists on disk
            val missingFileUris = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                existingItems.filter { item ->
                    !item.isStream && item.uri.startsWith("file://") &&
                        (Uri.parse(item.uri).path?.let { !File(it).exists() } == true)
                }.map { it.uri }
            } else {
                emptyList()
            }

            val toInsert = discovered.filter { it.uri !in existingUris }
            mediaDao.syncScannedMedia(toInsert = toInsert, toDeleteUris = missingFileUris)

            return@withContext DeviceScanResult(
                totalScanned = discovered.size,
                newlyAdded = toInsert.size
            )
        } else {
            // Fallback using MediaStore content:// URIs if only scoped media permission was granted
            for (entry in mediaStoreEntries) {
                if (entry.contentUri in existingUris) {
                    discovered += MediaItemEntity(uri = entry.contentUri, title = entry.title)
                    continue
                }
                discovered += MediaItemEntity(
                    uri = entry.contentUri,
                    title = entry.title,
                    artist = entry.artist,
                    album = entry.album,
                    durationMs = entry.durationMs,
                    mimeType = entry.mimeType,
                    isVideo = entry.isVideo,
                    width = entry.width,
                    height = entry.height,
                    fileSizeBytes = entry.sizeBytes,
                    folderName = entry.bucketName,
                    isStream = false
                )
            }
        }

        val toInsert = discovered.filter { it.uri !in existingUris }
        mediaDao.syncScannedMedia(toInsert = toInsert, toDeleteUris = emptyList())

        DeviceScanResult(
            totalScanned = discovered.size,
            newlyAdded = toInsert.size
        )
    }

    private data class StoreMediaEntry(
        val contentUri: String,
        val filePath: String?,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val mimeType: String,
        val bucketName: String,
        val isVideo: Boolean
    )

    @Suppress("DEPRECATION")
    private fun queryMediaStoreEntries(
        includeVideo: Boolean,
        includeAudio: Boolean
    ): List<StoreMediaEntry> {
        val resolver = context.contentResolver
        val results = mutableListOf<StoreMediaEntry>()

        if (includeVideo) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
            runCatching {
                resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
                    val idCol = c.getColumnIndex(MediaStore.Video.Media._ID)
                    val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
                    val durCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val wCol = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val hCol = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val mimeCol = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (c.moveToNext()) {
                        val id = if (idCol >= 0) c.getLong(idCol) else continue
                        val path = if (dataCol >= 0) c.getString(dataCol) else null
                        val displayName = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                        val rawTitle = if (titleCol >= 0) c.getString(titleCol) ?: "" else ""
                        val folder = if (bucketCol >= 0) c.getString(bucketCol) ?: "Videos" else "Videos"
                        val title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Video" }
                        val mime = (if (mimeCol >= 0) c.getString(mimeCol) else null) ?: "video/*"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                        results += StoreMediaEntry(
                            contentUri = contentUri,
                            filePath = path,
                            title = title,
                            artist = folder,
                            album = folder,
                            durationMs = if (durCol >= 0) c.getLong(durCol).coerceAtLeast(0L) else 0L,
                            width = if (wCol >= 0) c.getInt(wCol).coerceAtLeast(0) else 0,
                            height = if (hCol >= 0) c.getInt(hCol).coerceAtLeast(0) else 0,
                            sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol).coerceAtLeast(0L) else 0L,
                            mimeType = mime,
                            bucketName = folder,
                            isVideo = true
                        )
                    }
                }
            }
        }

        if (includeAudio) {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
            )
            runCatching {
                resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
                    val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                    val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val bucketCol = c.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

                    while (c.moveToNext()) {
                        val id = if (idCol >= 0) c.getLong(idCol) else continue
                        val path = if (dataCol >= 0) c.getString(dataCol) else null
                        val displayName = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                        val rawTitle = if (titleCol >= 0) c.getString(titleCol) ?: "" else ""
                        val rawArtist = if (artistCol >= 0) c.getString(artistCol) ?: "" else ""
                        val rawAlbum = if (albumCol >= 0) c.getString(albumCol) ?: "" else ""
                        val folderFromPath = path?.let { File(it).parentFile?.name }
                        val folder = folderFromPath?.takeIf { it.isNotBlank() }
                            ?: (if (bucketCol >= 0) c.getString(bucketCol) else null)?.takeIf { it.isNotBlank() }
                            ?: "Audio"
                        val title = rawTitle.ifBlank { displayName.substringBeforeLast('.') }.ifBlank { "Audio" }
                        val artist = rawArtist.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                        val album = rawAlbum.takeIf { it.isNotBlank() && it != "<unknown>" } ?: folder
                        val mime = (if (mimeCol >= 0) c.getString(mimeCol) else null) ?: "audio/*"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                        results += StoreMediaEntry(
                            contentUri = contentUri,
                            filePath = path,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = if (durCol >= 0) c.getLong(durCol).coerceAtLeast(0L) else 0L,
                            width = 0,
                            height = 0,
                            sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol).coerceAtLeast(0L) else 0L,
                            mimeType = mime,
                            bucketName = folder,
                            isVideo = false
                        )
                    }
                }
            }
        }

        return results
    }

    private fun collectStorageRoots(): List<File> {
        val candidates = mutableListOf<File>()
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory()?.let { candidates += it }
        context.getExternalFilesDirs(null)?.forEach { appDir ->
            val path = appDir?.absolutePath ?: return@forEach
            val idx = path.indexOf("/Android/")
            if (idx > 0) {
                candidates += File(path.substring(0, idx))
            }
        }
        val unique = LinkedHashMap<String, File>()
        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                val canon = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
                unique.putIfAbsent(canon, dir)
            }
        }
        return unique.values.toList()
    }

    private fun shouldSkipDirectory(name: String, fullPath: String): Boolean {
        if (name.startsWith(".")) return true
        if (name == "lost+found") return true
        val normalized = fullPath.replace('\\', '/')
        if (normalized.endsWith("/Android/data") || normalized.endsWith("/Android/obb")) return true
        return false
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
        try {
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
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // Keep safe filename-derived fallback metadata
        } finally {
            runCatching { retriever.release() }
        }

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

        fun hasMediaReadPermission(context: Context): Boolean {
            if (hasAllFilesPermission(context)) return true
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
            } else {
                false
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
