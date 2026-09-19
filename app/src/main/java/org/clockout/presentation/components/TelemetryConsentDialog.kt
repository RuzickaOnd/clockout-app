package org.clockout.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.clockout.R
import org.clockout.ui.theme.DialogSurface
import org.clockout.ui.theme.IndigoAccent
import org.clockout.ui.theme.TextLight
import org.clockout.ui.theme.TextSlate

/**
 * The opt-in question, asked once per install.
 *
 * Neither the back button nor a tap outside dismisses it: an unanswered dialog would otherwise
 * resolve to no answer at all, and the app would keep asking on every launch. The two buttons are
 * the only ways out.
 */
@Composable
fun TelemetryConsentDialog(
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { /* Answering is the only way out - see the KDoc. */ },
        // The body runs to several lines, and left to the platform width the dialog stretches to
        // both screen edges - unlike every other dialog in the app, whose text is short enough to
        // stay narrower. Taking the width off the platform and setting it here keeps the margin.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .padding(horizontal = 28.dp)
            .widthIn(max = 400.dp)
            .testTag("telemetry_consent_dialog"),
        // The same opaque surface every other dialog in the app sets by hand. Left on the Material
        // default this would inherit surfaceContainerHigh, which this theme defines as 8% white,
        // and the ambient glows on the canvas would read straight through the text.
        containerColor = DialogSurface,
        titleContentColor = TextLight,
        textContentColor = TextSlate,
        title = { Text(text = stringResource(R.string.telemetry_consent_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.telemetry_consent_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.telemetry_consent_policy_link),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = IndigoAccent,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .testTag("telemetry_privacy_policy_link")
                        .clickable { onOpenPrivacyPolicy() }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onGrant,
                modifier = Modifier.testTag("telemetry_consent_allow")
            ) {
                Text(
                    text = stringResource(R.string.telemetry_consent_allow),
                    color = IndigoAccent
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeny,
                modifier = Modifier.testTag("telemetry_consent_deny")
            ) {
                Text(
                    text = stringResource(R.string.telemetry_consent_deny),
                    color = TextSlate
                )
            }
        }
    )
}
