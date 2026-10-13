package org.metucet.redlink.location

// The most the GPS message's accuracy field carries.
private const val COARSEST_ACCURACY_M = 6553.5

// Where the phone was. A field the provider did not give is null, not 0: the
// GPS message sends it as 0 and the log leaves its cell empty. `timeUtc` is
// when the fix was taken, in Unix milliseconds; `at` is the same moment on the
// phone's monotonic clock, SystemClock.elapsedRealtime, in milliseconds.
data class Fix(
    val latDeg: Double,
    val lonDeg: Double,
    val altM: Double?,
    val speedMS: Double?,
    val headingDeg: Double?,
    val accuracyM: Double?,
    val timeUtc: Long,
    val at: Long,
)

// A reading from the location provider as a fix, or null when it is coarser
// than the GPS message can carry: such a reading is not sent, not logged and
// not shown as a fix.
fun fixOrNull(
    latDeg: Double,
    lonDeg: Double,
    altM: Double?,
    speedMS: Double?,
    headingDeg: Double?,
    accuracyM: Double?,
    timeUtc: Long,
    at: Long,
): Fix? {
    if (accuracyM != null && accuracyM > COARSEST_ACCURACY_M) return null
    return Fix(latDeg, lonDeg, altM, speedMS, headingDeg, accuracyM, timeUtc, at)
}
