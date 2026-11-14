package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerEngine
import com.example.player.EqualizerPreset
import com.example.player.EqualizerUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerEngine: EqualizerEngine,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<EqualizerUiState> = equalizerEngine.state

    fun setEnabled(enabled: Boolean) {
        equalizerEngine.setEnabled(enabled)
        persist()
    }

    fun applyPreset(preset: EqualizerPreset) {
        equalizerEngine.applyPreset(preset)
        persist()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        equalizerEngine.setBandLevel(bandIndex, levelMb)
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
                    mediaRepository = container.mediaRepository
                )
            }
        }
    }
}
