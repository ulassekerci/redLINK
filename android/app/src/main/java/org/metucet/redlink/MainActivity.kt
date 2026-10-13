package org.metucet.redlink

import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.metucet.redlink.log.LogFolder
import org.metucet.redlink.ui.LogsScreen
import org.metucet.redlink.ui.MainScreen
import org.metucet.redlink.ui.RedLinkTheme
import org.metucet.redlink.ui.SetupScreen
import org.metucet.redlink.ui.StopDialog

private enum class Screen { MAIN, LOGS, SETUP }

class MainActivity : ComponentActivity() {
    companion object {
        // What the run's notification opens the activity with: a tap on it,
        // and a tap on its Stop action.
        const val ACTION_SHOW_MAIN = "org.metucet.redlink.SHOW_MAIN"
        const val ACTION_ASK_STOP = "org.metucet.redlink.ASK_STOP"
    }

    private var granted by mutableStateOf(emptySet<Grant>())
    private val grantRequests = GrantRequests(this)

    // The action of a tap on the notification that the screen has yet to
    // follow.
    private var tapped by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))
        // An activity Android made again still holds the intent it was first
        // opened with, which has been followed already.
        if (savedInstanceState == null) follow(intent)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val settings = SavedSettings(this)
        // Made here on first launch, and the same until New code.
        val firstTeamCode = settings.teamCode
        val logs = LogFolder(this)
        setContent {
            RedLinkTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
                var board by remember { mutableStateOf(settings.board) }
                var hub by remember { mutableStateOf(settings.hub) }
                var teamCode by remember { mutableStateOf(firstTeamCode) }
                var stopDialog by rememberSaveable { mutableStateOf(false) }
                val run by RunService.run.collectAsState()
                val logsClosed by RunService.logsClosed.collectAsState()
                LaunchedEffect(screen) { granted = grantsInPlace() }
                LaunchedEffect(tapped) {
                    val action = tapped ?: return@LaunchedEffect
                    screen = Screen.MAIN
                    if (action == ACTION_ASK_STOP) stopDialog = run != null
                    tapped = null
                }
                // A run that ended takes its dialog with it.
                LaunchedEffect(run == null) { if (run == null) stopDialog = false }
                BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }
                when (screen) {
                    Screen.MAIN -> MainScreen(
                        run = run,
                        missingGrants = (grantsNeeded(board) - granted).size,
                        boardPicked = board != null,
                        hub = hub,
                        onStart = { RunService.start(this) },
                        onStop = { stopDialog = true },
                        onLogs = { screen = Screen.LOGS },
                        onSetup = { screen = Screen.SETUP },
                    )
                    Screen.LOGS -> LogsScreen(run = run, logsClosed = logsClosed, folder = logs, onBack = { screen = Screen.MAIN })
                    Screen.SETUP -> SetupScreen(
                        granted = granted,
                        board = board,
                        running = run != null,
                        hub = hub,
                        teamCode = teamCode,
                        version = version,
                        onGrant = grantRequests::request,
                        onPick = {
                            settings.board = it
                            board = it
                        },
                        onHub = {
                            settings.hub = it
                            hub = it
                        },
                        onNewCode = { teamCode = settings.newTeamCode() },
                        onBack = { screen = Screen.MAIN },
                    )
                }
                if (stopDialog && run != null) {
                    StopDialog(
                        onKeepRunning = { stopDialog = false },
                        onStop = {
                            stopDialog = false
                            RunService.stop(this)
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        follow(intent)
    }

    // Opening the app again from recents hands back the intent that first
    // opened it, which is not a new tap on the notification.
    private fun follow(intent: Intent) {
        val fromRecents = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        val action = intent.action
        if (!fromRecents && (action == ACTION_SHOW_MAIN || action == ACTION_ASK_STOP)) tapped = action
    }

    // Coming back from a system dialog or from system settings.
    override fun onResume() {
        super.onResume()
        granted = grantsInPlace()
    }

    // Coming back from the quick settings panel, which does not pause the
    // activity and holds the location switch.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) granted = grantsInPlace()
    }
}
