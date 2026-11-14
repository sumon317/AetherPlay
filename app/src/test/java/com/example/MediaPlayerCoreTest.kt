package com.example

import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerUiState
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
