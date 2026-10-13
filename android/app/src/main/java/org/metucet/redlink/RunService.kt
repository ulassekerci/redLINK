package org.metucet.redlink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.PowerManager
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.metucet.redlink.board.BoardLink
import org.metucet.redlink.board.BoardPoller
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.simulation
import org.metucet.redlink.hub.Bridge
import org.metucet.redlink.hub.BridgeState
import org.metucet.redlink.hub.PUBLIC_HUB_HOST
import org.metucet.redlink.hub.PUBLIC_HUB_PORT
import org.metucet.redlink.hub.TcpHubSockets
import org.metucet.redlink.location.Fix
import org.metucet.redlink.location.LocationUpdates
import org.metucet.redlink.log.LogState
import org.metucet.redlink.log.LogWriter
import org.metucet.redlink.log.MediaStoreLogFile
import org.metucet.redlink.log.logFileName
import org.metucet.redlink.ui.notificationText
import org.metucet.redlink.ui.vehicleRow
import org.metucet.redlink.ui.viewersRow

// A run in progress, as the screens see it. `startedAt` is on the phone's
// monotonic clock, SystemClock.elapsedRealtime, in milliseconds. `logName` is
// the log's file name. `fix` is the latest, or null before the first.
data class Run(
    val startedAt: Long,
    val simulated: Boolean,
    val logName: String,
    val board: BoardState,
    val fix: Fix? = null,
    val log: LogState = LogState.Writing(0),
    val bridge: BridgeState = BridgeState(),
)

private const val CHANNEL = "run"
private const val NOTIFICATION = 1
private const val LOG_CHANNEL = "log"
private const val NOT_LOGGING_NOTIFICATION = 2

// A run is one lifetime of this service: it begins when the service is made
// and ends when it is destroyed. The service owns the board link, the hub
// connections, location updates, the log and the wake lock, so outside a run
// the app holds none of them. The activity only watches `run`: closing it, or
// swiping the app out of recents, changes nothing here.
class RunService : Service() {
    companion object {
        private val _run = MutableStateFlow<Run?>(null)

        // The run in progress, or null outside one.
        val run: StateFlow<Run?> = _run

        private val _logsClosed = MutableStateFlow(0)

        // Counts the runs whose log has had its last rows written and been
        // closed, which comes a moment after `run` turns null.
        val logsClosed: StateFlow<Int> = _logsClosed

        // Android lets a foreground service of these types start only from an
        // app the person can see, so this is called from the activity.
        fun start(context: Context) {
            context.startForegroundService(Intent(context, RunService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RunService::class.java))
        }
    }

    // The state the screens watch is written on the main thread only, so
    // nothing can write it after onDestroy has cleared it.
    private val scope = MainScope()

    private var link: BoardLink? = null
    private var location: LocationUpdates? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // When the run started on the wall clock, for the notification's count.
    private var startedAtWallClock = 0L

