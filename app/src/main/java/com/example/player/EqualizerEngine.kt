package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.local.CustomEqualizerProfile
import com.example.data.local.EqualizerSettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
) {
    val label: String
        get() = when {
            centerFreqHz >= 1000 && centerFreqHz % 1000 == 0 -> "${centerFreqHz / 1000}kHz"
            centerFreqHz >= 1000 -> String.format(Locale.US, "%.1fkHz", centerFreqHz / 1000f)
            else -> "${centerFreqHz}Hz"
        }

    val roleLabel: String
        get() = when {
            centerFreqHz < 120 -> "SUB"
            centerFreqHz < 450 -> "BASS"
            centerFreqHz < 1800 -> "MID"
            centerFreqHz < 6500 -> "PRES"
            else -> "AIR"
        }

    val levelDb: Float
        get() = levelMb / 100f
}

data class EqualizerPreset(
    val name: String,
    val levelsMb: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGainMb: Int = 0,
    val category: String = "Studio",
    val isCustom: Boolean = false,
    val profileId: String? = null
)

data class EqualizerUiState(
    val enabled: Boolean = false,
    val selectedPreset: String = "Flat",
    val activeCustomProfileId: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val bands: List<EqualizerBand> = defaultBands(),
    val bassBoostStrength: Int = 0,     // 0..1000
    val virtualizerStrength: Int = 0,   // 0..1000
    val loudnessGainMb: Int = 0,        // 0..1500
    val isHardwareEffectBound: Boolean = false
) {
    val estimatedPeakBoostDb: Float
        get() {
            val maxBandDb = bands.maxOfOrNull { it.levelDb.coerceAtLeast(0f) } ?: 0f
            val bassDb = (bassBoostStrength / 1000f) * 3.5f
            val preampDb = loudnessGainMb / 100f
            return maxBandDb + bassDb + preampDb
        }

    companion object {
        val BUILTIN_PRESETS = listOf(
            EqualizerPreset(
                name = "Flat",
                levelsMb = listOf(0, 0, 0, 0, 0),
                bassBoost = 0,
                virtualizer = 0,
                loudnessGainMb = 0,
                category = "Reference"
            ),
            EqualizerPreset(
                name = "Studio Monitor",
                levelsMb = listOf(200, 100, 0, 150, 300),
                bassBoost = 150,
                virtualizer = 100,
                loudnessGainMb = 150,
                category = "Reference"
            ),
            EqualizerPreset(
                name = "Cinema",
                levelsMb = listOf(350, 150, -100, 200, 400),
                bassBoost = 450,
                virtualizer = 500,
                loudnessGainMb = 250,
                category = "Cinema"
            ),
            EqualizerPreset(
                name = "Bass Boost",
                levelsMb = listOf(750, 500, 100, 0, 150),
                bassBoost = 800,
                virtualizer = 150,
                loudnessGainMb = 200,
                category = "Bass"
            ),
            EqualizerPreset(
                name = "Vocal Clarity",
                levelsMb = listOf(-250, 100, 550, 450, 200),
                bassBoost = 0,
                virtualizer = 200,
                loudnessGainMb = 200,
                category = "Vocal"
            ),
            EqualizerPreset(
                name = "Electronic",
                levelsMb = listOf(600, 400, -150, 350, 650),
                bassBoost = 600,
                virtualizer = 400,
                loudnessGainMb = 300,
                category = "Music"
            ),
            EqualizerPreset(
                name = "Acoustic",
                levelsMb = listOf(250, 150, 200, 300, 350),
                bassBoost = 100,
                virtualizer = 250,
                loudnessGainMb = 100,
                category = "Music"
            ),
            EqualizerPreset(
                name = "Jazz",
                levelsMb = listOf(300, 200, -150, 250, 400),
                bassBoost = 200,
                virtualizer = 350,
                loudnessGainMb = 150,
                category = "Music"
            ),
            EqualizerPreset(
                name = "Rock",
                levelsMb = listOf(500, 300, -100, 350, 550),
                bassBoost = 400,
                virtualizer = 300,
                loudnessGainMb = 250,
                category = "Music"
            ),
            EqualizerPreset(
                name = "Night Mode",
                levelsMb = listOf(-350, -100, 350, 250, -150),
                bassBoost = 0,
                virtualizer = 100,
                loudnessGainMb = 350,
                category = "Utility"
            )
        )

        fun defaultBands(): List<EqualizerBand> {
            val freqs = listOf(60, 230, 910, 3600, 14000)
            return freqs.mapIndexed { idx, freq ->
                EqualizerBand(
                    index = idx,
                    centerFreqHz = freq,
                    levelMb = 0,
                    minLevelMb = -1500,
                    maxLevelMb = 1500
                )
            }
        }
    }
}

