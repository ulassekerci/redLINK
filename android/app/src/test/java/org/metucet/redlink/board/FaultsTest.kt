package org.metucet.redlink.board

import org.junit.Assert.assertEquals
import org.junit.Test

class FaultsTest {
    @Test
    fun `a fault is named as firmware 6_06 names it, without the prefix`() {
        assertEquals("OVER_VOLTAGE", faultName(1))
        assertEquals("OVER_TEMP_FET", faultName(5))
        assertEquals("LV_OUTPUT_FAULT", faultName(29))
    }

    @Test
    fun `a code with no name is shown as its number`() {
        assertEquals("30", faultName(30))
        assertEquals("-1", faultName(-1))
    }
}
