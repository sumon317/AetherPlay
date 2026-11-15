package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aetherplay_settings")

@Serializable
data class CustomEqualizerProfile(
    val id: String,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGainMb: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ThemeMode(val label: String) {
    DARK("Obsidian Dark"),
    LIGHT("Daylight"),
    SYSTEM("System Default")
}

enum class DecoderMode(val label: String, val subtitle: String) {
    HARDWARE_FIRST("Hardware Accelerated", "MediaCodec zero-copy GPU decoding with fallback"),
    AUTO("Balanced Auto", "Adaptive hardware/software track decoder"),
    SOFTWARE_PREFERRED("Software Fallback", "Prefer software decoders for niche formats")
}

enum class SubtitleFontStyle(val label: String) {
    SANS("Plus Jakarta Sans"),
    MONO("JetBrains Mono"),
    DISPLAY("Space Grotesk")
}

enum class SubtitleEdgeStyle(val label: String) {
    OUTLINE("Crisp Outline"),
    DROP_SHADOW("Cinema Shadow"),
    BOXED("Translucent Box"),
    NONE("Clean Plain")
}

enum class AudioSwipeAction(val label: String) {
    MINIMISE("Minimise"),
    OPEN_QUEUE("Open Queue"),
    OPEN_EQUALIZER("Open Equalizer"),
    OPEN_LIKED_SONGS("Open Liked Songs")
}

enum class AudioDoubleTapAction(val label: String) {
    TOGGLE_LIKE("Like / Dislike"),
    PLAY_PAUSE("Play / Pause")
}

data class SubtitlePreferences(
    val fontSizeSp: Float = 18f,
    val fontStyle: SubtitleFontStyle = SubtitleFontStyle.SANS,
    val textColorHex: Long = 0xFFFFFFFFL,
    val backgroundOpacity: Float = 0.55f,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.DROP_SHADOW,
    val bottomMarginDp: Float = 44f,
    val syncDelayMs: Long = 0L
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = false,
    val decoderMode: DecoderMode = DecoderMode.HARDWARE_FIRST,
    val useSurfaceView: Boolean = true,
    val defaultSeekSeconds: Int = 10,
    val resumePlayback: Boolean = true,
    val backgroundVideoAudio: Boolean = true,
    val autoPip: Boolean = true,
    val gestureBrightnessEnabled: Boolean = true,
    val gestureVolumeEnabled: Boolean = true,
    val brightnessSensitivity: Float = 1.0f,
    val volumeSensitivity: Float = 1.0f,
    val seekSensitivity: Float = 1.0f,
    val gestureHorizontalSeek: Boolean = true,
    val doubleTapSeek: Boolean = true,
    val longPress2xSpeed: Boolean = true,
    val videoGridMode: Boolean = false,
    val videoFolderView: Boolean = true,
    val audioFolderView: Boolean = true,
    val rotateAudioArtwork: Boolean = true,
    val audioSwipeDownAction: AudioSwipeAction = AudioSwipeAction.MINIMISE,
    val audioSwipeUpAction: AudioSwipeAction = AudioSwipeAction.OPEN_QUEUE,
    val audioDoubleTapAction: AudioDoubleTapAction = AudioDoubleTapAction.TOGGLE_LIKE,
    val minVideoDurationSeconds: Int = 0,
    val minAudioDurationSeconds: Int = 0,
    val subtitle: SubtitlePreferences = SubtitlePreferences()
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val USE_SURFACE_VIEW = booleanPreferencesKey("use_surface_view")
        val DEFAULT_SEEK_SECONDS = intPreferencesKey("default_seek_seconds")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val BACKGROUND_VIDEO_AUDIO = booleanPreferencesKey("background_video_audio")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness_enabled")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume_enabled")
        val BRIGHTNESS_SENSITIVITY = floatPreferencesKey("brightness_sensitivity")
        val VOLUME_SENSITIVITY = floatPreferencesKey("volume_sensitivity")
        val SEEK_SENSITIVITY = floatPreferencesKey("seek_sensitivity")
        val GESTURE_HORIZONTAL_SEEK = booleanPreferencesKey("gesture_horizontal_seek")
        val DOUBLE_TAP_SEEK = booleanPreferencesKey("double_tap_seek")
        val LONG_PRESS_2X = booleanPreferencesKey("long_press_2x")
        val VIDEO_GRID_MODE = booleanPreferencesKey("video_grid_mode")
        val VIDEO_FOLDER_VIEW = booleanPreferencesKey("video_folder_view")
        val AUDIO_FOLDER_VIEW = booleanPreferencesKey("audio_folder_view")
        val ROTATE_AUDIO_ARTWORK = booleanPreferencesKey("rotate_audio_artwork")
        val AUDIO_SWIPE_DOWN_ACTION = stringPreferencesKey("audio_swipe_down_action")
        val AUDIO_SWIPE_UP_ACTION = stringPreferencesKey("audio_swipe_up_action")
        val AUDIO_DOUBLE_TAP_ACTION = stringPreferencesKey("audio_double_tap_action")
        val MIN_VIDEO_DURATION_SEC = intPreferencesKey("min_video_duration_seconds")
        val MIN_AUDIO_DURATION_SEC = intPreferencesKey("min_audio_duration_seconds")

        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size")
        val SUB_FONT_STYLE = stringPreferencesKey("sub_font_style")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_EDGE_STYLE = stringPreferencesKey("sub_edge_style")
        val SUB_BOTTOM_MARGIN = floatPreferencesKey("sub_bottom_margin")
        val SUB_DELAY_MS = longPreferencesKey("sub_delay_ms")
        val CUSTOM_EQ_PROFILES = stringPreferencesKey("custom_eq_profiles_json")
    }

    private val profileJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val customEqualizerProfilesFlow: Flow<List<CustomEqualizerProfile>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_EQ_PROFILES] ?: return@map emptyList()
        runCatching { profileJson.decodeFromString<List<CustomEqualizerProfile>>(raw) }
            .getOrDefault(emptyList())
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            decoderMode = prefs[Keys.DECODER_MODE]?.let { runCatching { DecoderMode.valueOf(it) }.getOrNull() }
                ?: DecoderMode.HARDWARE_FIRST,
            useSurfaceView = prefs[Keys.USE_SURFACE_VIEW] ?: true,
            defaultSeekSeconds = prefs[Keys.DEFAULT_SEEK_SECONDS] ?: 10,
            resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
            backgroundVideoAudio = prefs[Keys.BACKGROUND_VIDEO_AUDIO] ?: true,
            autoPip = prefs[Keys.AUTO_PIP] ?: true,
            gestureBrightnessEnabled = prefs[Keys.GESTURE_BRIGHTNESS] ?: true,
            gestureVolumeEnabled = prefs[Keys.GESTURE_VOLUME] ?: true,
            brightnessSensitivity = (prefs[Keys.BRIGHTNESS_SENSITIVITY] ?: 1.0f).coerceIn(0.25f, 2.5f),
            volumeSensitivity = (prefs[Keys.VOLUME_SENSITIVITY] ?: 1.0f).coerceIn(0.25f, 2.5f),
            seekSensitivity = (prefs[Keys.SEEK_SENSITIVITY] ?: 1.0f).coerceIn(0.25f, 2.5f),
            gestureHorizontalSeek = prefs[Keys.GESTURE_HORIZONTAL_SEEK] ?: true,
            doubleTapSeek = prefs[Keys.DOUBLE_TAP_SEEK] ?: true,
            longPress2xSpeed = prefs[Keys.LONG_PRESS_2X] ?: true,
            videoGridMode = prefs[Keys.VIDEO_GRID_MODE] ?: false,
            videoFolderView = prefs[Keys.VIDEO_FOLDER_VIEW] ?: true,
            audioFolderView = prefs[Keys.AUDIO_FOLDER_VIEW] ?: true,
            rotateAudioArtwork = prefs[Keys.ROTATE_AUDIO_ARTWORK] ?: true,
            audioSwipeDownAction = prefs[Keys.AUDIO_SWIPE_DOWN_ACTION]?.let {
                runCatching { AudioSwipeAction.valueOf(it) }.getOrNull()
            } ?: AudioSwipeAction.MINIMISE,
            audioSwipeUpAction = prefs[Keys.AUDIO_SWIPE_UP_ACTION]?.let {
                runCatching { AudioSwipeAction.valueOf(it) }.getOrNull()
            } ?: AudioSwipeAction.OPEN_QUEUE,
            audioDoubleTapAction = prefs[Keys.AUDIO_DOUBLE_TAP_ACTION]?.let {
                runCatching { AudioDoubleTapAction.valueOf(it) }.getOrNull()
            } ?: AudioDoubleTapAction.TOGGLE_LIKE,
            minVideoDurationSeconds = (prefs[Keys.MIN_VIDEO_DURATION_SEC] ?: 0).coerceIn(0, 3600),
            minAudioDurationSeconds = (prefs[Keys.MIN_AUDIO_DURATION_SEC] ?: 0).coerceIn(0, 3600),
            subtitle = SubtitlePreferences(
                fontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18f,
                fontStyle = prefs[Keys.SUB_FONT_STYLE]?.let {
                    runCatching { SubtitleFontStyle.valueOf(it) }.getOrNull()
                } ?: SubtitleFontStyle.SANS,
                textColorHex = prefs[Keys.SUB_TEXT_COLOR] ?: 0xFFFFFFFFL,
                backgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.55f,
                edgeStyle = prefs[Keys.SUB_EDGE_STYLE]?.let {
                    runCatching { SubtitleEdgeStyle.valueOf(it) }.getOrNull()
                } ?: SubtitleEdgeStyle.DROP_SHADOW,
                bottomMarginDp = prefs[Keys.SUB_BOTTOM_MARGIN] ?: 44f,
                syncDelayMs = prefs[Keys.SUB_DELAY_MS] ?: 0L
            )
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDecoderMode(mode: DecoderMode) {
        context.dataStore.edit { it[Keys.DECODER_MODE] = mode.name }
    }

    suspend fun setUseSurfaceView(useSurface: Boolean) {
        context.dataStore.edit { it[Keys.USE_SURFACE_VIEW] = useSurface }
    }

    suspend fun setDefaultSeekSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SEEK_SECONDS] = seconds.coerceIn(5, 60) }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setBackgroundVideoAudio(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_VIDEO_AUDIO] = enabled }
    }

    suspend fun setAutoPip(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    }

    suspend fun setGestureBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURE_BRIGHTNESS] = enabled }
    }

    suspend fun setGestureVolumeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURE_VOLUME] = enabled }
    }

    suspend fun setBrightnessSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[Keys.BRIGHTNESS_SENSITIVITY] = sensitivity.coerceIn(0.25f, 2.5f) }
    }

    suspend fun setVolumeSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[Keys.VOLUME_SENSITIVITY] = sensitivity.coerceIn(0.25f, 2.5f) }
    }

    suspend fun setSeekSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[Keys.SEEK_SENSITIVITY] = sensitivity.coerceIn(0.25f, 2.5f) }
    }

    suspend fun setGestureHorizontalSeek(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURE_HORIZONTAL_SEEK] = enabled }
    }

    suspend fun setDoubleTapSeek(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_SEEK] = enabled }
    }

    suspend fun setLongPress2xSpeed(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LONG_PRESS_2X] = enabled }
    }

    suspend fun resetGestureDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.GESTURE_BRIGHTNESS] = true
            prefs[Keys.GESTURE_VOLUME] = true
            prefs[Keys.BRIGHTNESS_SENSITIVITY] = 1.0f
            prefs[Keys.VOLUME_SENSITIVITY] = 1.0f
            prefs[Keys.SEEK_SENSITIVITY] = 1.0f
            prefs[Keys.GESTURE_HORIZONTAL_SEEK] = true
            prefs[Keys.DOUBLE_TAP_SEEK] = true
            prefs[Keys.LONG_PRESS_2X] = true
            prefs[Keys.DEFAULT_SEEK_SECONDS] = 10
        }
    }

    suspend fun setVideoGridMode(grid: Boolean) {
        context.dataStore.edit { it[Keys.VIDEO_GRID_MODE] = grid }
    }

    suspend fun toggleVideoGridMode(): Boolean {
        var next = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.VIDEO_GRID_MODE] ?: false
            next = !current
            prefs[Keys.VIDEO_GRID_MODE] = next
        }
        return next
    }

    suspend fun setVideoFolderView(folderView: Boolean) {
        context.dataStore.edit { it[Keys.VIDEO_FOLDER_VIEW] = folderView }
    }

    suspend fun toggleVideoFolderView(): Boolean {
        var next = true
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.VIDEO_FOLDER_VIEW] ?: true
            next = !current
            prefs[Keys.VIDEO_FOLDER_VIEW] = next
        }
        return next
    }

    suspend fun setAudioFolderView(folderView: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_FOLDER_VIEW] = folderView }
    }

    suspend fun toggleAudioFolderView(): Boolean {
        var next = true
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.AUDIO_FOLDER_VIEW] ?: true
            next = !current
            prefs[Keys.AUDIO_FOLDER_VIEW] = next
        }
        return next
    }

    suspend fun setRotateAudioArtwork(rotate: Boolean) {
        context.dataStore.edit { it[Keys.ROTATE_AUDIO_ARTWORK] = rotate }
    }

    suspend fun setAudioSwipeDownAction(action: AudioSwipeAction) {
        context.dataStore.edit { it[Keys.AUDIO_SWIPE_DOWN_ACTION] = action.name }
    }

    suspend fun setAudioSwipeUpAction(action: AudioSwipeAction) {
        context.dataStore.edit { it[Keys.AUDIO_SWIPE_UP_ACTION] = action.name }
    }

    suspend fun setAudioDoubleTapAction(action: AudioDoubleTapAction) {
        context.dataStore.edit { it[Keys.AUDIO_DOUBLE_TAP_ACTION] = action.name }
    }

    suspend fun setMinVideoDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_VIDEO_DURATION_SEC] = seconds.coerceIn(0, 3600) }
    }

    suspend fun setMinAudioDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.MIN_AUDIO_DURATION_SEC] = seconds.coerceIn(0, 3600) }
    }

    suspend fun updateSubtitlePreferences(transform: (SubtitlePreferences) -> SubtitlePreferences) {
        context.dataStore.edit { prefs ->
            val current = SubtitlePreferences(
                fontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18f,
                fontStyle = prefs[Keys.SUB_FONT_STYLE]?.let {
                    runCatching { SubtitleFontStyle.valueOf(it) }.getOrNull()
                } ?: SubtitleFontStyle.SANS,
                textColorHex = prefs[Keys.SUB_TEXT_COLOR] ?: 0xFFFFFFFFL,
                backgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.55f,
                edgeStyle = prefs[Keys.SUB_EDGE_STYLE]?.let {
                    runCatching { SubtitleEdgeStyle.valueOf(it) }.getOrNull()
                } ?: SubtitleEdgeStyle.DROP_SHADOW,
                bottomMarginDp = prefs[Keys.SUB_BOTTOM_MARGIN] ?: 44f,
                syncDelayMs = prefs[Keys.SUB_DELAY_MS] ?: 0L
            )
            val updated = transform(current)
            prefs[Keys.SUB_FONT_SIZE] = updated.fontSizeSp
            prefs[Keys.SUB_FONT_STYLE] = updated.fontStyle.name
            prefs[Keys.SUB_TEXT_COLOR] = updated.textColorHex
            prefs[Keys.SUB_BG_OPACITY] = updated.backgroundOpacity
            prefs[Keys.SUB_EDGE_STYLE] = updated.edgeStyle.name
            prefs[Keys.SUB_BOTTOM_MARGIN] = updated.bottomMarginDp
            prefs[Keys.SUB_DELAY_MS] = updated.syncDelayMs
        }
    }

    suspend fun saveCustomEqualizerProfile(profile: CustomEqualizerProfile) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_EQ_PROFILES]?.let { raw ->
                runCatching { profileJson.decodeFromString<List<CustomEqualizerProfile>>(raw) }
                    .getOrNull()
            } ?: emptyList()

            val updated = buildList {
                add(profile)
                current.filterNot {
                    it.id == profile.id || it.name.equals(profile.name, ignoreCase = true)
                }.forEach { add(it) }
            }
            prefs[Keys.CUSTOM_EQ_PROFILES] = profileJson.encodeToString(updated)
        }
    }

    suspend fun deleteCustomEqualizerProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_EQ_PROFILES]?.let { raw ->
                runCatching { profileJson.decodeFromString<List<CustomEqualizerProfile>>(raw) }
                    .getOrNull()
            } ?: emptyList()

            val updated = current.filterNot { it.id == profileId }
            prefs[Keys.CUSTOM_EQ_PROFILES] = profileJson.encodeToString(updated)
        }
    }
}