class EqualizerEngine {
    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    private val effectExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AetherPlay-AudioEffect").apply { isDaemon = true }
    }
    private val applyPending = AtomicBoolean(false)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedAudioSessionId: Int = 0
    @Volatile
    var hasLoadedPersisted: Boolean = false
        private set

    fun loadPersistedSettings(entity: EqualizerSettingsEntity?) {
        hasLoadedPersisted = true
        if (entity == null) return
        val parsedLevels = entity.bandLevelsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        _state.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val level = parsedLevels.getOrElse(idx) { band.levelMb }
                    .coerceIn(band.minLevelMb, band.maxLevelMb)
                band.copy(levelMb = level)
            }
            val isBuiltIn = EqualizerUiState.BUILTIN_PRESETS.any { it.name == entity.presetName }
            current.copy(
                enabled = entity.enabled,
                selectedPreset = entity.presetName,
                activeCustomProfileId = null,
                hasUnsavedChanges = !isBuiltIn && entity.presetName == "Custom",
                bands = updatedBands,
                bassBoostStrength = entity.bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = entity.virtualizerStrength.coerceIn(0, 1000),
                loudnessGainMb = entity.loudnessGainMb.coerceIn(0, 1500)
            )
        }
        scheduleApplyAllEffects()
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        effectExecutor.execute {
            if (audioSessionId == attachedAudioSessionId) return@execute
            releaseEffectsInternal()
            attachedAudioSessionId = audioSessionId

            var hardwareBound = false
            runCatching {
                val eq = Equalizer(0, audioSessionId)
                equalizer = eq
                hardwareBound = true
                val count = eq.numberOfBands.toInt().coerceAtLeast(1)
                val range = eq.bandLevelRange
                val minMb = range.getOrNull(0)?.toInt() ?: -1500
                val maxMb = range.getOrNull(1)?.toInt() ?: 1500

                _state.update { current ->
                    val hardwareBands = (0 until count).map { idx ->
                        val centerHz = (eq.getCenterFreq(idx.toShort()) / 1000).coerceAtLeast(30)
                        val preservedLevel = current.bands.getOrNull(idx)?.levelMb ?: 0
                        EqualizerBand(
                            index = idx,
                            centerFreqHz = centerHz,
                            levelMb = preservedLevel.coerceIn(minMb, maxMb),
                            minLevelMb = minMb,
                            maxLevelMb = maxMb
                        )
                    }
                    current.copy(bands = hardwareBands, isHardwareEffectBound = true)
                }
            }

            runCatching {
                bassBoost = BassBoost(0, audioSessionId)
            }
            runCatching {
                virtualizer = Virtualizer(0, audioSessionId)
            }
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            }

            if (!hardwareBound) {
                _state.update { it.copy(isHardwareEffectBound = false) }
            }
            applyAllEffectsInternal()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        scheduleApplyAllEffects()
    }

    fun applyPreset(preset: EqualizerPreset) {
        _state.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val target = preset.levelsMb.getOrElse(idx) { 0 }
                    .coerceIn(band.minLevelMb, band.maxLevelMb)
                band.copy(levelMb = target)
            }
            current.copy(
                enabled = true,
                selectedPreset = preset.name,
                activeCustomProfileId = preset.profileId,
                hasUnsavedChanges = false,
                bands = updatedBands,
                bassBoostStrength = preset.bassBoost.coerceIn(0, 1000),
                virtualizerStrength = preset.virtualizer.coerceIn(0, 1000),
                loudnessGainMb = preset.loudnessGainMb.coerceIn(0, 1500)
            )
        }
        scheduleApplyAllEffects()
    }

    fun applyCustomProfile(profile: CustomEqualizerProfile) {
        applyPreset(
            EqualizerPreset(
                name = profile.name,
                levelsMb = profile.bandLevelsMb,
                bassBoost = profile.bassBoost,
                virtualizer = profile.virtualizer,
                loudnessGainMb = profile.loudnessGainMb,
                category = "Custom",
                isCustom = true,
                profileId = profile.id
            )
        )
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        _state.update { current ->
            val updatedBands = current.bands.map { band ->
                if (band.index == bandIndex) {
                    band.copy(levelMb = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb))
                } else band
            }
            val nextPreset = if (current.activeCustomProfileId != null) {
                current.selectedPreset
            } else {
                "Custom"
            }
            current.copy(
                enabled = true,
                selectedPreset = nextPreset,
                hasUnsavedChanges = true,
                bands = updatedBands
            )
        }
        scheduleApplyAllEffects()
    }

    fun resetBandToFlat(bandIndex: Int) {
        setBandLevel(bandIndex, 0)
    }

    fun setBassBoost(strength: Int) {
        _state.update { current ->
            val nextPreset = when {
                current.activeCustomProfileId != null -> current.selectedPreset
                current.selectedPreset == "Flat" -> "Custom"
                else -> current.selectedPreset
            }
            current.copy(
                enabled = true,
                selectedPreset = nextPreset,
                hasUnsavedChanges = true,
                bassBoostStrength = strength.coerceIn(0, 1000)
            )
        }
        scheduleApplyAllEffects()
    }

    fun setVirtualizer(strength: Int) {
        _state.update { current ->
            val nextPreset = when {
                current.activeCustomProfileId != null -> current.selectedPreset
                current.selectedPreset == "Flat" -> "Custom"
                else -> current.selectedPreset
            }
            current.copy(
                enabled = true,
                selectedPreset = nextPreset,
                hasUnsavedChanges = true,
                virtualizerStrength = strength.coerceIn(0, 1000)
            )
        }
        scheduleApplyAllEffects()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.update { current ->
            val nextPreset = when {
                current.activeCustomProfileId != null -> current.selectedPreset
                current.selectedPreset == "Flat" -> "Custom"
                else -> current.selectedPreset
            }
            current.copy(
                enabled = true,
                selectedPreset = nextPreset,
                hasUnsavedChanges = true,
                loudnessGainMb = gainMb.coerceIn(0, 1500)
            )
        }
        scheduleApplyAllEffects()
    }

    fun createCustomProfileSnapshot(name: String, existingProfileId: String? = null): CustomEqualizerProfile {
        val current = _state.value
        val profile = CustomEqualizerProfile(
            id = existingProfileId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            bandLevelsMb = current.bands.map { it.levelMb },
            bassBoost = current.bassBoostStrength,
            virtualizer = current.virtualizerStrength,
            loudnessGainMb = current.loudnessGainMb,
            createdAt = System.currentTimeMillis()
        )
        _state.update {
            it.copy(
                enabled = true,
                selectedPreset = profile.name,
                activeCustomProfileId = profile.id,
                hasUnsavedChanges = false
            )
        }
        return profile
    }

    fun onCustomProfileDeleted(deletedId: String) {
        _state.update { current ->
            if (current.activeCustomProfileId == deletedId) {
                current.copy(
                    selectedPreset = "Custom",
                    activeCustomProfileId = null,
                    hasUnsavedChanges = true
                )
            } else {
                current
            }
        }
    }

    fun resetToFlat() {
        applyPreset(EqualizerUiState.BUILTIN_PRESETS.first())
    }

    fun toEntity(): EqualizerSettingsEntity {
        val current = _state.value
        return EqualizerSettingsEntity(
            id = 1,
            enabled = current.enabled,
            presetName = current.selectedPreset,
            bandLevelsCsv = current.bands.joinToString(",") { it.levelMb.toString() },
            bassBoostStrength = current.bassBoostStrength,
            virtualizerStrength = current.virtualizerStrength,
            loudnessGainMb = current.loudnessGainMb
        )
    }

    private fun scheduleApplyAllEffects() {
        if (applyPending.compareAndSet(false, true)) {
            effectExecutor.execute {
                applyPending.set(false)
                applyAllEffectsInternal()
            }
        }
    }

    private fun applyAllEffectsInternal() {
        val current = _state.value
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = current.enabled
                if (current.enabled) {
                    current.bands.forEach { band ->
                        if (band.index < eq.numberOfBands) {
                            eq.setBandLevel(band.index.toShort(), band.levelMb.toShort())
                        }
                    }
                }
            }
        }
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = current.enabled && current.bassBoostStrength > 0
                if (bb.strengthSupported && current.enabled) {
                    bb.setStrength(current.bassBoostStrength.toShort())
                }
            }
        }
        runCatching {
            virtualizer?.let { vz ->
                vz.enabled = current.enabled && current.virtualizerStrength > 0
                if (vz.strengthSupported && current.enabled) {
                    vz.setStrength(current.virtualizerStrength.toShort())
                }
            }
        }
        runCatching {
            loudnessEnhancer?.let { le ->
                le.enabled = current.enabled && current.loudnessGainMb > 0
                if (current.enabled) {
                    le.setTargetGain(current.loudnessGainMb)
                }
            }
        }
    }

    fun releaseEffects() {
        effectExecutor.execute {
            releaseEffectsInternal()
        }
    }

    private fun releaseEffectsInternal() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        attachedAudioSessionId = 0
    }
}
