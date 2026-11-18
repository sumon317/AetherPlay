package com.example.player

import com.example.data.local.MediaItemEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class ShuffleMode(val label: String, val shortLabel: String) {
    OFF("Shuffle Off", "Off"),
    STANDARD("Shuffle", "Shuffle"),
    SMART("Smart Shuffle", "Smart")
}

data class SmartQueueResult(
    val orderedQueue: List<MediaItemEntity>,
    val smartPickUris: Set<String>
)

object SmartQueueEngine {

    /**
     * Builds a smart-shuffled queue that:
     * 1. Preserves [currentItem] at index 0 so active playback is never interrupted.
     * 2. Ranks existing queue items using favorite status, freshness, and controlled variety.
     * 3. Avoids consecutive tracks from the same artist or folder when alternatives exist.
     * 4. Interleaves complementary same-type discoveries from [libraryPool] not already in [baseQueue].
     */
    fun buildSmartQueue(
        currentItem: MediaItemEntity,
        baseQueue: List<MediaItemEntity>,
        libraryPool: List<MediaItemEntity>,
        random: Random = Random.Default
    ): SmartQueueResult {
        val remainingFromQueue = baseQueue
            .filter { it.uri != currentItem.uri }
            .distinctBy { it.uri }

        val existingUris = (baseQueue.map { it.uri } + currentItem.uri).toHashSet()

        val candidateDiscoveries = libraryPool
            .asSequence()
            .filter { it.uri !in existingUris && it.isVideo == currentItem.isVideo }
            .distinctBy { it.uri }
            .toList()

        val maxDiscoveries = if (remainingFromQueue.isEmpty()) {
            min(5, candidateDiscoveries.size)
        } else {
            min(max(1, baseQueue.size / 3), min(5, candidateDiscoveries.size))
        }

        val scoredDiscoveries = candidateDiscoveries
            .sortedByDescending { item ->
                scoreCandidate(
                    item = item,
                    anchor = currentItem,
                    isDiscovery = true,
                    random = random
                )
            }
            .take(maxDiscoveries)

        val smartPickUris = scoredDiscoveries.map { it.uri }.toSet()

        // Greedily order remaining queue items to maximize artist/folder diversity
        val orderedRemaining = diversifyOrder(
            anchor = currentItem,
            candidates = remainingFromQueue,
            random = random
        )

        if (scoredDiscoveries.isEmpty()) {
            return SmartQueueResult(
                orderedQueue = listOf(currentItem) + orderedRemaining,
                smartPickUris = emptySet()
            )
        }

        // Interleave smart discoveries naturally every ~3 tracks
        val merged = mutableListOf(currentItem)
        val remainingPicks = ArrayDeque(scoredDiscoveries)
        var tracksSincePick = 0

        for (track in orderedRemaining) {
            merged += track
            tracksSincePick++
            if (tracksSincePick >= 2 && remainingPicks.isNotEmpty()) {
                merged += remainingPicks.removeFirst()
                tracksSincePick = 0
            }
        }
        while (remainingPicks.isNotEmpty()) {
            merged += remainingPicks.removeFirst()
        }

        return SmartQueueResult(
            orderedQueue = merged,
            smartPickUris = smartPickUris
        )
    }

    private fun diversifyOrder(
        anchor: MediaItemEntity,
        candidates: List<MediaItemEntity>,
        random: Random
    ): List<MediaItemEntity> {
        if (candidates.size <= 1) return candidates
        val pool = candidates.toMutableList()
        val result = mutableListOf<MediaItemEntity>()
        var lastArtist = anchor.artist.trim().lowercase()
        var lastFolder = anchor.folderName.trim().lowercase()

        while (pool.isNotEmpty()) {
            var bestIdx = 0
            var bestScore = Double.NEGATIVE_INFINITY

            for (i in pool.indices) {
                val item = pool[i]
                var score = scoreCandidate(
                    item = item,
                    anchor = anchor,
                    isDiscovery = false,
                    random = random
                )
                val artist = item.artist.trim().lowercase()
                val folder = item.folderName.trim().lowercase()

                if (!item.isVideo && artist.isNotEmpty() && artist != "unknown artist" && artist == lastArtist) {
                    score -= 40.0
                }
                if (folder.isNotEmpty() && folder == lastFolder) {
                    score -= 18.0
                }
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = i
                }
            }

            val chosen = pool.removeAt(bestIdx)
            result += chosen
            lastArtist = chosen.artist.trim().lowercase()
            lastFolder = chosen.folderName.trim().lowercase()
        }
        return result
    }

    private fun scoreCandidate(
        item: MediaItemEntity,
        anchor: MediaItemEntity,
        isDiscovery: Boolean,
        random: Random
    ): Double {
        var score = random.nextDouble(0.0, 22.0)
        if (item.isLiked) score += 26.0
        if (item.lastPlayedAt == 0L) score += 12.0
        if (isDiscovery) {
            // Slight affinity boost if artist or folder relates softly, while still keeping variety
            if (!item.isVideo &&
                anchor.artist.isNotBlank() &&
                item.artist.equals(anchor.artist, ignoreCase = true)
            ) {
                score += 10.0
            }
            if (anchor.folderName.isNotBlank() &&
                item.folderName.equals(anchor.folderName, ignoreCase = true)
            ) {
                score += 6.0
            }
        }
        return score
    }
}
