package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.data.local.EqualizerSettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
) {
    val label: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000}kHz"
        } else {
            "${centerFreqHz}Hz"
        }

    val levelDb: Float
        get() = levelMb / 100f
}

data class EqualizerPreset(
    val name: String,
    val levelsMb: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0
)

data class EqualizerUiState(
    val enabled: Boolean = false,
    val selectedPreset: String = "Flat",
    val bands: List<EqualizerBand> = defaultBands(),
    val bassBoostStrength: Int = 0,     // 0..1000
    val virtualizerStrength: Int = 0,   // 0..1000
    val loudnessGainMb: Int = 0,        // 0..1500
    val isHardwareEffectBound: Boolean = false
) {
    companion object {
        val BUILTIN_PRESETS = listOf(
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0), bassBoost = 0, virtualizer = 0),
            EqualizerPreset("Cinema", listOf(350, 150, -100, 200, 400), bassBoost = 450, virtualizer = 500),
            EqualizerPreset("Bass Boost", listOf(750, 500, 100, 0, 150), bassBoost = 800, virtualizer = 150),
            EqualizerPreset("Vocal Clarity", listOf(-250, 100, 550, 450, 200), bassBoost = 0, virtualizer = 200),
            EqualizerPreset("Studio Monitor", listOf(200, 100, 0, 150, 300), bassBoost = 150, virtualizer = 100),
            EqualizerPreset("Electronic", listOf(600, 400, -150, 350, 650), bassBoost = 600, virtualizer = 400),
            EqualizerPreset("Night Mode", listOf(-350, -100, 350, 250, -150), bassBoost = 0, virtualizer = 100)
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

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedAudioSessionId: Int = 0

    fun loadPersistedSettings(entity: EqualizerSettingsEntity?) {
        if (entity == null) return
        val parsedLevels = entity.bandLevelsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        _state.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val level = parsedLevels.getOrElse(idx) { band.levelMb }
                    .coerceIn(band.minLevelMb, band.maxLevelMb)
                band.copy(levelMb = level)
            }
            current.copy(
                enabled = entity.enabled,
                selectedPreset = entity.presetName,
                bands = updatedBands,
                bassBoostStrength = entity.bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = entity.virtualizerStrength.coerceIn(0, 1000),
                loudnessGainMb = entity.loudnessGainMb.coerceIn(0, 1500)
            )
        }
        applyAllEffects()
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedAudioSessionId) return
        releaseEffects()
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

            val currentBands = _state.value.bands
            val hardwareBands = (0 until count).map { idx ->
                val centerHz = (eq.getCenterFreq(idx.toShort()) / 1000).coerceAtLeast(30)
                val preservedLevel = currentBands.getOrNull(idx)?.levelMb ?: 0
                EqualizerBand(
                    index = idx,
                    centerFreqHz = centerHz,
                    levelMb = preservedLevel.coerceIn(minMb, maxMb),
                    minLevelMb = minMb,
                    maxLevelMb = maxMb
                )
            }
            _state.update { it.copy(bands = hardwareBands, isHardwareEffectBound = true) }
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
        applyAllEffects()
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        applyAllEffects()
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
                bands = updatedBands,
                bassBoostStrength = preset.bassBoost,
                virtualizerStrength = preset.virtualizer
            )
        }
        applyAllEffects()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        _state.update { current ->
            val updatedBands = current.bands.map { band ->
                if (band.index == bandIndex) {
                    band.copy(levelMb = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb))
                } else band
            }
            current.copy(
                enabled = true,
                selectedPreset = "Custom",
                bands = updatedBands
            )
        }
        applyAllEffects()
    }

    fun setBassBoost(strength: Int) {
        _state.update {
            it.copy(
                enabled = true,
                selectedPreset = if (it.selectedPreset == "Flat") "Custom" else it.selectedPreset,
                bassBoostStrength = strength.coerceIn(0, 1000)
            )
        }
        applyAllEffects()
    }

    fun setVirtualizer(strength: Int) {
        _state.update {
            it.copy(
                enabled = true,
                selectedPreset = if (it.selectedPreset == "Flat") "Custom" else it.selectedPreset,
                virtualizerStrength = strength.coerceIn(0, 1000)
            )
        }
        applyAllEffects()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.update {
            it.copy(
                enabled = true,
                loudnessGainMb = gainMb.coerceIn(0, 1500)
            )
        }
        applyAllEffects()
    }

    fun resetToFlat() {
        applyPreset(EqualizerUiState.BUILTIN_PRESETS.first())
        _state.update { it.copy(loudnessGainMb = 0) }
        applyAllEffects()
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

    private fun applyAllEffects() {
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
