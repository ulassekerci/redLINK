package org.metucet.redlink.board

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ConnectionPriorityRequest
import no.nordicsemi.android.ble.observer.ConnectionObserver

// The Nordic UART service the board offers, and its two characteristics,
// named from the board's side: it receives on RX and transmits on TX.
internal val NUS_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
private val NUS_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
private val NUS_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

// The largest MTU there is. The board may settle on less.
private const val MTU = 517

// Direct connects tried after the link goes down, before Android's
// autoConnect takes over.
private const val DIRECT_ATTEMPTS = 3

// How long a direct connect waits for the board. autoConnect waits for as
// long as it takes.
private const val DIRECT_TIMEOUT_MS = 5000L

// The least time between the starts of two attempts, so one that fails at
// once, as every one does with Bluetooth off, does not spin.
private const val ATTEMPT_SPACING_MS = 1000L

// The board link's Bluetooth implementation: the real board, at the address
// picked on Setup, reached without scanning. From the moment it is made until
// close() it keeps trying to connect: a direct connect at once whenever the
// link is down, and after a few failures Android's autoConnect, which waits
// for the board to come into range. Bluetooth switched off on the phone is a
// link that is down like any other.
//
// The Bluetooth grant is in place: Start is disabled without it.
@SuppressLint("MissingPermission")
class BluetoothBoard(private val context: Context, address: String) : BoardLink {
    private val _connected = MutableStateFlow(false)

    // Up from the moment the board's notifications are on until the link
    // drops.
    override val connected: StateFlow<Boolean> = _connected

    private val bytes = Channel<ByteArray>(Channel.UNLIMITED)
    override val received = bytes.receiveAsFlow()

    private val manager = UartManager(context) { bytes.trySend(it) }

    // The library calls back on the main thread, and the attempts and their
    // state are kept on it too.
    private val scope = MainScope()

    // The attempt under way, ended with whether the link came up.
    private var attempt: CompletableDeferred<Boolean>? = null

    // Set when Bluetooth comes back on, so the next attempt is a direct one,
    // as after a drop.
    private var bluetoothBackOn = false

    // An autoConnect attempt that is waiting hears nothing from the library
    // when Bluetooth goes off, and Android drops it, so it is ended here.
    private val bluetoothSwitch = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> attempt?.complete(false)
                BluetoothAdapter.STATE_ON -> bluetoothBackOn = true
            }
        }
    }

    init {
        manager.connectionObserver = object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) = Unit
            override fun onDeviceConnected(device: BluetoothDevice) = Unit
            override fun onDeviceDisconnecting(device: BluetoothDevice) = Unit

            // A link whose notifications could not be turned on would be up
            // and silent for good, so it is dropped and tried again.
            override fun onDeviceReady(device: BluetoothDevice) {
                if (!manager.listening) {
                    manager.disconnect().enqueue()
                    return
                }
                _connected.value = true
                attempt?.complete(true)
            }

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                attempt?.complete(false)
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                _connected.value = false
                attempt?.complete(false)
            }
        }
        context.registerReceiver(bluetoothSwitch, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        // A phone with no Bluetooth at all never connects.
        val device = context.getSystemService(BluetoothManager::class.java).adapter?.getRemoteDevice(address)
        if (device != null) scope.launch { keepConnecting(device) }
    }

    private suspend fun keepConnecting(device: BluetoothDevice) {
        var failures = 0
        while (true) {
            val startedAt = SystemClock.elapsedRealtime()
            if (connect(device, autoConnect = failures >= DIRECT_ATTEMPTS)) {
                failures = 0
                // The next attempt, once the link drops, is a direct one at once.
                _connected.first { !it }
            } else {
                failures = if (bluetoothBackOn) 0 else failures + 1
                bluetoothBackOn = false
                delay(startedAt + ATTEMPT_SPACING_MS - SystemClock.elapsedRealtime())
            }
        }
    }

    // One attempt, and whether the link came up. An autoConnect request is
    // done as soon as Android has it, so the link is up only once the board's
    // notifications are on, whichever kind of attempt this is.
    private suspend fun connect(device: BluetoothDevice, autoConnect: Boolean): Boolean {
        // What is left of the last connection is closed first, autoConnect's
        // above all, so this attempt is the only one trying.
        manager.close()
        val attempt = CompletableDeferred<Boolean>().also { attempt = it }
        val request = manager.connect(device).useAutoConnect(autoConnect, false)
        if (!autoConnect) request.timeout(DIRECT_TIMEOUT_MS)
        request.fail { _, _ -> attempt.complete(false) }.enqueue()
        return try {
            attempt.await()
        } finally {
            this.attempt = null
        }
    }

    // A request sent while the link is down goes nowhere.
    override fun send(request: ByteArray) {
        if (_connected.value) manager.write(request)
    }

    override fun close() {
        scope.cancel()
        context.unregisterReceiver(bluetoothSwitch)
        manager.close()
        bytes.close()
    }

    // The board's UART service as the library reaches it.
    private class UartManager(context: Context, private val onBytes: (ByteArray) -> Unit) : BleManager(context) {
        // Written on the library's thread, read on the poller's.
        @Volatile private var rx: BluetoothGattCharacteristic? = null
        @Volatile private var tx: BluetoothGattCharacteristic? = null

        // Whether the board's notifications were turned on in this connection.
        @Volatile var listening = false
            private set

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(NUS_SERVICE) ?: return false
            rx = service.getCharacteristic(NUS_RX)
            tx = service.getCharacteristic(NUS_TX)
            return rx != null && tx.let { it != null && it.properties and PROPERTY_NOTIFY != 0 }
        }

        override fun initialize() {
            listening = false
            requestMtu(MTU).enqueue()
            // The shortest connection interval, for two requests and replies
            // every 50 ms.
            requestConnectionPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH).enqueue()
            setNotificationCallback(tx).with { _, data -> data.value?.let(onBytes) }
            enableNotifications(tx).done { listening = true }.enqueue()
        }

        override fun onServicesInvalidated() {
            rx = null
            tx = null
            listening = false
        }

        fun write(frame: ByteArray) {
            val rx = rx ?: return
            val type = if (rx.properties and PROPERTY_WRITE_NO_RESPONSE != 0) WRITE_TYPE_NO_RESPONSE else WRITE_TYPE_DEFAULT
            // A frame longer than the MTU allows goes in pieces.
            writeCharacteristic(rx, frame, type).split().enqueue()
        }
    }
}
