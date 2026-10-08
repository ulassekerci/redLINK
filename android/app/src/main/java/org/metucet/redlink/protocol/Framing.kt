package org.metucet.redlink.protocol

private const val SHORT_START = 0x02
private const val LONG_START = 0x03
private const val STOP = 0x03

// The largest payload the board's firmware handles. Capping the long form here
// bounds how long a decoder waits after landing on a stray long start byte.
private const val MAX_PAYLOAD = 512

fun encodeFrame(payload: ByteArray): ByteArray {
    require(payload.size in 1..MAX_PAYLOAD) { "Bad payload length: ${payload.size}" }
    val header = if (payload.size > 0xff) {
        byteArrayOf(LONG_START.toByte(), (payload.size shr 8).toByte(), payload.size.toByte())
    } else {
        byteArrayOf(SHORT_START.toByte(), payload.size.toByte())
    }
    val crc = crc16(payload)
    return header + payload + byteArrayOf((crc shr 8).toByte(), crc.toByte(), STOP.toByte())
}

// Turns a byte stream into frame payloads. A read may hold part of a frame or
// several frames, so bytes are buffered until a frame is complete. On a bad
// start byte, length, CRC or stop byte the decoder skips one byte and scans on.
// The stop byte is also the long start byte, so a skip can land on what reads
// as a long frame; the decoder then waits for at most MAX_PAYLOAD bytes.
class FrameDecoder {
    private var buffer = ByteArray(0)

    fun push(bytes: ByteArray): List<ByteArray> {
        val buffer = this.buffer + bytes

        val payloads = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < buffer.size) {
            when (val result = readFrame(buffer, offset)) {
                Incomplete -> break
                Invalid -> offset += 1
                is Frame -> {
                    payloads += result.payload
                    offset = result.end
                }
            }
        }

        this.buffer = buffer.copyOfRange(offset, buffer.size)
        return payloads
    }
}

private sealed interface ReadResult
private data object Incomplete : ReadResult
private data object Invalid : ReadResult
private class Frame(val payload: ByteArray, val end: Int) : ReadResult

internal fun ByteArray.unsigned(index: Int) = this[index].toInt() and 0xff

private fun readFrame(buffer: ByteArray, offset: Int): ReadResult {
    val length: Int
    val payloadStart: Int
    when (buffer.unsigned(offset)) {
        SHORT_START -> {
            if (buffer.size < offset + 2) return Incomplete
            length = buffer.unsigned(offset + 1)
            payloadStart = offset + 2
        }
        LONG_START -> {
            if (buffer.size < offset + 3) return Incomplete
            length = (buffer.unsigned(offset + 1) shl 8) or buffer.unsigned(offset + 2)
            if (length <= 0xff || length > MAX_PAYLOAD) return Invalid
            payloadStart = offset + 3
        }
        else -> return Invalid
    }
    if (length == 0) return Invalid

    val payloadEnd = payloadStart + length
    val end = payloadEnd + 3
    if (buffer.size < end) return Incomplete
    if (buffer.unsigned(end - 1) != STOP) return Invalid

    val payload = buffer.copyOfRange(payloadStart, payloadEnd)
    val crc = (buffer.unsigned(payloadEnd) shl 8) or buffer.unsigned(payloadEnd + 1)
    if (crc != crc16(payload)) return Invalid
    return Frame(payload, end)
}
