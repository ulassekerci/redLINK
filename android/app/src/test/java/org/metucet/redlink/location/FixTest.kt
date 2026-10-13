package org.metucet.redlink.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FixTest {
    private fun reading(accuracyM: Double?) = fixOrNull(
        latDeg = 39.8875,
        lonDeg = 32.7811,
        altM = null,
        speedMS = null,
        headingDeg = null,
        accuracyM = accuracyM,
        timeUtc = 1_790_000_000_000,
        at = 5_000,
    )

    @Test
    fun `a reading coarser than the GPS message carries is not a fix`() {
        assertNull(reading(accuracyM = 6553.6))
        assertNull(reading(accuracyM = 20_000.0))
    }

    @Test
    fun `a reading as coarse as the GPS message carries, or finer, is a fix`() {
        assertEquals(6553.5, reading(accuracyM = 6553.5)?.accuracyM)
        assertEquals(4.0, reading(accuracyM = 4.0)?.accuracyM)
    }

    @Test
    fun `a reading with no accuracy is a fix whose accuracy is missing`() {
        val fix = reading(accuracyM = null)
        assertEquals(
            Fix(39.8875, 32.7811, null, null, null, null, timeUtc = 1_790_000_000_000, at = 5_000),
            fix,
        )
    }
}
