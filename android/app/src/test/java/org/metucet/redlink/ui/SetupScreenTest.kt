package org.metucet.redlink.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.protocol.FwVersion

class SetupScreenTest {
    @Test
    fun `the firmware line shows the minor as two digits`() {
        assertEquals("Firmware 6.06", firmwareLine(FwVersion(fwMajor = 6, fwMinor = 6)))
        assertEquals("Firmware 5.03", firmwareLine(FwVersion(fwMajor = 5, fwMinor = 3)))
        assertEquals("Firmware 6.10", firmwareLine(FwVersion(fwMajor = 6, fwMinor = 10)))
    }
}
