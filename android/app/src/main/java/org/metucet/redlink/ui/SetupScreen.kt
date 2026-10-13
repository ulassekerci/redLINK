package org.metucet.redlink.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import org.metucet.redlink.Grant
import org.metucet.redlink.PickedBoard
import org.metucet.redlink.board.simulation
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
    teamCode: String,
    version: String,
    onGrant: (Grant) -> Unit,
    onPick: (PickedBoard) -> Unit,
    onBack: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
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
            Heading("Team code")
            Group { ListRow(formatTeamCode(teamCode)) }
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
}

@Composable
private fun State(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)
