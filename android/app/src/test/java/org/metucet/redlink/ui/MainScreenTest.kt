package org.metucet.redlink.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.Run
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.hub.BridgeState
import org.metucet.redlink.hub.HubAddress
import org.metucet.redlink.hub.HubState
import org.metucet.redlink.hub.PUBLIC_HUB
import org.metucet.redlink.location.Fix
import org.metucet.redlink.log.LogState

class MainScreenTest {
    private fun run(board: BoardState, simulated: Boolean = false) =
        Run(startedAt = 0, simulated = simulated, logName = "redLINK_2026-09-26_14-22-09.csv", board = board)

    @Test
    fun `the Vehicle row outside a run says whether a board is picked`() {
        assertEquals(RowState("No vehicle picked", Dot.AMBER), vehicleRow(boardPicked = false, run = null))
        assertEquals(RowState("Not connected", Dot.GREY), vehicleRow(boardPicked = true, run = null))
    }

    @Test
    fun `the Vehicle row during a run follows the board`() {
        val answering = BoardState.Answering(faultCode = 0)
        assertEquals(RowState("Connected", Dot.GREEN), vehicleRow(true, run(answering)))
        assertEquals(RowState("Simulated", Dot.GREEN), vehicleRow(true, run(answering, simulated = true)))
        assertEquals(RowState("Reconnecting", Dot.RED), vehicleRow(true, run(BoardState.Unreachable)))
        assertEquals(RowState("Reconnecting", Dot.RED), vehicleRow(true, run(BoardState.Unreachable, simulated = true)))
    }

    @Test
    fun `the Vehicle row names a fault in red`() {
        val faulty = BoardState.Answering(faultCode = 5)
        assertEquals(RowState("Connected · fault OVER_TEMP_FET", Dot.RED), vehicleRow(true, run(faulty)))
        assertEquals(RowState("Connected · fault OVER_TEMP_FET", Dot.RED), vehicleRow(true, run(faulty, simulated = true)))
        assertEquals(RowState("Connected · fault 77", Dot.RED), vehicleRow(true, run(BoardState.Answering(faultCode = 77))))
    }

    private fun bridging(hub: HubState, viewers: Int = 0) =
        run(BoardState.Answering(faultCode = 0)).copy(bridge = BridgeState(hub, viewers))

    @Test
    fun `the Hub row says whether the lobby is registered`() {
        assertEquals(RowState("Not connected", Dot.GREY), hubRow(null, now = 0))
        assertEquals(RowState("Connecting", Dot.AMBER), hubRow(bridging(HubState.Connecting), now = 0))
        assertEquals(RowState("Connected", Dot.GREEN), hubRow(bridging(HubState.Connected), now = 0))
    }

    @Test
    fun `the Hub row counts down the wait left before the next attempt, in whole seconds rounded up`() {
        val waiting = bridging(HubState.Reconnecting(at = 14_000))
        assertEquals(RowState("Reconnecting in 4 s", Dot.RED), hubRow(waiting, now = 10_000))
        assertEquals(RowState("Reconnecting in 4 s", Dot.RED), hubRow(waiting, now = 10_001))
        assertEquals(RowState("Reconnecting in 3 s", Dot.RED), hubRow(waiting, now = 11_000))
        assertEquals(RowState("Reconnecting in 1 s", Dot.RED), hubRow(waiting, now = 13_999))
        // The attempt is due, and the row is about to read Connecting.
        assertEquals(RowState("Reconnecting in 1 s", Dot.RED), hubRow(waiting, now = 14_000))
    }

    @Test
    fun `the Hub row says so while Android reports no network`() {
        for (hub in listOf(HubState.Connecting, HubState.Connected, HubState.Reconnecting(at = 14_000))) {
            assertEquals(RowState("No network", Dot.RED), hubRow(bridging(hub).copy(networkAvailable = false), now = 10_000))
        }
    }

    @Test
    fun `the Hub row shows a hub other than the public one as a second line`() {
        assertEquals(null, hubLine(PUBLIC_HUB))
        assertEquals("203.0.113.7:65101", hubLine(HubAddress("203.0.113.7", 65101)))
        assertEquals("veschub.vedder.se:65102", hubLine(HubAddress("veschub.vedder.se", 65102)))
    }

