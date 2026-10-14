package org.metucet.redlink

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// The update line (Android spec 2.13): one check of the repo's latest release,
// and nothing downloaded or installed.

private const val REPO = "ulassekerci/redLINK"

private val releaseTag = Regex("""v(\d+)\.(\d+)\.(\d+)""")
private val ownVersion = Regex("""(\d+)\.(\d+)\.(\d+)(-.*)?""")

// The version to offer for the latest release's tag, or null for none.
//
// Only major, minor and patch are compared; a suffix on the app's own version
// is ignored. A local build is the latest tag with -dev added, so it is that
// release or later, and a candidate is the same build as its final release:
// 1.2.0-dev and 1.2.0-rc1 are offered 1.3.0 but not 1.2.0. A tag with a suffix
// is a pre-release, which the endpoint leaves out, and is never offered.
fun offeredUpdate(latestTag: String, version: String): String? {
    val latest = releaseTag.matchEntire(latestTag)?.groupValues ?: return null
    val own = ownVersion.matchEntire(version)?.groupValues ?: return null
    for (part in 1..3) {
        val difference = (latest[part].toIntOrNull() ?: return null).compareTo(own[part].toIntOrNull() ?: return null)
        if (difference != 0) return if (difference > 0) latestTag.removePrefix("v") else null
    }
    return null
}

// The GitHub Release page of a version the update line offers.
fun releasePage(version: String) = "https://github.com/$REPO/releases/tag/v$version"

// The newer release found the first time the app was opened with no run
// active, once per process. Nothing is checked during a run: a check still
// under way when one starts is stopped.
object UpdateCheck {
    private val _newer = MutableStateFlow<String?>(null)

    // The version Setup offers, or null when there is none or the check
    // failed, which says nothing.
    val newer: StateFlow<String?> = _newer

    private var checked = false
    private var check: Job? = null
    @Volatile private var connection: HttpURLConnection? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkOnce(version: String) {
        if (checked) return
        checked = true
        check = scope.launch {
            val tag = runCatching { latestReleaseTag() }.getOrNull()
            ensureActive()
            _newer.value = tag?.let { offeredUpdate(it, version) }
        }
    }

    // Closing the connection ends a request blocked on the network.
    fun stop() {
        check?.cancel()
        connection?.disconnect()
    }

    // The latest release's tag. Throws when there is none, as when the repo
    // has no release yet, or the request fails.
    private fun latestReleaseTag(): String {
        val connection = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        this.connection = connection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body).getString("tag_name")
        } finally {
            connection.disconnect()
            this.connection = null
        }
    }
}
