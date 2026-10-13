package org.metucet.redlink

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionTest {
    @Test
    fun `the major version is the first number of the version`() {
        assertEquals(1, majorVersion("1.2.0"))
        assertEquals(12, majorVersion("12.0.3"))
        assertEquals(1, majorVersion("1.2.0-dev"))
        assertEquals(2, majorVersion("2.0.0-rc.1"))
        assertEquals(0, majorVersion("0.0.0-dev"))
    }
}
