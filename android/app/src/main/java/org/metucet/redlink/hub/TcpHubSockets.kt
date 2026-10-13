package org.metucet.redlink.hub

import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull

// The public hub, from the hub usage spec's "The hub".
const val PUBLIC_HUB_HOST = "veschub.vedder.se"
const val PUBLIC_HUB_PORT = 65101

private const val CONNECT_TIMEOUT_MS = 5000L

// The hub's connections in the app: plain TCP. Blocking calls run on the IO
// threads and are interrupted when the coroutine waiting on them is
// cancelled, which closes the connection, so Stop never waits on the network.
class TcpHubSockets(private val host: String, private val port: Int) : HubSockets {
    override suspend fun open(): HubSocket? {
        val channel = SocketChannel.open()
        try {
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
            // Each write must leave as its own segment: the hub drops what
            // arrives with the login line.
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
            val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                // A name lookup cannot be interrupted, so it is left to finish
                // on its own rather than waited for.
                val address = CompletableFuture.supplyAsync(
                    { InetSocketAddress(host, port) },
                    Dispatchers.IO.asExecutor(),
                ).await()
                // No network, or no such host.
                if (address.isUnresolved) return@withTimeoutOrNull false
                runInterruptible(Dispatchers.IO) { channel.connect(address) }
            }
            if (connected == true) return TcpHubSocket(channel)
            channel.close()
            return null
        } catch (_: IOException) {
            // The host refuses the connection.
            channel.close()
            return null
        } catch (e: Throwable) {
            channel.close()
            throw e
        }
    }
}

private class TcpHubSocket(private val channel: SocketChannel) : HubSocket {
    override val received: Flow<ByteArray> = flow {
        val buffer = ByteBuffer.allocate(4096)
        while (true) {
            buffer.clear()
            val read = try {
                runInterruptible { channel.read(buffer) }
            } catch (_: IOException) {
                -1
            }
            if (read < 0) break
            emit(buffer.array().copyOf(read))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun write(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        try {
            runInterruptible(Dispatchers.IO) { while (buffer.hasRemaining()) channel.write(buffer) }
        } catch (_: IOException) {
            // The connection is gone; its reader learns it.
        }
    }

    override fun close() = channel.close()
}
