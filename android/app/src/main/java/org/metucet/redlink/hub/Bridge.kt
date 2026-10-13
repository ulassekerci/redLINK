package org.metucet.redlink.hub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.location.Fix
import org.metucet.redlink.protocol.COMM_GET_DECODED_ADC
import org.metucet.redlink.protocol.COMM_GET_VALUES_SETUP
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.Gps
import org.metucet.redlink.protocol.Heartbeat
import org.metucet.redlink.protocol.LobbyRequest
import org.metucet.redlink.protocol.Status
import org.metucet.redlink.protocol.decodeMessage
import org.metucet.redlink.protocol.encodeFrame
import org.metucet.redlink.protocol.encodeMessage
import org.metucet.redlink.protocol.hubPassword
import org.metucet.redlink.protocol.lobbyId
import org.metucet.redlink.protocol.viewerId

// How long the hub has to answer a PING.
private const val ANSWER_TIMEOUT_MS = 5000L

// A write in or right after the login line's is lost on the hub, so the PING
// that confirms a registration follows its login line by this long.
private const val AFTER_LOGIN_MS = 100L

// A login the hub lost is tried again after this long, so the bridge does not
// log in back to back while the hub is discarding what it is sent.
private const val AGAIN_MS = 250L

private const val STATUS_MS = 1000L

// A registration is active for this long after a heartbeat, and closed this
// long after its last one, or after opening when it never had one.
private const val ACTIVE_MS = 3000L
private const val CLOSE_MS = 10_000L

// Frames waiting to be written to one registration. When it is full the
// oldest go, for that viewer only.
private const val OUTBOX = 64

// The bridge's hold on the lobby.
sealed interface HubState {
    // Registering the lobby when the run starts.
    data object Connecting : HubState

    // The lobby is registered.
    data object Connected : HubState

    // The lobby could not be registered, or its connection dropped.
    data object Lost : HubState
}

// What the screens show of the bridge. `viewers` counts the active
// registrations.
data class BridgeState(val hub: HubState = HubState.Connecting, val viewers: Int = 0)

private enum class Ping { PONG, NULL }

// A registration the hub has confirmed, with the time its connection opened.
private class Login(val socket: HubSocket, val openedAt: Long)

// One viewer's registration, once the hub has confirmed it.
private class Registration(val socket: HubSocket) {
    // Active from its first heartbeat until 3 s without one. Nothing is
    // written to it while it is not, frames already waiting included.
    var active = false
    val heartbeats = Channel<Unit>(Channel.CONFLATED)
    val outbox = Channel<ByteArray>(OUTBOX, BufferOverflow.DROP_OLDEST)
}

