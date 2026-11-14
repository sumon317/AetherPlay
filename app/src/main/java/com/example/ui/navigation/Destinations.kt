package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object VideosRoute

@Serializable
object AudioRoute

@Serializable
object PlaylistsRoute

@Serializable
object SettingsRoute

@Serializable
data class PlaylistDetailRoute(val playlistId: Long)

@Serializable
object VideoPlayerRoute

@Serializable
object AudioPlayerRoute

@Serializable
object EqualizerRoute

@Serializable
object GestureControlsRoute
