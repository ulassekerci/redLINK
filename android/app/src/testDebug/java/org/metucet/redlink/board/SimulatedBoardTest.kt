package org.metucet.redlink.board

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.metucet.redlink.protocol.DecodedAdc
import org.metucet.redlink.protocol.DecodedAdcRequest
import org.metucet.redlink.protocol.DecodedMessage
import org.metucet.redlink.protocol.EncodableMessage
import org.metucet.redlink.protocol.FrameDecoder
import org.metucet.redlink.protocol.FwVersion
import org.metucet.redlink.protocol.FwVersionRequest
import org.metucet.redlink.protocol.ValuesSetup
import org.metucet.redlink.protocol.ValuesSetupRequest
import org.metucet.redlink.protocol.decodeMessage
import org.metucet.redlink.protocol.encodeFrame
import org.metucet.redlink.protocol.encodeMessage
import org.metucet.redlink.protocol.toHex

// The simulated board through the board link's seam: a request goes in as a
// frame and the reply is read back as bytes.
class SimulatedBoardTest {
    // A simulated board with the test's hand on its clock and its switch.
    private class Bench {
        var elapsedMs = 0L
        var answering = true
        val board = SimulatedBoard({ elapsedMs }, { answering })

        // The reply's frame, or null when the board answers nothing.
        suspend fun frame(request: ByteArray, atMs: Long = elapsedMs): ByteArray? {
            elapsedMs = atMs
            board.send(request)
            return withTimeoutOrNull(1000) { board.received.first() }
        }

        suspend fun ask(request: EncodableMessage, atMs: Long = elapsedMs): DecodedMessage? =
            frame(encodeMessage(request), atMs)?.let { decodeMessage(FrameDecoder().push(it).single()) }

        suspend fun values(atMs: Long) = ask(ValuesSetupRequest, atMs) as ValuesSetup

        suspend fun adc(atMs: Long) = ask(DecodedAdcRequest, atMs) as DecodedAdc

        // The setup replies a bridge polling at 20 Hz would get.
        suspend fun polled(fromMs: Long, toMs: Long) = (fromMs until toMs step POLL_MS).map { values(it) }
    }

    private fun assertNeverFalls(name: String, values: List<Double>) =
        values.zipWithNext().forEachIndexed { index, (before, after) ->
            assertTrue("$name fell from $before to $after at sample ${index + 1}", after >= before)
        }

    @Test
    fun `answers a setup request with a full reply`() = runTest {
        val bench = Bench()

        // 75 bytes on the wire: a 70-byte payload in a short frame.
        assertEquals(75, bench.frame(encodeMessage(ValuesSetupRequest))!!.size)
        val values = bench.values(atMs = 30_000)
        assertEquals(1, values.boardCount)
        assertEquals(30_000L, values.boardUptimeMs)
    }

    @Test
    fun `answers a decoded ADC request`() = runTest {
        assertEquals(22, Bench().frame(encodeMessage(DecodedAdcRequest))!!.size)
    }

    @Test
    fun `answers a firmware version request as firmware 6_06`() = runTest {
        assertEquals(FwVersion(fwMajor = 6, fwMinor = 6), Bench().ask(FwVersionRequest))
    }

    @Test
    fun `answers nothing to any other command`() = runTest {
        // COMM_GET_VALUES, which the bridge never sends.
        assertEquals(null, Bench().frame(encodeFrame(byteArrayOf(4))))
    }

    @Test
    fun `answers nothing while told to stop answering`() = runTest {
        val bench = Bench()

        bench.answering = false
        assertEquals(null, bench.ask(ValuesSetupRequest))
        assertEquals(null, bench.ask(DecodedAdcRequest))
        assertEquals(null, bench.ask(FwVersionRequest))

        bench.answering = true
        assertEquals(FwVersion(fwMajor = 6, fwMinor = 6), bench.ask(FwVersionRequest))
    }

    @Test
    fun `a lap accelerates, cruises and coasts to a stop`() = runTest {
        val speeds = Bench().polled(0, LAP_MS).map { it.speedMS!! }

        val cruise = speeds.max()
        val cruiseFrom = speeds.indexOf(cruise)
        val cruiseTo = speeds.lastIndexOf(cruise)
        assertTrue("the car moves", cruise > 0)
        assertEquals(0.0, speeds.first(), 0.0)
        assertNeverFalls("speed while accelerating", speeds.take(cruiseFrom + 1))
        assertTrue("cruises for a good part of the lap", (cruiseTo - cruiseFrom) * POLL_MS > LAP_MS / 4)
        assertTrue("every sample between is at cruising speed", speeds.subList(cruiseFrom, cruiseTo).all { it == cruise })
        assertNeverFalls("slowness while coasting", speeds.drop(cruiseTo).map { -it })
        assertEquals(0.0, speeds.last(), 0.0)
    }

    @Test
    fun `the lap repeats`() = runTest {
        val bench = Bench()
        val live = { v: ValuesSetup ->
            listOf(v.speedMS, v.motorCurrentA, v.batteryCurrentA, v.batteryVoltageV, v.dutyCycle, v.faultCode)
        }

        for (ms in 0 until LAP_MS step 1000) {
            assertEquals("at $ms ms", live(bench.values(ms)), live(bench.values(ms + 3 * LAP_MS)))
        }
    }

