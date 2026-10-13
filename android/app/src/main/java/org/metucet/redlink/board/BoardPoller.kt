package org.metucet.redlink.board

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.metucet.redlink.protocol.DecodedAdc
import org.metucet.redlink.protocol.DecodedAdcRequest
import org.metucet.redlink.protocol.DecodedMessage
import org.metucet.redlink.protocol.EncodableMessage
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.ValuesSetup
import org.metucet.redlink.protocol.ValuesSetupRequest
import org.metucet.redlink.protocol.decodeMessage
import org.metucet.redlink.protocol.encodeMessage

private const val CYCLE_MS = 50L
private const val REPLY_TIMEOUT_MS = 250L
private const val UNREACHABLE_MS = 2000L

// Five seconds of cycles.
private const val CYCLE_BUFFER = 100

// Two and a half seconds of replies, at two a cycle.
private const val REPLY_BUFFER = 100

// What the bridge knows of the board during a run.
sealed interface BoardState {
    // No reply yet, or none for a while.
    data object Unreachable : BoardState

    // The fault code is the latest setup reply's, 0 for none.
    data class Answering(val faultCode: Int) : BoardState
}

// One poll cycle in which at least one reply arrived. A reply that did not
// arrive is null. `at` is when the cycle ended, on the poller's clock.
data class Cycle(val values: ValuesSetup?, val adc: DecodedAdc?, val at: Long)

// Polls the board through its link for as long as run() runs. The clock is a
// monotonic one, in milliseconds.
class BoardPoller(private val link: BoardLink, private val now: () -> Long) {
    private val decodedReplies = Channel<DecodedMessage>(Channel.UNLIMITED)
    private val anyReply = Channel<Unit>(Channel.CONFLATED)

    private val _state = MutableStateFlow<BoardState>(BoardState.Unreachable)
    val state: StateFlow<BoardState> = _state

    private val _cycles = MutableSharedFlow<Cycle>(extraBufferCapacity = CYCLE_BUFFER)

    // Every cycle with a reply, in order, for the parts that must not miss
    // one, such as the log. A collector may fall five seconds behind before
    // cycles are lost.
    val cycles: SharedFlow<Cycle> = _cycles

    private val _replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = REPLY_BUFFER)

    // Every frame the board sends, its payload, as it comes: late replies and
    // ones this app cannot read included. For the stream, which picks what it
    // copies. A collector may fall two and a half seconds behind before replies
    // are lost.
    val replies: SharedFlow<ByteArray> = _replies

    suspend fun run(): Unit = coroutineScope {
        launch { receive() }
        launch { watchForSilence() }
        // A dropped link is known at once, with no wait for the silence.
        launch { link.connected.collect { up -> if (!up) _state.value = BoardState.Unreachable } }
        var cycleStart = now()
        while (true) {
            val values = ask<ValuesSetup>(ValuesSetupRequest)
            val adc = ask<DecodedAdc>(DecodedAdcRequest)
            if (values != null || adc != null) _cycles.tryEmit(Cycle(values, adc, now()))
            // A cycle that overran is followed at once by the next.
            cycleStart = maxOf(cycleStart + CYCLE_MS, now())
            delay(cycleStart - now())
        }
    }

    // A frame can span several reads, so the decoder buffers.
    private suspend fun receive() {
        val decoder = FrameDecoder()
        link.received.collect { bytes ->
            for (payload in decoder.push(bytes)) {
                _replies.tryEmit(payload)
                decodeMessage(payload)?.let { heard(it) }
            }
        }
    }

    // Any reply shows the board is there, whichever request is being waited on.
    // A fault lasts until a setup reply says otherwise or the board goes quiet.
    private suspend fun heard(reply: DecodedMessage) {
        val faultCode = (reply as? ValuesSetup)?.faultCode
            ?: (_state.value as? BoardState.Answering)?.faultCode
            ?: 0
        _state.value = BoardState.Answering(faultCode)
        anyReply.send(Unit)
        decodedReplies.send(reply)
    }

    // Every reply starts the wait again.
    private suspend fun watchForSilence() = anyReply.receiveAsFlow().collectLatest {
        delay(UNREACHABLE_MS)
        _state.value = BoardState.Unreachable
    }

    // Sends a request and waits for its reply, giving up after the timeout. A
    // reply that came before the request went out is to an earlier one, and is
    // passed over.
    private suspend inline fun <reified Reply : DecodedMessage> ask(request: EncodableMessage): Reply? {
        while (decodedReplies.tryReceive().isSuccess) Unit
        link.send(encodeMessage(request))
        return withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            var reply: Reply? = null
            while (reply == null) reply = decodedReplies.receive() as? Reply
            reply
        }
    }
}
