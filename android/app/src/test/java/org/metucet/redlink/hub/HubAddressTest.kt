package org.metucet.redlink.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class HubAddressTest {
    @Test
    fun `a typed host and port are the hub`() {
        assertEquals(HubAddress("203.0.113.7", 65101), typedHubAddress("203.0.113.7", "65101"))
        assertEquals(HubAddress("hub.example.org", 1), typedHubAddress("  hub.example.org ", " 1 "))
        assertEquals(HubAddress("hub.example.org", 65535), typedHubAddress("hub.example.org", "65535"))
    }

    @Test
    fun `an empty host is the public hub's`() {
        assertEquals(HubAddress("veschub.vedder.se", 65102), typedHubAddress("  ", "65102"))
    }

    @Test
    fun `a port that is not one is the public hub's`() {
        for (port in listOf("", "0", "65536", "-1", "6510a", "1.5", "99999999999")) {
            assertEquals(port, HubAddress("hub.example.org", 65101), typedHubAddress("hub.example.org", port))
        }
    }

    @Test
    fun `a hub is shown as host and port`() {
        assertEquals("veschub.vedder.se:65101", PUBLIC_HUB.toString())
    }
}
