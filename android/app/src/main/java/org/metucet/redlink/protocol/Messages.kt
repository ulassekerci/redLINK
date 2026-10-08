package org.metucet.redlink.protocol

import java.nio.ByteBuffer

private const val COMM_FW_VERSION = 0
private const val COMM_GET_DECODED_ADC = 32
private const val COMM_CUSTOM_APP_DATA = 36
private const val COMM_GET_VALUES_SETUP = 47

private const val GPS = 1
private const val LOBBY_REQUEST = 2
private const val HEARTBEAT = 3
private const val STATUS = 4

// A message a frame's payload decodes to.
sealed interface DecodedMessage

// A message this module can put on the wire.
sealed interface EncodableMessage

data object FwVersionRequest : EncodableMessage

data object ValuesSetupRequest : EncodableMessage

data object DecodedAdcRequest : EncodableMessage

// Property names are the log of record's column names, and the names
// protocol/vectors.json uses, in camel case.

// Every field of a COMM_GET_VALUES_SETUP reply. A short reply is missing its
// tail, so a field is null when the reply did not reach it.
data class ValuesSetup(
    val mosfetTempC: Double?,
    val motorTempC: Double?,
    val motorCurrentA: Double?,
    val batteryCurrentA: Double?,
    val dutyCycle: Double?,
    val erpm: Int?,
    val speedMS: Double?,
    val batteryVoltageV: Double?,
    val batteryLevel: Double?,
    val chargeUsedAh: Double?,
    val chargeChargedAh: Double?,
    val energyUsedWh: Double?,
    val energyChargedWh: Double?,
    val distanceM: Double?,
    val distanceAbsM: Double?,
    val position: Double?,
    val faultCode: Int?,
    val boardId: Int?,
    val boardCount: Int?,
    val batteryCapacityWh: Double?,
    val odometerM: Long?,
    val boardUptimeMs: Long?,
) : DecodedMessage

data class DecodedAdc(
    val adcLevel1: Double,
    val adcVoltage1: Double,
    val adcLevel2: Double,
    val adcVoltage2: Double,
) : DecodedMessage

data class FwVersion(val fwMajor: Int, val fwMinor: Int) : DecodedMessage

// A field the phone's fix lacks is sent as 0. The fix time is Unix milliseconds.
data class Gps(
    val gpsLatDeg: Double,
    val gpsLonDeg: Double,
    val gpsAltM: Double,
    val gpsSpeedMS: Double,
    val gpsHeadingDeg: Double,
    val gpsAccuracyM: Double,
    val gpsFixTimeUtc: Long,
) : DecodedMessage, EncodableMessage

data class LobbyRequest(val token: String) : DecodedMessage, EncodableMessage

data object Heartbeat : DecodedMessage, EncodableMessage

// boardState: 0 answering, 1 unreachable.
data class Status(val protocolVersion: Int, val boardState: Int) : DecodedMessage, EncodableMessage

// Decodes a frame's payload. Returns null for anything this protocol does not
// handle: an unknown command ID, or a reply too short to carry its fields.
// Bytes after the last field read are ignored.
fun decodeMessage(payload: ByteArray): DecodedMessage? = when (payload.firstOrNull()?.toInt()) {
    COMM_GET_VALUES_SETUP -> decodeValuesSetup(payload)
    COMM_GET_DECODED_ADC -> decodeDecodedAdc(payload)
    COMM_FW_VERSION -> decodeFwVersion(payload)
    COMM_CUSTOM_APP_DATA -> decodeCustomAppData(payload)
    else -> null
}

// Reads a reply's big-endian fields in order, from after the command ID. Once
// a field does not fit in what is left, it and every field after it read as
// null.
private class FieldReader(payload: ByteArray) {
    private val buffer = ByteBuffer.wrap(payload, 1, payload.size - 1)
    private var ended = false

    private fun <T> read(size: Int, field: ByteBuffer.() -> T): T? {
        if (buffer.remaining() < size) ended = true
        return if (ended) null else buffer.field()
    }

    fun i8() = read(1) { get().toInt() }
    fun u8() = read(1) { get().toInt() and 0xff }
    fun i16() = read(2) { short.toInt() }
    fun i32() = read(4) { int }
    fun u32() = read(4) { int.toLong() and 0xffffffffL }
}

private fun decodeValuesSetup(payload: ByteArray): ValuesSetup {
    val reply = FieldReader(payload)
    return ValuesSetup(
        mosfetTempC = reply.i16()?.div(10.0),
        motorTempC = reply.i16()?.div(10.0),
        motorCurrentA = reply.i32()?.div(100.0),
        batteryCurrentA = reply.i32()?.div(100.0),
        dutyCycle = reply.i16()?.div(1000.0),
        erpm = reply.i32(),
        speedMS = reply.i32()?.div(1000.0),
        batteryVoltageV = reply.i16()?.div(10.0),
        batteryLevel = reply.i16()?.div(1000.0),
        chargeUsedAh = reply.i32()?.div(10000.0),
        chargeChargedAh = reply.i32()?.div(10000.0),
        energyUsedWh = reply.i32()?.div(10000.0),
        energyChargedWh = reply.i32()?.div(10000.0),
        distanceM = reply.i32()?.div(1000.0),
        distanceAbsM = reply.i32()?.div(1000.0),
        position = reply.i32()?.div(1000000.0),
        faultCode = reply.i8(),
        boardId = reply.u8(),
        boardCount = reply.u8(),
        batteryCapacityWh = reply.i32()?.div(1000.0),
        odometerM = reply.u32(),
        boardUptimeMs = reply.u32(),
    )
}

