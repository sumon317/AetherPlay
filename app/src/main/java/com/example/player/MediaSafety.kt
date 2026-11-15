package com.example.player

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.URI
import java.util.Locale

object MediaSafety {

    private val ALLOWED_STREAM_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtp", "udp")
    private val ALLOWED_INCOMING_SCHEMES = setOf("content", "file", "http", "https")

    fun isValidStreamUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.length > 2048) return false
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return false

        val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme !in ALLOWED_STREAM_SCHEMES) return false

        val host = parsed.host
        return !host.isNullOrBlank()
    }

    fun isSafeExternalMediaUri(context: Context, uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme !in ALLOWED_INCOMING_SCHEMES) return false

        return when (scheme) {
            "http", "https" -> !uri.host.isNullOrBlank() && !uri.toString().any { it.isISOControl() }
            "content" -> !uri.authority.isNullOrBlank() && uri.authority != "${context.packageName}.fileprovider"
            "file" -> {
                val path = uri.path?.takeIf { it.isNotBlank() } ?: return false
                !isAppPrivateFile(context, File(path))
            }
            else -> false
        }
    }

    fun isAppPrivateFile(context: Context, file: File): Boolean {
        val protectedDirs = buildList {
            context.applicationInfo?.dataDir?.let { add(it) }
            context.filesDir?.parentFile?.let { add(it.absolutePath) }
            context.filesDir?.let { add(it.absolutePath) }
            context.cacheDir?.let { add(it.absolutePath) }
            context.noBackupFilesDir?.let { add(it.absolutePath) }
            context.codeCacheDir?.let { add(it.absolutePath) }
            context.getDatabasePath("aetherplay_db").parentFile?.let { add(it.absolutePath) }
        }
        return isPathInsideProtectedDirs(file.absolutePath, protectedDirs)
    }

    internal fun isPathInsideProtectedDirs(candidatePath: String, protectedDirs: List<String>): Boolean {
        val candidateCanon = runCatching { File(candidatePath).canonicalPath }
            .getOrDefault(File(candidatePath).absolutePath)
            .trimEnd('/')

        return protectedDirs.any { dir ->
            if (dir.isBlank()) return@any false
            val dirCanon = runCatching { File(dir).canonicalPath }
                .getOrDefault(File(dir).absolutePath)
                .trimEnd('/')
            candidateCanon == dirCanon || candidateCanon.startsWith("$dirCanon/")
        }
    }

    fun sanitizeDisplayText(raw: String?, maxLength: Int = 120): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = buildString(raw.length.coerceAtMost(maxLength)) {
            for (ch in raw) {
                if (!ch.isISOControl()) append(ch)
                if (length >= maxLength) break
            }
        }.trim()
        return cleaned
    }
}
