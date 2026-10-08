package org.metucet.redlink.protocol

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// protocol/vectors.json, the cases this app's suites and the desktop app's
// both read. The build passes its path; protocol/README.md gives its shape.
object Vectors {
    private val file = Json.parseToJsonElement(File(System.getProperty("redlink.vectors")).readText()).jsonObject

    val frames = cases("frames")
    val teamCodes = cases("team_codes")

    private fun cases(group: String) =
        file.getValue(group).jsonArray.map { it.jsonObject }.also { check(it.isNotEmpty()) { "No $group cases" } }
}

fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
