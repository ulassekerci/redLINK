package org.metucet.redlink

// A run as the app stores it while it is active, so that it can be resumed.
// `startedAt` is on the phone's monotonic clock in milliseconds, which counts
// from boot, and `startedAtWallClock` is Unix milliseconds. `bootCount` is the
// boot the run started on. `begun` turns true once the service has been made
// for the run, so each time after is an automatic restart. `restarts` are
// those within the last minute, on the monotonic clock.
data class StoredRun(
    val startedAt: Long,
    val startedAtWallClock: Long,
    val logName: String,
    val bootCount: Int,
    val begun: Boolean = false,
    val restarts: List<Long> = emptyList(),
)

// How many automatic restarts within a minute end a run.
private const val RESTART_LIMIT = 3
private const val RESTART_WINDOW_MS = 60_000L

// Where the stored run is kept, which outlives the app's process.
interface RunStore {
    // The active run, or null outside one.
    var run: StoredRun?

    // Whether the app has yet to say that a phone restart ended the last run.
    var endedByPhoneRestart: Boolean
}

// What the service does when it is made.
sealed interface ServiceStart {
    // Runs the stored run: a new one, or one resumed, which goes on with the
    // file it has already written.
    data class Run(val run: StoredRun, val resumed: Boolean) : ServiceStart

    // The run came back too often and has ended. The app says so.
    data object RestartLimit : ServiceStart

    // There is no run to run.
    data object NoRun : ServiceStart
}

// What the app remembers of a run across its process being killed. `bootCount`
// is the phone's boot now, and `now` the monotonic clock in milliseconds.
class RunMemory(private val store: RunStore, private val bootCount: Int, private val now: () -> Long) {
    // Whether to show that a phone restart ended the last run.
    val endedByPhoneRestart: Boolean get() = store.endedByPhoneRestart

    // Start was pressed: a new run, with a new file. The last run is behind
    // it, so the notice about how that one ended goes.
    fun start(wallClock: Long, logName: String) {
        store.endedByPhoneRestart = false
        store.run = StoredRun(now(), wallClock, logName, bootCount)
    }

    // Stop was pressed.
    fun stop() {
        store.run = null
    }

    // The app was opened. A run stored on an earlier boot was ended by the
    // phone restarting, which the app then says. True when a run stored on
    // this boot has no service running, and is to be resumed.
    fun appOpened(serviceRunning: Boolean): Boolean {
        val stored = store.run ?: return false
        if (stored.bootCount != bootCount) {
            store.run = null
            store.endedByPhoneRestart = true
            return false
        }
        return !serviceRunning
    }

    // The notice about a phone restart was dismissed. It is not shown again.
    fun dismissPhoneRestart() {
        store.endedByPhoneRestart = false
    }

    // The service was made: by Start, by Android bringing it back after a
    // kill or a crash, or by the app resuming the run when it is opened. The
    // restart that would be the third within a minute is not resumed: the app
    // has then restarted 3 times in a minute. A run from an earlier boot is
    // never resumed: the app clears it when it is next opened.
    fun serviceMade(): ServiceStart {
        val stored = store.run?.takeIf { it.bootCount == bootCount } ?: return ServiceStart.NoRun
        val run = if (stored.begun) {
            val now = now()
            stored.copy(restarts = stored.restarts.filter { now - it < RESTART_WINDOW_MS } + now)
        } else {
            stored.copy(begun = true)
        }
        if (run.restarts.size >= RESTART_LIMIT) {
            store.run = null
            return ServiceStart.RestartLimit
        }
        store.run = run
        return ServiceStart.Run(run, resumed = stored.begun)
    }
}
