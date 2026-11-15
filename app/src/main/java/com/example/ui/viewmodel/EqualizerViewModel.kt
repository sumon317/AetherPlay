package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.data.local.CustomEqualizerProfile
import com.example.data.local.SettingsRepository
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerPreset
import com.example.player.EqualizerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerEngine: EqualizerEngine,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<EqualizerUiState> = equalizerEngine.state

    val customProfiles: StateFlow<List<CustomEqualizerProfile>> =
        settingsRepository.customEqualizerProfilesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            if (!equalizerEngine.hasLoadedPersisted) {
                val saved = mediaRepository.getEqualizerSettings()
                equalizerEngine.loadPersistedSettings(saved)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizerEngine.setEnabled(enabled)
        persist()
    }

    fun applyPreset(preset: EqualizerPreset) {
        equalizerEngine.applyPreset(preset)
        persist()
    }

    fun applyCustomProfile(profile: CustomEqualizerProfile) {
        equalizerEngine.applyCustomProfile(profile)
        persist()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        equalizerEngine.setBandLevel(bandIndex, levelMb)
        persist()
    }

    fun resetBand(bandIndex: Int) {
        equalizerEngine.resetBandToFlat(bandIndex)
        persist()
    }

    fun setBassBoost(strength: Int) {
        equalizerEngine.setBassBoost(strength)
        persist()
    }

    fun setVirtualizer(strength: Int) {
        equalizerEngine.setVirtualizer(strength)
        persist()
    }

    fun setLoudnessGain(gainMb: Int) {
        equalizerEngine.setLoudnessGain(gainMb)
        persist()
    }

    fun saveCustomProfile(name: String, existingProfileId: String? = null) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val snapshot = equalizerEngine.createCustomProfileSnapshot(cleanName, existingProfileId)
            settingsRepository.saveCustomEqualizerProfile(snapshot)
            mediaRepository.saveEqualizerSettings(equalizerEngine.toEntity())
        }
    }

    fun deleteCustomProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepository.deleteCustomEqualizerProfile(profileId)
            equalizerEngine.onCustomProfileDeleted(profileId)
            mediaRepository.saveEqualizerSettings(equalizerEngine.toEntity())
        }
    }

    fun reset() {
        equalizerEngine.resetToFlat()
        persist()
    }

    private fun persist() {
        viewModelScope.launch {
            mediaRepository.saveEqualizerSettings(equalizerEngine.toEntity())
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EqualizerViewModel(
                    equalizerEngine = container.equalizerEngine,
                    mediaRepository = container.mediaRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
