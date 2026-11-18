package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithItems
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.theme.AetherPlayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaylistSwipeToDismissTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `swiping left on playlist track removes item and offers undo`() {
        val track1 = MediaItemEntity(
            uri = "file:///music/track1.flac",
            title = "Midnight Echo",
            artist = "Aether Synth",
            folderName = "Music",
            isVideo = false,
            durationMs = 195_000L
        )
        val track2 = MediaItemEntity(
            uri = "file:///music/track2.mp3",
            title = "Horizon Pulse",
            artist = "Studio Echo",
            folderName = "Music",
            isVideo = false,
            durationMs = 210_000L
        )
        val playlist = PlaylistWithItems(
            playlist = PlaylistEntity(id = 1L, name = "Late Night Mix"),
            items = listOf(track1, track2)
        )

        val removedUris = mutableListOf<String>()
        val restoredUris = mutableListOf<String>()

        composeRule.setContent {
            AetherPlayTheme {
                PlaylistDetailScreen(
                    playlistWithItems = playlist,
                    onBack = {},
                    onPlayQueue = { _, _ -> },
                    onShuffleQueue = {},
                    onRemoveItem = { removedUris += it },
                    onRestoreItem = { restoredUris += it }
                )
            }
        }

        composeRule.onNodeWithTag("playlist_swipe_item_0")
            .performTouchInput { swipeLeft() }

        composeRule.waitForIdle()
        assertEquals(listOf(track1.uri), removedUris)

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(track1.uri), restoredUris)
    }
}
