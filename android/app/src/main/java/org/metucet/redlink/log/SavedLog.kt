package org.metucet.redlink.log

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val nameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

// A run's log file name, from the phone's local time when the run started.
fun logFileName(startedAt: LocalDateTime, simulated: Boolean): String {
    val name = "redLINK_${nameFormat.format(startedAt)}.csv"
    return if (simulated) "SIM_$name" else name
}

// What a log's file name says about its run.
data class LogName(val start: LocalDateTime, val simulated: Boolean)

// A log in Documents/redLINK/ that the app owns. `id` is its MediaStore ID.
data class SavedLog(val id: Long, val name: String, val bytes: Long, val durationS: Long) {
    // Null for a name the app did not give, such as one MediaStore changed
    // to keep it apart from another of the same name.
    val parsed: LogName? = parseLogName(name)
}

// By start time, newest first. A log with no start time to read goes last.
fun newestFirst(logs: List<SavedLog>): List<SavedLog> =
    logs.sortedWith(compareByDescending(nullsFirst()) { it.parsed?.start })

private val namePattern = Regex("""^(SIM_)?redLINK_(.+)\.csv$""")

// The inverse of logFileName, or null for a name it does not give.
fun parseLogName(name: String): LogName? {
    val match = namePattern.find(name) ?: return null
    val start = try {
        LocalDateTime.parse(match.groupValues[2], nameFormat)
    } catch (_: DateTimeParseException) {
        return null
    }
    return LogName(start, simulated = match.groupValues[1].isNotEmpty())
}

// A log's duration in whole seconds: the elapsed_s of its last row, read from
// the end of the file. The end can start mid-row, and a kill can cut the last
// row short, so only a line with every cell counts. No row is no time.
fun durationSeconds(tail: String): Long {
    for (line in tail.lines().asReversed()) {
        val cells = line.split(',')
        if (cells.size != COLUMN_COUNT) continue
        // The column names, when the log has no rows.
        val elapsed = cells[1].toDoubleOrNull() ?: return 0
        return elapsed.toLong()
    }
    return 0
}
