package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomEqualizerProfile
import com.example.player.EqualizerBand
import com.example.player.EqualizerPreset
import com.example.player.EqualizerUiState
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

enum class ProfileFilterTab(val label: String) {
    ALL("All"),
    PRESETS("Presets"),
    CUSTOM("Custom")
}

@Composable
fun StudioConsoleStatusStrip(
    uiState: EqualizerUiState,
    isAudioPlaying: Boolean,
    activeMediaTitle: String?,
    onToggleEnabled: (Boolean) -> Unit
) {
    val peakDb = uiState.estimatedPeakBoostDb
    val peakColor = when {
        !uiState.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        peakDb >= 14f -> MaterialTheme.colorScheme.error
        peakDb >= 8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            width = 1.dp,
            color = if (uiState.enabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniLiveSpectrumBadge(
                active = uiState.enabled && isAudioPlaying,
                enabled = uiState.enabled,
                accentColor = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeMediaTitle ?: "Master Stereo Output Bus",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isHardwareEffectBound) "HW DSP BOUND" else "STUDIO DSP READY",
                        style = MonoMetricsStyle,
                        color = if (uiState.enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "•",
                        style = MonoMetricsStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "+%.1f dB PEAK", peakDb),
                        style = MonoMetricsStyle,
                        color = peakColor
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (uiState.enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (uiState.enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleEnabled(!uiState.enabled) }
                    .testTag("eq_bypass_pill")
            ) {
                Text(
                    text = if (uiState.enabled) "ACTIVE" else "BYPASS",
                    style = MonoMetricsStyle,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniLiveSpectrumBadge(
    active: Boolean,
    enabled: Boolean,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "eq_meter")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_meter_phase"
    )
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val bars = 4
            val barWidth = size.width / (bars * 1.8f)
            val gap = (size.width - bars * barWidth) / (bars - 1)
            for (i in 0 until bars) {
                val ratio = when {
                    !enabled -> 0.25f
                    active -> 0.35f + 0.6f * ((sin(phase + i * 1.3f) + 1f) * 0.5f)
                    else -> 0.45f + (i % 2) * 0.2f
                }
                val h = size.height * ratio
                val x = i * (barWidth + gap)
                drawRoundRect(
                    color = if (enabled) accentColor else inactiveColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
fun StudioFrequencyCurvePanel(
    bands: List<EqualizerBand>,
    enabled: Boolean,
    isAudioPlaying: Boolean,
    selectedPresetName: String,
    hasUnsavedChanges: Boolean,
    onBandLevelChange: (Int, Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val scopeBg = MaterialTheme.colorScheme.surfaceContainerLowest
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)

    var draggedBandIndex by remember { mutableIntStateOf(-1) }

    val animatedLevels = bands.map { band ->
        val animated by animateFloatAsState(
            targetValue = band.levelMb.toFloat(),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "eq_band_anim_${band.index}"
        )
        animated
    }

    val spectrumTransition = rememberInfiniteTransition(label = "eq_spectrum")
    val spectrumPhase by spectrumTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_spectrum_phase"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (enabled) primaryColor else mutedColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Parametric Frequency Curve", style = MaterialTheme.typography.titleMedium)
                }

                val badgeText = when {
                    draggedBandIndex in bands.indices -> {
                        val b = bands[draggedBandIndex]
                        String.format(Locale.US, "%s %+.1f dB", b.label, b.levelDb)
                    }
                    hasUnsavedChanges -> "$selectedPresetName • Modified"
                    else -> selectedPresetName
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = if (enabled) 0.14f else 0.06f)
                ) {
                    Text(
                        text = badgeText,
                        style = MonoMetricsStyle,
                        color = if (enabled) primaryColor else mutedColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Recessed Oscilloscope / Analyzer Scope
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scopeBg)
                    .pointerInput(bands.size, bands.firstOrNull()?.minLevelMb, bands.firstOrNull()?.maxLevelMb) {
                        if (bands.isEmpty()) return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = { startOffset ->
                                val padLeft = 42.dp.toPx()
                                val padRight = 16.dp.toPx()
                                val usableW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                                val relX = ((startOffset.x - padLeft) / usableW).coerceIn(0f, 1f)
                                val idx = if (bands.size == 1) 0 else {
                                    (relX * (bands.size - 1)).let { kotlin.math.round(it).toInt() }
                                        .coerceIn(0, bands.lastIndex)
                                }
                                draggedBandIndex = idx
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val idx = draggedBandIndex
                                if (idx in bands.indices) {
                                    val band = bands[idx]
                                    val padV = 18.dp.toPx()
                                    val usableH = (size.height - padV * 2f).coerceAtLeast(1f)
                                    val norm = 1f - ((change.position.y - padV) / usableH).coerceIn(0f, 1f)
                                    val span = (band.maxLevelMb - band.minLevelMb).coerceAtLeast(1)
                                    val newMb = (band.minLevelMb + norm * span).toInt()
                                    onBandLevelChange(band.index, newMb)
                                }
                            },
                            onDragEnd = { draggedBandIndex = -1 },
                            onDragCancel = { draggedBandIndex = -1 }
                        )
                    }
                    .testTag("eq_frequency_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (bands.isEmpty()) return@Canvas
                    val padLeft = 42.dp.toPx()
                    val padRight = 16.dp.toPx()
                    val padV = 18.dp.toPx()
                    val graphW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                    val graphH = (size.height - padV * 2f).coerceAtLeast(1f)
                    val midY = padV + graphH * 0.5f

                    // dB horizontal reference lines (+15, +6, 0, -6, -15)
                    val dbLevels = listOf(1f, 0.7f, 0.5f, 0.3f, 0f)
                    dbLevels.forEachIndexed { idx, frac ->
                        val y = padV + frac * graphH
                        val isZeroLine = idx == 2
                        drawLine(
                            color = if (isZeroLine) {
                                mutedColor.copy(alpha = 0.5f)
                            } else {
                                mutedColor.copy(alpha = 0.16f)
                            },
                            start = Offset(padLeft, y),
                            end = Offset(size.width - padRight, y),
                            strokeWidth = if (isZeroLine) 1.5.dp.toPx() else 1.dp.toPx(),
                            pathEffect = if (isZeroLine) {
                                PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            } else null
                        )
                    }

                    // Band vertical guide lines
                    val points = bands.mapIndexed { idx, band ->
                        val x = if (bands.size == 1) padLeft + graphW / 2f else {
                            padLeft + (idx.toFloat() / (bands.size - 1).toFloat()) * graphW
                        }
                        drawLine(
                            color = mutedColor.copy(alpha = 0.14f),
                            start = Offset(x, padV),
                            end = Offset(x, size.height - padV),
                            strokeWidth = 1.dp.toPx()
                        )
                        val span = (band.maxLevelMb - band.minLevelMb).coerceAtLeast(1).toFloat()
                        val animatedLevel = animatedLevels.getOrElse(idx) { band.levelMb.toFloat() }
                        val norm = ((animatedLevel - band.minLevelMb) / span).coerceIn(0f, 1f)
                        val y = padV + (1f - norm) * graphH
                        Offset(x, y)
                    }

                    // Subtle background harmonic spectrum analyzer bars
                    val barCount = 26
                    val barSlotW = graphW / barCount
                    for (b in 0 until barCount) {
                        val t = b.toFloat() / (barCount - 1).coerceAtLeast(1)
                        val interpolatedCurveNorm = interpolateNormalizedY(points, padLeft + t * graphW, padV, graphH)
                        val harmonicWave = if (enabled && isAudioPlaying) {
                            0.18f * ((sin(spectrumPhase + b * 0.55f) + 1f) * 0.5f)
                        } else if (enabled) {
                            0.06f * ((sin(b * 0.8f) + 1f) * 0.5f)
                        } else {
                            0.02f
                        }
                        val barHeight = ((interpolatedCurveNorm * 0.55f + harmonicWave) * graphH)
                            .coerceIn(4.dp.toPx(), graphH)
                        drawRoundRect(
                            color = if (enabled) {
                                primaryColor.copy(alpha = if (isAudioPlaying) 0.16f else 0.09f)
                            } else {
                                mutedColor.copy(alpha = 0.06f)
                            },
                            topLeft = Offset(padLeft + b * barSlotW + barSlotW * 0.22f, size.height - padV - barHeight),
                            size = Size(barSlotW * 0.56f, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    // Smooth cubic spline path
                    val curvePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    // Filled area between curve and 0dB reference line
                    val fillPath = Path().apply {
                        addPath(curvePath)
                        lineTo(points.last().x, midY)
                        lineTo(points.first().x, midY)
                        close()
                    }

                    val activeBrush = Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(primaryColor, tertiaryColor, secondaryColor)
                        } else {
                            listOf(mutedColor.copy(alpha = 0.5f), mutedColor.copy(alpha = 0.5f))
                        }
                    )

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = if (enabled) {
                                listOf(
                                    primaryColor.copy(alpha = 0.26f),
                                    secondaryColor.copy(alpha = 0.08f)
                                )
                            } else {
                                listOf(
                                    mutedColor.copy(alpha = 0.08f),
                                    mutedColor.copy(alpha = 0.02f)
                                )
                            }
                        )
                    )

                    // Outer glow stroke
                    if (enabled) {
                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.24f),
                                    secondaryColor.copy(alpha = 0.24f)
                                )
                            ),
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Crisp core spline stroke
                    drawPath(
                        path = curvePath,
                        brush = activeBrush,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Interactive nodes
                    points.forEachIndexed { idx, pt ->
                        val isDragged = idx == draggedBandIndex
                        val nodeColor = when {
                            !enabled -> mutedColor
                            bands[idx].levelMb > 50 -> primaryColor
                            bands[idx].levelMb < -50 -> secondaryColor
                            else -> tertiaryColor
                        }
                        if (isDragged || enabled) {
                            drawCircle(
                                color = nodeColor.copy(alpha = if (isDragged) 0.35f else 0.18f),
                                radius = if (isDragged) 14.dp.toPx() else 9.dp.toPx(),
                                center = pt
                            )
                        }
                        drawCircle(
                            color = scopeBg,
                            radius = 5.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Left dB Scale Overlay Labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+15", style = MonoMetricsStyle, color = mutedColor.copy(alpha = 0.75f))
                    Text("0dB", style = MonoMetricsStyle, color = mutedColor)
                    Text("-15", style = MonoMetricsStyle, color = mutedColor.copy(alpha = 0.75f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Drag nodes on curve or adjust console faders below",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Double-tap fader = 0 dB",
                    style = MonoMetricsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun interpolateNormalizedY(
    points: List<Offset>,
    x: Float,
    padV: Float,
    graphH: Float
): Float {
    if (points.isEmpty()) return 0.5f
    if (x <= points.first().x) return 1f - ((points.first().y - padV) / graphH).coerceIn(0f, 1f)
    if (x >= points.last().x) return 1f - ((points.last().y - padV) / graphH).coerceIn(0f, 1f)
    for (i in 0 until points.lastIndex) {
        val p0 = points[i]
        val p1 = points[i + 1]
        if (x in p0.x..p1.x) {
            val frac = ((x - p0.x) / (p1.x - p0.x).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val y = p0.y + (p1.y - p0.y) * frac
            return 1f - ((y - padV) / graphH).coerceIn(0f, 1f)
        }
    }
    return 0.5f
}

@Composable
fun StudioPresetProfileDeck(
    uiState: EqualizerUiState,
    customProfiles: List<CustomEqualizerProfile>,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onSelectCustomProfile: (CustomEqualizerProfile) -> Unit,
    onRequestSaveProfile: () -> Unit,
    onUpdateActiveCustomProfile: (CustomEqualizerProfile) -> Unit,
    onDeleteCustomProfile: (String) -> Unit
) {
    var filterTab by remember { mutableStateOf(ProfileFilterTab.ALL) }
    val activeCustom = remember(uiState.activeCustomProfileId, customProfiles) {
        customProfiles.firstOrNull { it.id == uiState.activeCustomProfileId }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Frequency Presets & Profiles", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${EqualizerUiState.BUILTIN_PRESETS.size} studio presets • ${customProfiles.size} custom",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeCustom != null && uiState.hasUnsavedChanges) {
                        OutlinedButton(
                            onClick = { onUpdateActiveCustomProfile(activeCustom) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("eq_update_profile_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update")
                        }
                    }
                    FilledTonalButton(
                        onClick = onRequestSaveProfile,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("eq_save_profile_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Profile")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileFilterTab.entries.forEach { tab ->
                    val label = if (tab == ProfileFilterTab.CUSTOM) {
                        "Custom (${customProfiles.size})"
                    } else tab.label
                    FilterChip(
                        selected = filterTab == tab,
                        onClick = { filterTab = tab },
                        label = { Text(label) },
                        modifier = Modifier.testTag("eq_filter_${tab.name}")
                    )
                }
            }

            if (filterTab == ProfileFilterTab.CUSTOM && customProfiles.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRequestSaveProfile)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BookmarkAdded,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No custom sound profiles yet", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Adjust the frequency sliders and tap Save Profile to store your signature sound.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filterTab == ProfileFilterTab.ALL || filterTab == ProfileFilterTab.CUSTOM) {
                        items(customProfiles, key = { "custom_${it.id}" }) { profile ->
                            val isSelected = uiState.activeCustomProfileId == profile.id
                            ProfileCurveCard(
                                title = profile.name,
                                subtitle = "CUSTOM",
                                levelsMb = profile.bandLevelsMb,
                                selected = isSelected,
                                isCustom = true,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onSelect = { onSelectCustomProfile(profile) },
                                onDelete = { onDeleteCustomProfile(profile.id) },
                                modifier = Modifier.testTag("eq_custom_profile_${profile.id}")
                            )
                        }
                    }
                    if (filterTab == ProfileFilterTab.ALL || filterTab == ProfileFilterTab.PRESETS) {
                        items(EqualizerUiState.BUILTIN_PRESETS, key = { "builtin_${it.name}" }) { preset ->
                            val isSelected = uiState.activeCustomProfileId == null &&
                                uiState.selectedPreset == preset.name &&
                                !uiState.hasUnsavedChanges
                            ProfileCurveCard(
                                title = preset.name,
                                subtitle = preset.category.uppercase(Locale.US),
                                levelsMb = preset.levelsMb,
                                selected = isSelected,
                                isCustom = false,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onSelect = { onSelectPreset(preset) },
                                onDelete = null,
                                modifier = Modifier.testTag("eq_preset_${preset.name}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCurveCard(
    title: String,
    subtitle: String,
    levelsMb: List<Int>,
    selected: Boolean,
    isCustom: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        accentColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val border = BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = border,
        modifier = modifier
            .width(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCustom) {
                        accentColor.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    }
                ) {
                    Text(
                        text = subtitle,
                        style = MonoMetricsStyle,
                        color = if (isCustom || selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete $title profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Mini Sparkline Curve Preview
            val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            val curveColor = if (selected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val midY = size.height / 2f
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1.dp.toPx()
                )
                if (levelsMb.isNotEmpty()) {
                    val pts = levelsMb.mapIndexed { idx, mb ->
                        val x = if (levelsMb.size == 1) size.width / 2f else {
                            (idx.toFloat() / (levelsMb.size - 1).toFloat()) * (size.width - 8f) + 4f
                        }
                        val norm = ((mb + 1500f) / 3000f).coerceIn(0f, 1f)
                        val y = (1f - norm) * (size.height - 8f) + 4f
                        Offset(x, y)
                    }
                    val p = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 0 until pts.lastIndex) {
                            val a = pts[i]
                            val b = pts[i + 1]
                            val cx = (a.x + b.x) / 2f
                            cubicTo(cx, a.y, cx, b.y, b.x, b.y)
                        }
                    }
                    drawPath(
                        path = p,
                        color = curveColor,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StudioChannelFaderBank(
    bands: List<EqualizerBand>,
    enabled: Boolean,
    onBandLevelChange: (Int, Int) -> Unit,
    onResetBand: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("5-Band Console Faders", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "±15.0 dB RANGE",
                    style = MonoMetricsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bands.forEach { band ->
                    StudioChannelStripFader(
                        band = band,
                        enabled = enabled,
                        onLevelChange = { level -> onBandLevelChange(band.index, level) },
                        onResetBand = { onResetBand(band.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioChannelStripFader(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    onResetBand: () -> Unit
) {
    val activeColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        band.levelMb > 40 -> MaterialTheme.colorScheme.primary
        band.levelMb < -40 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val trackBg = MaterialTheme.colorScheme.surfaceContainerLowest
    val capBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val capGripColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val zeroNotchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val span = (band.maxLevelMb - band.minLevelMb).coerceAtLeast(1).toFloat()
    val norm = ((band.levelMb - band.minLevelMb) / span).coerceIn(0f, 1f)
    val animatedNorm by animateFloatAsState(
        targetValue = norm,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fader_norm_${band.index}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = activeColor.copy(alpha = if (enabled) 0.14f else 0.08f)
        ) {
            Text(
                text = String.format(Locale.US, "%+.1f", band.levelDb),
                style = MonoMetricsStyle,
                fontWeight = FontWeight.Bold,
                color = activeColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(56.dp)
                .height(180.dp)
                .pointerInput(band.index, band.minLevelMb, band.maxLevelMb) {
                    detectTapGestures(onDoubleTap = { onResetBand() })
                }
                .pointerInput(band.index, band.minLevelMb, band.maxLevelMb) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val yNorm = 1f - (change.position.y / h).coerceIn(0f, 1f)
                        val newLevel = (band.minLevelMb + yNorm * span).toInt()
                        onLevelChange(newLevel)
                    }
                }
                .testTag("eq_fader_${band.index}"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(42.dp)
            ) {
                val trackW = 6.dp.toPx()
                val centerX = size.width / 2f
                val trackX = centerX - trackW / 2f
                val padY = 10.dp.toPx()
                val usableH = (size.height - padY * 2f).coerceAtLeast(1f)
                val midY = padY + usableH / 2f

                // Scale ticks on sides of channel slot
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                    val ty = padY + frac * usableH
                    val isCenter = frac == 0.5f
                    val tickLen = if (isCenter) 9.dp.toPx() else 5.dp.toPx()
                    val color = if (isCenter) zeroNotchColor else tickColor
                    drawLine(
                        color = color,
                        start = Offset(trackX - tickLen - 3.dp.toPx(), ty),
                        end = Offset(trackX - 3.dp.toPx(), ty),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                    drawLine(
                        color = color,
                        start = Offset(trackX + trackW + 3.dp.toPx(), ty),
                        end = Offset(trackX + trackW + 3.dp.toPx() + tickLen, ty),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Channel recessed slot
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(trackX, padY),
                    size = Size(trackW, usableH),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                val thumbY = padY + (1f - animatedNorm) * usableH
                val fillTop = minOf(midY, thumbY)
                val fillHeight = abs(thumbY - midY)

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackX, fillTop),
                    size = Size(trackW, fillHeight.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                // Tactile Studio Fader Cap
                val capW = 28.dp.toPx()
                val capH = 14.dp.toPx()
                drawRoundRect(
                    color = capBg,
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.85f),
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawLine(
                    color = capGripColor,
                    start = Offset(centerX - 8.dp.toPx(), thumbY),
                    end = Offset(centerX + 8.dp.toPx(), thumbY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = band.label,
                style = MonoMetricsStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = band.roleLabel,
                style = MonoMetricsStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudioDynamicsAndSpatialRack(
    uiState: EqualizerUiState,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Spatial & Output Dynamics", style = MaterialTheme.typography.titleMedium)
                val headroomLabel = when {
                    uiState.estimatedPeakBoostDb >= 14f -> "CLIP RISK"
                    uiState.estimatedPeakBoostDb >= 8f -> "HOT"
                    else -> "CLEAN"
                }
                val headroomColor = when {
                    uiState.estimatedPeakBoostDb >= 14f -> MaterialTheme.colorScheme.error
                    uiState.estimatedPeakBoostDb >= 8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = headroomColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = headroomLabel,
                        style = MonoMetricsStyle,
                        color = headroomColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            StudioEffectSliderRow(
                title = "Sub-Bass Punch",
                subtitle = "Low-frequency harmonic shelf enhancement",
                valueLabel = "${uiState.bassBoostStrength / 10}%",
                value = uiState.bassBoostStrength.toFloat(),
                valueRange = 0f..1000f,
                icon = Icons.Rounded.Speaker,
                accentColor = MaterialTheme.colorScheme.primary,
                enabled = uiState.enabled,
                onValueChange = { onBassBoostChange(it.toInt()) },
                testTag = "eq_bass_slider"
            )

            StudioEffectSliderRow(
                title = "3D Stereo Virtualizer",
                subtitle = "Binaural soundstage & crossfeed width",
                valueLabel = "${uiState.virtualizerStrength / 10}%",
                value = uiState.virtualizerStrength.toFloat(),
                valueRange = 0f..1000f,
                icon = Icons.Rounded.SurroundSound,
                accentColor = MaterialTheme.colorScheme.secondary,
                enabled = uiState.enabled,
                onValueChange = { onVirtualizerChange(it.toInt()) },
                testTag = "eq_virtualizer_slider"
            )

            StudioEffectSliderRow(
                title = "Preamp / Loudness Maximizer",
                subtitle = "Clean makeup gain & dynamic floor lift",
                valueLabel = String.format(Locale.US, "+%.1f dB", uiState.loudnessGainMb / 100f),
                value = uiState.loudnessGainMb.toFloat(),
                valueRange = 0f..1500f,
                icon = Icons.Rounded.SpatialAudioOff,
                accentColor = MaterialTheme.colorScheme.tertiary,
                enabled = uiState.enabled,
                onValueChange = { onLoudnessChange(it.toInt()) },
                testTag = "eq_loudness_slider"
            )
        }
    }
}

@Composable
private fun StudioEffectSliderRow(
    title: String,
    subtitle: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val active = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f))
        .coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = active, modifier = Modifier.size(18.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = active.copy(alpha = 0.14f)
            ) {
                Text(
                    text = valueLabel,
                    style = MonoMetricsStyle,
                    fontWeight = FontWeight.Bold,
                    color = active,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Segmented LED meter bar
        val ledBg = MaterialTheme.colorScheme.surfaceContainerLowest
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(top = 2.dp)
        ) {
            val segments = 20
            val gap = 3.dp.toPx()
            val segW = (size.width - gap * (segments - 1)) / segments
            val litCount = (fraction * segments).toInt()
            for (i in 0 until segments) {
                drawRoundRect(
                    color = if (i < litCount && value > 0f) active else ledBg,
                    topLeft = Offset(i * (segW + gap), 0f),
                    size = Size(segW, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = active,
                activeTrackColor = active
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveCustomProfileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var profileName by remember(initialName) { mutableStateOf(initialName) }
    val suggestions = listOf("IEM Reference", "Late Night Studio", "Car Sub", "Podcast Crisp")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Sound Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Store your 5-band EQ curve, Bass Boost, 3D Virtualizer, and Preamp gain as a reusable profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    singleLine = true,
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Studio Late Night") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_profile_name_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { label ->
                        AssistChip(
                            onClick = { profileName = label },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(profileName.trim())
                    onDismiss()
                },
                enabled = profileName.isNotBlank(),
                modifier = Modifier.testTag("eq_confirm_save_profile")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
