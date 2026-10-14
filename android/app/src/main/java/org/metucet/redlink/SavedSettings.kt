package org.metucet.redlink

import android.content.Context
import java.security.SecureRandom
import kotlin.random.asKotlinRandom
import org.metucet.redlink.board.simulation
import org.metucet.redlink.hub.HubAddress
import org.metucet.redlink.hub.PUBLIC_HUB
import org.metucet.redlink.protocol.FwVersion
import org.metucet.redlink.protocol.generateTeamCode

// The board a run uses, picked once on Setup and remembered.
sealed interface PickedBoard {
    data object Simulated : PickedBoard

    // A real board, by its address. The name is what it was called in the
    // scan, where it gave one.
    data class Bluetooth(val address: String, val name: String?) : PickedBoard
}

private const val BOARD = "board"
private const val SIMULATED = "simulated"
private const val BLUETOOTH = "bluetooth"
private const val BOARD_ADDRESS = "board_address"
private const val BOARD_NAME = "board_name"
private const val FIRMWARE_MAJOR = "firmware_major"
private const val FIRMWARE_MINOR = "firmware_minor"
private const val TEAM_CODE = "team_code"
private const val HUB_HOST = "hub_host"
private const val HUB_PORT = "hub_port"

// What the app remembers between launches, in its private settings. A debug
// build is a separate app with settings of its own.
class SavedSettings(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Picking another board forgets the firmware version.
    var board: PickedBoard?
        get() = when (preferences.getString(BOARD, null)) {
            SIMULATED -> PickedBoard.Simulated.takeIf { simulation != null }
            BLUETOOTH -> preferences.getString(BOARD_ADDRESS, null)?.let {
                PickedBoard.Bluetooth(it, preferences.getString(BOARD_NAME, null))
            }
            else -> null
        }
        set(board) {
            if (board == this.board) return
            firmware = null
            val edit = preferences.edit().remove(BOARD_ADDRESS).remove(BOARD_NAME)
            when (board) {
                PickedBoard.Simulated -> edit.putString(BOARD, SIMULATED)
                is PickedBoard.Bluetooth ->
                    edit.putString(BOARD, BLUETOOTH).putString(BOARD_ADDRESS, board.address).putString(BOARD_NAME, board.name)
                null -> edit.remove(BOARD)
            }
            edit.apply()
        }

    // The picked board's firmware version, as last read, or null before it
    // has been.
    var firmware: FwVersion?
        get() = if (preferences.contains(FIRMWARE_MAJOR)) {
            FwVersion(preferences.getInt(FIRMWARE_MAJOR, 0), preferences.getInt(FIRMWARE_MINOR, 0))
        } else {
            null
        }
        set(firmware) {
            val edit = preferences.edit()
            if (firmware == null) {
                edit.remove(FIRMWARE_MAJOR).remove(FIRMWARE_MINOR)
            } else {
                edit.putInt(FIRMWARE_MAJOR, firmware.fwMajor).putInt(FIRMWARE_MINOR, firmware.fwMinor)
            }
            edit.apply()
        }

    // The team code, without its dash. It is made the first time it is read,
    // on first launch, and kept across runs. It is not put in the keystore:
    // it crosses the internet in clear text on every connection.
    val teamCode: String
        get() = preferences.getString(TEAM_CODE, null) ?: newTeamCode()

    // Replaces the team code with a new one, and returns it. Every laptop
    // must then be given the new code.
    fun newTeamCode(): String = generateTeamCode(SecureRandom().asKotlinRandom()).also {
        preferences.edit().putString(TEAM_CODE, it).apply()
    }

    // The hub, the public one until another is set. It has nothing to do with
    // the team code.
    var hub: HubAddress
        get() = HubAddress(
            preferences.getString(HUB_HOST, null) ?: PUBLIC_HUB.host,
            preferences.getInt(HUB_PORT, PUBLIC_HUB.port),
        )
        set(hub) {
            preferences.edit().putString(HUB_HOST, hub.host).putInt(HUB_PORT, hub.port).apply()
        }
}
