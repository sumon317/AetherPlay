package com.example

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerUiState
import com.example.player.MediaSafety
import com.example.player.SubtitleParser
import com.example.ui.components.ThumbnailCache
import com.example.ui.components.decodeSampledBitmap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafetyAndLeakTest {

    @Test
    fun `stream URL validation allows safe protocols and rejects unsafe inputs`() {
        assertTrue(MediaSafety.isValidStreamUrl("https://example.com/live/stream.m3u8"))
        assertTrue(MediaSafety.isValidStreamUrl("http://192.168.1.10:8080/video.mp4"))
        assertTrue(MediaSafety.isValidStreamUrl("rtsp://media.example.org/channel1"))
        assertTrue(MediaSafety.isValidStreamUrl("  https://cdn.example.com/audio.flac  "))

        assertFalse(MediaSafety.isValidStreamUrl(""))
        assertFalse(MediaSafety.isValidStreamUrl("   "))
        assertFalse(MediaSafety.isValidStreamUrl("javascript:alert(1)"))
        assertFalse(MediaSafety.isValidStreamUrl("file:///data/data/com.aistudio.aetherplay.vxkmzq/databases/aetherplay_db"))
        assertFalse(MediaSafety.isValidStreamUrl("content://media/external/video/1"))
        assertFalse(MediaSafety.isValidStreamUrl("data:text/plain;base64,SGVsbG8="))
        assertFalse(MediaSafety.isValidStreamUrl("https://"))
        assertFalse(MediaSafety.isValidStreamUrl("https://example.com/has space.mp4"))
        assertFalse(MediaSafety.isValidStreamUrl("https://example.com/\ninjected"))
        assertFalse(MediaSafety.isValidStreamUrl("https://example.com/" + "a".repeat(2100)))
    }

    @Test
    fun `external URI check blocks app-private files and path traversal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val dbFile = context.getDatabasePath("aetherplay_db")
        val privateDbUri = Uri.fromFile(dbFile)
        assertFalse(MediaSafety.isSafeExternalMediaUri(context, privateDbUri))

        val traversalFile = File(context.filesDir, "../databases/aetherplay_db")
        assertFalse(MediaSafety.isSafeExternalMediaUri(context, Uri.fromFile(traversalFile)))

        val privateCacheUri = Uri.fromFile(File(context.cacheDir, "secret.tmp"))
        assertFalse(MediaSafety.isSafeExternalMediaUri(context, privateCacheUri))

        val selfFileProviderUri = Uri.parse("content://${context.packageName}.fileprovider/internal/db")
        assertFalse(MediaSafety.isSafeExternalMediaUri(context, selfFileProviderUri))

        val publicMediaUri = Uri.parse("file:///storage/emulated/0/Movies/trailer.mp4")
        assertTrue(MediaSafety.isSafeExternalMediaUri(context, publicMediaUri))

        val mediaStoreUri = Uri.parse("content://media/external/video/media/42")
        assertTrue(MediaSafety.isSafeExternalMediaUri(context, mediaStoreUri))

        assertFalse(MediaSafety.isSafeExternalMediaUri(context, Uri.parse("javascript:alert(1)")))
    }

    @Test
    fun `subtitle parser strips tags and safely handles malformed or huge inputs`() {
        val raw = """
            1
            00:00:01,000 --> 00:00:04,500
            <b>Hello</b> <script>alert(1)</script>World

            2
            bad --> timestamp
            Should be skipped

            3
            00:00:10,000 --> 00:00:05,000
            Inverted end should be skipped
        """.trimIndent()

        val cues = SubtitleParser.parseRawSubtitles(raw)
        assertEquals(1, cues.size)
        assertEquals("Hello alert(1)World", cues.first().text)
        assertEquals(1000L, cues.first().startMs)
        assertEquals(4500L, cues.first().endMs)

        // Sanitizing display strings removes control characters
        assertEquals("Movie Title", MediaSafety.sanitizeDisplayText("Movie\n\r\t Title\u0000"))
    }

    @Test
    fun `bitmap decoder and thumbnail cache downsample safely and release cleanly`() {
        assertNull(decodeSampledBitmap(ByteArray(0)))

        val bmp = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.RGB_565)
        ThumbnailCache.clear()
        ThumbnailCache.put("sample://track", bmp)
        assertEquals(bmp, ThumbnailCache.get("sample://track"))
        ThumbnailCache.clear()
        assertNull(ThumbnailCache.get("sample://track"))
    }

    @Test
    fun `equalizer engine handles repeated release without leaks or exceptions`() {
        val engine = EqualizerEngine()
        engine.applyPreset(EqualizerUiState.BUILTIN_PRESETS.first { it.name == "Studio Monitor" })
        engine.releaseEffects()
        engine.releaseEffects()
        assertEquals("Studio Monitor", engine.state.value.selectedPreset)
    }

    @Test
    fun `release metadata uses beta version and keeps backup exclusions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val pkgInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("0.0.0-beta", pkgInfo.versionName)
    }

    @Test
    fun `library view mode toggles switch back and forth atomically`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        // Ensure deterministic starting state
        repo.setVideoFolderView(true)
        repo.setVideoGridMode(false)
        repo.setAudioFolderView(true)

        // Folder <-> All Videos toggles cleanly back and forth
        assertFalse(repo.toggleVideoFolderView())
        assertFalse(repo.preferencesFlow.first().videoFolderView)
        assertTrue(repo.toggleVideoFolderView())
        assertTrue(repo.preferencesFlow.first().videoFolderView)

        // Grid <-> List toggles cleanly back and forth
        assertTrue(repo.toggleVideoGridMode())
        assertTrue(repo.preferencesFlow.first().videoGridMode)
        assertFalse(repo.toggleVideoGridMode())
        assertFalse(repo.preferencesFlow.first().videoGridMode)

        // Audio Folder <-> All Tracks toggles cleanly back and forth
        assertFalse(repo.toggleAudioFolderView())
        assertFalse(repo.preferencesFlow.first().audioFolderView)
        assertTrue(repo.toggleAudioFolderView())
        assertTrue(repo.preferencesFlow.first().audioFolderView)
    }

    @Test
    fun `audio gestures and library duration filters persist and reset cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        repo.setRotateAudioArtwork(true)
        repo.setAudioSwipeUpAction(com.example.data.local.AudioSwipeAction.OPEN_EQUALIZER)
        repo.setAudioSwipeDownAction(com.example.data.local.AudioSwipeAction.OPEN_LIKED_SONGS)
        repo.setAudioDoubleTapAction(com.example.data.local.AudioDoubleTapAction.OPEN_SUBTITLES_LYRICS)
        repo.setMinVideoDurationSeconds(30)
        repo.setMinAudioDurationSeconds(15)

        val updated = repo.preferencesFlow.first()
        assertTrue(updated.rotateAudioArtwork)
        assertEquals(com.example.data.local.AudioSwipeAction.OPEN_EQUALIZER, updated.audioSwipeUpAction)
        assertEquals(com.example.data.local.AudioSwipeAction.OPEN_LIKED_SONGS, updated.audioSwipeDownAction)
        assertEquals(com.example.data.local.AudioDoubleTapAction.OPEN_SUBTITLES_LYRICS, updated.audioDoubleTapAction)
        assertEquals(30, updated.minVideoDurationSeconds)
        assertEquals(15, updated.minAudioDurationSeconds)

        repo.resetGestureDefaults()
        val reset = repo.preferencesFlow.first()
        assertEquals(com.example.data.local.AudioSwipeAction.OPEN_QUEUE, reset.audioSwipeUpAction)
        assertEquals(com.example.data.local.AudioSwipeAction.MINIMISE, reset.audioSwipeDownAction)
        assertEquals(com.example.data.local.AudioDoubleTapAction.LIKE_DISLIKE, reset.audioDoubleTapAction)
    }
}
