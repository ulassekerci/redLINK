package org.metucet.redlink.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Nothing writes a log yet, so there is none to list.
@Composable
fun LogsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { BackBar("Logs", onBack) }) { padding ->
        Text(
            "No logs yet",
            modifier = Modifier.padding(padding).padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
