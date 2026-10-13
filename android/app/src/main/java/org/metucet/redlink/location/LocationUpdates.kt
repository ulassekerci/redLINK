package org.metucet.redlink.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

private const val INTERVAL_MS = 1000L

// Precise location once a second from the phone's GPS, from when this is made
// until it is closed. It uses the platform's GPS provider, not Google Play
// services: docs/adr/0003-location-from-the-platform-gps-provider.md says why.
//
// Fixes arrive on the main thread. With location services switched off none
// arrive, and they start again when the switch is turned back on.
class LocationUpdates(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(LocationManager::class.java)

    private val _latest = MutableStateFlow<Fix?>(null)
    private val _fixes = MutableSharedFlow<Fix>(extraBufferCapacity = 64)

    // The latest fix, or null before the first. A collector that falls behind
    // sees only the newest, which is all the screen needs.
    val latest: StateFlow<Fix?> = _latest

    // Every fix, in order, for the parts that must not miss one: the log and
    // the stream. A collector may fall a minute behind before fixes are lost.
    val fixes: SharedFlow<Fix> = _fixes

    // A batch of fixes arrives as one call per fix.
    private val listener = LocationListener { location ->
        location.toFix()?.let {
            _latest.value = it
            _fixes.tryEmit(it)
        }
    }

    init {
        val request = LocationRequest.Builder(INTERVAL_MS).setQuality(LocationRequest.QUALITY_HIGH_ACCURACY).build()
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request, context.mainExecutor, listener)
        } catch (_: SecurityException) {
            // The grant was taken away in system settings: no fix, and the run
            // goes on.
        } catch (_: IllegalArgumentException) {
            // A phone with no GPS: the same.
        }
    }

    override fun close() = manager.removeUpdates(listener)
}

private fun Location.toFix() = fixOrNull(
    latDeg = latitude,
    lonDeg = longitude,
    altM = if (hasAltitude()) altitude else null,
    speedMS = if (hasSpeed()) speed.toDouble() else null,
    headingDeg = if (hasBearing()) bearing.toDouble() else null,
    accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
    timeUtc = time,
    at = elapsedRealtimeNanos / 1_000_000,
)
