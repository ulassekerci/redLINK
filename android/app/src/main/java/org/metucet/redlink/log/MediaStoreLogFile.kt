package org.metucet.redlink.log

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val nameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

// A run's log file name, from the phone's local time when the run started.
fun logFileName(startedAt: LocalDateTime, simulated: Boolean): String {
    val name = "redLINK_${nameFormat.format(startedAt)}.csv"
    return if (simulated) "SIM_$name" else name
}

// A log in the phone's shared Documents/redLINK/ folder, made through
// MediaStore, which needs no storage permission. The files stay reachable
// from the Files app and over USB, and nothing here deletes one.
//
// The file is made at the first write and kept open between writes. After a
// failure it is opened again, or made again if it never was, at the next.
class MediaStoreLogFile(context: Context, private val name: String) : LogFile, AutoCloseable {
    private val resolver = context.contentResolver
    private var uri: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var stream: FileOutputStream? = null

    override fun size(): Long = guarded { if (uri == null) 0 else open().channel.size() }

    override fun append(bytes: ByteArray): Long = guarded {
        val stream = open()
        stream.channel.position(stream.channel.size())
        stream.write(bytes)
        // Onto storage, not only into the system's cache: a dead battery
        // loses at most what came since.
        stream.fd.sync()
        stream.channel.size()
    }

    override fun close() {
        runCatching { stream?.close() }
        runCatching { descriptor?.close() }
        stream = null
        descriptor = null
    }

    private fun open(): FileOutputStream {
        stream?.let { return it }
        val uri = uri ?: make().also { uri = it }
        val descriptor = resolver.openFileDescriptor(uri, "rw") ?: throw IOException("Cannot open $uri")
        this.descriptor = descriptor
        return FileOutputStream(descriptor.fileDescriptor).also { stream = it }
    }

    private fun make(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/redLINK")
        }
        return resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IOException("Cannot make $name")
    }

    // MediaStore reports storage it cannot reach in several ways, a full
    // database among them, and each is a file that cannot be written. The
    // file is closed, so the next try opens it afresh.
    private fun <T> guarded(access: () -> T): T = try {
        access()
    } catch (e: Exception) {
        close()
        throw e as? IOException ?: IOException(e)
    }
}