private fun decodeDecodedAdc(payload: ByteArray): DecodedAdc? {
    if (payload.size < 17) return null
    val reply = ByteBuffer.wrap(payload)
    return DecodedAdc(
        adcLevel1 = reply.getInt(1) / 1e6,
        adcVoltage1 = reply.getInt(5) / 1e6,
        adcLevel2 = reply.getInt(9) / 1e6,
        adcVoltage2 = reply.getInt(13) / 1e6,
    )
}

private fun decodeFwVersion(payload: ByteArray): FwVersion? {
    if (payload.size < 3) return null
    return FwVersion(fwMajor = payload.unsigned(1), fwMinor = payload.unsigned(2))
}

private fun ByteBuffer.u16(index: Int) = getShort(index).toInt() and 0xffff

// Our messages: [36, type, ...content].
private fun decodeCustomAppData(payload: ByteArray): DecodedMessage? {
    if (payload.size < 2) return null
    val content = payload.size - 2
    val message = ByteBuffer.wrap(payload)
    return when (payload.unsigned(1)) {
        GPS -> {
            if (content < 26) return null
            Gps(
                gpsLatDeg = message.getInt(2) / 1e7,
                gpsLonDeg = message.getInt(6) / 1e7,
                gpsAltM = message.getInt(10) / 100.0,
                gpsSpeedMS = message.u16(14) / 100.0,
                gpsHeadingDeg = message.u16(16) / 100.0,
                gpsAccuracyM = message.u16(18) / 10.0,
                gpsFixTimeUtc = message.getLong(20),
            )
        }
        LOBBY_REQUEST -> {
            if (content < 8) return null
            // Latin-1 keeps every byte a character of its own, so a byte
            // outside ASCII fails the alphabet check.
            val token = String(payload, 2, 8, Charsets.ISO_8859_1)
            if (isToken(token)) LobbyRequest(token) else null
        }
        HEARTBEAT -> Heartbeat
        STATUS -> {
            if (content < 2) return null
            Status(protocolVersion = payload.unsigned(2), boardState = payload.unsigned(3))
        }
        else -> null
    }
}

// Scales a value to its wire integer, refusing one the field cannot carry
// rather than letting it wrap.
private fun scaled(value: Double, scale: Double, range: LongRange, field: String): Long {
    require(!value.isNaN()) { "$field is not a number" }
    val integer = Math.round(value * scale)
    require(integer in range) { "$field out of range: $value" }
    return integer
}

private val I32 = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
private val U16 = 0L..0xffffL

// A fix whose accuracy is past what its field carries is not a fix: this
// throws for it, and the bridge sends nothing.
private fun encodeGps(message: Gps): ByteArray {
    require(message.gpsFixTimeUtc >= 0) { "gpsFixTimeUtc out of range: ${message.gpsFixTimeUtc}" }
    return ByteBuffer.allocate(28)
        .put(COMM_CUSTOM_APP_DATA.toByte())
        .put(GPS.toByte())
        .putInt(scaled(message.gpsLatDeg, 1e7, I32, "gpsLatDeg").toInt())
        .putInt(scaled(message.gpsLonDeg, 1e7, I32, "gpsLonDeg").toInt())
        .putInt(scaled(message.gpsAltM, 100.0, I32, "gpsAltM").toInt())
        .putShort(scaled(message.gpsSpeedMS, 100.0, U16, "gpsSpeedMS").toShort())
        .putShort(scaled(message.gpsHeadingDeg, 100.0, U16, "gpsHeadingDeg").toShort())
        .putShort(scaled(message.gpsAccuracyM, 10.0, U16, "gpsAccuracyM").toShort())
        .putLong(message.gpsFixTimeUtc)
        .array()
}

private fun encodeStatus(message: Status): ByteArray {
    require(message.protocolVersion in 0..0xff) { "protocolVersion out of range: ${message.protocolVersion}" }
    require(message.boardState in 0..0xff) { "boardState out of range: ${message.boardState}" }
    return byteArrayOf(
        COMM_CUSTOM_APP_DATA.toByte(),
        STATUS.toByte(),
        message.protocolVersion.toByte(),
        message.boardState.toByte(),
    )
}

// Encodes a message as a whole frame, ready to write. Throws
// IllegalArgumentException for a message whose values the wire cannot carry.
fun encodeMessage(message: EncodableMessage): ByteArray = when (message) {
    FwVersionRequest -> encodeFrame(byteArrayOf(COMM_FW_VERSION.toByte()))
    DecodedAdcRequest -> encodeFrame(byteArrayOf(COMM_GET_DECODED_ADC.toByte()))
    ValuesSetupRequest -> encodeFrame(byteArrayOf(COMM_GET_VALUES_SETUP.toByte()))
    is Gps -> encodeFrame(encodeGps(message))
    is LobbyRequest -> {
        require(isToken(message.token)) { "Not a token: ${message.token}" }
        val token = message.token.toByteArray(Charsets.US_ASCII)
        encodeFrame(byteArrayOf(COMM_CUSTOM_APP_DATA.toByte(), LOBBY_REQUEST.toByte()) + token)
    }
    Heartbeat -> encodeFrame(byteArrayOf(COMM_CUSTOM_APP_DATA.toByte(), HEARTBEAT.toByte()))
    is Status -> encodeFrame(encodeStatus(message))
}
