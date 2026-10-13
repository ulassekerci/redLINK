package org.metucet.redlink.hub

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.metucet.redlink.protocol.DecodedMessage
import org.metucet.redlink.protocol.EncodableMessage
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.decodeMessage
import org.metucet.redlink.protocol.encodeMessage

// A hub on the far side of the seam, as far as the bridge relies on one: a
// VESC login registers its connection, replacing and closing any earlier one
// of the same ID, and PING answers whether an ID is registered. It notes every
// connection with the time it was opened and what the bridge wrote to it after
// its login line.
class FakeHub(private val now: () -> Long) : HubSockets {
    val sockets = mutableListOf<FakeSocket>()
    private val registered = mutableMapOf<String, FakeSocket>()

    // While false, no connection can be made.
    var reachable = true

    // The number of VESC logins still to come that the hub loses, as it does
    // when another party's slow connection holds it up.
    var loseLogins = 0

    override suspend fun open(): HubSocket? {
        if (!reachable) return null
        return FakeSocket().also { sockets += it }
    }

    // The connection registered under an ID, or null.
    fun registration(id: String) = registered[id]

    // Every VESC login, in order, by ID.
    fun registrations() = sockets.filter { it.type == "VESC" }.map { it.id }

    fun pings() = sockets.filter { it.type == "PING" }.map { it.id }

    inner class FakeSocket : HubSocket {
        private val incoming = Channel<ByteArray>(Channel.UNLIMITED)
        override val received = incoming.receiveAsFlow()

        val openedAt = now()
        var type: String? = null
        var id: String? = null
        var closed = false
            private set

        // What the bridge wrote after its login line, decoded, with the time.
        val written = mutableListOf<Pair<Long, DecodedMessage?>>()
        private val decoder = FrameDecoder()

        override suspend fun write(bytes: ByteArray) {
            if (closed) return
            if (type != null) {
                for (payload in decoder.push(bytes)) written += now() to decodeMessage(payload)
                return
            }
            val (type, id, _) = String(bytes, Charsets.US_ASCII).trimEnd('\n').split(":")
            this.type = type
            this.id = id
            when (type) {
                "VESC" -> if (loseLogins > 0) loseLogins -= 1 else {
                    registered.put(id, this)?.hubCloses()
                }
                "PING" -> {
                    incoming.trySend((if (id in registered) "PONG\n" else "NULL\n").toByteArray())
                    hubCloses()
                }
            }
        }

        override fun close() {
            closed = true
            hubCloses()
        }

        // What a viewer attached to this registration sends.
        fun send(message: EncodableMessage) = send(encodeMessage(message))

        fun send(bytes: ByteArray) {
            incoming.trySend(bytes)
        }

        // The hub ends the connection, and with it any registration it holds.
        fun hubCloses() {
            incoming.close()
            if (registered[id] === this) registered.remove(id)
        }
    }
}
