
package com.lostlink.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Purple = Color(0xFF6545C1)
private val DarkPurple = Color(0xFF30205D)
private val PalePurple = Color(0xFFF2EEFF)
private val PageBg = Color(0xFFF8F7FC)
private val Muted = Color(0xFF777387)

class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager
    private var activeListener: LocationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val prefs = getSharedPreferences("lostlink", MODE_PRIVATE)

        setContent {
            val context = LocalContext.current

            var status by remember {
                mutableStateOf("Ready when you are")
            }

            var history by remember {
                mutableStateOf(loadHistory(prefs))
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val allowed =
                        result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                    if (allowed) {
                        recordLocation(
                            prefs,
                            onStatus = { status = it },
                            onHistory = { history = it }
                        )
                    } else {
                        status = "Location permission is required."
                    }
                }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Purple,
                    onPrimary = Color.White,
                    background = PageBg,
                    surface = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PageBg)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 28.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            Text(
                                "LostLink",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkPurple
                            )
                            Text(
                                "OFFLINE PHONE RECOVERY",
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = Muted
                            )
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkPurple
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "YOUR DEVICE",
                                        color = Color(0xFFD8CCFF),
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        "Offline GPS logger",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        status,
                                        color = Color(0xFFE5DFFF),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    val coarse = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (fine || coarse) {
                                        recordLocation(
                                            prefs,
                                            { status = it },
                                            { history = it }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    "Record my location",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            Text(
                                "Latest location",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkPurple
                            )
                        }

                        item {
                            val latest = history.firstOrNull()

                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {
                                    if (latest == null) {
                                        Text(
                                            "No location recorded yet.",
                                            color = Muted
                                        )
                                    } else {
                                        Text(
                                            "GPS coordinates",
                                            color = Muted,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Latitude",
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            latest.latitude.toString(),
                                            color = DarkPurple,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Longitude",
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            latest.longitude.toString(),
                                            color = DarkPurple,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        HorizontalDivider(color = PageBg)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement =
                                                Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    "Accuracy",
                                                    color = Muted,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    "${latest.accuracy} m",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DarkPurple
                                                )
                                            }
                                            Column {
                                                Text(
                                                    "Recorded",
                                                    color = Muted,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    latest.time,
                                                    fontSize = 12.sp,
                                                    color = DarkPurple
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard =
                                                    context.getSystemService(
                                                        Context.CLIPBOARD_SERVICE
                                                    ) as ClipboardManager

                                                clipboard.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        "LostLink coordinates",
                                                        "${latest.latitude}, ${latest.longitude}"
                                                    )
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Coordinates copied",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Copy coordinates")
                                        }
                                        Button(
                                            onClick = {
                                                val uri = Uri.parse(
                                                    "geo:${latest.latitude}," +
                                                        "${latest.longitude}?q=" +
                                                        "${latest.latitude}," +
                                                        "${latest.longitude}"
                                                )
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        uri
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Open in Maps")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Recent history",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPurple
                                )
                                Text(
                                    "${history.size} saved",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (history.isEmpty()) {
                            item {
                                Text(
                                    "Your saved readings will appear here.",
                                    color = Muted
                                )
                            }
                        } else {
                            items(history.drop(1)) { entry ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement =
                                            Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "${entry.latitude}, ${entry.longitude}",
                                            color = DarkPurple,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${entry.time} • ±${entry.accuracy} m",
                                            color = Muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = {
                                        prefs.edit()
                                            .remove("location_history")
                                            .remove("last_location")
                                            .apply()
                                        history = emptyList()
                                        status = "Local history cleared."
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Clear location history")
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PalePurple
                            ) {
                                Text(
                                    "Privacy first: locations stay on this " +
                                        "device. They are not uploaded or " +
                                        "shared automatically.",
                                    modifier = Modifier.padding(14.dp),
                                    color = DarkPurple,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private data class LocationEntry(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val time: String
    )

    private fun loadHistory(
        prefs: android.content.SharedPreferences
    ): List<LocationEntry> {
        val json = prefs.getString("location_history", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                LocationEntry(
                    obj.getDouble("latitude"),
                    obj.getDouble("longitude"),
                    obj.getDouble("accuracy").toFloat(),
                    obj.getString("time")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun recordLocation(
        prefs: android.content.SharedPreferences,
        onStatus: (String) -> Unit,
        onHistory: (List<LocationEntry>) -> Unit
    ) {
        if (activeListener != null) {
            onStatus("Already waiting for a GPS reading.")
            return
        }

        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            onStatus("Grant location permission first.")
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onStatus("Turn on Location/GPS in Android settings.")
            return
        }

        onStatus("Waiting for GPS. Try outdoors.")

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                )
                val entry = LocationEntry(
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    formatter.format(Date(location.time))
                )

                val oldHistory = loadHistory(prefs)
                val updated = (listOf(entry) + oldHistory).take(50)

                val array = JSONArray()
                updated.forEach { item ->
                    array.put(
                        JSONObject().apply {
                            put("latitude", item.latitude)
                            put("longitude", item.longitude)
                            put("accuracy", item.accuracy.toDouble())
                            put("time", item.time)
                        }
                    )
                }

                prefs.edit()
                    .putString("location_history", array.toString())
                    .putString(
                        "last_location",
                        "Latitude: ${entry.latitude}\n" +
                            "Longitude: ${entry.longitude}\n" +
                            "Accuracy: ${entry.accuracy} metres\n" +
                            "Time: ${entry.time}"
                    )
                    .apply()

                onHistory(updated)
                onStatus("Location saved locally.")
                locationManager.removeUpdates(this)
                activeListener = null
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

        activeListener = listener

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            activeListener = null
            onStatus("Location permission is required.")
        } catch (e: Exception) {
            activeListener = null
            onStatus("Could not start GPS: ${e.message}")
        }
    }

    override fun onDestroy() {
        activeListener?.let { locationManager.removeUpdates(it) }
        activeListener = null
        super.onDestroy()
    }
}

