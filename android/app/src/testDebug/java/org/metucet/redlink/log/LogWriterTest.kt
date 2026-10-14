package org.metucet.redlink.log

import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.board.BoardLink
import org.metucet.redlink.board.BoardPoller
import org.metucet.redlink.board.ScriptedBoard
import org.metucet.redlink.board.SimulatedBoard
import org.metucet.redlink.board.VALUES_SETUP
import org.metucet.redlink.board.valuesSetupReply
import org.metucet.redlink.location.Fix

// The log writer fed by a poller on a board, with the test's clock as both
// the monotonic one and, shifted to a fixed date, the wall clock.
@OptIn(ExperimentalCoroutinesApi::class)
class LogWriterTest {
    // A file in memory. While `failing` it cannot be read or written.
    private class FakeLogFile(var content: ByteArray = ByteArray(0)) : LogFile {
        var failing = false

        override fun size(): Long {
            if (failing) throw IOException("storage full")
            return content.size.toLong()
        }

        override fun append(bytes: ByteArray): Long {
            if (failing) throw IOException("storage full")
            content += bytes
            return content.size.toLong()
        }

        fun text() = content.toString(Charsets.UTF_8)
    }

    private class Logging(val writer: LogWriter, val fixes: MutableSharedFlow<Fix>, val job: Job)

    // Starts a run at the test's time 0, or resumes one that started earlier,
    // on this board, writing to this file.
    private fun TestScope.logging(board: BoardLink, file: FakeLogFile, startedAt: Long = 0): Logging {
        val poller = BoardPoller(board) { currentTime }
        val writer = LogWriter(file, startedAt, now = { currentTime }, wallClock = { START_UTC + currentTime })
        val fixes = MutableSharedFlow<Fix>(extraBufferCapacity = 64)
        backgroundScope.launch { poller.run() }
        val job = backgroundScope.launch { writer.run(poller.cycles, fixes, poller.state) }
        return Logging(writer, fixes, job)
    }

