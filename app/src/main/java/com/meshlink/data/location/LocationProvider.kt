package com.meshlink.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Offline GPS location provider. Uses the device's GPS hardware directly
 * without any internet or Google Play Services dependency.
 */
@Singleton
class LocationProvider @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "LocationProvider"
    }

    data class DeviceLocation(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val batteryPercent: Int,
        val timestamp: Long
    )

    /**
     * Get the current GPS location. Falls back to last known location
     * if a fresh fix isn't available within timeout.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceLocation? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val batteryPercent = getBatteryPercent()

            // Try to get last known from GPS first, then network
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> {
                    // Request a single fresh GPS fix
                    requestSingleUpdate(locationManager)
                }
            }

            bestLocation?.let {
                DeviceLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    batteryPercent = batteryPercent,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Location fetch failed: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(locationManager: LocationManager): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    @Deprecated("Deprecated in API")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, android.os.Looper.getMainLooper())
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, android.os.Looper.getMainLooper())
                } else {
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    fun getBatteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