    @Test
    fun `the Viewers row counts the active registrations while the lobby is registered`() {
        assertEquals(RowState("–", Dot.GREY), viewersRow(null))
        assertEquals(RowState("0 (hub down)", Dot.AMBER), viewersRow(bridging(HubState.Connecting)))
        assertEquals(RowState("0 (hub down)", Dot.AMBER), viewersRow(bridging(HubState.Reconnecting(at = 1000), viewers = 2)))
        assertEquals(RowState("0 viewers", Dot.AMBER), viewersRow(bridging(HubState.Connected)))
        assertEquals(RowState("1 viewer", Dot.GREEN), viewersRow(bridging(HubState.Connected, viewers = 1)))
        assertEquals(RowState("3 viewers", Dot.GREEN), viewersRow(bridging(HubState.Connected, viewers = 3)))
    }

    @Test
    fun `the notification's text is the Vehicle row's then the Viewers row's`() {
        assertEquals(
            "Vehicle connected · 3 viewers",
            notificationText(RowState("Connected", Dot.GREEN), RowState("3 viewers", Dot.GREEN)),
        )
        assertEquals(
            "Vehicle reconnecting · 0 (hub down)",
            notificationText(RowState("Reconnecting", Dot.RED), RowState("0 (hub down)", Dot.AMBER)),
        )
        // A fault's name keeps its case.
        assertEquals(
            "Vehicle connected · fault OVER_TEMP_FET · 1 viewer",
            notificationText(RowState("Connected · fault OVER_TEMP_FET", Dot.RED), RowState("1 viewer", Dot.GREEN)),
        )
    }

    private fun fixAt(at: Long, accuracyM: Double?) =
        Fix(39.8875, 32.7811, null, null, null, accuracyM, timeUtc = 1_790_000_000_000, at = at)

    @Test
    fun `the GPS row gives the accuracy of a fix in the last 3 s`() {
        assertEquals(RowState("±4 m", Dot.GREEN), gpsRow(fixAt(10_000, 4.0), now = 10_000))
        assertEquals(RowState("±4 m", Dot.GREEN), gpsRow(fixAt(10_000, 4.0), now = 12_999))
        // Rounded up, so the row never claims better than the fix.
        assertEquals(RowState("±4 m", Dot.GREEN), gpsRow(fixAt(10_000, 3.2), now = 10_500))
        assertEquals(RowState("±1 m", Dot.GREEN), gpsRow(fixAt(10_000, 0.3), now = 10_500))
        assertEquals(RowState("±6554 m", Dot.GREEN), gpsRow(fixAt(10_000, 6553.5), now = 10_500))
        assertEquals(RowState("Fix", Dot.GREEN), gpsRow(fixAt(10_000, null), now = 10_500))
    }

    @Test
    fun `the GPS row has no fix before the first one or once the latest is 3 s old`() {
        assertEquals(RowState("No fix", Dot.AMBER), gpsRow(null, now = 10_000))
        assertEquals(RowState("No fix", Dot.AMBER), gpsRow(fixAt(10_000, 4.0), now = 13_000))
        assertEquals(RowState("No fix", Dot.AMBER), gpsRow(fixAt(10_000, 4.0), now = 600_000))
    }

    @Test
    fun `the Log row says whether the run's log is being written`() {
        assertEquals(RowState("Idle", Dot.GREY), logRow(null))
        assertEquals(RowState("Recording", Dot.GREEN), logRow(LogState.Writing(1_250_000)))
        assertEquals(RowState("Recording", Dot.GREEN), logRow(LogState.Writing(0)))
        assertEquals(RowState("Not writing", Dot.RED), logRow(LogState.NotWriting))
    }

    @Test
    fun `the setup notice gives the reason Start is disabled`() {
        assertEquals(
            "Setup incomplete: 2 grants missing. Start is disabled. Open setup →",
            setupNotice(missingGrants = 2, boardPicked = false),
        )
        assertEquals(
            "Setup incomplete: 1 grant missing. Start is disabled. Open setup →",
            setupNotice(missingGrants = 1, boardPicked = true),
        )
        assertEquals(
            "Setup incomplete: no vehicle picked. Start is disabled. Open setup →",
            setupNotice(missingGrants = 0, boardPicked = false),
        )
        assertEquals(null, setupNotice(missingGrants = 0, boardPicked = true))
    }
}
