package org.metucet.redlink

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions

// The six grants a run needs, in the order Setup lists them.
enum class Grant(val label: String) {
    BLUETOOTH("Bluetooth"),
    PRECISE_LOCATION("Precise location"),
    BACKGROUND_LOCATION("Location \"all the time\""),
    LOCATION_SERVICES("Location services on"),
    BATTERY_EXEMPTION("Battery optimisation exemption"),
    NOTIFICATIONS("Notifications"),
}

private val bluetooth = arrayOf(BLUETOOTH_CONNECT, BLUETOOTH_SCAN)

// Android offers precise location only when approximate is asked for with it.
private val foregroundLocation = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)

private val backgroundLocation = arrayOf(ACCESS_BACKGROUND_LOCATION)

private fun Context.holds(permission: String) = checkSelfPermission(permission) == PERMISSION_GRANTED

private fun Context.inPlace(grant: Grant) = when (grant) {
    Grant.BLUETOOTH -> bluetooth.all(::holds)
    Grant.PRECISE_LOCATION -> holds(ACCESS_FINE_LOCATION)
    Grant.BACKGROUND_LOCATION -> holds(ACCESS_BACKGROUND_LOCATION)
    Grant.LOCATION_SERVICES -> getSystemService(LocationManager::class.java).isLocationEnabled
    Grant.BATTERY_EXEMPTION -> getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    // On Android 13 and later this is the POST_NOTIFICATIONS permission; on
    // Android 12, that notifications are not blocked for the app.
    Grant.NOTIFICATIONS -> getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}

// The grants in place right now. Any can be taken away in system settings, so
// this is read again whenever a screen is shown.
fun Context.grantsInPlace(): Set<Grant> = Grant.entries.filterTo(mutableSetOf(), ::inPlace)

// What a Grant button does: Android's own dialog where the grant has one, and
// the system screen that holds the switch where it has none or Android no
// longer shows it. Made before the activity starts, as Android requires of
// anything that waits for a result.
class GrantRequests(private val activity: ComponentActivity) {
    // A request Android has not answered yet, with whether it would have
    // explained the permissions when asked.
    private class Asked(val grant: Grant, val permissions: Array<String>, val wouldExplain: Boolean)

    private var asked: Asked? = null

    private val launcher = activity.registerForActivityResult(RequestMultiplePermissions()) {
        // Cleared first: the answer may be to ask again.
        val answeredFor = asked
        asked = null
        answeredFor?.let(::answered)
    }

    fun request(grant: Grant) = when (grant) {
        Grant.BLUETOOTH -> ask(grant, bluetooth)
        Grant.PRECISE_LOCATION -> ask(grant, foregroundLocation)
        // Android ignores a request for "all the time" until the app has
        // location at all, so that is asked for first.
        Grant.BACKGROUND_LOCATION ->
            ask(grant, if (foregroundLocation.any(activity::holds)) backgroundLocation else foregroundLocation)
        Grant.LOCATION_SERVICES -> open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        Grant.BATTERY_EXEMPTION -> open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, appUri()))
        Grant.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ask(grant, arrayOf(POST_NOTIFICATIONS))
        } else {
            openSettings(grant)
        }
    }

    private fun ask(grant: Grant, permissions: Array<String>) {
        // A second tap before the dialog is up. Android would answer it at
        // once with nothing granted, which reads as a refusal.
        if (asked != null) return
        asked = Asked(grant, permissions, wouldExplain(permissions))
        launcher.launch(permissions)
    }

    private fun answered(asked: Asked) {
        if (asked.grant == Grant.BACKGROUND_LOCATION && asked.permissions.contentEquals(foregroundLocation)) {
            if (foregroundLocation.any(activity::holds)) return request(asked.grant)
        }
        // After a second refusal Android answers without showing its dialog,
        // and the only way left is the app's page in system settings. Android
        // does not say that it did so. The sign is that it would explain the
        // permission neither before the request nor after it. A first dialog
        // dismissed without an answer looks the same, and opens settings too.
        val refused = !asked.permissions.all(activity::holds)
        if (refused && !asked.wouldExplain && !wouldExplain(asked.permissions)) openSettings(asked.grant)
    }

    private fun wouldExplain(permissions: Array<String>) =
        permissions.any(activity::shouldShowRequestPermissionRationale)

    private fun openSettings(grant: Grant) = open(
        if (grant == Grant.NOTIFICATIONS) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        } else {
            appSettings()
        },
    )

    private fun appSettings() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri())

    private fun appUri() = Uri.fromParts("package", activity.packageName, null)

    // A phone may lack one of the narrower screens. The app's own page is
    // always there.
    private fun open(intent: Intent) = try {
        activity.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        activity.startActivity(appSettings())
    }
}
