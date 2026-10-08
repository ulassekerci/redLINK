package org.metucet.redlink.board

import java.nio.ByteBuffer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.metucet.redlink.protocol.COMM_FW_VERSION
import org.metucet.redlink.protocol.COMM_GET_DECODED_ADC
import org.metucet.redlink.protocol.COMM_GET_VALUES_SETUP
import org.metucet.redlink.protocol.DecodedAdc
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.ValuesSetup
import org.metucet.redlink.protocol.encodeFrame

// The board link's second implementation: a stand-in for the board that
// answers the bridge's three requests with the lap's values. The clock gives
// the milliseconds since the run started, the only thing a reply depends on.
// While `answering` gives false it answers nothing.
class SimulatedBoard(private val elapsedMs: () -> Long, private val answering: () -> Boolean) : BoardLink {
    // Nothing carries this link, so it is never down.
    override val connected = MutableStateFlow(true)

    private val replies = Channel<ByteArray>(Channel.UNLIMITED)
    override val received = replies.receiveAsFlow()

    private val decoder = FrameDecoder()

    override fun send(request: ByteArray) {
        for (payload in decoder.push(request)) {
            if (!answering()) continue
            val reply = when (payload.singleOrNull()?.toInt()) {
                COMM_GET_VALUES_SETUP -> valuesSetupReply(lapValues(elapsedMs()))
                COMM_GET_DECODED_ADC -> decodedAdcReply(lapAdc(elapsedMs()))
                COMM_FW_VERSION -> fwVersionReply
                else -> continue
            }
            replies.trySend(encodeFrame(reply))
        }
    }

    override fun close() {
        replies.close()
    }
}

// The replies as the board's firmware lays them out, in protocol/README.md's
// "Board commands". The protocol module only reads them, so writing them is
// here.

private fun Double.scaled(scale: Double) = Math.round(this * scale)

private fun valuesSetupReply(values: ValuesSetup): ByteArray = with(values) {
    ByteBuffer.allocate(70)
        .put(COMM_GET_VALUES_SETUP.toByte())
        .putShort(mosfetTempC!!.scaled(10.0).toShort())
        .putShort(motorTempC!!.scaled(10.0).toShort())
        .putInt(motorCurrentA!!.scaled(100.0).toInt())
        .putInt(batteryCurrentA!!.scaled(100.0).toInt())
        .putShort(dutyCycle!!.scaled(1000.0).toShort())
        .putInt(erpm!!)
        .putInt(speedMS!!.scaled(1000.0).toInt())
        .putShort(batteryVoltageV!!.scaled(10.0).toShort())
        .putShort(batteryLevel!!.scaled(1000.0).toShort())
        .putInt(chargeUsedAh!!.scaled(10000.0).toInt())
        .putInt(chargeChargedAh!!.scaled(10000.0).toInt())
        .putInt(energyUsedWh!!.scaled(10000.0).toInt())
        .putInt(energyChargedWh!!.scaled(10000.0).toInt())
        .putInt(distanceM!!.scaled(1000.0).toInt())
        .putInt(distanceAbsM!!.scaled(1000.0).toInt())
        .putInt(position!!.scaled(1000000.0).toInt())
        .put(faultCode!!.toByte())
        .put(boardId!!.toByte())
        .put(boardCount!!.toByte())
        .putInt(batteryCapacityWh!!.scaled(1000.0).toInt())
        .putInt(odometerM!!.toInt())
        .putInt(boardUptimeMs!!.toInt())
        .array()
}

private fun decodedAdcReply(adc: DecodedAdc): ByteArray = ByteBuffer.allocate(17)
    .put(COMM_GET_DECODED_ADC.toByte())
    .putInt(adc.adcLevel1.scaled(1e6).toInt())
    .putInt(adc.adcVoltage1.scaled(1e6).toInt())
    .putInt(adc.adcLevel2.scaled(1e6).toInt())
    .putInt(adc.adcVoltage2.scaled(1e6).toInt())
    .array()

// Firmware 6.06. A real board's reply goes on with its hardware name and ID,
// which the bridge does not read.
private val fwVersionReply = byteArrayOf(COMM_FW_VERSION.toByte(), 6, 6)
