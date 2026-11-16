package com.nuvio.app.features.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.profiles.ProfileRepository

@Composable
internal fun rememberLibraryMalRatings(items: List<LibraryItem>, sortOption: LibrarySortOption): LibraryMalRatings {
    val settings by remember {
        MdbListSettingsRepository.ensureLoaded()
        MdbListSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val profile by ProfileRepository.state.collectAsStateWithLifecycle()
    val profileId = profile.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    var ratings by remember(profileId, items, sortOption, settings) { mutableStateOf(LibraryMalRatings()) }
    ScreenActivityEffect(profileId, items, sortOption, settings) { active ->
        if (active) observeLibraryMalRatings(items, sortOption, settings).collect { ratings = it }
    }
    return ratings
}
