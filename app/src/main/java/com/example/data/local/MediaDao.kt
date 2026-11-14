package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY addedAt DESC")
    fun observeVideos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeAudio(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY lastPlayedAt DESC")
    fun observeAllByRecent(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isLiked = 1 ORDER BY addedAt DESC")
    fun observeLikedItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 12")
    fun observeContinueWatching(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaIfAbsent(items: List<MediaItemEntity>)

    @Query("UPDATE media_items SET isLiked = :liked WHERE uri = :uri")
    suspend fun setLiked(uri: String, liked: Boolean)

    @Query("UPDATE media_items SET lastPlayedPositionMs = :positionMs, lastPlayedAt = :timestampMs WHERE uri = :uri")
    suspend fun updatePlaybackProgress(uri: String, positionMs: Long, timestampMs: Long)

    @Query("UPDATE media_items SET externalSubtitleUri = :subtitleUri WHERE uri = :uri")
    suspend fun updateExternalSubtitle(uri: String, subtitleUri: String?)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteMediaByUri(uri: String)

    // Playlists
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylistsWithItems(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    fun observePlaylistById(playlistId: Long): Flow<PlaylistWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description WHERE id = :playlistId")
    suspend fun updatePlaylist(playlistId: Long, name: String, description: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItemToPlaylist(crossRef: PlaylistMediaCrossRef)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removeItemFromPlaylist(playlistId: Long, mediaUri: String)

    // Equalizer Settings
    @Query("SELECT * FROM equalizer_settings WHERE id = 1 LIMIT 1")
    fun observeEqualizerSettings(): Flow<EqualizerSettingsEntity?>

    @Query("SELECT * FROM equalizer_settings WHERE id = 1 LIMIT 1")
    suspend fun getEqualizerSettings(): EqualizerSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqualizerSettings(settings: EqualizerSettingsEntity)
}
