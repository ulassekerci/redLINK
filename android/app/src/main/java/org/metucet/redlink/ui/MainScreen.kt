package org.metucet.redlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.metucet.redlink.R

// Why Start is disabled, or null when setup is complete.
private fun setupNotice(missingGrants: Int, boardPicked: Boolean): String? {
    val missing = when {
        missingGrants == 1 -> "1 grant missing"
        missingGrants > 1 -> "$missingGrants grants missing"
        !boardPicked -> "no vehicle picked"
        else -> return null
    }
    return "Setup incomplete: $missing. Start is disabled. Open setup →"
}

// The main screen outside a run. It shows whether each link is up and never a
// telemetry value: nobody reads the phone while driving.
@Composable
fun MainScreen(missingGrants: Int, boardPicked: Boolean, onLogs: () -> Unit, onSetup: () -> Unit) {
    val notice = setupNotice(missingGrants, boardPicked)
    Scaffold(
        bottomBar = {
            // Start does nothing yet: there is no run to start.
            Button(
                onClick = {},
                modifier = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(56.dp),
                enabled = notice == null,
            ) {
                Text("Start", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                "redLINK",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            notice?.let { Notice(it, onSetup) }
            Group {
                if (boardPicked) {
                    StateRow("Vehicle", "Not connected", Dot.GREY)
                } else {
                    StateRow("Vehicle", "No vehicle picked", Dot.AMBER)
                }
                StateRow("Hub", "Not connected", Dot.GREY)
                StateRow("Viewers", "–", Dot.GREY)
                StateRow("Log", "Idle", Dot.GREY)
            }
            Group {
                NavRow("Logs", onLogs)
                HorizontalDivider()
                NavRow("Setup", onSetup)
            }
        }
    }
}

@Composable
private fun Notice(text: String, onClick: () -> Unit) = Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(Dot.AMBER.color))
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StateRow(label: String, state: String, dot: Dot) = ListRow(label, dot = dot) {
    Text(state, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) = ListRow(label, Modifier.clickable(onClick = onClick)) {
    Icon(painterResource(R.drawable.chevron_right), contentDescription = null)
}
