package org.metucet.redlink.log

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore

// Where every run's log goes, relative to shared storage.
internal val LOG_FOLDER = "${Environment.DIRECTORY_DOCUMENTS}/redLINK"

// How much of the end of a log is read for its last row: several rows.
private const val TAIL_BYTES = 4096L

// The logs in Documents/redLINK/ that the app owns. After a reinstall the app
// no longer owns earlier files, so they are not listed and nothing here can
// touch them; they stay reachable from the Files app and over USB.
//
// Every call reaches storage, so each is made off the main thread.
class LogFolder(private val context: Context) {
    private val resolver = context.contentResolver
    private val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    fun list(): List<SavedLog> {
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"
        val args = arrayOf("$LOG_FOLDER/", context.packageName)
        val logs = mutableListOf<SavedLog>()
        resolver.query(collection, columns, selection, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                logs += read(id = cursor.getLong(0), name = cursor.getString(1), knownBytes = cursor.getLong(2))
            }
        }
        return newestFirst(logs)
    }

    // MediaStore's size lags behind a file that is being written, so the size
    // is the file's own. A file that cannot be read is still listed, so it
    // can be deleted, with what MediaStore knows of it.
    private fun read(id: Long, name: String, knownBytes: Long): SavedLog = runCatching {
        val descriptor = resolver.openFileDescriptor(uri(id), "r") ?: error("Cannot open $name")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            val bytes = stream.channel.size()
            stream.channel.position((bytes - TAIL_BYTES).coerceAtLeast(0))
            SavedLog(id, name, bytes, durationSeconds(stream.readBytes().toString(Charsets.UTF_8)))
        }
    }.getOrDefault(SavedLog(id, name, knownBytes, durationS = 0))

    // Removes the files from the folder. One that is gone already is passed
    // over.
    fun delete(logs: List<SavedLog>) {
        for (log in logs) runCatching { resolver.delete(uri(log.id), null, null) }
    }

    // The share sheet for the logs as CSV files, with leave for the app picked
    // to read them.
    fun share(logs: List<SavedLog>): Intent {
        val uris = logs.map { uri(it.id) }
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        send.type = "text/csv"
        send.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null)
    }

    private fun uri(id: Long): Uri = ContentUris.withAppendedId(collection, id)
}
