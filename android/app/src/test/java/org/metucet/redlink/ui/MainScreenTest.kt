package org.metucet.redlink.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.Run
import org.metucet.redlink.board.BoardState

class MainScreenTest {
    private fun run(board: BoardState, simulated: Boolean = false) = Run(startedAt = 0, simulated = simulated, board = board)

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

    @Test
    fun `a run's duration is minutes and seconds, with hours from one hour`() {
        assertEquals("0:00", runDuration(0))
        assertEquals("0:59", runDuration(59))
        assertEquals("12:44", runDuration(12 * 60 + 44))
        assertEquals("59:59", runDuration(3599))
        assertEquals("1:00:00", runDuration(3600))
        assertEquals("1:02:44", runDuration(3600 + 2 * 60 + 44))
        assertEquals("26:00:07", runDuration(26 * 3600 + 7))
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
