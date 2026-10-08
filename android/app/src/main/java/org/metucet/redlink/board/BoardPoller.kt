package org.metucet.redlink.board

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

// What the bridge knows of the board during a run.
sealed interface BoardState {
    // No reply yet, or none for a while.
    data object Unreachable : BoardState

    // The fault code is the latest setup reply's, 0 for none.
    data class Answering(val faultCode: Int) : BoardState
}

// Polls the board through its link for as long as run() runs. The clock is a
// monotonic one, in milliseconds.
class BoardPoller(private val link: BoardLink, private val now: () -> Long) {
    private val replies = Channel<DecodedMessage>(Channel.UNLIMITED)
    private val anyReply = Channel<Unit>(Channel.CONFLATED)

    private val _state = MutableStateFlow<BoardState>(BoardState.Unreachable)
    val state: StateFlow<BoardState> = _state

    suspend fun run(): Unit = coroutineScope {
        launch { receive() }
        launch { watchForSilence() }
        // A dropped link is known at once, with no wait for the silence.
        launch { link.connected.collect { up -> if (!up) _state.value = BoardState.Unreachable } }
        var cycleStart = now()
        while (true) {
            ask<ValuesSetup>(ValuesSetupRequest)
            ask<DecodedAdc>(DecodedAdcRequest)
            // A cycle that overran is followed at once by the next.
            cycleStart = maxOf(cycleStart + CYCLE_MS, now())
            delay(cycleStart - now())
        }
    }

    // A frame can span several reads, so the decoder buffers.
    private suspend fun receive() {
        val decoder = FrameDecoder()
        link.received.collect { bytes ->
            for (payload in decoder.push(bytes)) decodeMessage(payload)?.let { heard(it) }
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
        replies.send(reply)
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
        while (replies.tryReceive().isSuccess) Unit
        link.send(encodeMessage(request))
        return withTimeoutOrNull(REPLY_TIMEOUT_MS) {
            var reply: Reply? = null
            while (reply == null) reply = replies.receive() as? Reply
            reply
        }
    }
}
