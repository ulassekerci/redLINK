package org.metucet.redlink

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

private const val STARTED_AT = "started_at"
private const val STARTED_AT_WALL_CLOCK = "started_at_wall_clock"
private const val LOG_NAME = "log_name"
private const val BOOT_COUNT = "boot_count"
private const val BEGUN = "begun"
private const val RESTARTS = "restarts"
private const val ENDED_BY_PHONE_RESTART = "ended_by_phone_restart"

// The stored run, in the app's private settings. Each change is on storage
// before it returns: the process can be killed at any moment after.
class SavedRun(context: Context) : RunStore {
    private val preferences = context.getSharedPreferences("run", Context.MODE_PRIVATE)

    override var run: StoredRun?
        get() {
            val logName = preferences.getString(LOG_NAME, null) ?: return null
            return StoredRun(
                startedAt = preferences.getLong(STARTED_AT, 0),
                startedAtWallClock = preferences.getLong(STARTED_AT_WALL_CLOCK, 0),
                logName = logName,
                bootCount = preferences.getInt(BOOT_COUNT, 0),
                begun = preferences.getBoolean(BEGUN, false),
                restarts = preferences.getString(RESTARTS, "")!!.split(',').mapNotNull { it.toLongOrNull() },
            )
        }
        set(run) {
            val edit = preferences.edit()
            if (run == null) {
                edit.remove(STARTED_AT).remove(STARTED_AT_WALL_CLOCK).remove(LOG_NAME).remove(BOOT_COUNT)
                    .remove(BEGUN).remove(RESTARTS)
            } else {
                edit.putLong(STARTED_AT, run.startedAt)
                    .putLong(STARTED_AT_WALL_CLOCK, run.startedAtWallClock)
                    .putString(LOG_NAME, run.logName)
                    .putInt(BOOT_COUNT, run.bootCount)
                    .putBoolean(BEGUN, run.begun)
                    .putString(RESTARTS, run.restarts.joinToString(","))
            }
            edit.commit()
        }

    override var endedByPhoneRestart: Boolean
        get() = preferences.getBoolean(ENDED_BY_PHONE_RESTART, false)
        set(ended) {
            preferences.edit().putBoolean(ENDED_BY_PHONE_RESTART, ended).commit()
        }
}

// The app's memory of its run, on this boot of the phone and its monotonic
// clock.
fun runMemory(context: Context) = RunMemory(
    SavedRun(context),
    Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0),
    SystemClock::elapsedRealtime,
)
