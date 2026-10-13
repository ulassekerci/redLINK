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
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.delay
import org.metucet.redlink.R
import org.metucet.redlink.Run
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.faultName
import org.metucet.redlink.hub.HubAddress
import org.metucet.redlink.hub.HubState
import org.metucet.redlink.hub.PUBLIC_HUB
import org.metucet.redlink.location.Fix
import org.metucet.redlink.log.LogState

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

// The whole seconds left until the bridge's next attempt at `at`, from `now`,
// both on the monotonic clock in milliseconds. Rounded up, so the Hub row
// never claims an attempt sooner than it comes, and never less than one.
private fun secondsLeft(at: Long, now: Long) = ((at - now + 999) / 1000).coerceAtLeast(1)

// The Hub row, with the monotonic clock's time now in milliseconds.
internal fun hubRow(run: Run?, now: Long): RowState {
    if (run == null) return RowState("Not connected", Dot.GREY)
    if (!run.networkAvailable) return RowState("No network", Dot.RED)
    return when (val hub = run.bridge.hub) {
        HubState.Connecting -> RowState("Connecting", Dot.AMBER)
        HubState.Connected -> RowState("Connected", Dot.GREEN)
        is HubState.Reconnecting -> RowState("Reconnecting in ${secondsLeft(hub.at, now)} s", Dot.RED)
    }
}

// The Hub row's second line: the hub, when it is not the public one.
internal fun hubLine(hub: HubAddress): String? = hub.takeIf { it != PUBLIC_HUB }?.toString()

// The Viewers row: the active registrations, which count only while the lobby
// is registered.
internal fun viewersRow(run: Run?): RowState {
    val bridge = run?.bridge ?: return RowState("–", Dot.GREY)
    return when {
        bridge.hub != HubState.Connected -> RowState("0 (hub down)", Dot.AMBER)
        bridge.viewers == 0 -> RowState("0 viewers", Dot.AMBER)
        bridge.viewers == 1 -> RowState("1 viewer", Dot.GREEN)
        else -> RowState("${bridge.viewers} viewers", Dot.GREEN)
    }
}

// The run notification's text: Vehicle connected · 3 viewers. Only the
// Vehicle row's first letter is lowered, so a fault keeps its name.
internal fun notificationText(vehicle: RowState, viewers: RowState) =
    "Vehicle ${vehicle.text.replaceFirstChar { it.lowercase() }} · ${viewers.text}"

// How long a fix keeps the GPS row green.
private const val FIX_FRESH_MS = 3000L

// The GPS row during a run, from the latest fix and the monotonic clock's
// time now, both in milliseconds.
internal fun gpsRow(fix: Fix?, now: Long): RowState {
    if (fix == null || now - fix.at >= FIX_FRESH_MS) return RowState("No fix", Dot.AMBER)
    // Rounded up, so the row never claims better than the fix.
    val accuracy = fix.accuracyM?.let { ", ±${ceil(it).toInt()} m" }.orEmpty()
    return RowState("Fix$accuracy", Dot.GREEN)
}

// The Log row, from the run's log, or null outside a run. Megabytes are
// millions of bytes, as the Files app counts them.
internal fun logRow(log: LogState?): RowState = when (log) {
    null -> RowState("Idle", Dot.GREY)
    is LogState.Writing -> RowState("Recording, ${"%.2f".format(Locale.ROOT, log.bytes / 1e6)} MB", Dot.GREEN)
    LogState.NotWriting -> RowState("Not writing", Dot.RED)
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
// what ends a run. `hub` is the one Setup holds, which a run uses.
@Composable
fun MainScreen(
    run: Run?,
    missingGrants: Int,
    boardPicked: Boolean,
    hub: HubAddress,
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
                HubRow(run, hub)
                StateRow("Viewers", viewersRow(run))
                run?.let { GpsRow(it.fix) }
                StateRow("Log", logRow(run?.log))
            }
            Group {
                NavRow("Logs", onLogs)
                HorizontalDivider()
                NavRow("Setup", onSetup)
            }
        }
    }
}

// The run's duration in whole seconds, counting along once a second.
@Composable
internal fun runSeconds(run: Run): Long {
    fun seconds() = (SystemClock.elapsedRealtime() - run.startedAt) / 1000
    var shown by remember(run.startedAt) { mutableLongStateOf(seconds()) }
    LaunchedEffect(run.startedAt) {
        while (true) {
            shown = seconds()
            // Until the next whole second of the run.
            delay(1000 - (SystemClock.elapsedRealtime() - run.startedAt) % 1000)
        }
    }
    return shown
}

@Composable
private fun RunClock(run: Run) {
    Text(
        runDuration(runSeconds(run)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Digits of one width, so the count does not shift as it changes.
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
    )
}

// The GPS row, which turns amber when the latest fix grows old with no new
// one to replace it.
@Composable
private fun GpsRow(fix: Fix?) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(fix) {
        now = SystemClock.elapsedRealtime()
        if (fix == null) return@LaunchedEffect
        delay(fix.at + FIX_FRESH_MS - now)
        now = SystemClock.elapsedRealtime()
    }
    StateRow("GPS", gpsRow(fix, now))
}

// The Hub row, which counts down the wait before the bridge's next attempt.
@Composable
private fun HubRow(run: Run?, hub: HubAddress) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val retryAt = (run?.bridge?.hub as? HubState.Reconnecting)?.at
    LaunchedEffect(retryAt) {
        now = SystemClock.elapsedRealtime()
        if (retryAt == null) return@LaunchedEffect
        while (now < retryAt) {
            // Until the count shown goes down a second.
            delay(retryAt - now - (secondsLeft(retryAt, now) - 1) * 1000)
            now = SystemClock.elapsedRealtime()
        }
    }
    StateRow("Hub", hubRow(run, now), supporting = hubLine(hub))
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
private fun StateRow(label: String, state: RowState, supporting: String? = null) =
    ListRow(label, dot = state.dot, supporting = supporting) {
        Text(state.text, style = MaterialTheme.typography.bodyMedium)
    }

@Composable
private fun NavRow(label: String, onClick: () -> Unit) = ListRow(label, Modifier.clickable(onClick = onClick)) {
    Icon(painterResource(R.drawable.chevron_right), contentDescription = null)
}
