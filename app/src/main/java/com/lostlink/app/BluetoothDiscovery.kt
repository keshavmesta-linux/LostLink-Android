
package com.lostlink.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

class BluetoothDiscovery(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID =
            UUID.fromString("8a7b6c5d-4e3f-4210-9a8b-7c6d5e4f3210")
    }

    private val manager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = manager.adapter

    private var activeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var activeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    fun isSupported(): Boolean = adapter != null

    fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun startAdvertising(onStatus: (String) -> Unit) {
        if (!hasPermissions()) {
            onStatus("Bluetooth permissions are required.")
            return
        }

        try {
            val bluetoothAdapter = adapter
            if (bluetoothAdapter == null) {
                onStatus("Bluetooth is not supported.")
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                onStatus("Turn on Bluetooth in Android settings.")
                return
            }

            val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (advertiser == null) {
                onStatus("BLE advertising is unavailable on this phone.")
                return
            }

            stopAdvertising()

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(false)
                .build()

            val data = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .setIncludeDeviceName(false)
                .build()

            val callback = object : AdvertiseCallback() {
                override fun onStartSuccess(
                    settingsInEffect: AdvertiseSettings?
                ) {
                    onStatus("LostLink is discoverable nearby.")
                }

                override fun onStartFailure(errorCode: Int) {
                    activeAdvertiser = null
                    advertiseCallback = null
                    onStatus("Advertising failed (code $errorCode).")
                }
            }

            activeAdvertiser = advertiser
            advertiseCallback = callback
            advertiser.startAdvertising(settings, data, callback)
        } catch (_: SecurityException) {
            onStatus("Bluetooth permission was denied.")
        } catch (_: IllegalStateException) {
            onStatus("Bluetooth is not ready. Try again.")
        }
    }

    fun startScanning(
        onFound: (String) -> Unit,
        onStatus: (String) -> Unit
    ) {
        if (!hasPermissions()) {
            onStatus("Bluetooth permissions are required.")
            return
        }

        try {
            val bluetoothAdapter = adapter
            if (bluetoothAdapter == null) {
                onStatus("Bluetooth is not supported.")
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                onStatus("Turn on Bluetooth in Android settings.")
                return
            }

            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                onStatus("BLE scanning is unavailable.")
                return
            }

            stopScanning()

            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val callback = object : ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult
                ) {
                    onFound("A LostLink signal was detected nearby.")
                }

                override fun onScanFailed(errorCode: Int) {
                    activeScanner = null
                    scanCallback = null
                    onStatus("Scanning failed (code $errorCode).")
                }
            }

            activeScanner = scanner
            scanCallback = callback
            scanner.startScan(filters, settings, callback)
            onStatus("Searching for nearby LostLink phones…")
        } catch (_: SecurityException) {
            onStatus("Bluetooth permission was denied.")
        } catch (_: IllegalStateException) {
            onStatus("Bluetooth is not ready. Try again.")
        }
    }

    fun stopScanning() {
        try {
            val scanner = activeScanner
            val callback = scanCallback
            if (scanner != null && callback != null) {
                scanner.stopScan(callback)
            }
        } catch (_: SecurityException) {
            // Permission may have been revoked.
        } finally {
            activeScanner = null
            scanCallback = null
        }
    }

    fun stopAdvertising() {
        try {
            val advertiser = activeAdvertiser
            val callback = advertiseCallback
            if (advertiser != null && callback != null) {
                advertiser.stopAdvertising(callback)
            }
        } catch (_: SecurityException) {
            // Permission may have been revoked.
        } finally {
            activeAdvertiser = null
            advertiseCallback = null
        }
    }

    fun stop() {
        stopScanning()
        stopAdvertising()
    }
}
