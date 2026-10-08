package org.metucet.redlink.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.metucet.redlink.R
import org.metucet.redlink.Run
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.faultName

// Why Start is disabled, or null when setup is complete.
internal fun setupNotice(missingGrants: Int, boardPicked: Boolean): String? {
    val missing = when {
        missingGrants == 1 -> "1 grant missing"
        missingGrants > 1 -> "$missingGrants grants missing"
        !boardPicked -> "no vehicle picked"
        else -> return null
    }
    return "Setup incomplete: $missing. Start is disabled. Open setup →"
}

// What a status row says beside its label, and its dot.
internal data class RowState(val text: String, val dot: Dot)

internal fun vehicleRow(boardPicked: Boolean, run: Run?): RowState {
    if (run == null) {
        return if (boardPicked) RowState("Not connected", Dot.GREY) else RowState("No vehicle picked", Dot.AMBER)
    }
    return when (val board = run.board) {
        BoardState.Unreachable -> RowState("Reconnecting", Dot.RED)
        is BoardState.Answering -> when {
            board.faultCode != 0 -> RowState("Connected · fault ${faultName(board.faultCode)}", Dot.RED)
            run.simulated -> RowState("Simulated", Dot.GREEN)
            else -> RowState("Connected", Dot.GREEN)
        }
    }
}

// A run's duration as it is shown: 12:44, and 1:02:44 from one hour.
internal fun runDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val rest = "%02d".format(seconds % 60)
    return if (hours > 0) "$hours:${"%02d".format(minutes)}:$rest" else "$minutes:$rest"
}

// The main screen. It shows whether each link is up and never a telemetry
// value: nobody reads the phone while driving. Stop only asks; StopDialog is
// what ends a run.
@Composable
fun MainScreen(
    run: Run?,
    missingGrants: Int,
    boardPicked: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onLogs: () -> Unit,
    onSetup: () -> Unit,
) {
    // A run has its setup behind it, and the button then reads Stop.
    val notice = if (run == null) setupNotice(missingGrants, boardPicked) else null
    Scaffold(
        bottomBar = {
            val button = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth().height(56.dp)
            if (run == null) {
                Button(onClick = onStart, modifier = button, enabled = notice == null) {
                    Text("Start", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                FilledTonalButton(
                    onClick = onStop,
                    modifier = button,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text("Stop", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "redLINK",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                run?.let { RunClock(it) }
            }
            notice?.let { Notice(it, onSetup) }
            Group {
                StateRow("Vehicle", vehicleRow(boardPicked, run))
                // Nothing joins the hub or writes a log during a run yet.
                StateRow("Hub", RowState("Not connected", Dot.GREY))
                StateRow("Viewers", RowState("–", Dot.GREY))
                StateRow("Log", RowState("Idle", Dot.GREY))
            }
            Group {
                NavRow("Logs", onLogs)
                HorizontalDivider()
                NavRow("Setup", onSetup)
            }
        }
    }
}

// Counts along with the run, once a second.
@Composable
private fun RunClock(run: Run) {
    fun seconds() = (SystemClock.elapsedRealtime() - run.startedAt) / 1000
    var shown by remember(run.startedAt) { mutableLongStateOf(seconds()) }
    LaunchedEffect(run.startedAt) {
        while (true) {
            shown = seconds()
            // Until the next whole second of the run.
            delay(1000 - (SystemClock.elapsedRealtime() - run.startedAt) % 1000)
        }
    }
    Text(
        runDuration(shown),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Digits of one width, so the count does not shift as it changes.
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
    )
}

@Composable
fun StopDialog(onKeepRunning: () -> Unit, onStop: () -> Unit) = AlertDialog(
    onDismissRequest = onKeepRunning,
    title = { Text("Stop redLINK?") },
    text = { Text("Bridging and logging end. The log is saved.") },
    dismissButton = { FilledTonalButton(onClick = onKeepRunning) { Text("Keep running") } },
    confirmButton = { Button(onClick = onStop) { Text("Save & stop") } },
)

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
private fun StateRow(label: String, state: RowState) = ListRow(label, dot = state.dot) {
    Text(state.text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) = ListRow(label, Modifier.clickable(onClick = onClick)) {
    Icon(painterResource(R.drawable.chevron_right), contentDescription = null)
}
