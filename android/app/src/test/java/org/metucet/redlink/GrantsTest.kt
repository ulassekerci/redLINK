package org.metucet.redlink

import org.junit.Assert.assertEquals
import org.junit.Test

class GrantsTest {
    @Test
    fun `a run needs all six grants`() {
        assertEquals(Grant.entries.toSet(), grantsNeeded(board = null))
    }

    @Test
    fun `a run on the simulated board needs every grant but Bluetooth`() {
        assertEquals(Grant.entries.toSet() - Grant.BLUETOOTH, grantsNeeded(PickedBoard.Simulated))
    }
}
