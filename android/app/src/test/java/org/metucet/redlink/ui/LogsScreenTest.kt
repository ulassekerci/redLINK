package org.metucet.redlink.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.metucet.redlink.log.SavedLog

class LogsScreenTest {
    @Test
    fun `the summary gives the number of logs and their size in megabytes`() {
        assertEquals("5 logs · 49.7 MB in Documents/redLINK", logsSummary(count = 5, bytes = 49_700_000))
        assertEquals("1 log · 0.1 MB in Documents/redLINK", logsSummary(count = 1, bytes = 80_000))
        assertEquals("0 logs · 0.0 MB in Documents/redLINK", logsSummary(count = 0, bytes = 0))
    }

    @Test
    fun `a past log reads as its start time, with its duration and size`() {
        val log = SavedLog(id = 1, name = "redLINK_2026-09-26_14-22-09.csv", bytes = 10_200_000, durationS = 2460)
        assertEquals("2026-09-26 14:22", pastLogTitle(log))
        assertEquals("41:00 · 10.2 MB", pastLogLine(log))
        val long = SavedLog(id = 2, name = "redLINK_2026-09-26_14-22-09.csv", bytes = 56_040_000, durationS = 3764)
        assertEquals("1:02:44 · 56.0 MB", pastLogLine(long))
    }

    @Test
    fun `a simulated run's log is marked SIM`() {
        val log = SavedLog(id = 1, name = "SIM_redLINK_2026-09-26_14-22-09.csv", bytes = 0, durationS = 0)
        assertEquals("SIM 2026-09-26 14:22", pastLogTitle(log))
    }

    @Test
    fun `a log whose name has no start time reads as its name`() {
        val log = SavedLog(id = 1, name = "redLINK_2026-09-26_14-22-09 (1).csv", bytes = 0, durationS = 0)
        assertEquals("redLINK_2026-09-26_14-22-09 (1).csv", pastLogTitle(log))
    }

    @Test
    fun `the log in progress gives the run's duration and cannot be shared`() {
        assertEquals("12:44 · in progress, cannot be shared yet", currentLogLine(seconds = 764))
    }

    @Test
    fun `Share and Delete give the number selected`() {
        assertEquals("Share 2", selectionAction("Share", selected = 2))
        assertEquals("Delete 1", selectionAction("Delete", selected = 1))
        // Disabled then, with no number to give.
        assertEquals("Share", selectionAction("Share", selected = 0))
    }

    @Test
    fun `Select all turns to Select none once every log is selected`() {
        assertEquals("Select all", selectAllLabel(selected = 0, logs = 5))
        assertEquals("Select all", selectAllLabel(selected = 4, logs = 5))
        assertEquals("Select none", selectAllLabel(selected = 5, logs = 5))
        assertEquals("Select all", selectAllLabel(selected = 0, logs = 0))
    }

    @Test
    fun `the delete dialog names how many logs go`() {
        assertEquals("Delete 1 log?", deleteTitle(1))
        assertEquals("Delete 2 logs?", deleteTitle(2))
    }
}
