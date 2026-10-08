package org.metucet.redlink.board

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// The board link's seam. Everything above the link sends a request, receives
// bytes and reads the connection state through this, and knows nothing of
// what carries them. The Bluetooth link and the simulated board are the two
// implementations.
interface BoardLink {
    // Whether the link is up. A request sent while it is down goes nowhere.
    val connected: StateFlow<Boolean>

    // The board's bytes as they arrive. One item may hold part of a frame or
    // several frames. Read by one collector.
    val received: Flow<ByteArray>

    // Sends one request, a whole frame.
    fun send(request: ByteArray)

    // Ends the link for good.
    fun close()
}
