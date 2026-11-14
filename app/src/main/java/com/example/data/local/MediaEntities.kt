package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Local Media",
    val durationMs: Long = 0L,
    val mimeType: String = "video/*",
    val isVideo: Boolean = true,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0L,
    val folderName: String = "Imported",
    val isStream: Boolean = false,
    val isLiked: Boolean = false,
    val lastPlayedPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val externalSubtitleUri: String? = null,
    val preferredSpeed: Float = 1.0f
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_media_cross_ref",
    primaryKeys = ["playlistId", "mediaUri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["uri"],
            childColumns = ["mediaUri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("mediaUri")]
)
data class PlaylistMediaCrossRef(
    val playlistId: Long,
    val mediaUri: String,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "uri",
        associateBy = Junction(
            value = PlaylistMediaCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "mediaUri"
        )
    )
    val items: List<MediaItemEntity>
)

@Entity(tableName = "equalizer_settings")
data class EqualizerSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val presetName: String = "Flat",
    val bandLevelsCsv: String = "0,0,0,0,0",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0
)
