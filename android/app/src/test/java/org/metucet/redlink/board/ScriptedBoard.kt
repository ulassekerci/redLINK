package org.metucet.redlink.board

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.encodeFrame

const val VALUES_SETUP = 47
const val DECODED_ADC = 32

// A full setup reply with every value zero but the fault code.
fun valuesSetupReply(faultCode: Int = 0) = ByteArray(70).also {
    it[0] = VALUES_SETUP.toByte()
    it[55] = faultCode.toByte()
}

val decodedAdcReply = ByteArray(17).also { it[0] = DECODED_ADC.toByte() }

// A board on the far side of the seam. It notes the command ID of every
// request with the time it came, and answers as the test tells it to.
class ScriptedBoard(private val now: () -> Long) : BoardLink {
    class Request(val at: Long, val command: Int) {
        override fun toString() = "$command at $at ms"
    }

    override val connected = MutableStateFlow(true)

    private val bytes = Channel<ByteArray>(Channel.UNLIMITED)
    override val received = bytes.receiveAsFlow()

    private val decoder = FrameDecoder()
    val requests = mutableListOf<Request>()

    // What the board does with a request. It answers both commands at once
    // until a test says otherwise.
    var answer: (command: Int) -> Unit = { command ->
        when (command) {
            VALUES_SETUP -> reply(valuesSetupReply())
            DECODED_ADC -> reply(decodedAdcReply)
        }
    }

    override fun send(request: ByteArray) {
        for (payload in decoder.push(request)) {
            val command = payload[0].toInt()
            requests += Request(now(), command)
            answer(command)
        }
    }

    fun reply(payload: ByteArray) = write(encodeFrame(payload))

    fun write(bytes: ByteArray) {
        this.bytes.trySend(bytes)
    }

    override fun close() {}
}
