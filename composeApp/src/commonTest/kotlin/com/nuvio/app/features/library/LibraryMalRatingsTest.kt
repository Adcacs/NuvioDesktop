package com.nuvio.app.features.library

import com.nuvio.app.features.mdblist.MdbListSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryMalRatingsTest {
    private val settings = MdbListSettings(enabled = true, apiKey = "test")
    private fun item(id: String, name: String = id, type: String = "series") =
        LibraryItem(id = id, type = type, name = name, savedAtEpochMs = 0)

    @Test
    fun `sorts numerically in both directions with missing and invalid scores last`() {
        val items = listOf(item("low"), item("missing"), item("top"), item("equal", "Alpha tie"), item("invalid"))
        val scores = mapOf("series:low" to 7.9, "series:top" to 10.0, "series:equal" to 7.9, "series:invalid" to Double.NaN)
        for (source in LibrarySourceMode.entries) {
            assertEquals(listOf("top", "equal", "low", "invalid", "missing"),
                sortLibraryItems(items, LibrarySortOption.MAL_RATING_DESC, source, malRatings = scores).map { it.id })
            assertEquals(listOf("equal", "low", "top", "invalid", "missing"),
                sortLibraryItems(items, LibrarySortOption.MAL_RATING_ASC, source, malRatings = scores).map { it.id })
        }
        for (value in listOf(null, 0.0, -1.0, 10.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(null, validLibraryMalRating(value))
        }
    }

    @Test
    fun `horizontal and vertical layouts use the same scores and preserve membership`() {
        val low = item("low")
        val high = item("high")
        val film = item("high", type = "movie")
        val sections = listOf(LibrarySection("series", "Series", listOf(low, high)), LibrarySection("movie", "Movies", listOf(film)))
        val scores = mapOf("series:low" to 5.0, "series:high" to 9.0, "movie:high" to 7.0)
        val horizontal = sortLibrarySections(sections, LibrarySortOption.MAL_RATING_DESC, LibrarySourceMode.LOCAL, malRatings = scores)
        assertEquals(listOf("high", "low"), horizontal.first().items.map { it.id })
        val vertical = buildLibraryVerticalProjection(sections, LibrarySourceMode.LOCAL, null, null, LibrarySortOption.MAL_RATING_DESC, malRatings = scores)
        assertEquals(listOf("series:high", "movie:high", "series:low"), vertical.entries.map { libraryDisplayItemKey(it.item) })
        val filtered = buildLibraryVerticalProjection(sections, LibrarySourceMode.LOCAL, null, "movie", LibrarySortOption.MAL_RATING_ASC, malRatings = scores)
        assertEquals(listOf(film), filtered.entries.map { it.item })
    }

    @Test
    fun `rating sort choice survives settings persistence`() {
        for (sort in listOf(LibrarySortOption.MAL_RATING_DESC, LibrarySortOption.MAL_RATING_ASC)) {
            val state = LibraryDisplaySettingsUiState(sortOption = sort)
            assertEquals(state, decodeLibraryDisplaySettings(encodeLibraryDisplaySettings(state)))
        }
    }

    @Test
    fun `saving a non IMDb catalog item retains its explicit IMDb mapping`() {
        val meta = com.nuvio.app.features.details.MetaDetails(
            id = "mal:5114", type = "series", name = "Example", imdbId = "tt1355642",
        )
        assertEquals("tt1355642", meta.toLibraryItem(1).imdbId)
        assertEquals("mal:5114", meta.toLibraryItem(1).id)
    }

    @Test
    fun `does not request ratings for other sorts or when integration is disabled`() = runTest {
        var calls = 0
        val fetch: suspend (LibraryItem) -> Double? = { calls++; 8.0 }
        val ordinary = observeLibraryMalRatings(listOf(item("one")), LibrarySortOption.TITLE_ASC, settings, fetch).last()
        val disabled = observeLibraryMalRatings(listOf(item("one")), LibrarySortOption.MAL_RATING_DESC, MdbListSettings(), fetch).last()
        val empty = observeLibraryMalRatings(emptyList(), LibrarySortOption.MAL_RATING_DESC, settings, fetch).last()
        assertEquals(0, calls)
        assertFalse(ordinary.needsConfiguration)
        assertTrue(disabled.needsConfiguration)
        assertFalse(empty.isLoading)
    }

    @Test
    fun `deduplicates requests bounds concurrency and reports partial missing scores`() = runTest {
        var active = 0
        var peak = 0
        var calls = 0
        val items = (1..85).map { item("tt$it") }
        val states = observeLibraryMalRatings(items + items.take(3), LibrarySortOption.MAL_RATING_DESC, settings) {
            active++
            peak = maxOf(peak, active)
            calls++
            delay(10)
            active--
            if (it.id == "tt1") error("offline")
            if (it.id == "tt2") null else 8.5
        }.toList()
        assertEquals(85, calls)
        assertTrue(peak <= 40)
        assertEquals(listOf(0, 40, 80, 85), states.map { it.completed })
        assertTrue(states.first().isLoading)
        assertFalse(states.last().isLoading)
        assertEquals(83, states.last().scores.size)
    }

    @Test
    fun `cancellation is propagated instead of converted to a missing rating`() = runTest {
        assertFailsWith<CancellationException> {
            observeLibraryMalRatings(listOf(item("one")), LibrarySortOption.MAL_RATING_DESC, settings) {
                throw CancellationException("screen left")
            }.toList()
        }
    }
}
