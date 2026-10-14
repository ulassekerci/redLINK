package org.metucet.redlink.board

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// A device found by a scan: its address, and its Bluetooth name where it
// gives one. Once picked, it is the board.
data class Device(val address: String, val name: String?)

// Scans for devices offering the Nordic UART service, with no name filter,
// for as long as the flow is collected. A device is given each time it is
// heard. With Bluetooth off nothing is found.
//
// The Bluetooth grant is in place: the device list scans only with it.
@SuppressLint("MissingPermission")
fun Context.scanForDevices(): Flow<Device> = callbackFlow {
    val scanner = getSystemService(BluetoothManager::class.java).adapter?.bluetoothLeScanner
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            trySend(Device(result.device.address, result.scanRecord?.deviceName ?: result.device.name))
        }
    }
    scanner?.startScan(
        listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(NUS_SERVICE)).build()),
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
        callback,
    )
    // Android refuses to stop a scan once Bluetooth is off, which has
    // stopped it already.
    awaitClose { runCatching { scanner?.stopScan(callback) } }
}
