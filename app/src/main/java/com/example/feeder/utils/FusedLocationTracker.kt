package com.example.feeder.utils

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.IOException
import java.util.Locale

class FusedLocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var location: Location? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private val geocoderMaxResults = 1

    companion object {
        const val TAG = "FusedLocationTracker"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // Check if location permissions are granted
    fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Start location updates
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationReceived: (Location) -> Unit) {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // High accuracy for GPS
            10000 // Update interval: 10 seconds
        ).apply {
            setMinUpdateDistanceMeters(0f) // No minimum distance threshold
            setWaitForAccurateLocation(false) // Don't wait for precise GPS
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.locations.forEach { newLocation ->
                    location = newLocation
                    latitude = newLocation.latitude
                    longitude = newLocation.longitude
                    onLocationReceived(newLocation)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationReceived: (Location?) -> Unit) {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Permissions not granted")
            onLocationReceived(null)
            return
        }

        // Request a fresh location fix
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY, // Force GPS usage
            null // Optional cancellation token
        ).addOnSuccessListener { location ->
            location?.let {
                this.location = it
                latitude = it.latitude
                longitude = it.longitude
            }
            onLocationReceived(location)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get fresh location: ${e.message}")
            onLocationReceived(null)
        }
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Stop location updates
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    // Get the last known location
    @SuppressLint("MissingPermission")
    fun getLastLocation(onLocationReceived: (Location?) -> Unit) {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            onLocationReceived(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLocation ->
                location = lastLocation
                lastLocation?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                }
                onLocationReceived(lastLocation)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last location", e)
                onLocationReceived(null)
            }
    }


    // Geocoding methods
    fun getGeocoderAddress(): List<Address>? {
        // Ensure valid coordinates
        if (location == null || latitude == 0.0 || longitude == 0.0) {
            Log.w(TAG, "Invalid coordinates")
            return null
        }

        val geocoder = Geocoder(context, Locale.ENGLISH)
        return try {
            geocoder.getFromLocation(latitude, longitude, geocoderMaxResults)
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed: ${e.message}")
            null
        }
    }

    fun getAddressLine(): String? {
        return getGeocoderAddress()?.get(0)?.getAddressLine(0)
    }

    fun getLocality(): String? {
        return getGeocoderAddress()?.get(0)?.locality
    }

    fun getPostalCode(): String? {
        return getGeocoderAddress()?.get(0)?.postalCode
    }

    fun getCountryName(): String? {
        return getGeocoderAddress()?.get(0)?.countryName
    }

    fun getAddress(lat: Double, lng: Double): String {
        val geocoder = Geocoder(context)
        val list = geocoder.getFromLocation(lat, lng, 1)
        return list?.get(0)!!.getAddressLine(0)
    }

}