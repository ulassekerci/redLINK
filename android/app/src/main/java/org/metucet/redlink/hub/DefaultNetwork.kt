package org.metucet.redlink.hub

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Android's default network, as a run needs it: whether there is one, for the
// Hub row, and each newly available one, for the bridge to register a lost
// lobby at once. Watched from when it is made until it is closed.
class DefaultNetwork(context: Context) : AutoCloseable {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    // Android calls back on one thread of its own. Only the watch's first
    // read of this is on another.
    private var current: Network? = connectivity.activeNetwork

    private val _available = MutableStateFlow(current != null)

    // False while Android reports no network at all.
    val available: StateFlow<Boolean> = _available

    private val _arrivals =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // One item for each newly available default network. The one that was the
    // default when the watch began is not new.
    val arrivals: Flow<Unit> = _arrivals

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val arrived = network != current
            current = network
            _available.value = true
            if (arrived) _arrivals.tryEmit(Unit)
        }

        // A default network that is replaced is not lost: its successor
        // arrives instead.
        override fun onLost(network: Network) {
            if (network != current) return
            current = null
            _available.value = false
        }
    }

    init {
        connectivity.registerDefaultNetworkCallback(callback)
    }

    override fun close() = connectivity.unregisterNetworkCallback(callback)
}
