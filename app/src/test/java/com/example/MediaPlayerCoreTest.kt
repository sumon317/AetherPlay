package com.example

import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.MediaItemEntity
import com.example.data.local.UserPreferences
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerUiState
import com.example.player.SubtitleParser
import com.example.player.TimedSubtitleCue
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatResolution
import com.example.ui.components.formatSignedDeltaSeconds
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.MediaSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerCoreTest {

    @Test
    fun `format detection supports video, audio, and adaptive stream formats`() {
        assertTrue(MediaRepository.isSupportedMediaFile("movie.mkv", ""))
        assertTrue(MediaRepository.isVideoMedia("movie.mkv", ""))
        assertTrue(MediaRepository.isSupportedMediaFile("clip.flv", ""))
        assertTrue(MediaRepository.isVideoMedia("stream.m3u8", ""))

        assertTrue(MediaRepository.isSupportedMediaFile("song.flac", ""))
        assertFalse(MediaRepository.isVideoMedia("song.flac", ""))
        assertTrue(MediaRepository.isSupportedMediaFile("podcast.opus", ""))
        assertFalse(MediaRepository.isVideoMedia("podcast.opus", ""))

        assertFalse(MediaRepository.isSupportedMediaFile("readme.txt", "text/plain"))
        assertEquals("audio/flac", MediaRepository.guessMimeFromName("track.flac"))
        assertEquals("video/x-matroska", MediaRepository.guessMimeFromName("film.mkv"))
    }

    @Test
    fun `folder view and music player gesture defaults match requirements`() {
        val defaults = UserPreferences()
        assertTrue(defaults.videoFolderView)
        assertTrue(defaults.audioFolderView)
        assertFalse(defaults.rotateAudioArtwork)
        assertEquals(AudioSwipeAction.MINIMISE, defaults.audioSwipeDownAction)
        assertEquals(AudioSwipeAction.OPEN_QUEUE, defaults.audioSwipeUpAction)
        assertEquals(AudioDoubleTapAction.TOGGLE_LIKE, defaults.audioDoubleTapAction)
        assertEquals(4, AudioSwipeAction.entries.size)
        assertEquals(2, AudioDoubleTapAction.entries.size)
    }

    @Test
    fun `buildFolders groups file uris by parent folder and formats storage paths`() {
        val items = listOf(
            MediaItemEntity(
                uri = "file:///storage/emulated/0/Movies/Action/trailer.mp4",
                title = "Trailer",
                durationMs = 90_000L,
                fileSizeBytes = 10_000_000L,
                isVideo = true,
                folderName = "Action"
            ),
            MediaItemEntity(
                uri = "file:///storage/emulated/0/Movies/Action/behind_scenes.mkv",
                title = "Behind Scenes",
                durationMs = 30_000L,
                fileSizeBytes = 4_000_000L,
                isVideo = true,
                folderName = "Action"
            ),
            MediaItemEntity(
                uri = "file:///storage/ABCD-1234/Download/concert.mp4",
                title = "Concert",
                durationMs = 120_000L,
                fileSizeBytes = 25_000_000L,
                isVideo = true,
                folderName = "Download"
            ),
            MediaItemEntity(
                uri = "https://example.com/live.m3u8",
                title = "Live Feed",
                isVideo = true,
                folderName = "Streams",
                isStream = true
            )
        )

        val folders = LibraryViewModel.buildFolders(items, isVideo = true, sortOrder = MediaSortOrder.NAME_ASC)
        assertEquals(3, folders.size)

        val actionFolder = folders.first { it.name == "Action" }
        assertEquals("/storage/emulated/0/Movies/Action", actionFolder.key)
        assertEquals("Internal Storage/Movies/Action", actionFolder.pathLabel)
        assertEquals(2, actionFolder.items.size)
        assertEquals(120_000L, actionFolder.totalDurationMs)
        assertEquals(14_000_000L, actionFolder.totalSizeBytes)
        assertEquals("Behind Scenes", actionFolder.items.first().title)

        val sdFolder = folders.first { it.name == "Download" }
        assertEquals("SD Card/Download", sdFolder.pathLabel)

        val streamFolder = folders.first { it.name == "Streams" }
        assertEquals("virtual:streams", streamFolder.key)
        assertEquals("Network Streams", streamFolder.pathLabel)
    }

    @Test
    fun `subtitle sync delay shifts active cue window accurately`() {
        val cues = listOf(
            TimedSubtitleCue(startMs = 5000L, endMs = 8000L, text = "Hello Studio")
        )
        assertEquals("Hello Studio", SubtitleParser.activeCueTextAt(cues, positionMs = 6000L, syncDelayMs = 0L))
        // +2s delay means cue appears at 7000ms..10000ms
        assertNull(SubtitleParser.activeCueTextAt(cues, positionMs = 6000L, syncDelayMs = 2000L))
        assertEquals("Hello Studio", SubtitleParser.activeCueTextAt(cues, positionMs = 8500L, syncDelayMs = 2000L))
        // -1.5s delay means cue appears earlier at 3500ms..6500ms
        assertEquals("Hello Studio", SubtitleParser.activeCueTextAt(cues, positionMs = 4000L, syncDelayMs = -1500L))
    }

    @Test
    fun `equalizer engine applies presets, clamps gains, and serializes`() {
        val engine = EqualizerEngine()
        val cinemaPreset = EqualizerUiState.BUILTIN_PRESETS.first { it.name == "Cinema" }
        engine.applyPreset(cinemaPreset)

        val state = engine.state.value
        assertTrue(state.enabled)
        assertEquals("Cinema", state.selectedPreset)
        assertEquals(350, state.bands[0].levelMb)
        assertEquals(450, state.bassBoostStrength)

        engine.setBandLevel(0, 9999)
        assertEquals("Custom", engine.state.value.selectedPreset)
        assertEquals(1500, engine.state.value.bands[0].levelMb)

        val entity = engine.toEntity()
        assertTrue(entity.enabled)
        assertEquals("Custom", entity.presetName)
    }

    @Test
    fun `media formatters format duration, delta, size, and resolution`() {
        assertEquals("02:05", formatDuration(125_000L))
        assertEquals("1:01:01", formatDuration(3_661_000L))
        assertEquals("+10s", formatSignedDeltaSeconds(10_000L))
        assertEquals("-15s", formatSignedDeltaSeconds(-15_000L))
        assertEquals("4K", formatResolution(3840, 2160))
        assertEquals("1080p", formatResolution(1920, 1080))
        assertEquals("1.5 MB", formatFileSize(1_572_864L))
    }
}
