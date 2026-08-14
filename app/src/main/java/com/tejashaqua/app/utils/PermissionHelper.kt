package com.tejashaqua.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class PermissionType {
    LOCATION,
    CAMERA,
    PHOTOS,
    NOTIFICATIONS
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_REQUIRED
}

object PermissionHelper {

    fun getPermissionsForType(type: PermissionType): List<String> {
        return when (type) {
            PermissionType.LOCATION -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            PermissionType.CAMERA -> listOf(Manifest.permission.CAMERA)
            PermissionType.PHOTOS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            }
        }
    }

    fun getStatus(context: Context, type: PermissionType): PermissionStatus {
        val permissions = getPermissionsForType(type)
        if (permissions.isEmpty()) return PermissionStatus.NOT_REQUIRED

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) return PermissionStatus.GRANTED

        // Check if it's permanently denied. 
        // This requires an Activity to check shouldShowRequestPermissionRationale.
        // We handle the "Permanently Denied" state in the ViewModel/Activity layer
        // because it needs a reference to the Activity.
        return PermissionStatus.DENIED
    }

    fun getHumanReadableName(type: PermissionType): String {
        return when (type) {
            PermissionType.LOCATION -> "Location access"
            PermissionType.CAMERA -> "Camera access"
            PermissionType.PHOTOS -> "Photo access"
            PermissionType.NOTIFICATIONS -> "Notification access"
        }
    }
}
