package org.metucet.redlink.protocol

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProtocolVectorsTest(private val name: String, private val case: JsonObject) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = Vectors.frames.map { arrayOf(it.string("name"), it) }
    }

    @Test
    fun passes() {
        when (val check = case.string("check")) {
            "deframe" -> deframes()
            "encode" -> encodes()
            "decode" -> decodes()
            "both" -> {
                encodes()
                decodes()
            }
            else -> fail("Unknown check: $check")
        }
    }

    private fun deframes() {
        val decoder = FrameDecoder()
        val payloads = case.strings("reads").flatMap { decoder.push(it.hexToBytes()) }
        assertEquals(case.strings("payloads"), payloads.map { it.toHex() })
    }

    // A null frame means the encoder refuses the message.
    private fun encodes() {
        val message = message(case.getValue("message").jsonObject) as EncodableMessage
        val frame = case.getValue("frame")
        if (frame is JsonNull) {
            assertThrows(IllegalArgumentException::class.java) { encodeMessage(message) }
        } else {
            assertEquals(frame.jsonPrimitive.content, encodeMessage(message).toHex())
        }
    }

    // A null message means the payload is ignored.
    private fun decodes() {
        val payloads = FrameDecoder().push(case.string("frame").hexToBytes())
        assertEquals(1, payloads.size)
        val message = case.getValue("message")
        assertEquals(if (message is JsonNull) null else message(message.jsonObject), decodeMessage(payloads[0]))
    }
}

// A case's message, remembering which keys were read: a key this suite does
// not know fails the case instead of passing unchecked.
private class Fields(private val json: JsonObject) {
    private val read = mutableSetOf<String>()

    fun string(key: String) = optional(key)!!.content
    fun double(key: String) = optional(key)!!.double
    fun long(key: String) = optional(key)!!.long
    fun int(key: String) = optional(key)!!.int

    // A short reply's message leaves out the keys it did not reach.
    fun optional(key: String) = json[key]?.jsonPrimitive.also { read += key }

    fun unread() = json.keys - read
}

// A case's message as the protocol module's own type.
private fun message(json: JsonObject): Any {
    val fields = Fields(json)
    val message: Any = when (val type = fields.string("type")) {
        "fw_version_request" -> FwVersionRequest
        "values_setup_request" -> ValuesSetupRequest
        "decoded_adc_request" -> DecodedAdcRequest
        "values_setup" -> ValuesSetup(
            mosfetTempC = fields.optional("mosfet_temp_c")?.double,
            motorTempC = fields.optional("motor_temp_c")?.double,
            motorCurrentA = fields.optional("motor_current_a")?.double,
            batteryCurrentA = fields.optional("battery_current_a")?.double,
            dutyCycle = fields.optional("duty_cycle")?.double,
            erpm = fields.optional("erpm")?.int,
            speedMS = fields.optional("speed_m_s")?.double,
            batteryVoltageV = fields.optional("battery_voltage_v")?.double,
            batteryLevel = fields.optional("battery_level")?.double,
            chargeUsedAh = fields.optional("charge_used_ah")?.double,
            chargeChargedAh = fields.optional("charge_charged_ah")?.double,
            energyUsedWh = fields.optional("energy_used_wh")?.double,
            energyChargedWh = fields.optional("energy_charged_wh")?.double,
            distanceM = fields.optional("distance_m")?.double,
            distanceAbsM = fields.optional("distance_abs_m")?.double,
            position = fields.optional("position")?.double,
            faultCode = fields.optional("fault_code")?.int,
            boardId = fields.optional("board_id")?.int,
            boardCount = fields.optional("board_count")?.int,
            batteryCapacityWh = fields.optional("battery_capacity_wh")?.double,
            odometerM = fields.optional("odometer_m")?.long,
            boardUptimeMs = fields.optional("board_uptime_ms")?.long,
        )
        "decoded_adc" -> DecodedAdc(
            adcLevel1 = fields.double("adc_level1"),
            adcVoltage1 = fields.double("adc_voltage1"),
            adcLevel2 = fields.double("adc_level2"),
            adcVoltage2 = fields.double("adc_voltage2"),
        )
        "fw_version" -> FwVersion(fwMajor = fields.int("fw_major"), fwMinor = fields.int("fw_minor"))
        "gps" -> Gps(
            gpsLatDeg = fields.double("gps_lat_deg"),
            gpsLonDeg = fields.double("gps_lon_deg"),
            gpsAltM = fields.double("gps_alt_m"),
            gpsSpeedMS = fields.double("gps_speed_m_s"),
            gpsHeadingDeg = fields.double("gps_heading_deg"),
            gpsAccuracyM = fields.double("gps_accuracy_m"),
            gpsFixTimeUtc = fields.long("gps_fix_time_utc"),
        )
        "lobby_request" -> LobbyRequest(token = fields.string("token"))
        "heartbeat" -> Heartbeat
        "status" -> Status(protocolVersion = fields.int("protocol_version"), boardState = fields.int("board_state"))
        else -> throw AssertionError("Unknown message type: $type")
    }
    assertEquals("keys this suite does not read", emptySet<String>(), fields.unread())
    return message
}
