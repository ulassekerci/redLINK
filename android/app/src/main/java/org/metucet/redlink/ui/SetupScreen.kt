package org.metucet.redlink.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.metucet.redlink.Grant

@Composable
fun SetupScreen(granted: Set<Grant>, version: String, onGrant: (Grant) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { BackBar("Setup", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                "Grants (${granted.size} of ${Grant.entries.size})",
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Group {
                for (grant in Grant.entries) {
                    val inPlace = grant in granted
                    ListRow(grant.label, dot = if (inPlace) Dot.GREEN else Dot.RED) {
                        if (inPlace) {
                            Text("Granted", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            FilledTonalButton(onClick = { onGrant(grant) }) { Text("Grant") }
                        }
                    }
                }
            }
            Group { ListRow("Version $version") }
        }
    }
}
