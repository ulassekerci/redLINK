package org.metucet.redlink

import android.content.Context
import org.metucet.redlink.board.simulation

// The board a run uses, picked once on Setup and remembered. Only the
// simulated board can be picked so far.
sealed interface PickedBoard {
    data object Simulated : PickedBoard
}

private const val BOARD = "board"
private const val SIMULATED = "simulated"

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
}
