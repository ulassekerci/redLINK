package org.metucet.redlink.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.metucet.redlink.Grant
import org.metucet.redlink.PickedBoard
import org.metucet.redlink.board.simulation
import org.metucet.redlink.hub.HubAddress
import org.metucet.redlink.hub.typedHubAddress
import org.metucet.redlink.grantsNeeded
import org.metucet.redlink.protocol.formatTeamCode

// The boards the picker offers. A debug build offers the simulated board.
private val pickable = listOfNotNull<PickedBoard>(PickedBoard.Simulated.takeIf { simulation != null })

private val PickedBoard.label
    get() = when (this) {
        PickedBoard.Simulated -> "Simulated board"
    }

// `running` locks what cannot change during a run.
@Composable
fun SetupScreen(
    granted: Set<Grant>,
    board: PickedBoard?,
    running: Boolean,
    hub: HubAddress,
    teamCode: String,
    version: String,
    onGrant: (Grant) -> Unit,
    onPick: (PickedBoard) -> Unit,
    onHub: (HubAddress) -> Unit,
    onNewCode: () -> Unit,
    onBack: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var editingHub by rememberSaveable { mutableStateOf(false) }
    var askingNewCode by rememberSaveable { mutableStateOf(false) }
    val needed = grantsNeeded(board)
    Scaffold(topBar = { BackBar("Setup", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            // A grant the picked board does not need counts as in place.
            Heading("Grants (${Grant.entries.size - (needed - granted).size} of ${Grant.entries.size})")
            Group {
                for (grant in Grant.entries) {
                    when {
                        grant in granted -> ListRow(grant.label, dot = Dot.GREEN) { State("Granted") }
                        grant !in needed -> ListRow(grant.label, dot = Dot.GREY) { State("Not needed") }
                        else -> ListRow(grant.label, dot = Dot.RED) {
                            FilledTonalButton(onClick = { onGrant(grant) }) { Text("Grant") }
                        }
                    }
                }
            }
            Heading("Vehicle")
            Group {
                ListRow(board?.label ?: "No vehicle picked") {
                    FilledTonalButton(onClick = { picking = true }, enabled = !running) {
                        Text(if (board == null) "Pick vehicle" else "Change vehicle")
                    }
                }
            }
            Heading("Hub")
            Group {
                ListRow(hub.toString()) {
                    FilledTonalButton(onClick = { editingHub = true }, enabled = !running) { Text("Edit") }
                }
            }
            Heading("Team code")
            Group {
                ListRow(formatTeamCode(teamCode)) {
                    FilledTonalButton(onClick = { askingNewCode = true }, enabled = !running) { Text("New code") }
                }
            }
            Group { ListRow("Version $version") }
            simulation?.let {
                val stopAnswering by it.stopAnswering.collectAsState()
                Group {
                    ListRow("Stop answering") {
                        Switch(checked = stopAnswering, onCheckedChange = { on -> it.stopAnswering.value = on })
                    }
                }
            }
        }
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Pick vehicle") },
            text = {
                Column {
                    for (option in pickable) {
                        ListRow(
                            option.label,
                            Modifier.clickable {
                                onPick(option)
                                picking = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { picking = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) { Text("Cancel") }
            },
        )
    }
    // A run that starts while a dialog is open takes the dialog with it.
    if (editingHub && !running) {
        HubDialog(
            hub,
            onCancel = { editingHub = false },
            onSave = {
                onHub(it)
                editingHub = false
            },
        )
    }
    if (askingNewCode && !running) {
        NewCodeDialog(
            onCancel = { askingNewCode = false },
            onNewCode = {
                onNewCode()
                askingNewCode = false
            },
        )
    }
}

// Edits the hub's host and port. An empty host, or a port that is not one, is
// the public hub's.
@Composable
private fun HubDialog(hub: HubAddress, onCancel: () -> Unit, onSave: (HubAddress) -> Unit) {
    var host by rememberSaveable { mutableStateOf(hub.host) }
    var port by rememberSaveable { mutableStateOf(hub.port.toString()) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Hub") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { typed -> port = typed.filter(Char::isDigit) },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        dismissButton = { FilledTonalButton(onClick = onCancel) { Text("Cancel") } },
        confirmButton = { Button(onClick = { onSave(typedHubAddress(host, port)) }) { Text("Save") } },
    )
}

// Asks first, because a new code locks out every laptop.
@Composable
private fun NewCodeDialog(onCancel: () -> Unit, onNewCode: () -> Unit) = AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Make a new team code?") },
    text = { Text("Every laptop must be given the new code before it can watch.") },
    dismissButton = { FilledTonalButton(onClick = onCancel) { Text("Cancel") } },
    confirmButton = { Button(onClick = onNewCode) { Text("New code") } },
)

@Composable
private fun State(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)
