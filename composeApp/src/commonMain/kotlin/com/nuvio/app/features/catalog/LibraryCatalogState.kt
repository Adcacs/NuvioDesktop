package com.nuvio.app.features.catalog

import com.nuvio.app.features.library.LibraryUiState
import com.nuvio.app.features.library.LibraryProviderOrders
import com.nuvio.app.features.library.LibrarySortOption
import com.nuvio.app.features.library.LibraryMalRatings
import com.nuvio.app.features.library.observeLibraryMalRatings
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.library.librarySorter
import com.nuvio.app.features.library.observeLibraryProviderOrders
import com.nuvio.app.features.library.sortLibraryItems
import com.nuvio.app.features.library.toMetaPreview
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.library_error_sort_failed
import nuvio.composeapp.generated.resources.library_mal_configuration
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf

internal fun Flow<LibraryUiState>.libraryCatalogStates(
    target: CatalogTarget.Library,
    providerOrders: Flow<LibraryProviderOrders>? = null,
    malRatings: Flow<LibraryMalRatings>? = null,
): Flow<CatalogUiState> = if (providerOrders == null && malRatings == null) {
    map { it.libraryCatalogState(target, LibraryProviderOrders(), LibraryMalRatings()) }.distinctUntilChanged()
} else combine(
    this, providerOrders ?: flowOf(LibraryProviderOrders()), malRatings ?: flowOf(LibraryMalRatings()),
) { libraryState, orders, ratings -> libraryState.libraryCatalogState(target, orders, ratings) }.distinctUntilChanged()

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun Flow<LibraryUiState>.libraryCatalogMalRatings(target: CatalogTarget.Library): Flow<LibraryMalRatings> {
    MdbListSettingsRepository.ensureLoaded()
    return combine(this, ProfileRepository.state, MdbListSettingsRepository.uiState) { library, profile, settings ->
        Triple(library.sections.firstOrNull { it.type == target.sectionType }?.items.orEmpty(),
            profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId, settings)
    }.distinctUntilChanged().flatMapLatest { (items, _, settings) ->
        observeLibraryMalRatings(items, target.sortOption, settings)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun Flow<LibraryUiState>.libraryCatalogOrders(target: CatalogTarget.Library): Flow<LibraryProviderOrders> =
    combine(ProfileRepository.state) { library, profile ->
        library.sourceMode to (profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId)
    }.distinctUntilChanged().flatMapLatest { (source, _) ->
        observeLibraryProviderOrders(source.librarySorter(), listOf(target.sectionType), target.sortOption)
    }

private suspend fun LibraryUiState.libraryCatalogState(
    target: CatalogTarget.Library,
    orders: LibraryProviderOrders,
    ratings: LibraryMalRatings,
): CatalogUiState {
    val items = sections.firstOrNull { it.type == target.sectionType }?.items.orEmpty()
    return CatalogUiState(
        items = sortLibraryItems(
            items = items,
            selected = if (orders.failed) LibrarySortOption.DEFAULT else target.sortOption,
            sourceMode = sourceMode,
            listKey = target.sectionType,
            providerOrder = orders.ranks[target.sectionType],
            malRatings = ratings.scores,
        ).map { it.toMetaPreview() }.let(::dedupeCatalogItems),
        isLoading = isLoading || ratings.isLoading,
        errorMessage = when {
            orders.failed -> getString(Res.string.library_error_sort_failed)
            ratings.needsConfiguration -> getString(Res.string.library_mal_configuration)
            else -> errorMessage
        },
    )
}