    // The notification's text, once the run has one.
    private var notificationText: String? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Run", NotificationManager.IMPORTANCE_LOW),
        )
        // Loud enough to be noticed: the run goes on with nothing recorded.
        notifications.createNotificationChannel(
            NotificationChannel(LOG_CHANNEL, "Log problems", NotificationManager.IMPORTANCE_DEFAULT),
        )
        // Start is disabled until a board is picked.
        val settings = SavedSettings(this)
        val board = settings.board ?: return
        val startedAt = SystemClock.elapsedRealtime()
        startedAtWallClock = System.currentTimeMillis()

        // No timeout: a run lasts hours and ends only at Stop.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "redLINK:run")
            .also { it.acquire() }

        val link = when (board) {
            PickedBoard.Simulated -> checkNotNull(simulation).board { SystemClock.elapsedRealtime() - startedAt }
        }
        this.link = link
        val simulated = board == PickedBoard.Simulated
        val poller = BoardPoller(link, SystemClock::elapsedRealtime)
        // GPS comes from the phone whichever board is picked.
        val location = LocationUpdates(this)
        this.location = location
        val startedAtLocal = LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtWallClock), ZoneId.systemDefault())
        val logName = logFileName(startedAtLocal, simulated)
        val file = MediaStoreLogFile(this, logName)
        val log = LogWriter(file, startedAt, SystemClock::elapsedRealtime, System::currentTimeMillis)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val bridge = Bridge(
            settings.teamCode,
            majorVersion(version),
            poller.state,
            TcpHubSockets(PUBLIC_HUB_HOST, PUBLIC_HUB_PORT),
            SystemClock::elapsedRealtime,
        )
        _run.value = Run(startedAt, simulated, logName, poller.state.value)
        scope.launch {
            combine(poller.state, location.latest, log.state, bridge.state) { board, fix, logState, bridging ->
                Run(startedAt, simulated, logName, board, fix, logState, bridging)
            }.collect { _run.value = it }
        }
        scope.launch {
            run.filterNotNull().map { notificationText(vehicleRow(true, it), viewersRow(it)) }.distinctUntilChanged().collect {
                notificationText = it
                notifications.notify(NOTIFICATION, notification())
            }
        }
        scope.launch {
            log.state.map { it == LogState.NotWriting }.distinctUntilChanged().collect { failing ->
                if (failing) notifications.notify(NOT_LOGGING_NOTIFICATION, notLogging()) else notifications.cancel(NOT_LOGGING_NOTIFICATION)
            }
        }
        // Writing blocks, so it has its own thread. The file is closed after
        // the writer's last write, which comes once the run has ended. Polling
        // waits for the writer to listen, so the first cycle has its row.
        val logListening = CompletableDeferred<Unit>()
        val cycles = poller.cycles.onSubscription { logListening.complete(Unit) }
        scope.launch(Dispatchers.IO) {
            try {
                file.use { log.run(cycles, location.fixes, poller.state) }
            } finally {
                _logsClosed.update { it + 1 }
            }
        }
        // One thread, so the poller's parts take turns.
        scope.launch(Dispatchers.Default.limitedParallelism(1)) {
            logListening.await()
            poller.run()
        }
        // A thread of its own, for the same reason. Cancelling it closes every
        // hub connection, so the hub closes each viewer's socket.
        scope.launch(Dispatchers.Default.limitedParallelism(1)) { bridge.run() }
    }

    // Android gives a service started as a foreground one a few seconds to
    // become one, on every start, so this does not depend on a run having
    // begun.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android refuses the connectedDevice type to an app without the
        // Bluetooth grant, which a run on the simulated board does not need.
        val connectedDevice =
            if (Grant.BLUETOOTH in grantsInPlace()) FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        startForeground(NOTIFICATION, notification(), FOREGROUND_SERVICE_TYPE_LOCATION or connectedDevice)
        if (link == null) stopSelf()
        // A service Android killed stays gone: nothing resumes a run yet.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        _run.value = null
        getSystemService(NotificationManager::class.java).cancel(NOT_LOGGING_NOTIFICATION)
        link?.close()
        location?.close()
        wakeLock?.release()
        super.onDestroy()
    }

    private fun notification(): Notification = Notification.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.notification)
        .setContentTitle("redLINK")
        .setContentText(notificationText)
        // The run's duration, counting beside the title.
        .setWhen(startedAtWallClock)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(mainScreen(MainActivity.ACTION_SHOW_MAIN))
        // Stop asks first, on the main screen. It never stops the run by itself.
        .addAction(Notification.Action.Builder(null, "Stop", mainScreen(MainActivity.ACTION_ASK_STOP)).build())
        .build()

    private fun notLogging(): Notification = Notification.Builder(this, LOG_CHANNEL)
        .setSmallIcon(R.drawable.notification)
        .setContentTitle("redLINK is not logging")
        .setContentText("The log file cannot be written. Viewers are not affected.")
        .setContentIntent(mainScreen(MainActivity.ACTION_SHOW_MAIN))
        .build()

    private fun mainScreen(action: String) = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
