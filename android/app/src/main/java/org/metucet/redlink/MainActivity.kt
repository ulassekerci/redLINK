package org.metucet.redlink

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.metucet.redlink.ui.LogsScreen
import org.metucet.redlink.ui.MainScreen
import org.metucet.redlink.ui.RedLinkTheme
import org.metucet.redlink.ui.SetupScreen

private enum class Screen { MAIN, LOGS, SETUP }

class MainActivity : ComponentActivity() {
    private var granted by mutableStateOf(emptySet<Grant>())
    private val grantRequests = GrantRequests(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            RedLinkTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
                LaunchedEffect(screen) { granted = grantsInPlace() }
                BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }
                when (screen) {
                    Screen.MAIN -> MainScreen(
                        missingGrants = Grant.entries.size - granted.size,
                        // Nothing picks a board yet.
                        boardPicked = false,
                        onLogs = { screen = Screen.LOGS },
                        onSetup = { screen = Screen.SETUP },
                    )
                    Screen.LOGS -> LogsScreen(onBack = { screen = Screen.MAIN })
                    Screen.SETUP -> SetupScreen(
                        granted = granted,
                        version = version,
                        onGrant = grantRequests::request,
                        onBack = { screen = Screen.MAIN },
                    )
                }
            }
        }
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
