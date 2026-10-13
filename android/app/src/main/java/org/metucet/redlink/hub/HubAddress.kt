package org.metucet.redlink.hub

// A hub's host and port, shown as host:port.
data class HubAddress(val host: String, val port: Int) {
    override fun toString() = "$host:$port"
}

// The public hub, from the hub usage spec's "The hub".
val PUBLIC_HUB = HubAddress("veschub.vedder.se", 65101)

// A host and port as typed on Setup, read as the desktop app reads them: an
// empty host, or a port that is not one, is the public hub's.
fun typedHubAddress(host: String, port: String): HubAddress {
    val number = port.trim().takeIf { it.all(Char::isDigit) }?.toIntOrNull()
    return HubAddress(
        host.trim().ifEmpty { PUBLIC_HUB.host },
        number?.takeIf { it in 1..65535 } ?: PUBLIC_HUB.port,
    )
}
