package com.example

import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.MediaItemEntity
import com.example.data.local.UserPreferences
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerUiState
import com.example.player.PlayerControllerHolder
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
        assertTrue(defaults.rotateAudioArtwork)
        assertEquals(AudioSwipeAction.MINIMISE, defaults.audioSwipeDownAction)
        assertEquals(AudioSwipeAction.OPEN_QUEUE, defaults.audioSwipeUpAction)
        assertEquals(AudioDoubleTapAction.TOGGLE_LIKE, defaults.audioDoubleTapAction)
        assertEquals(4, AudioSwipeAction.entries.size)
        assertEquals(2, AudioDoubleTapAction.entries.size)
        assertTrue(AudioDoubleTapAction.entries.contains(AudioDoubleTapAction.PLAY_PAUSE))
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
                uri = "file:///storage/emulated/0/Music/My%20Songs/song.mp3",
                title = "Song",
                durationMs = 180_000L,
                fileSizeBytes = 5_000_000L,
                isVideo = false,
                folderName = "My Songs"
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
        assertEquals(4, folders.size)

        val actionFolder = folders.first { it.name == "Action" }
        assertEquals("/storage/emulated/0/Movies/Action", actionFolder.key)
        assertEquals("Internal Storage/Movies/Action", actionFolder.pathLabel)
        assertEquals(2, actionFolder.items.size)
        assertEquals(120_000L, actionFolder.totalDurationMs)
        assertEquals(14_000_000L, actionFolder.totalSizeBytes)
        assertEquals("Behind Scenes", actionFolder.items.first().title)

        val mySongsFolder = folders.first { it.name == "My Songs" }
        assertEquals("/storage/emulated/0/Music/My Songs", mySongsFolder.key)
        assertEquals("Internal Storage/Music/My Songs", mySongsFolder.pathLabel)

        val sdFolder = folders.first { it.name == "Download" }
        assertEquals("SD Card/Download", sdFolder.pathLabel)

        val streamFolder = folders.first { it.name == "Streams" }
        assertEquals("virtual:streams", streamFolder.key)
        assertEquals("Network Streams", streamFolder.pathLabel)
    }

    @Test
    fun `windowQueueAroundIndex bounds massive queues without losing selected item`() {
        val largeLibrary = (0 until 1000).map { idx ->
            MediaItemEntity(uri = "file:///storage/emulated/0/Music/track_$idx.mp3", title = "Track $idx", isVideo = false)
        }

        val (midWindow, midMapped) = PlayerControllerHolder.windowQueueAroundIndex(largeLibrary, 500)
        assertEquals(PlayerControllerHolder.MAX_PLAYER_QUEUE_SIZE, midWindow.size)
        assertEquals("Track 500", midWindow[midMapped].title)

        val (headWindow, headMapped) = PlayerControllerHolder.windowQueueAroundIndex(largeLibrary, 3)
        assertEquals(PlayerControllerHolder.MAX_PLAYER_QUEUE_SIZE, headWindow.size)
        assertEquals(3, headMapped)
        assertEquals("Track 3", headWindow[headMapped].title)

        val (tailWindow, tailMapped) = PlayerControllerHolder.windowQueueAroundIndex(largeLibrary, 998)
        assertEquals(PlayerControllerHolder.MAX_PLAYER_QUEUE_SIZE, tailWindow.size)
        assertEquals(198, tailMapped)
        assertEquals("Track 998", tailWindow[tailMapped].title)

        val small = largeLibrary.take(25)
        val (smallWindow, smallMapped) = PlayerControllerHolder.windowQueueAroundIndex(small, 10)
        assertEquals(25, smallWindow.size)
        assertEquals(10, smallMapped)
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

    @Test
    fun `minimum duration filter hides short clips while preserving streams and unknown durations`() {
        val defaults = UserPreferences()
        assertEquals(0, defaults.minVideoDurationSeconds)
        assertEquals(0, defaults.minAudioDurationSeconds)

        val shortClip = MediaItemEntity(
            uri = "file:///storage/emulated/0/WhatsApp/status.mp4",
            title = "Status",
            durationMs = 12_000L,
            isVideo = true
        )
        val longMovie = MediaItemEntity(
            uri = "file:///storage/emulated/0/Movies/film.mkv",
            title = "Film",
            durationMs = 600_000L,
            isVideo = true
        )
        val exactBoundary = MediaItemEntity(
            uri = "file:///storage/emulated/0/Movies/intro.mp4",
            title = "Intro",
            durationMs = 30_000L,
            isVideo = true
        )
        val unknownDuration = MediaItemEntity(
            uri = "file:///storage/emulated/0/Movies/unindexed.mkv",
            title = "Unindexed",
            durationMs = 0L,
            isVideo = true
        )
        val liveStream = MediaItemEntity(
            uri = "https://example.com/live.m3u8",
            title = "Live",
            durationMs = 0L,
            isVideo = true,
            isStream = true
        )

        val all = listOf(shortClip, longMovie, exactBoundary, unknownDuration, liveStream)

        // Filter Off (0s) shows everything
        assertEquals(5, LibraryViewModel.filterByMinDuration(all, 0).size)

        // 30s filter hides 12s status clip, keeps 30s, 600s, unknown, and stream
        val filtered30 = LibraryViewModel.filterByMinDuration(all, 30)
        assertEquals(4, filtered30.size)
        assertFalse(filtered30.contains(shortClip))
        assertTrue(filtered30.contains(exactBoundary))
        assertTrue(filtered30.contains(longMovie))
        assertTrue(filtered30.contains(unknownDuration))
        assertTrue(filtered30.contains(liveStream))

        // 60s filter hides both 12s and 30s clips
        val filtered60 = LibraryViewModel.filterByMinDuration(all, 60)
        assertEquals(3, filtered60.size)
        assertFalse(filtered60.contains(exactBoundary))
    }
}
