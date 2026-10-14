package org.metucet.redlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateTest {
    @Test
    fun `a newer release is offered by its version`() {
        assertEquals("1.3.0", offeredUpdate("v1.3.0", "1.2.0"))
        assertEquals("1.2.1", offeredUpdate("v1.2.1", "1.2.0"))
        assertEquals("2.0.0", offeredUpdate("v2.0.0", "1.9.9"))
        assertEquals("1.10.0", offeredUpdate("v1.10.0", "1.9.0"))
    }

    @Test
    fun `the same or an older release is not offered`() {
        assertNull(offeredUpdate("v1.2.0", "1.2.0"))
        assertNull(offeredUpdate("v1.1.9", "1.2.0"))
        assertNull(offeredUpdate("v0.9.0", "1.0.0"))
    }

    @Test
    fun `a dev or candidate build compares by its major, minor and patch alone`() {
        // A local build is the latest tag with -dev added: it is that release or later.
        assertNull(offeredUpdate("v1.2.0", "1.2.0-dev"))
        assertEquals("1.3.0", offeredUpdate("v1.3.0", "1.2.0-dev"))
        assertEquals("0.1.0", offeredUpdate("v0.1.0", "0.0.0-dev"))
        // A candidate and its final release are the same build.
        assertNull(offeredUpdate("v1.2.0", "1.2.0-rc1"))
        assertEquals("1.3.0", offeredUpdate("v1.3.0", "1.2.0-rc1"))
    }

    @Test
    fun `a pre-release is never offered`() {
        assertNull(offeredUpdate("v1.3.0-rc1", "1.2.0"))
        assertNull(offeredUpdate("v2.0.0-beta", "1.2.0-dev"))
    }

    @Test
    fun `a tag that is not a version is not offered`() {
        assertNull(offeredUpdate("nightly", "1.2.0"))
        assertNull(offeredUpdate("1.3.0", "1.2.0"))
        assertNull(offeredUpdate("", "1.2.0"))
    }
}
