package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomEqualizerProfile
import com.example.player.EqualizerPreset
import com.example.player.EqualizerUiState
import com.example.ui.components.SaveCustomProfileDialog
import com.example.ui.components.StudioChannelFaderBank
import com.example.ui.components.StudioConsoleStatusStrip
import com.example.ui.components.StudioDynamicsAndSpatialRack
import com.example.ui.components.StudioFrequencyCurvePanel
import com.example.ui.components.StudioPresetProfileDeck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    uiState: EqualizerUiState,
    customProfiles: List<CustomEqualizerProfile>,
    isAudioPlaying: Boolean,
    activeMediaTitle: String?,
    onBack: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSelectCustomProfile: (CustomEqualizerProfile) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onResetBand: (Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onSaveCustomProfile: (String, String?) -> Unit,
    onDeleteCustomProfile: (String) -> Unit,
    onReset: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("eq_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Studio Equalizer", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${uiState.selectedPreset} • 5-Band Parametric Console",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("eq_reset_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset equalizer to flat")
                    }
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = onSetEnabled,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("eq_master_switch")
                    )
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            val isWideConsole = maxWidth >= 760.dp

            if (isWideConsole) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1.15f)
                    ) {
                        item {
                            StudioConsoleStatusStrip(
                                uiState = uiState,
                                isAudioPlaying = isAudioPlaying,
                                activeMediaTitle = activeMediaTitle,
                                onToggleEnabled = onSetEnabled
                            )
                        }
                        item {
                            StudioFrequencyCurvePanel(
                                bands = uiState.bands,
                                enabled = uiState.enabled,
                                isAudioPlaying = isAudioPlaying,
                                selectedPresetName = uiState.selectedPreset,
                                hasUnsavedChanges = uiState.hasUnsavedChanges,
                                onBandLevelChange = onBandLevelChange
                            )
                        }
                        item {
                            StudioChannelFaderBank(
                                bands = uiState.bands,
                                enabled = uiState.enabled,
                                onBandLevelChange = onBandLevelChange,
                                onResetBand = onResetBand
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(0.95f)
                    ) {
                        item {
                            StudioPresetProfileDeck(
                                uiState = uiState,
                                customProfiles = customProfiles,
                                onSelectPreset = onSelectPreset,
                                onSelectCustomProfile = onSelectCustomProfile,
                                onRequestSaveProfile = { showSaveDialog = true },
                                onUpdateActiveCustomProfile = { existing ->
                                    onSaveCustomProfile(existing.name, existing.id)
                                },
                                onDeleteCustomProfile = onDeleteCustomProfile
                            )
                        }
                        item {
                            StudioDynamicsAndSpatialRack(
                                uiState = uiState,
                                onBassBoostChange = onBassBoostChange,
                                onVirtualizerChange = onVirtualizerChange,
                                onLoudnessChange = onLoudnessChange
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 44.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        StudioConsoleStatusStrip(
                            uiState = uiState,
                            isAudioPlaying = isAudioPlaying,
                            activeMediaTitle = activeMediaTitle,
                            onToggleEnabled = onSetEnabled
                        )
                    }
                    item {
                        StudioPresetProfileDeck(
                            uiState = uiState,
                            customProfiles = customProfiles,
                            onSelectPreset = onSelectPreset,
                            onSelectCustomProfile = onSelectCustomProfile,
                            onRequestSaveProfile = { showSaveDialog = true },
                            onUpdateActiveCustomProfile = { existing ->
                                onSaveCustomProfile(existing.name, existing.id)
                            },
                            onDeleteCustomProfile = onDeleteCustomProfile
                        )
                    }
                    item {
                        StudioFrequencyCurvePanel(
                            bands = uiState.bands,
                            enabled = uiState.enabled,
                            isAudioPlaying = isAudioPlaying,
                            selectedPresetName = uiState.selectedPreset,
                            hasUnsavedChanges = uiState.hasUnsavedChanges,
                            onBandLevelChange = onBandLevelChange
                        )
                    }
                    item {
                        StudioChannelFaderBank(
                            bands = uiState.bands,
                            enabled = uiState.enabled,
                            onBandLevelChange = onBandLevelChange,
                            onResetBand = onResetBand
                        )
                    }
                    item {
                        StudioDynamicsAndSpatialRack(
                            uiState = uiState,
                            onBassBoostChange = onBassBoostChange,
                            onVirtualizerChange = onVirtualizerChange,
                            onLoudnessChange = onLoudnessChange
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        val suggested = if (uiState.activeCustomProfileId != null || uiState.selectedPreset == "Custom") {
            "My Studio Curve"
        } else {
            "${uiState.selectedPreset} Custom"
        }
        SaveCustomProfileDialog(
            initialName = suggested,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSaveCustomProfile(name, null)
            }
        )
    }
}
