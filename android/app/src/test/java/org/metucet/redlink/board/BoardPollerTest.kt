package org.metucet.redlink.board

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.protocol.encodeFrame

@OptIn(ExperimentalCoroutinesApi::class)
class BoardPollerTest {
    private class Polling(val board: ScriptedBoard, val poller: BoardPoller)

    // Starts a poller on a scripted board, both on the test's clock.
    private fun TestScope.polling(): Polling {
        val board = ScriptedBoard { currentTime }
        val poller = BoardPoller(board) { currentTime }
        backgroundScope.launch { poller.run() }
        return Polling(board, poller)
    }

    // Lets the clock reach a time, and everything due at it happen.
    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - currentTime)
        runCurrent()
    }

    private fun ScriptedBoard.asked() = requests.map { it.at to it.command }

    @Test
    fun `asks for setup values then decoded ADC every 50 ms`() = runTest {
        val polling = polling()

        runUntil(100)

        assertEquals(
            listOf(
                0L to VALUES_SETUP, 0L to DECODED_ADC,
                50L to VALUES_SETUP, 50L to DECODED_ADC,
                100L to VALUES_SETUP, 100L to DECODED_ADC,
            ),
            polling.board.asked(),
        )
    }

    @Test
    fun `waits for the setup reply before asking for decoded ADC`() = runTest {
        val polling = polling()
        polling.board.answer = { command ->
            backgroundScope.launch {
                delay(20)
                polling.board.reply(if (command == VALUES_SETUP) valuesSetupReply() else decodedAdcReply)
            }
        }

        runUntil(50)

        assertEquals(
            listOf(0L to VALUES_SETUP, 20L to DECODED_ADC, 50L to VALUES_SETUP),
            polling.board.asked(),
        )
    }

    @Test
    fun `gives up on a command after 250 ms and sends the next`() = runTest {
        val polling = polling()
        polling.board.answer = { command -> if (command == DECODED_ADC) polling.board.reply(decodedAdcReply) }

        runUntil(500)

        assertEquals(
            listOf(
                0L to VALUES_SETUP, 250L to DECODED_ADC,
                250L to VALUES_SETUP, 500L to DECODED_ADC,
                500L to VALUES_SETUP,
            ),
            polling.board.asked(),
        )
    }

    @Test
    fun `the board is unreachable until it answers`() = runTest {
        val polling = polling()
        polling.board.answer = {}

        runUntil(100)
        assertEquals(BoardState.Unreachable, polling.poller.state.value)

        polling.board.reply(decodedAdcReply)
        runCurrent()
        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
    }

    @Test
    fun `after 2 s with no reply the board is unreachable and polling goes on`() = runTest {
        val polling = polling()
        runUntil(1000)
        polling.board.answer = {}

        // The last reply came at 1000 ms, to the cycle that began then.
        runUntil(2999)
        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
        runUntil(3000)
        assertEquals(BoardState.Unreachable, polling.poller.state.value)

        // Unanswered since the cycle of 1050 ms, a request has gone out every
        // 250 ms, and still does.
        polling.board.requests.clear()
        runUntil(3500)
        assertEquals(listOf(3050L to VALUES_SETUP, 3300L to DECODED_ADC), polling.board.asked())
    }

    @Test
    fun `a board that answers again is answering again`() = runTest {
        val polling = polling()
        polling.board.answer = {}
        runUntil(5000)
        assertEquals(BoardState.Unreachable, polling.poller.state.value)

        polling.board.answer = { polling.board.reply(decodedAdcReply) }
        runUntil(5250)

        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
    }

    @Test
    fun `the fault code is the latest setup reply's`() = runTest {
        val polling = polling()
        var faultCode = 5
        polling.board.answer = { command ->
            polling.board.reply(if (command == VALUES_SETUP) valuesSetupReply(faultCode) else decodedAdcReply)
        }

        runUntil(100)
        assertEquals(BoardState.Answering(faultCode = 5), polling.poller.state.value)

        faultCode = 0
        runUntil(200)
        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
    }

    @Test
    fun `a fault from before the board went quiet is not shown when it answers again`() = runTest {
        val polling = polling()
        polling.board.answer = { command ->
            polling.board.reply(if (command == VALUES_SETUP) valuesSetupReply(faultCode = 5) else decodedAdcReply)
        }
        runUntil(100)
        polling.board.answer = {}
        runUntil(5000)

        polling.board.answer = { polling.board.reply(decodedAdcReply) }
        runUntil(5250)

        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
    }

    @Test
    fun `a reply that came before a request was sent is not its reply`() = runTest {
        val polling = polling()
        polling.board.answer = { command ->
            backgroundScope.launch {
                delay(10)
                polling.board.reply(if (command == VALUES_SETUP) valuesSetupReply() else decodedAdcReply)
            }
        }

        // A second setup reply, in the wait between two cycles.
        runUntil(30)
        polling.board.reply(valuesSetupReply())
        runUntil(60)

        assertEquals(
            listOf(0L to VALUES_SETUP, 10L to DECODED_ADC, 50L to VALUES_SETUP, 60L to DECODED_ADC),
            polling.board.asked(),
        )
    }

    @Test
    fun `a link that drops makes the board unreachable at once`() = runTest {
        val polling = polling()
        runUntil(100)

        polling.board.connected.value = false
        runCurrent()

        assertEquals(BoardState.Unreachable, polling.poller.state.value)
    }

    @Test
    fun `a reply that comes in pieces is one reply`() = runTest {
        val polling = polling()
        val reply = encodeFrame(valuesSetupReply(faultCode = 5))
        polling.board.answer = { command ->
            if (command == VALUES_SETUP) {
                polling.board.write(reply.copyOfRange(0, 20))
                polling.board.write(reply.copyOfRange(20, reply.size))
            }
        }

        runUntil(0)

        assertEquals(BoardState.Answering(faultCode = 5), polling.poller.state.value)
        assertEquals(listOf(0L to VALUES_SETUP, 0L to DECODED_ADC), polling.board.asked())
    }

    @Test
    fun `a board that always answers late is still answering`() = runTest {
        val polling = polling()
        polling.board.answer = { command ->
            backgroundScope.launch {
                delay(300)
                polling.board.reply(if (command == VALUES_SETUP) valuesSetupReply() else decodedAdcReply)
            }
        }

        runUntil(5000)

        assertEquals(BoardState.Answering(faultCode = 0), polling.poller.state.value)
    }
}
