package com.nuvio.app.features.library

import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal val LibrarySortOption.isMalRatingSort: Boolean
    get() = this == LibrarySortOption.MAL_RATING_DESC || this == LibrarySortOption.MAL_RATING_ASC

internal fun validLibraryMalRating(value: Double?): Double? =
    value?.takeIf { it.isFinite() && it in 1.0..10.0 }

internal data class LibraryMalRatings(
    val scores: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val needsConfiguration: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
)

/** Fetch only on explicit rating sort. Bounded waves reuse MDBList's batch/cache service. */
internal fun observeLibraryMalRatings(
    items: List<LibraryItem>,
    sortOption: LibrarySortOption,
    settings: MdbListSettings,
    fetchRating: suspend (LibraryItem) -> Double? = { item ->
        MdbListMetadataService.getMalRating(item.type, item.imdbId?.takeIf { it.isNotBlank() } ?: item.id, settings)
    },
): Flow<LibraryMalRatings> = flow {
    if (!sortOption.isMalRatingSort || items.isEmpty()) {
        emit(LibraryMalRatings())
        return@flow
    }
    val uniqueItems = items.distinctBy(::libraryDisplayItemKey)
    if (!settings.isActive) {
        emit(LibraryMalRatings(needsConfiguration = true, total = uniqueItems.size))
        return@flow
    }
    val scores = mutableMapOf<String, Double>()
    var completed = 0
    emit(LibraryMalRatings(isLoading = true, total = uniqueItems.size))
    for (batch in uniqueItems.chunked(40)) {
        val results = coroutineScope {
            batch.map { item ->
                async {
                    val score = try {
                        validLibraryMalRating(fetchRating(item))
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }
                    libraryDisplayItemKey(item) to score
                }
            }.awaitAll()
        }
        results.forEach { (key, score) -> if (score != null) scores[key] = score }
        completed += batch.size
        emit(LibraryMalRatings(scores.toMap(), completed < uniqueItems.size, completed = completed, total = uniqueItems.size))
    }
}
