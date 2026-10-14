package org.metucet.redlink.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.metucet.redlink.Run
import org.metucet.redlink.log.LogFolder
import org.metucet.redlink.log.LogState
import org.metucet.redlink.log.SavedLog

// Megabytes are millions of bytes, as the Files app counts them.
private fun megabytes(bytes: Long) = "${"%.1f".format(Locale.ROOT, bytes / 1e6)} MB"

// The line under the title: every log in the folder and their total size.
internal fun logsSummary(count: Int, bytes: Long): String {
    val logs = if (count == 1) "1 log" else "$count logs"
    return "$logs · ${megabytes(bytes)} in Documents/redLINK"
}

private val startFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

// A past log's start time, marked SIM for a simulated run's. A name with no
// start time to read is shown as it is.
internal fun pastLogTitle(log: SavedLog): String {
    val parsed = log.parsed ?: return log.name
    val start = startFormat.format(parsed.start)
    return if (parsed.simulated) "SIM $start" else start
}

// A run's duration with its units named, such as 11 min 54 sec: 11:54 beside
// a start time reads as a time of day. A unit that is zero is left out.
internal fun runDuration(seconds: Long): String {
    val parts = listOf(seconds / 3600 to "hr", seconds % 3600 / 60 to "min", seconds % 60 to "sec")
        .filter { (value, _) -> value > 0 }
        .map { (value, unit) -> "$value $unit" }
    return parts.joinToString(" ").ifEmpty { "0 sec" }
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

internal fun pastLogLine(log: SavedLog) = "${runDuration(log.durationS)} · ${megabytes(log.bytes)}"

internal fun currentLogLine(seconds: Long) = "${runDuration(seconds)} · in progress, cannot be shared yet"

// Share or Delete with the number of logs it acts on. With none selected the
// button is disabled and has no number.
internal fun selectionAction(verb: String, selected: Int) = if (selected == 0) verb else "$verb $selected"

internal fun selectAllLabel(selected: Int, logs: Int) =
    if (logs > 0 && selected == logs) "Select none" else "Select all"

internal fun deleteTitle(count: Int) = if (count == 1) "Delete 1 log?" else "Delete $count logs?"

// The logs in Documents/redLINK/: during a run the one in progress, which
// cannot be touched, then the past ones, which can be shared or deleted.
@Composable
fun LogsScreen(run: Run?, logsClosed: Int, folder: LogFolder, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Null until the folder has been read.
    var logs by remember { mutableStateOf<List<SavedLog>?>(null) }
    // Counts the deletes, each of which has the folder read again.
    var deletes by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    // A run's log becomes a past one once its last rows are written.
    LaunchedEffect(logsClosed, deletes) { logs = withContext(Dispatchers.IO) { folder.list() } }

    val listed = logs.orEmpty()
    val past = listed.filter { it.name != run?.logName }
    // Logs that went since they were selected drop out of the selection.
    val chosen = past.filter { it.id in selected }
    // Nor does the dialog outlive them.
    LaunchedEffect(logs != null && chosen.isEmpty()) { if (logs != null && chosen.isEmpty()) deleting = false }
    // MediaStore's size for the log in progress lags behind; the run's own
    // is current, unless the run cannot write it.
    val currentBytes = when (val log = run?.log) {
        is LogState.Writing -> log.bytes
        else -> listed.find { it.name == run?.logName }?.bytes ?: 0
    }

    Scaffold(
        topBar = { BackBar("Logs", onBack) },
        bottomBar = {
            Row(
                Modifier.navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = { context.startActivity(folder.share(chosen)) }, enabled = chosen.isNotEmpty()) {
                    Text(selectionAction("Share", chosen.size))
                }
                OutlinedButton(onClick = { deleting = true }, enabled = chosen.isNotEmpty()) {
                    Text(selectionAction("Delete", chosen.size))
                }
                TextButton(
                    onClick = { selected = if (chosen.size == past.size) emptySet() else past.map { it.id }.toSet() },
                    enabled = past.isNotEmpty(),
                    // The red is for the wordmark and Start.
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text(selectAllLabel(chosen.size, past.size))
                }
            }
        },
    ) { padding ->
        if (logs == null) return@Scaffold
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            val count = past.size + if (run == null) 0 else 1
            Heading(logsSummary(count, past.sumOf { it.bytes } + currentBytes))
            run?.let {
                Group { ListRow("Current log", dot = Dot.GREEN, supporting = currentLogLine(runSeconds(it))) }
            }
            if (count == 0) {
                Group { ListRow("No logs yet") }
            } else if (past.isNotEmpty()) {
                Group {
                    for (log in past) {
                        val on = log in chosen
                        fun toggle() {
                            selected = if (on) selected - log.id else selected + log.id
                        }
                        ListRow(
                            pastLogTitle(log),
                            Modifier.clickable(onClick = ::toggle),
                            supporting = pastLogLine(log),
                            leading = { Checkbox(checked = on, onCheckedChange = { toggle() }) },
                        )
                    }
                }
            }
        }
    }
    if (deleting && chosen.isNotEmpty()) {
        DeleteDialog(
            count = chosen.size,
            onCancel = { deleting = false },
            onDelete = {
                deleting = false
                val gone = chosen
                scope.launch {
                    withContext(Dispatchers.IO) { folder.delete(gone) }
                    selected = selected - gone.map { it.id }.toSet()
                    deletes++
                }
            },
        )
    }
}

@Composable
private fun DeleteDialog(count: Int, onCancel: () -> Unit, onDelete: () -> Unit) = AlertDialog(
    onDismissRequest = onCancel,
    title = { Text(deleteTitle(count)) },
    text = { Text("The files are removed from Documents/redLINK. This cannot be undone.") },
    dismissButton = { FilledTonalButton(onClick = onCancel) { Text("Cancel") } },
    confirmButton = { Button(onClick = onDelete) { Text("Delete") } },
)