// The bridge's side of the hub usage spec, "The lobby, bridge side" and
// "Polling and the stream", for as long as run() runs. `replies` is every reply
// the board gives, a frame's payload each, and `fixes` every fix. Its hub is
// the socket seam it is given. Its clock is a monotonic one in milliseconds,
// with the coroutine dispatcher's for waiting, which tests replace with a
// virtual one.
class Bridge(
    private val teamCode: String,
    private val protocolVersion: Int,
    private val board: StateFlow<BoardState>,
    private val replies: Flow<ByteArray>,
    private val fixes: Flow<Fix>,
    private val sockets: HubSockets,
    private val now: () -> Long,
) {
    private val password = hubPassword(teamCode)

    private val _state = MutableStateFlow(BridgeState())
    val state: StateFlow<BridgeState> = _state

    // The tokens of the registrations open or being opened.
    private val tokens = mutableSetOf<String>()
    private val registrations = mutableListOf<Registration>()

    // Must run on a dispatcher that runs one coroutine at a time, so the
    // bridge's parts take turns with its state.
    suspend fun run(): Unit = coroutineScope {
        launch {
            while (true) {
                delay(STATUS_MS)
                sendStatus()
            }
        }
        launch {
            board.map(::boardState).distinctUntilChanged().drop(1).collect { sendStatus() }
        }
        // Framing a payload again gives back the bytes the board sent, because
        // the decoder takes only a frame that frames so.
        launch {
            replies.collect { payload -> if (isStreamed(payload)) sendToActive(encodeFrame(payload)) }
        }
        launch {
            fixes.collect { fix -> gpsFrame(fix)?.let(::sendToActive) }
        }
        holdLobby(this)
        // Re-registering a lost lobby is ticket 41. Until then the viewers
        // already registered stay, as the spec says they do.
        awaitCancellation()
    }

    // Registers the lobby and opens a registration for each lobby request
    // that arrives there, until the lobby's connection drops. The lobby is
    // never written to.
    private suspend fun holdLobby(scope: CoroutineScope) {
        val lobby = register(lobbyId(teamCode))?.socket
        if (lobby == null) {
            _state.update { it.copy(hub = HubState.Lost) }
            return
        }
        _state.update { it.copy(hub = HubState.Connected) }
        lobby.use {
            val decoder = FrameDecoder()
            // Anything but a well-formed lobby request is discarded. A request
            // for a token that has a registration is a no-op.
            it.received.collect { bytes ->
                for (payload in decoder.push(bytes)) {
                    val request = decodeMessage(payload) as? LobbyRequest ?: continue
                    if (tokens.add(request.token)) scope.launch { serve(request.token) }
                }
            }
        }
        _state.update { it.copy(hub = HubState.Lost) }
    }

    // One viewer's registration, from opening until it is closed.
    private suspend fun serve(token: String) {
        var opened: Registration? = null
        try {
            val login = register(viewerId(teamCode, token)) ?: return
            val socket = login.socket
            val registration = Registration(socket)
            opened = registration
            registrations += registration
            socket.use {
                coroutineScope {
                    launch {
                        for (frame in registration.outbox) if (registration.active) socket.write(frame)
                    }
                    launch {
                        val decoder = FrameDecoder()
                        // Anything but a heartbeat is discarded.
                        socket.received.collect { bytes ->
                            for (payload in decoder.push(bytes)) {
                                if (decodeMessage(payload) == Heartbeat) registration.heartbeats.trySend(Unit)
                            }
                        }
                        // The hub closed the connection.
                        registration.heartbeats.close()
                    }
                    keepActive(registration, login.openedAt)
                    coroutineContext.cancelChildren()
                }
            }
        } finally {
            tokens.remove(token)
            if (opened != null) {
                registrations -= opened
                countViewers()
            }
        }
    }

    // Follows a registration's heartbeats until it is to be closed: 10 s
    // without one, counted from opening for one that never had one, or the
    // hub closing its connection.
    private suspend fun keepActive(registration: Registration, openedAt: Long) {
        var silentSince = openedAt
        while (true) {
            val wait = (if (registration.active) ACTIVE_MS else CLOSE_MS) - (now() - silentSince)
            val heard = withTimeoutOrNull(wait) { registration.heartbeats.receiveCatching() }
            when {
                heard == null && registration.active -> setActive(registration, false)
                heard == null || heard.isClosed -> return
                else -> {
                    silentSince = now()
                    setActive(registration, true)
                }
            }
        }
    }

    // A registration that becomes active is sent status at once. One that
    // stops being active loses what was waiting for it, so nothing from before
    // a silence follows the status that ends it.
    private fun setActive(registration: Registration, active: Boolean) {
        if (registration.active == active) return
        registration.active = active
        if (active) {
            registration.outbox.trySend(statusFrame())
        } else {
            while (registration.outbox.tryReceive().isSuccess) Unit
        }
        countViewers()
    }

    private fun countViewers() = _state.update { state -> state.copy(viewers = registrations.count { it.active }) }

    private fun statusFrame() = encodeMessage(Status(protocolVersion, boardState(board.value)))

    private fun sendStatus() = sendToActive(statusFrame())

    private fun sendToActive(frame: ByteArray) {
        for (registration in registrations) if (registration.active) registration.outbox.trySend(frame)
    }

    // Registers an ID on the hub and confirms it with PING, registering again
    // on NULL. Null when the hub cannot be reached.
    private suspend fun register(id: String): Login? {
        while (true) {
            val socket = sockets.open() ?: return null
            val openedAt = now()
            try {
                socket.write(loginLine("VESC", id, password))
                delay(AFTER_LOGIN_MS)
                when (ping(id)) {
                    Ping.PONG -> return Login(socket, openedAt)
                    Ping.NULL -> {
                        socket.close()
                        delay(AGAIN_MS)
                    }
                    null -> {
                        socket.close()
                        return null
                    }
                }
            } catch (e: Throwable) {
                socket.close()
                throw e
            }
        }
    }

    // Asks the hub whether an ID is registered, on a connection of its own.
    // Null when the hub cannot be reached or does not answer in time.
    private suspend fun ping(id: String): Ping? = withTimeoutOrNull(ANSWER_TIMEOUT_MS) {
        val socket = sockets.open() ?: return@withTimeoutOrNull null
        socket.use {
            it.write(loginLine("PING", id, "0"))
            var text = ""
            it.received.takeWhile { bytes ->
                text += String(bytes, Charsets.ISO_8859_1)
                '\n' !in text
            }.collect()
            when (text.substringBefore('\n')) {
                "PONG" -> Ping.PONG
                "NULL" -> Ping.NULL
                else -> null
            }
        }
    }
}

// Only the replies to polls are copied: not the firmware version's, nor
// anything else the board says.
private fun isStreamed(payload: ByteArray) =
    payload[0].toInt() == COMM_GET_VALUES_SETUP || payload[0].toInt() == COMM_GET_DECODED_ADC

// A fix as a GPS message, with 0 for a field it lacks, or null for one whose
// values the message cannot carry, which is not sent.
private fun gpsFrame(fix: Fix): ByteArray? = try {
    encodeMessage(
        Gps(
            gpsLatDeg = fix.latDeg,
            gpsLonDeg = fix.lonDeg,
            gpsAltM = fix.altM ?: 0.0,
            gpsSpeedMS = fix.speedMS ?: 0.0,
            gpsHeadingDeg = fix.headingDeg ?: 0.0,
            gpsAccuracyM = fix.accuracyM ?: 0.0,
            gpsFixTimeUtc = fix.timeUtc,
        ),
    )
} catch (_: IllegalArgumentException) {
    null
}

// The board state as the status message carries it.
private fun boardState(board: BoardState) = if (board is BoardState.Answering) 0 else 1

private fun loginLine(type: String, id: String, password: String) =
    "$type:$id:$password\n".toByteArray(Charsets.US_ASCII)
