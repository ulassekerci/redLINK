package org.metucet.redlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMemoryTest {
    // What SavedRun keeps in the app's settings, in memory.
    private class MemoryStore : RunStore {
        override var run: StoredRun? = null
        override var endedByPhoneRestart = false
    }

    private val store = MemoryStore()
    private var now = 5_000L

    // The app as it is on a boot of the phone, sharing what it stored.
    private fun memory(bootCount: Int = 7) = RunMemory(store, bootCount) { now }

    @Test
    fun `the service made for a run that was just started begins it`() {
        memory().start(wallClock = 1_790_000_000_000, logName = "redLINK_2026-09-26_14-22-09.csv")
        now = 5_040

        val made = memory().serviceMade()

        assertEquals(5_000L, (made as ServiceStart.Run).run.startedAt)
        assertEquals(1_790_000_000_000, made.run.startedAtWallClock)
        assertEquals("redLINK_2026-09-26_14-22-09.csv", made.run.logName)
        // A new run makes a new file, even where one has its name.
        assertFalse(made.resumed)
    }

    // A run started, with its service made for it.
    private fun startAndMakeService() {
        memory().start(wallClock = 1_790_000_000_000, logName = "redLINK_2026-09-26_14-22-09.csv")
        memory().serviceMade()
    }

    @Test
    fun `a service made again after a kill resumes the same run`() {
        startAndMakeService()
        now = 600_000

        val made = memory().serviceMade()

        assertEquals(5_000L, (made as ServiceStart.Run).run.startedAt)
        assertEquals("redLINK_2026-09-26_14-22-09.csv", made.run.logName)
        assertTrue(made.resumed)
    }

    @Test
    fun `the third restart within a minute ends the run instead of resuming it`() {
        startAndMakeService()
        now = 100_000
        assertTrue(memory().serviceMade() is ServiceStart.Run)
        now = 130_000
        assertTrue(memory().serviceMade() is ServiceStart.Run)
        now = 159_999

        assertEquals(ServiceStart.RestartLimit, memory().serviceMade())
        assertEquals(null, store.run)
        // Nothing is left to resume.
        assertEquals(ServiceStart.NoRun, memory().serviceMade())
    }

    @Test
    fun `restarts more than a minute apart keep resuming`() {
        startAndMakeService()
        for (at in listOf(100_000L, 130_000L, 160_000L, 190_000L, 220_000L)) {
            now = at
            assertTrue("restart at $at", memory().serviceMade() is ServiceStart.Run)
        }
    }

    @Test
    fun `after Stop nothing is resumed`() {
        startAndMakeService()
        memory().stop()

        assertEquals(ServiceStart.NoRun, memory().serviceMade())
        assertFalse(memory().appOpened(serviceRunning = false))
    }

    @Test
    fun `the app opened on the run's boot with no service running resumes the run`() {
        startAndMakeService()

        assertTrue(memory().appOpened(serviceRunning = false))
        assertFalse(memory().appOpened(serviceRunning = true))
        assertFalse(memory().endedByPhoneRestart)
    }

    @Test
    fun `the app opened outside a run resumes nothing`() {
        assertFalse(memory().appOpened(serviceRunning = false))
        assertFalse(memory().endedByPhoneRestart)
    }

    @Test
    fun `the app opened after a phone restart ends the run and says so until dismissed`() {
        startAndMakeService()
        now = 9_000

        val rebooted = memory(bootCount = 8)
        assertFalse(rebooted.appOpened(serviceRunning = false))
        assertTrue(rebooted.endedByPhoneRestart)
        assertEquals(null, store.run)
        // Opened again before the notice is dismissed.
        assertFalse(memory(bootCount = 8).appOpened(serviceRunning = false))
        assertTrue(memory(bootCount = 8).endedByPhoneRestart)

        memory(bootCount = 8).dismissPhoneRestart()
        assertFalse(memory(bootCount = 8).endedByPhoneRestart)
    }

    @Test
    fun `a service made on a later boot does not resume the run`() {
        startAndMakeService()

        assertEquals(ServiceStart.NoRun, memory(bootCount = 8).serviceMade())
    }

    @Test
    fun `Start after a phone restart begins a new run and drops the notice`() {
        startAndMakeService()
        memory(bootCount = 8).appOpened(serviceRunning = false)
        now = 2_000

        memory(bootCount = 8).start(wallClock = 1_790_100_000_000, logName = "redLINK_2026-09-27_18-08-49.csv")

        assertFalse(memory(bootCount = 8).endedByPhoneRestart)
        val made = memory(bootCount = 8).serviceMade()
        assertEquals(2_000L, (made as ServiceStart.Run).run.startedAt)
        assertEquals("redLINK_2026-09-27_18-08-49.csv", made.run.logName)
        assertFalse(made.resumed)
    }
}
