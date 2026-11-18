package com.example

import com.example.data.local.MediaItemEntity
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerUiState
import com.example.player.SmartQueueEngine
import com.example.player.SubtitleParser
import com.example.player.TimedSubtitleCue
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatResolution
import com.example.ui.components.formatSignedDeltaSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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
    fun `equalizer engine creates, applies, updates, and cleans up custom sound profiles`() {
        val engine = EqualizerEngine()
        engine.setBandLevel(0, 600)
        engine.setBandLevel(4, -300)
        engine.setBassBoost(500)
        engine.setVirtualizer(400)
        engine.setLoudnessGain(250)

        val snapshot = engine.createCustomProfileSnapshot("Studio Late Night")
        assertEquals("Studio Late Night", snapshot.name)
        assertEquals(600, snapshot.bandLevelsMb[0])
        assertEquals(-300, snapshot.bandLevelsMb[4])
        assertEquals(500, snapshot.bassBoost)
        assertEquals(400, snapshot.virtualizer)
        assertEquals(250, snapshot.loudnessGainMb)
        assertEquals(snapshot.id, engine.state.value.activeCustomProfileId)
        assertFalse(engine.state.value.hasUnsavedChanges)

        engine.resetBandToFlat(0)
        assertEquals(0, engine.state.value.bands[0].levelMb)
        assertTrue(engine.state.value.hasUnsavedChanges)

        engine.applyCustomProfile(snapshot)
        assertEquals(600, engine.state.value.bands[0].levelMb)
        assertFalse(engine.state.value.hasUnsavedChanges)

        engine.onCustomProfileDeleted(snapshot.id)
        assertNull(engine.state.value.activeCustomProfileId)
    }

    @Test
    fun `smart queue engine preserves current track, diversifies order, and blends same-type discoveries`() {
        val track1 = MediaItemEntity(uri = "file:///a1.flac", title = "A1", artist = "Artist A", folderName = "Rock", isVideo = false)
        val track2 = MediaItemEntity(uri = "file:///a2.flac", title = "A2", artist = "Artist A", folderName = "Rock", isVideo = false)
        val track3 = MediaItemEntity(uri = "file:///b1.flac", title = "B1", artist = "Artist B", folderName = "Jazz", isVideo = false, isLiked = true)
        val track4 = MediaItemEntity(uri = "file:///c1.flac", title = "C1", artist = "Artist C", folderName = "Synth", isVideo = false)

        val discoveryAudio = MediaItemEntity(uri = "file:///d1.flac", title = "D1", artist = "Artist D", folderName = "Ambient", isVideo = false, isLiked = true)
        val unrelatedVideo = MediaItemEntity(uri = "file:///v1.mp4", title = "Movie", artist = "Cinema", folderName = "Movies", isVideo = true, isLiked = true)

        val result = SmartQueueEngine.buildSmartQueue(
            currentItem = track1,
            baseQueue = listOf(track1, track2, track3, track4),
            libraryPool = listOf(track1, track2, track3, track4, discoveryAudio, unrelatedVideo),
            random = Random(42)
        )

        assertEquals(track1.uri, result.orderedQueue.first().uri)
        assertTrue(result.orderedQueue.all { !it.isVideo })
        assertFalse(result.orderedQueue.any { it.uri == unrelatedVideo.uri })
        assertTrue(result.smartPickUris.contains(discoveryAudio.uri))
        assertTrue(result.orderedQueue.any { it.uri == discoveryAudio.uri })
        // Track right after track1 (Artist A) should prefer a different artist rather than track2 (Artist A)
        assertFalse(result.orderedQueue[1].artist == "Artist A")
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
