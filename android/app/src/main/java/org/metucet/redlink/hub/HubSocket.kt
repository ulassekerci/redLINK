package org.metucet.redlink.hub

import kotlinx.coroutines.flow.Flow

// The bridge's seam to the hub. The bridge opens connections and reads and
// writes bytes through this, and knows nothing of what carries them. TCP is the
// one implementation; tests put a fake hub behind it.

// One connection to the hub.
interface HubSocket : AutoCloseable {
    // The hub's bytes as they arrive. One item may hold part of a frame or
    // several frames. Ends when the hub closes the connection or it fails.
    // Read by one collector.
    val received: Flow<ByteArray>

    // Writes bytes as one write. A write to a closed connection goes nowhere.
    suspend fun write(bytes: ByteArray)

    // Ends the connection for good. Closing twice is harmless.
    override fun close()
}

fun interface HubSockets {
    // A new connection to the hub, with TCP keep-alive on, or null when it
    // cannot be made.
    suspend fun open(): HubSocket?
}