    @Test
    fun `distance, odometer, charge and energy only grow across laps`() = runTest {
        val polled = Bench().polled(0, 3 * LAP_MS)

        assertNeverFalls("distance", polled.map { it.distanceM!! })
        assertNeverFalls("absolute distance", polled.map { it.distanceAbsM!! })
        assertNeverFalls("odometer", polled.map { it.odometerM!!.toDouble() })
        assertNeverFalls("charge used", polled.map { it.chargeUsedAh!! })
        assertNeverFalls("energy used", polled.map { it.energyUsedWh!! })
        assertNeverFalls("battery drain", polled.map { -it.batteryLevel!! })
        assertTrue(polled.last().batteryLevel!! < polled.first().batteryLevel!!)
    }

    // What a viewer would get by adding up rate x time over the polled
    // replies, against what the board itself says it has accumulated.
    private fun summed(polled: List<ValuesSetup>, rate: (ValuesSetup) -> Double) =
        polled.zipWithNext().sumOf { (a, b) -> (rate(a) + rate(b)) / 2 * (POLL_MS / 1000.0) }

    @Test
    fun `distance agrees with speed`() = runTest {
        val polled = Bench().polled(0, 3 * LAP_MS)

        val travelled = polled.last().distanceM!! - polled.first().distanceM!!
        assertTrue("the car goes somewhere", travelled > 1000)
        assertEquals(summed(polled) { it.speedMS!! }, travelled, travelled * 0.001)
        assertEquals(travelled, polled.last().odometerM!! - polled.first().odometerM!!.toDouble(), 1.0)
    }

    @Test
    fun `charge and watt-hours agree with current and voltage`() = runTest {
        val polled = Bench().polled(0, 3 * LAP_MS)

        val ampHours = polled.last().chargeUsedAh!! - polled.first().chargeUsedAh!!
        val wattHours = polled.last().energyUsedWh!! - polled.first().energyUsedWh!!
        assertTrue("the car uses energy", wattHours > 1)
        assertEquals(summed(polled) { it.batteryCurrentA!! } / 3600, ampHours, ampHours * 0.005)
        assertEquals(summed(polled) { it.batteryVoltageV!! * it.batteryCurrentA!! } / 3600, wattHours, wattHours * 0.005)
    }

    @Test
    fun `the battery voltage sags as current is drawn`() = runTest {
        val byCurrent = Bench().polled(0, LAP_MS).sortedBy { it.batteryCurrentA }

        assertNeverFalls("sag with current", byCurrent.map { -it.batteryVoltageV!! })
        assertTrue(byCurrent.first().batteryVoltageV!! - byCurrent.last().batteryVoltageV!! > 0.5)
    }

    @Test
    fun `ADC level 1 follows the throttle`() = runTest {
        val bench = Bench()
        val throttle = (0 until LAP_MS step 1000).map { bench.values(it).motorCurrentA!! to bench.adc(it).adcLevel1 }

        assertTrue(throttle.all { (_, level) -> level in 0.0..1.0 })
        assertTrue("no throttle with no motor current", throttle.all { (current, level) -> (current == 0.0) == (level == 0.0) })
        assertNeverFalls("throttle with motor current", throttle.sortedBy { it.first }.map { it.second })
        assertEquals(1.0, throttle.maxOf { it.second }, 0.0)
    }

    @Test
    fun `one fault is raised for a few seconds once per lap`() = runTest {
        val bench = Bench()

        for (lap in 0L..2) {
            val faults = bench.polled(lap * LAP_MS, (lap + 1) * LAP_MS).map { it.faultCode!! }
            val raised = faults.indices.filter { faults[it] != 0 }
            assertEquals("one fault code in lap $lap", 1, raised.map { faults[it] }.distinct().size)
            assertEquals("raised once in lap $lap", raised.size - 1, raised.last() - raised.first())
            assertTrue("for a few seconds in lap $lap", raised.size * POLL_MS in 2000..10_000)
        }
    }

    @Test
    fun `the same time since the run started always gives the same reply`() = runTest {
        val first = Bench()
        val second = Bench()
        val setup = encodeMessage(ValuesSetupRequest)
        val adc = encodeMessage(DecodedAdcRequest)
        val times = listOf(0L, 7_350, 61_200, 119_950, 3_600_000)

        val early = times.map { first.frame(setup, it)!!.toHex() to first.frame(adc, it)!!.toHex() }
        // Asked later, in another order, of a board that has answered other things.
        val late = times.reversed().map { second.frame(adc, it)!!.toHex() to second.frame(setup, it)!!.toHex() }

        assertEquals(early, late.reversed().map { (adcFrame, setupFrame) -> setupFrame to adcFrame })
        assertEquals(early, times.map { first.frame(setup, it)!!.toHex() to first.frame(adc, it)!!.toHex() })
    }

    private companion object {
        const val POLL_MS = 50L

        // About 2 minutes.
        const val LAP_MS = 120_000L
    }
}
