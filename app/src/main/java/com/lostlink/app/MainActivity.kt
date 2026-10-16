
package com.lostlink.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val prefs = getSharedPreferences("lostlink", MODE_PRIVATE)

        setContent {
            var message by remember {
                mutableStateOf("Ready to record an offline location.")
            }

            var savedLocation by remember {
                mutableStateOf(
                    prefs.getString("last_location", "") ?: ""
                )
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val granted =
                        results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                    if (granted) {
                        startLocationUpdates(
                            prefs,
                            { message = it },
                            { savedLocation = it }
                        )
                    } else {
                        message = "Location permission was not granted."
                    }
                }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "LostLink",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            "Offline-first phone recovery",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(message)

                        Button(
                            onClick = {
                                val fine =
                                    ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                val coarse =
                                    ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                if (fine || coarse) {
                                    startLocationUpdates(
                                        prefs,
                                        { message = it },
                                        { savedLocation = it }
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record my location")
                        }

                        Text("Last saved location")

                        Text(
                            savedLocation.ifBlank {
                                "No location saved yet."
                            }
                        )

                        Text(
                            "Saved on this device only. " +
                            "Not sent to another phone."
                        )
                    }
                }
            }
        }
    }

    private fun startLocationUpdates(
        prefs: android.content.SharedPreferences,
        onStatus: (String) -> Unit,
        onSaved: (String) -> Unit
    ) {
        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            onStatus("Grant location permission first.")
            return
        }

        if (!locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        ) {
            onStatus("Turn on Location/GPS in Android settings.")
            return
        }

        onStatus("Waiting for GPS. Try outdoors under open sky.")

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date(location.time))

                val record = """
                    Latitude: ${location.latitude}
                    Longitude: ${location.longitude}
                    Accuracy: ${location.accuracy} metres
                    Time: $timestamp
                """.trimIndent()

                prefs.edit()
                    .putString("last_location", record)
                    .apply()

                onSaved(record)
                onStatus("Location saved locally.")

                locationManager.removeUpdates(this)
            }

            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                onStatus("GPS was disabled.")
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            onStatus("Location permission is required.")
        } catch (e: Exception) {
            onStatus("Could not start GPS: ${e.message}")
        }
    }
}
