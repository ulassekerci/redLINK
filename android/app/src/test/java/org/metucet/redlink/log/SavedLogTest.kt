package org.metucet.redlink.log

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedLogTest {
    private val start = LocalDateTime.of(2026, 9, 26, 14, 22, 9)

    @Test
    fun `a log's name gives back its start time and whether the run was simulated`() {
        assertEquals(LogName(start, simulated = false), parseLogName("redLINK_2026-09-26_14-22-09.csv"))
        assertEquals(LogName(start, simulated = true), parseLogName("SIM_redLINK_2026-09-26_14-22-09.csv"))
        // What the run named it.
        assertEquals(LogName(start, simulated = true), parseLogName(logFileName(start, simulated = true)))
    }

    @Test
    fun `a name the app did not give is not a log name`() {
        assertNull(parseLogName("notes.csv"))
        assertNull(parseLogName("redLINK_2026-09-26.csv"))
        assertNull(parseLogName("redLINK_2026-13-40_14-22-09.csv"))
    }

    private val header = "time_utc,elapsed_s,speed_m_s,distance_m,distance_abs_m,erpm,duty_cycle," +
        "battery_voltage_v,battery_current_a,motor_current_a,power_w,battery_level,energy_used_wh," +
        "energy_charged_wh,charge_used_ah,charge_charged_ah,mosfet_temp_c,motor_temp_c,fault_code," +
        "adc_level1,adc_level2,odometer_m,board_uptime_ms,gps_fix_time_utc,gps_lat_deg,gps_lon_deg," +
        "gps_alt_m,gps_speed_m_s,gps_heading_deg,gps_accuracy_m\n"

    private fun row(elapsed: String) = "2026-09-26T11:22:09.050Z,$elapsed,0.020,0.001,0.001,20,0.002,48.0,0.03," +
        "15.00,1.44,1.000,0.0000,0.0000,0.0000,0.0000,39.0,50.0,0,1.000000,0.000000,12000,50," +
        "2026-09-26T11:22:08.000Z,39.8875000,32.7811000,912.00,8.10,91.20,3.8\n"

    @Test
    fun `a log's duration is the elapsed time of its last row, in whole seconds`() {
        assertEquals(2460L, durationSeconds(header + row("2459.050") + row("2460.000")))
        assertEquals(2459L, durationSeconds(header + row("2459.050") + row("2459.999")))
    }

    @Test
    fun `the duration is read from the end of a file, which starts mid-row`() {
        val tail = (row("12.300") + row("12.350")).drop(30)
        assertEquals(12L, durationSeconds(tail))
    }

    @Test
    fun `a row cut short by a kill is passed over for the one before it`() {
        assertEquals(12L, durationSeconds(header + row("12.350") + row("12.400").take(40)))
    }

    @Test
    fun `a log with no rows lasted no time`() {
        assertEquals(0L, durationSeconds(header))
        assertEquals(0L, durationSeconds(""))
    }

    private fun log(name: String) = SavedLog(id = 1, name = name, bytes = 0, durationS = 0)

    @Test
    fun `logs are listed newest first, simulated or not`() {
        val logs = listOf(
            log("redLINK_2026-09-26_14-22-09.csv"),
            log("SIM_redLINK_2026-10-01_09-00-00.csv"),
            log("redLINK_2026-09-26_14-22-10.csv"),
            log("redLINK_2025-12-31_23-59-59.csv"),
        )
        assertEquals(
            listOf(
                "SIM_redLINK_2026-10-01_09-00-00.csv",
                "redLINK_2026-09-26_14-22-10.csv",
                "redLINK_2026-09-26_14-22-09.csv",
                "redLINK_2025-12-31_23-59-59.csv",
            ),
            newestFirst(logs).map { it.name },
        )
    }

    @Test
    fun `a log whose name has no start time is listed last`() {
        val logs = listOf(log("redLINK_2026-09-26_14-22-09 (1).csv"), log("redLINK_2026-09-26_14-22-09.csv"))
        assertEquals(
            listOf("redLINK_2026-09-26_14-22-09.csv", "redLINK_2026-09-26_14-22-09 (1).csv"),
            newestFirst(logs).map { it.name },
        )
    }
}
