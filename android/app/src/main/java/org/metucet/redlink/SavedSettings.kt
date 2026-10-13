package org.metucet.redlink

import android.content.Context
import java.security.SecureRandom
import kotlin.random.asKotlinRandom
import org.metucet.redlink.board.simulation
import org.metucet.redlink.protocol.generateTeamCode

// The board a run uses, picked once on Setup and remembered. Only the
// simulated board can be picked so far.
sealed interface PickedBoard {
    data object Simulated : PickedBoard
}

private const val BOARD = "board"
private const val SIMULATED = "simulated"
private const val TEAM_CODE = "team_code"

// What the app remembers between launches, in its private settings. A debug
// build is a separate app with settings of its own.
class SavedSettings(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var board: PickedBoard?
        get() = when (preferences.getString(BOARD, null)) {
            SIMULATED -> PickedBoard.Simulated.takeIf { simulation != null }
            else -> null
        }
        set(board) {
            val stored = when (board) {
                PickedBoard.Simulated -> SIMULATED
                null -> null
            }
            preferences.edit().putString(BOARD, stored).apply()
        }

    // The team code, without its dash. It is made the first time it is read,
    // on first launch, and kept across runs. It is not put in the keystore:
    // it crosses the internet in clear text on every connection.
    val teamCode: String
        get() = preferences.getString(TEAM_CODE, null)
            ?: generateTeamCode(SecureRandom().asKotlinRandom()).also {
                preferences.edit().putString(TEAM_CODE, it).apply()
            }
}
