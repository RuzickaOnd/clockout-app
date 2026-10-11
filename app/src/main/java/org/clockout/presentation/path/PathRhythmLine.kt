package org.clockout.presentation.path

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.clockout.R
import org.clockout.domain.model.PathProgress

/**
 * The rhythm as one line - "Rhythm: 3 weeks · longest 7", or only "Longest 7" once the current
 * rhythm has ended. Null until a first week has been in rhythm, so a newcomer is not shown zeros.
 * Shared by the home card and the path screen, which must not tell the same progress two ways.
 */
@Composable
fun pathRhythmLine(progress: PathProgress): String? {
    if (progress.longestRhythmWeeks == 0) return null
    val longest = stringResource(R.string.path_rhythm_longest, progress.longestRhythmWeeks)
    if (progress.rhythmWeeks == 0) return longest.replaceFirstChar { it.titlecase() }
    val current = pluralStringResource(R.plurals.path_rhythm_current, progress.rhythmWeeks, progress.rhythmWeeks)
    return "$current · $longest"
}
