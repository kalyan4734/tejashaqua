package com.tejashaqua.app.utils

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

object LocationUtils {

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun checkLocationSettings(
        activity: Activity,
        onEnabled: () -> Unit,
        onError: (ResolvableApiException) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setWaitForAccurateLocation(false)
                .build()

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)

            val client: SettingsClient = LocationServices.getSettingsClient(activity)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                onEnabled()
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onError(exception)
                } else {
                    onFailure(exception)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationUtils", "Error checking location settings", e)
            onFailure(e)
        }
    }
}
