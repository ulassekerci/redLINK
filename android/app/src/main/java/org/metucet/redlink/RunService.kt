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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.metucet.redlink.board.BoardLink
import org.metucet.redlink.board.BoardPoller
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.simulation

// A run in progress, as the screens see it. `startedAt` is on the phone's
// monotonic clock, SystemClock.elapsedRealtime, in milliseconds.
data class Run(val startedAt: Long, val simulated: Boolean, val board: BoardState)

private const val CHANNEL = "run"
private const val NOTIFICATION = 1

// A run is one lifetime of this service: it begins when the service is made
// and ends when it is destroyed. The service owns the board link and the wake
// lock, so outside a run the app holds neither. The activity only watches
// `run`: closing it, or swiping the app out of recents, changes nothing here.
class RunService : Service() {
    companion object {
        private val _run = MutableStateFlow<Run?>(null)

        // The run in progress, or null outside one.
        val run: StateFlow<Run?> = _run

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
    private var wakeLock: PowerManager.WakeLock? = null

    // When the run started on the wall clock, for the notification's count.
    private var startedAtWallClock = 0L

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        // Start is disabled until a board is picked.
        val board = SavedSettings(this).board ?: return
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
        _run.value = Run(startedAt, simulated, poller.state.value)
        scope.launch { poller.state.collect { _run.value = Run(startedAt, simulated, it) } }
        // One thread, so the poller's parts take turns.
        scope.launch(Dispatchers.Default.limitedParallelism(1)) { poller.run() }
    }

    // Android gives a service started as a foreground one a few seconds to
    // become one, on every start, so this does not depend on a run having
    // begun.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Run", NotificationManager.IMPORTANCE_LOW))
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
        link?.close()
        wakeLock?.release()
        super.onDestroy()
    }

    private fun notification(): Notification = Notification.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.notification)
        .setContentTitle("redLINK")
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

    private fun mainScreen(action: String) = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
