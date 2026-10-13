package org.metucet.redlink.hub

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.DECODED_ADC
import org.metucet.redlink.board.VALUES_SETUP
import org.metucet.redlink.location.Fix
import org.metucet.redlink.protocol.ALPHABET
import org.metucet.redlink.protocol.Gps
import org.metucet.redlink.protocol.Heartbeat
import org.metucet.redlink.protocol.LobbyRequest
import org.metucet.redlink.protocol.Status
import org.metucet.redlink.protocol.encodeFrame
import org.metucet.redlink.protocol.encodeMessage

private const val CODE = "K7QM3XPC"
private const val LOBBY = "REDLINKK7QM3XPC"
private const val TOKEN = "4HT9WQ2B"
private const val VIEWER = LOBBY + TOKEN
private const val FW_VERSION = 0

// A COMM_CUSTOM_APP_DATA frame of any type and content, well-formed or not.
private fun customAppData(type: Int, content: ByteArray) = encodeFrame(byteArrayOf(36, type.toByte()) + content)

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeLobbyTest {
    private class Bridging(
        val hub: FakeHub,
        val board: MutableStateFlow<BoardState>,
        val replies: MutableSharedFlow<ByteArray>,
        val fixes: MutableSharedFlow<Fix>,
        val bridge: Bridge,
        val running: Job,
    )

    // Starts a bridge on a fake hub, both on the test's clock.
    private fun TestScope.bridging(
        protocolVersion: Int = 1,
        board: BoardState = BoardState.Answering(faultCode = 0),
    ): Bridging {
        val hub = FakeHub { currentTime }
        val boardState = MutableStateFlow(board)
        val replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1000)
        val fixes = MutableSharedFlow<Fix>(extraBufferCapacity = 1000)
        val bridge = Bridge(CODE, protocolVersion, boardState, replies, fixes, hub) { currentTime }
        val running = backgroundScope.launch { bridge.run() }
        return Bridging(hub, boardState, replies, fixes, bridge, running)
    }

    // Lets the clock reach a time, and everything due at it happen.
    private fun TestScope.runUntil(ms: Long) {
        advanceTimeBy(ms - currentTime)
        runCurrent()
    }

    @Test
    fun `registers the lobby and confirms it with PING`() = runTest {
        val bridging = bridging()

        runUntil(1000)

        assertEquals(listOf(LOBBY), bridging.hub.registrations())
        assertEquals(listOf(LOBBY), bridging.hub.pings())
        assertEquals(BridgeState(HubState.Connected, viewers = 0), bridging.bridge.state.value)
    }

    @Test
    fun `registers the lobby again when PING answers NULL`() = runTest {
        val bridging = bridging()
        bridging.hub.loseLogins = 1

        runUntil(1000)

        assertEquals(listOf(LOBBY, LOBBY), bridging.hub.registrations())
        assertEquals(listOf(LOBBY, LOBBY), bridging.hub.pings())
        // The first connection is the bridge's own to close.
        assertEquals(true, bridging.hub.sockets.first().closed)
        assertEquals(HubState.Connected, bridging.bridge.state.value.hub)
    }

    @Test
    fun `a lobby request opens a registration and confirms it with PING`() = runTest {
        val bridging = bridging()
        runUntil(1000)

        bridging.hub.registration(LOBBY)!!.send(LobbyRequest(TOKEN))
        runUntil(2000)

        assertEquals(listOf(LOBBY, VIEWER), bridging.hub.registrations())
        assertEquals(listOf(LOBBY, VIEWER), bridging.hub.pings())
        assertEquals(1000L, bridging.hub.registration(VIEWER)!!.openedAt)
    }

    @Test
    fun `registers a viewer again when PING answers NULL, and counts its 10 s from the second`() = runTest {
        val bridging = bridging()
        runUntil(1000)
        bridging.hub.loseLogins = 1

        bridging.hub.registration(LOBBY)!!.send(LobbyRequest(TOKEN))
        runUntil(2000)

        assertEquals(listOf(LOBBY, VIEWER, VIEWER), bridging.hub.registrations())
        assertTrue(bridging.hub.sockets.first { it.id == VIEWER }.closed)
        val viewer = bridging.hub.registration(VIEWER)!!
        runUntil(viewer.openedAt + 9_999)
        assertFalse(viewer.closed)
        runUntil(viewer.openedAt + 10_000)
        assertTrue(viewer.closed)
    }

    @Test
    fun `a repeated request for the same token is a no-op`() = runTest {
        val bridging = bridging()
        runUntil(1000)
        val lobby = bridging.hub.registration(LOBBY)!!

        // Once while the registration is being confirmed, once after.
        lobby.send(LobbyRequest(TOKEN))
        lobby.send(LobbyRequest(TOKEN))
        runUntil(1500)
        lobby.send(LobbyRequest(TOKEN))
        runUntil(2000)

        assertEquals(listOf(LOBBY, VIEWER), bridging.hub.registrations())
    }

    @Test
    fun `a malformed request is ignored`() = runTest {
        val bridging = bridging()
        runUntil(1000)
        val lobby = bridging.hub.registration(LOBBY)!!

        // A short token, one with a letter outside the alphabet, and bytes
        // that are no frame.
        lobby.send(customAppData(2, "4HT9WQ2".toByteArray()))
        lobby.send(customAppData(2, "4HT9WQ2I".toByteArray()))
        lobby.send(byteArrayOf(1, 2, 3))
        runUntil(2000)

        assertEquals(listOf(LOBBY), bridging.hub.registrations())
    }

    @Test
    fun `the bridge never refuses a viewer`() = runTest {
        val bridging = bridging()
        runUntil(1000)

        val tokens = List(12) { "4HT9WQ" + ALPHABET[it] + ALPHABET[it] }
        for (token in tokens) bridging.hub.registration(LOBBY)!!.send(LobbyRequest(token))
        runUntil(2000)

        assertEquals(listOf(LOBBY) + tokens.map { LOBBY + it }, bridging.hub.registrations())
    }

    @Test
    fun `the lobby is never written to`() = runTest {
        val bridging = bridging()
        runUntil(1000)
        val lobby = bridging.hub.registration(LOBBY)!!
        lobby.send(LobbyRequest(TOKEN))
        lobby.send(Heartbeat)
        runUntil(2000)
        bridging.hub.registration(VIEWER)!!.send(Heartbeat)
        bridging.board.value = BoardState.Unreachable
        runUntil(5000)

        assertEquals(emptyList<Any>(), lobby.written)
    }

    // Asks for a registration through the lobby, at 1000 ms for the first,
    // and returns it once the bridge has opened it, 500 ms later.
    private fun TestScope.viewer(bridging: Bridging, token: String = TOKEN): FakeHub.FakeSocket {
        if (currentTime < 1000) runUntil(1000)
        bridging.hub.registration(LOBBY)!!.send(LobbyRequest(token))
        runUntil(currentTime + 500)
        return bridging.hub.registration(LOBBY + token)!!
    }

    private fun FakeHub.FakeSocket.heartbeatAt(ms: Long, scope: TestScope) {
        scope.runUntil(ms)
        send(Heartbeat)
        scope.runCurrent()
    }

    private val answering = Status(protocolVersion = 1, boardState = 0)
    private val unreachable = Status(protocolVersion = 1, boardState = 1)

    @Test
    fun `nothing is written before the first heartbeat, and the registration closes at 10 s without one`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)

        runUntil(10_999)
        assertEquals(emptyList<Any>(), viewer.written)
        assertFalse(viewer.closed)

        // Counted from opening, at 1000 ms.
        runUntil(11_000)
        assertTrue(viewer.closed)
        assertNull(bridging.hub.registration(VIEWER))
    }

    @Test
    fun `status is sent at once on the first heartbeat, then once a second`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)

        viewer.heartbeatAt(2500, this)
        viewer.heartbeatAt(3500, this)
        viewer.heartbeatAt(4500, this)

        assertEquals(listOf(2500L to answering, 3000L to answering, 4000L to answering), viewer.written)
    }

    @Test
    fun `writing stops at 3 s without a heartbeat and starts again with the next`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)

        viewer.heartbeatAt(2500, this)
        runUntil(8000)
        assertEquals(listOf(2500L, 3000L, 4000L, 5000L), viewer.written.map { it.first })

        viewer.heartbeatAt(8200, this)
        runUntil(9000)
        assertEquals(listOf(2500L, 3000L, 4000L, 5000L, 8200L, 9000L), viewer.written.map { it.first })
    }

    @Test
    fun `a registration closes 10 s after its last heartbeat`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)

        viewer.heartbeatAt(2500, this)
        viewer.heartbeatAt(9000, this)
        runUntil(18_999)
        assertFalse(viewer.closed)
        runUntil(19_000)
        assertTrue(viewer.closed)
    }

    @Test
    fun `status is sent at once when the board state changes`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)
        viewer.heartbeatAt(3500, this)

        runUntil(3700)
        bridging.board.value = BoardState.Unreachable
        runCurrent()
        // A fault is not a change of the board's state.
        runUntil(3800)
        bridging.board.value = BoardState.Answering(faultCode = 5)
        runCurrent()
        bridging.board.value = BoardState.Answering(faultCode = 0)
        runCurrent()

        assertEquals(
            listOf(2500L to answering, 3000L to answering, 3700L to unreachable, 3800L to answering),
            viewer.written,
        )
    }

    @Test
    fun `status is sent only to active registrations`() = runTest {
        val bridging = bridging()
        val watching = viewer(bridging)
        val quiet = viewer(bridging, token = "22222222")

        watching.heartbeatAt(2500, this)
        bridging.board.value = BoardState.Unreachable
        runUntil(3000)

        assertEquals(listOf(2500L to answering, 2500L to unreachable, 3000L to unreachable), watching.written)
        assertEquals(emptyList<Any>(), quiet.written)
    }

    @Test
    fun `the status message carries the app's major version`() = runTest {
        val viewer = viewer(bridging(protocolVersion = 7, board = BoardState.Unreachable))

        viewer.heartbeatAt(2500, this)

        assertEquals(listOf(2500L to Status(protocolVersion = 7, boardState = 1)), viewer.written)
    }

    @Test
    fun `every frame on a registration but a heartbeat is discarded`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)

        runUntil(2500)
        viewer.send(LobbyRequest("33333333"))
        viewer.send(Status(protocolVersion = 1, boardState = 0))
        viewer.send(byteArrayOf(2, 1, 47, 0, 0, 3))
        runUntil(11_000)

        // Never active, so closed 10 s after opening, and no registration was
        // opened for the request.
        assertEquals(emptyList<Any>(), viewer.written)
        assertTrue(viewer.closed)
        assertEquals(listOf(LOBBY, VIEWER), bridging.hub.registrations())
    }

    @Test
    fun `viewers counted are the registrations with a heartbeat in the last 3 s`() = runTest {
        val bridging = bridging()
        val first = viewer(bridging)
        val second = viewer(bridging, token = "22222222")
        viewer(bridging, token = "33333333")
        assertEquals(0, bridging.bridge.state.value.viewers)

        first.heartbeatAt(2500, this)
        assertEquals(1, bridging.bridge.state.value.viewers)
        second.heartbeatAt(3000, this)
        assertEquals(2, bridging.bridge.state.value.viewers)

        runUntil(5500)
        assertEquals(1, bridging.bridge.state.value.viewers)
        runUntil(6000)
        assertEquals(0, bridging.bridge.state.value.viewers)
    }

    @Test
    fun `registrations stay while the board is unreachable`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        bridging.board.value = BoardState.Unreachable

        for (ms in 2500L..60_500L step 1000) viewer.heartbeatAt(ms, this)

        assertFalse(viewer.closed)
        assertEquals(HubState.Connected, bridging.bridge.state.value.hub)
        assertEquals(1, bridging.bridge.state.value.viewers)
        assertEquals(unreachable, viewer.written.last().second)
    }

    @Test
    fun `a registration the hub closes is gone, and its token can ask again`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        viewer.hubCloses()
        runCurrent()
        assertEquals(0, bridging.bridge.state.value.viewers)

        bridging.hub.registration(LOBBY)!!.send(LobbyRequest(TOKEN))
        runUntil(4000)
        assertEquals(listOf(LOBBY, VIEWER, VIEWER), bridging.hub.registrations())
    }

    @Test
    fun `stopping closes every hub connection`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        bridging.running.cancel()
        runCurrent()

        assertTrue(bridging.hub.sockets.all { it.closed })
    }

    @Test
    fun `the lobby is lost when the hub cannot be reached`() = runTest {
        val bridging = bridging()
        bridging.hub.reachable = false

        runUntil(1000)

        assertEquals(BridgeState(HubState.Lost, viewers = 0), bridging.bridge.state.value)
    }

    // The board's replies as they come, each a frame's payload.
    private val setupReply = ByteArray(74) { (it * 7).toByte() }.also { it[0] = VALUES_SETUP.toByte() }
    private val adcReply = ByteArray(17) { (it * 13).toByte() }.also { it[0] = DECODED_ADC.toByte() }

    private fun FakeHub.FakeSocket.frames() = rawWrites.map { it.toList() }

    @Test
    fun `setup and ADC replies are copied unchanged to active registrations only`() = runTest {
        val bridging = bridging()
        val watching = viewer(bridging)
        val quiet = viewer(bridging, token = "22222222")
        watching.heartbeatAt(2500, this)

        runUntil(2600)
        bridging.replies.emit(setupReply)
        bridging.replies.emit(adcReply)
        runCurrent()

        assertEquals(
            listOf(encodeMessage(answering), encodeFrame(setupReply), encodeFrame(adcReply)).map { it.toList() },
            watching.frames(),
        )
        assertEquals(emptyList<Any>(), quiet.rawWrites)
    }

    @Test
    fun `the firmware version reply and other frames from the board are not copied`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        runUntil(2600)
        bridging.replies.emit(byteArrayOf(FW_VERSION.toByte(), 6, 6))
        bridging.replies.emit(byteArrayOf(99, 1, 2))
        runCurrent()

        assertEquals(listOf(2500L to answering), viewer.written)
    }

    @Test
    fun `replies stop at 3 s without a heartbeat and start again with the next`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        runUntil(5600)
        bridging.replies.emit(setupReply)
        viewer.heartbeatAt(5700, this)
        bridging.replies.emit(adcReply)
        runCurrent()

        assertEquals(
            listOf(answering, answering, answering, answering, answering).map(::encodeMessage).map { it.toList() } +
                listOf(encodeFrame(adcReply).toList()),
            viewer.frames(),
        )
    }

    private val fix = Fix(
        latDeg = 39.5, lonDeg = 32.75, altM = 950.5, speedMS = 12.25, headingDeg = 271.5, accuracyM = 3.5,
        timeUtc = 1_790_000_000_000, at = 2600,
    )

    // The fix above, as its GPS message reads.
    private val fixAsGps = Gps(39.5, 32.75, 950.5, 12.25, 271.5, 3.5, 1_790_000_000_000)

    @Test
    fun `each fix is sent as a GPS message to active registrations only, with 0 for what it lacks`() = runTest {
        val bridging = bridging()
        val watching = viewer(bridging)
        val quiet = viewer(bridging, token = "22222222")
        watching.heartbeatAt(2500, this)

        runUntil(2600)
        bridging.fixes.emit(fix)
        runUntil(2700)
        bridging.fixes.emit(fix.copy(altM = null, speedMS = null, headingDeg = null, accuracyM = null))
        runCurrent()

        assertEquals(
            listOf(
                2500L to answering,
                2600L to fixAsGps,
                2700L to Gps(39.5, 32.75, 0.0, 0.0, 0.0, 0.0, 1_790_000_000_000),
            ),
            watching.written,
        )
        assertEquals(emptyList<Any>(), quiet.rawWrites)
    }

    @Test
    fun `GPS keeps flowing while the board is unreachable`() = runTest {
        val bridging = bridging(board = BoardState.Unreachable)
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        runUntil(2600)
        bridging.fixes.emit(fix)
        runCurrent()

        assertEquals(
            listOf(2500L to unreachable, 2600L to fixAsGps),
            viewer.written,
        )
    }

    @Test
    fun `a fix the GPS message cannot carry is not sent, and the next one is`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        runUntil(2600)
        bridging.fixes.emit(fix.copy(speedMS = 700.0))
        runUntil(2700)
        bridging.fixes.emit(fix)
        runCurrent()

        assertEquals(
            listOf(2500L to answering, 2700L to fixAsGps),
            viewer.written,
        )
    }

    @Test
    fun `a full queue drops its oldest for that viewer only`() = runTest {
        val bridging = bridging()
        val slow = viewer(bridging)
        val fast = viewer(bridging, token = "22222222")
        slow.heartbeatAt(2500, this)
        fast.heartbeatAt(2500, this)

        // The slow viewer's hub connection takes nothing for a while.
        runUntil(2600)
        slow.stalled = true
        val replies = List(200) { n ->
            ByteArray(74).also {
                it[0] = VALUES_SETUP.toByte()
                it[1] = (n / 100).toByte()
                it[2] = (n % 100).toByte()
            }
        }
        for (reply in replies) {
            bridging.replies.emit(reply)
            runCurrent()
        }
        slow.stalled = false
        runUntil(2700)

        val frames = replies.map { encodeFrame(it).toList() }
        val status = encodeMessage(answering).toList()
        assertEquals(listOf(status) + frames, fast.frames())
        // The one already being written, then the newest the queue holds.
        assertEquals(listOf(status, frames.first()) + frames.takeLast(64), slow.frames())
    }

    @Test
    fun `frames waiting when a registration stops being active are not written when it comes back`() = runTest {
        val bridging = bridging()
        val viewer = viewer(bridging)
        viewer.heartbeatAt(2500, this)

        // Stuck on a write until after the viewer is back, with a reply and
        // statuses from before its silence waiting behind it.
        runUntil(2600)
        viewer.stalled = true
        bridging.replies.emit(setupReply)
        runCurrent()
        bridging.replies.emit(adcReply)
        viewer.heartbeatAt(8000, this)
        runUntil(8100)
        viewer.stalled = false
        runCurrent()

        val status = encodeMessage(answering).toList()
        assertEquals(listOf(status, encodeFrame(setupReply).toList(), status), viewer.frames())
    }
}
