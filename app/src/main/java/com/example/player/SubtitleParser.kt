package com.example.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class TimedSubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object SubtitleParser {

    fun buildSubtitleConfiguration(context: Context, subtitleUri: Uri): MediaItem.SubtitleConfiguration {
        val fileName = runCatching {
            context.contentResolver.query(subtitleUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: subtitleUri.lastPathSegment ?: "subtitle.srt"

        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeType = when (ext) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "ttml", "xml" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }

        return MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(mimeType)
            .setLanguage("en")
            .setLabel(fileName.substringBeforeLast('.'))
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }

    suspend fun parseExternalSubtitleCues(context: Context, uri: Uri): List<TimedSubtitleCue> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream)).readText()
                    if (content.contains("WEBVTT", ignoreCase = true)) {
                        parseVtt(content)
                    } else {
                        parseSrt(content)
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())
        }

    fun activeCueTextAt(cues: List<TimedSubtitleCue>, positionMs: Long, syncDelayMs: Long): String? {
        if (cues.isEmpty()) return null
        val adjustedPos = positionMs - syncDelayMs
        return cues.firstOrNull { adjustedPos in it.startMs..it.endMs }?.text
    }

    private fun parseSrt(raw: String): List<TimedSubtitleCue> {
        val blocks = raw.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
        val result = mutableListOf<TimedSubtitleCue>()
        val timeRegex = Regex("""(\d{1,2}:\d{2}:\d{2}[,.]\d{3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[,.]\d{3})""")

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val matchLineIdx = lines.indexOfFirst { timeRegex.containsMatchIn(it) }
            if (matchLineIdx < 0) continue
            val match = timeRegex.find(lines[matchLineIdx]) ?: continue
            val start = parseTimestamp(match.groupValues[1])
            val end = parseTimestamp(match.groupValues[2])
            val text = lines.drop(matchLineIdx + 1)
                .joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()
            if (text.isNotEmpty() && end > start) {
                result += TimedSubtitleCue(start, end, text)
            }
        }
        return result
    }

    private fun parseVtt(raw: String): List<TimedSubtitleCue> {
        val blocks = raw.replace("\r\n", "\n").split(Regex("\n\\s*\n"))
        val result = mutableListOf<TimedSubtitleCue>()
        val timeRegex = Regex("""((?:\d{1,2}:)?\d{2}:\d{2}\.\d{3})\s*-->\s*((?:\d{1,2}:)?\d{2}:\d{2}\.\d{3})""")

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val matchLineIdx = lines.indexOfFirst { timeRegex.containsMatchIn(it) }
            if (matchLineIdx < 0) continue
            val match = timeRegex.find(lines[matchLineIdx]) ?: continue
            val start = parseTimestamp(match.groupValues[1])
            val end = parseTimestamp(match.groupValues[2])
            val text = lines.drop(matchLineIdx + 1)
                .joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()
            if (text.isNotEmpty() && end > start) {
                result += TimedSubtitleCue(start, end, text)
            }
        }
        return result
    }

    private fun parseTimestamp(token: String): Long {
        val normalized = token.replace(',', '.')
        val parts = normalized.split(':')
        val (h, m, sAndMs) = when (parts.size) {
            3 -> Triple(parts[0].toLongOrNull() ?: 0L, parts[1].toLongOrNull() ?: 0L, parts[2])
            2 -> Triple(0L, parts[0].toLongOrNull() ?: 0L, parts[1])
            else -> return 0L
        }
        val secParts = sAndMs.split('.')
        val s = secParts.getOrNull(0)?.toLongOrNull() ?: 0L
        val ms = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
        return ((h * 3600L) + (m * 60L) + s) * 1000L + ms
    }
}
