package org.metucet.redlink

// The major of the app's version, such as 1 for 1.2.0 or 1.2.0-dev. It is the
// protocol version in the status message.
fun majorVersion(version: String) = version.substringBefore('.').toIntOrNull() ?: 0
