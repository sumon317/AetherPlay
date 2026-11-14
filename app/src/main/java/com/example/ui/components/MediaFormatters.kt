package com.example.ui.components

import java.util.Locale

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatSignedDeltaSeconds(deltaMs: Long): String {
    val totalSec = deltaMs / 1000L
    val prefix = if (totalSec >= 0) "+" else "-"
    val absSec = kotlin.math.abs(totalSec)
    val mins = absSec / 60L
    val secs = absSec % 60L
    return if (mins > 0) {
        String.format(Locale.US, "%s%d:%02d", prefix, mins, secs)
    } else {
        String.format(Locale.US, "%s%ds", prefix, secs)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatResolution(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return ""
    return when {
        height >= 2160 || width >= 3840 -> "4K"
        height >= 1440 || width >= 2560 -> "2K"
        height >= 1080 || width >= 1920 -> "1080p"
        height >= 720 || width >= 1280 -> "720p"
        height >= 480 -> "480p"
        else -> "${height}p"
    }
}
