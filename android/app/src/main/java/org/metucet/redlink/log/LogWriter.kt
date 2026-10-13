package org.metucet.redlink.log

import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import org.metucet.redlink.board.BoardState
import org.metucet.redlink.board.Cycle
import org.metucet.redlink.location.Fix

private const val FLUSH_MS = 1000L

// The log of record's first line.
private val COLUMNS = listOf(
    "time_utc", "elapsed_s", "speed_m_s", "distance_m", "distance_abs_m", "erpm", "duty_cycle",
    "battery_voltage_v", "battery_current_a", "motor_current_a", "power_w", "battery_level",
    "energy_used_wh", "energy_charged_wh", "charge_used_ah", "charge_charged_ah", "mosfet_temp_c",
    "motor_temp_c", "fault_code", "adc_level1", "adc_level2", "odometer_m", "board_uptime_ms",
    "gps_fix_time_utc", "gps_lat_deg", "gps_lon_deg", "gps_alt_m", "gps_speed_m_s", "gps_heading_deg",
    "gps_accuracy_m",
).joinToString(",")

// Whether the log is being written, as the Log row shows it.
sealed interface LogState {
    // The file's size after the latest write.
    data class Writing(val bytes: Long) : LogState

    // The latest write failed. The writer tries again every second.
    data object NotWriting : LogState
}

// Where a run's log goes. Both throw IOException when storage cannot be
// reached.
interface LogFile {
    // The file's size in bytes: 0 for one not made yet.
    fun size(): Long

    // Adds bytes to the end of the file and puts them on storage, making the
    // file first if need be. Gives the file's size after.
    fun append(bytes: ByteArray): Long
}

private object Tick

// Writes the log of record for a run that started at `startedAt` on the
// monotonic clock `now`, in milliseconds. `wallClock` is Unix milliseconds.
//
// Rows are held in memory and put on storage once a second, and once more
// when the writer ends. Rows held when a write fails are lost: the gap shows
// in the time columns. A file that already holds rows, from the same run
// before Android killed it, is appended to without a second line of column
// names.
class LogWriter(
    private val file: LogFile,
    private val startedAt: Long,
    private val now: () -> Long,
    private val wallClock: () -> Long,
) {
    private val _state = MutableStateFlow<LogState>(LogState.Writing(0))
    val state: StateFlow<LogState> = _state

    private val pending = StringBuilder()
    private var fix: Fix? = null
    private var headed = false

    // Writes until cancelled: a row for each poll cycle with a reply, and,
    // while the board is unreachable, one for each fix. Every row carries the
    // latest fix. File access blocks, so this is run off the main thread.
    suspend fun run(cycles: Flow<Cycle>, fixes: Flow<Fix>, board: StateFlow<BoardState>) {
        val ticks = flow {
            while (true) {
                emit(Tick)
                delay(FLUSH_MS)
            }
        }
        try {
            merge(cycles, fixes, ticks).collect { event ->
                when (event) {
                    is Cycle -> row(event.at, event)
                    is Fix -> {
                        fix = event
                        if (board.value == BoardState.Unreachable) row(now(), null)
                    }
                    Tick -> flush()
                }
            }
        } finally {
            flush()
        }
    }

    private fun flush() {
        try {
            val head = !headed && file.size() == 0L
            val text = if (head) "$COLUMNS\n$pending" else pending.toString()
            _state.value = LogState.Writing(file.append(text.toByteArray(Charsets.UTF_8)))
            headed = true
        } catch (_: IOException) {
            _state.value = LogState.NotWriting
        }
        pending.setLength(0)
    }

    // A row for a moment of the run, on the monotonic clock. Board values are
    // written with the decimals the wire carries: protocol/Messages.kt's
    // scales.
    private fun row(at: Long, cycle: Cycle?) {
        val values = cycle?.values
        val adc = cycle?.adc
        val fix = fix
        val power = values?.batteryVoltageV?.let { voltage -> values.batteryCurrentA?.let { voltage * it } }
        val cells = listOf(
            utc(wallClock() - (now() - at)),
            elapsed(at - startedAt),
            values?.speedMS.decimals(3),
            values?.distanceM.decimals(3),
            values?.distanceAbsM.decimals(3),
            values?.erpm?.toString(),
            values?.dutyCycle.decimals(3),
            values?.batteryVoltageV.decimals(1),
            values?.batteryCurrentA.decimals(2),
            values?.motorCurrentA.decimals(2),
            power.decimals(2),
            values?.batteryLevel.decimals(3),
            values?.energyUsedWh.decimals(4),
            values?.energyChargedWh.decimals(4),
            values?.chargeUsedAh.decimals(4),
            values?.chargeChargedAh.decimals(4),
            values?.mosfetTempC.decimals(1),
            values?.motorTempC.decimals(1),
            values?.faultCode?.toString(),
            adc?.adcLevel1.decimals(6),
            adc?.adcLevel2.decimals(6),
            values?.odometerM?.toString(),
            values?.boardUptimeMs?.toString(),
            fix?.let { utc(it.timeUtc) },
            fix?.latDeg.decimals(7),
            fix?.lonDeg.decimals(7),
            fix?.altM.decimals(2),
            fix?.speedMS.decimals(2),
            fix?.headingDeg.decimals(2),
            fix?.accuracyM.decimals(1),
        )
        cells.joinTo(pending, ",") { it.orEmpty() }
        pending.append('\n')
    }
}

private val utcFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

private fun utc(unixMs: Long) = utcFormat.format(Instant.ofEpochMilli(unixMs))

private fun elapsed(ms: Long) = "%d.%03d".format(Locale.ROOT, ms / 1000, ms % 1000)

// Adding 0.0 turns -0.0 into 0.0, which would otherwise read -0.00.
private fun Double?.decimals(places: Int) = this?.let { "%.${places}f".format(Locale.ROOT, it + 0.0) }
