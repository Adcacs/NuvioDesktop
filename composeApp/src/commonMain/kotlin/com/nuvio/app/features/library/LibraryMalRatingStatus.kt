package com.nuvio.app.features.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.library_mal_configuration
import nuvio.composeapp.generated.resources.library_mal_loading
import nuvio.composeapp.generated.resources.library_mal_summary
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LibraryMalRatingStatus(ratings: LibraryMalRatings, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (ratings.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = when {
                ratings.needsConfiguration -> stringResource(Res.string.library_mal_configuration)
                ratings.isLoading -> stringResource(Res.string.library_mal_loading, ratings.completed, ratings.total)
                else -> stringResource(Res.string.library_mal_summary, ratings.scores.size, ratings.total)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