    // Lets the clock reach a time, and everything due at it happen.
    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - currentTime)
        runCurrent()
    }

    // Stop: the writer ends and saves what it holds.
    private fun TestScope.stop(logging: Logging) {
        logging.job.cancel()
        runCurrent()
    }

    private fun expected(name: String) =
        checkNotNull(javaClass.getResource(name)) { "no $name" }.readText(Charsets.UTF_8)

    @Test
    fun `a second of the simulated board with a fixed fix`() = runTest {
        val file = FakeLogFile()
        val logging = logging(SimulatedBoard({ currentTime }, { true }), file)

        // Taken at 400 ms, reaching the phone at 520 ms, with no heading.
        runUntil(520)
        logging.fixes.emit(fix(at = 520))
        runUntil(1000)
        stop(logging)

        assertEquals(expected("simulated-second.csv"), file.text())
    }

    private fun FakeLogFile.lines() = text().lines().dropLast(1)

    private fun FakeLogFile.rows() = lines().drop(1).map { it.split(",") }

    @Test
    fun `a missing reply leaves its cells empty`() = runTest {
        val file = FakeLogFile()
        val board = ScriptedBoard { currentTime }
        board.answer = { command -> if (command == VALUES_SETUP) board.reply(valuesSetupReply()) }
        val logging = logging(board, file)

        // Each cycle waits 250 ms for the ADC reply that never comes.
        runUntil(500)
        stop(logging)

        val rows = file.rows()
        assertEquals(listOf("0.250", "0.500"), rows.map { it[ELAPSED] })
        for (row in rows) {
            assertEquals("0.000", row[SPEED])
            assertEquals(listOf("", ""), row.subList(ADC_LEVEL1, ADC_LEVEL2 + 1))
        }
    }

    @Test
    fun `while the board is unreachable there is one row per fix with the board cells empty`() = runTest {
        val file = FakeLogFile()
        val board = ScriptedBoard { currentTime }
        board.firmware = null
        board.answer = {}
        val logging = logging(board, file)

        for (second in 1L..3) {
            runUntil(second * 1000)
            logging.fixes.emit(fix(at = second * 1000))
        }
        runCurrent()
        stop(logging)

        val rows = file.rows()
        assertEquals(listOf("1.000", "2.000", "3.000"), rows.map { it[ELAPSED] })
        for (row in rows) {
            assertEquals(List(GPS_FIX_TIME - SPEED) { "" }, row.subList(SPEED, GPS_FIX_TIME))
            assertEquals("39.8875123", row[GPS_LAT])
        }
    }

    @Test
    fun `a resumed run appends to its file without a second line of column names`() = runTest {
        val earlier = expected("simulated-second.csv")
        val file = FakeLogFile(earlier.toByteArray(Charsets.UTF_8))

        // Android killed the run a minute in; it comes back at the test's 0.
        val logging = logging(SimulatedBoard({ currentTime + 60_000 }, { true }), file, startedAt = -60_000)
        runUntil(100)
        stop(logging)

        val text = file.text()
        assertEquals(earlier, text.take(earlier.length))
        assertEquals(1, file.lines().count { it.startsWith("time_utc,") })
        assertEquals(listOf("1.000", "60.000", "60.050", "60.100"), file.rows().drop(20).map { it[ELAPSED] })
    }

    @Test
    fun `rows reach storage once a second`() = runTest {
        val file = FakeLogFile()
        val logging = logging(SimulatedBoard({ currentTime }, { true }), file)

        // The column names go out as the run starts.
        runUntil(999)
        assertEquals(1, file.lines().size)
        assertEquals(LogState.Writing(file.content.size.toLong()), logging.writer.state.value)

        runUntil(1001)
        val firstSecond = file.rows().map { it[ELAPSED] }
        assertEquals("0.000", firstSecond.first())
        assertEquals(true, firstSecond.size in 20..21)

        runUntil(1999)
        assertEquals(firstSecond, file.rows().map { it[ELAPSED] })
        runUntil(2001)
        assertEquals(true, file.rows().size in 40..41)
        assertEquals(LogState.Writing(file.content.size.toLong()), logging.writer.state.value)
    }

    @Test
    fun `a file that cannot be written loses those rows and is tried again every second`() = runTest {
        val file = FakeLogFile()
        file.failing = true
        val logging = logging(SimulatedBoard({ currentTime }, { true }), file)

        runUntil(500)
        assertEquals(LogState.NotWriting, logging.writer.state.value)

        // Storage comes back after the try at 1 s failed.
        runUntil(1500)
        assertEquals(LogState.NotWriting, logging.writer.state.value)
        file.failing = false
        runUntil(2001)

        assertEquals(LogState.Writing(file.content.size.toLong()), logging.writer.state.value)
        assertEquals(1, file.lines().count { it.startsWith("time_utc,") })
        val elapsed = file.rows().map { it[ELAPSED] }
        assertEquals(true, elapsed.first() in listOf("1.000", "1.050"))
        assertEquals(true, elapsed.last() in listOf("1.950", "2.000"))
    }

    @Test
    fun `a log is named for the local time its run started, with SIM_ before a simulated run's`() {
        val start = LocalDateTime.of(2026, 9, 26, 14, 22, 9)
        assertEquals("redLINK_2026-09-26_14-22-09.csv", logFileName(start, simulated = false))
        assertEquals("SIM_redLINK_2026-09-26_14-22-09.csv", logFileName(start, simulated = true))
    }

    private companion object {
        const val ELAPSED = 1
        const val SPEED = 2
        const val ADC_LEVEL1 = 19
        const val ADC_LEVEL2 = 20
        const val GPS_FIX_TIME = 23
        const val GPS_LAT = 24

        val START_UTC = Instant.parse("2026-09-26T11:22:09.000Z").toEpochMilli()

        fun fix(at: Long) = Fix(
            latDeg = 39.8875123,
            lonDeg = 32.7811456,
            altM = 912.35,
            speedMS = 7.95,
            headingDeg = null,
            accuracyM = 3.8,
            timeUtc = START_UTC + at - 120,
            at = at,
        )
    }
}
