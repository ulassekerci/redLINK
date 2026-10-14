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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.metucet.redlink.Grant
import org.metucet.redlink.PickedBoard
import org.metucet.redlink.board.Device
import org.metucet.redlink.board.scanForDevices
import org.metucet.redlink.board.simulation
import org.metucet.redlink.hub.HubAddress
import org.metucet.redlink.hub.typedHubAddress
import org.metucet.redlink.grantsNeeded
import org.metucet.redlink.protocol.FwVersion
import org.metucet.redlink.protocol.formatTeamCode

// A board by its Bluetooth name, or by its address where it gave no name.
private val PickedBoard.label
    get() = when (this) {
        PickedBoard.Simulated -> "Simulated board"
        is PickedBoard.Bluetooth -> name ?: address
    }

// The line under a board's label: its address, where the label is not the
// address already.
private val PickedBoard.addressLine
    get() = (this as? PickedBoard.Bluetooth)?.takeIf { it.name != null }?.address

// The board's firmware version as Setup shows it: Firmware 6.06.
internal fun firmwareLine(version: FwVersion) = "Firmware %d.%02d".format(Locale.ROOT, version.fwMajor, version.fwMinor)

// `running` locks what cannot change during a run. `firmware` is the picked
// board's version, once one has been read.
@Composable
fun SetupScreen(
    granted: Set<Grant>,
    board: PickedBoard?,
    firmware: FwVersion?,
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
                val supporting = listOfNotNull(board?.addressLine, firmware?.let(::firmwareLine)).joinToString("\n")
                ListRow(board?.label ?: "No vehicle picked", supporting = supporting.ifEmpty { null }) {
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
    // A run that starts while a dialog is open takes the dialog with it.
    if (picking && !running) {
        PickerDialog(
            scanning = Grant.BLUETOOTH in granted,
            onCancel = { picking = false },
            onPick = {
                onPick(it)
                picking = false
            },
        )
    }
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

// The device list: the devices a scan finds for as long as it is open, and
// the simulated board in a debug build. Without the Bluetooth grant there is
// no scan.
@Composable
private fun PickerDialog(scanning: Boolean, onCancel: () -> Unit, onPick: (PickedBoard) -> Unit) {
    val context = LocalContext.current
    // Each device once, by address, under the latest name it gave.
    val devices by produceState(emptyMap<String, Device>(), scanning) {
        if (!scanning) return@produceState
        context.scanForDevices().collect { found ->
            value = value + (found.address to found.copy(name = found.name ?: value[found.address]?.name))
        }
    }
    // Each as the board it would be once picked.
    val options = devices.values.map { PickedBoard.Bluetooth(it.address, it.name) } +
        listOfNotNull(PickedBoard.Simulated.takeIf { simulation != null })
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Pick vehicle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (option in options) {
                    ListRow(option.label, Modifier.clickable { onPick(option) }, supporting = option.addressLine)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) { Text("Cancel") }
        },
    )
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
