package org.metucet.redlink.log

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.FileOutputStream
import java.io.IOException

// A log in the phone's shared Documents/redLINK/ folder, made through
// MediaStore, which needs no storage permission. The files stay reachable
// from the Files app and over USB, and nothing here deletes one.
//
// The file is made at the first write and kept open between writes. After a
// failure it is opened again, or made again if it never was, at the next. A
// `resumed` run finds the file it wrote before a kill by name and goes on with
// it. A new run never does: one started in the same second as the last has
// its name, and gets a file of its own.
class MediaStoreLogFile(
    private val context: Context,
    private val name: String,
    private val resumed: Boolean,
) : LogFile, AutoCloseable {
    private val resolver = context.contentResolver
    private val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private var uri: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var stream: FileOutputStream? = null

    override fun size(): Long = guarded {
        if (uri == null && resumed) uri = find()
        if (uri == null) 0 else open().channel.size()
    }

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
        val uri = uri ?: ((if (resumed) find() else null) ?: make()).also { uri = it }
        val descriptor = resolver.openFileDescriptor(uri, "rw") ?: throw IOException("Cannot open $uri")
        this.descriptor = descriptor
        return FileOutputStream(descriptor.fileDescriptor).also { stream = it }
    }

    // The file of this name the app made in the folder, or null before it is
    // made. Making it again would give a second file, which MediaStore names
    // apart from the first.
    private fun find(): Uri? {
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"
        val args = arrayOf(name, "$LOG_FOLDER/", context.packageName)
        resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) return ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
        return null
    }

    private fun make(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, LOG_FOLDER)
        }
        return resolver.insert(collection, values)
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
