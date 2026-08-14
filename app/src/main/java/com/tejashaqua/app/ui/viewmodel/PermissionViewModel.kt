package com.tejashaqua.app.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.tejashaqua.app.utils.PermissionHelper
import com.tejashaqua.app.utils.PermissionStatus
import com.tejashaqua.app.utils.PermissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val _permissionStates = MutableStateFlow<Map<PermissionType, PermissionStatus>>(emptyMap())
    val permissionStates: StateFlow<Map<PermissionType, PermissionStatus>> = _permissionStates.asStateFlow()

    private val _visiblePermissionRationale = MutableStateFlow<PermissionType?>(null)
    val visiblePermissionRationale = _visiblePermissionRationale.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow<PermissionType?>(null)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    private val _requestPermissionTrigger = MutableStateFlow<PermissionType?>(null)
    val requestPermissionTrigger = _requestPermissionTrigger.asStateFlow()

    // Feature we are currently requesting permissions for
    private var pendingFeatureAction: (() -> Unit)? = null
    private var currentRequiredPermissions: List<PermissionType> = emptyList()
    private var mustGrantAll: Boolean = true

    // To prevent nagging for non-critical permissions like notifications
    private val sessionDeniedPermissions = mutableSetOf<PermissionType>()

    fun updatePermissionStates() {
        val context = getApplication<Application>()
        val newState = PermissionType.entries.associateWith { type ->
            PermissionHelper.getStatus(context, type)
        }
        _permissionStates.value = newState
    }

    /**
     * Call this from Activity to refine the state (detect Permanent Denial)
     */
    fun refinePermissionStates(activity: Activity) {
        val context = getApplication<Application>()
        val newState = PermissionType.entries.associateWith { type ->
            val status = PermissionHelper.getStatus(context, type)
            if (status == PermissionStatus.DENIED) {
                val permissions = PermissionHelper.getPermissionsForType(type)
                val shouldShowRationale = permissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
                
                if (!shouldShowRationale) {
                    // This could be permanently denied OR never asked.
                    // We check this in handlePermissionResult.
                }
                status
            } else {
                status
            }
        }
        _permissionStates.value = newState
    }

    fun requestFeaturePermissions(
        permissions: List<PermissionType>,
        skipNagging: Boolean = false,
        mustGrantAll: Boolean = true,
        onHandled: () -> Unit
    ) {
        this.mustGrantAll = mustGrantAll
        currentRequiredPermissions = if (skipNagging) {
            // Filter out permissions that were denied in this session
            permissions.filter { !sessionDeniedPermissions.contains(it) }
        } else {
            permissions
        }
        
        pendingFeatureAction = onHandled
        checkAndRequestNextMissingPermission()
    }

    private fun checkAndRequestNextMissingPermission() {
        val context = getApplication<Application>()
        val missing = currentRequiredPermissions.firstOrNull {
            PermissionHelper.getStatus(context, it) == PermissionStatus.DENIED && 
            !sessionDeniedPermissions.contains(it)
        }

        if (missing != null) {
            _visiblePermissionRationale.value = missing
        } else {
            // All granted or session-denied for this feature!
            _visiblePermissionRationale.value = null
            _requestPermissionTrigger.value = null
            pendingFeatureAction?.invoke()
            pendingFeatureAction = null
        }
    }

    fun onRationaleConfirm() {
        val type = _visiblePermissionRationale.value ?: return
        _visiblePermissionRationale.value = null
        _requestPermissionTrigger.value = type
    }

    fun onRationaleDismiss() {
        val type = _visiblePermissionRationale.value
        if (type != null) {
            sessionDeniedPermissions.add(type)
        }
        _visiblePermissionRationale.value = null
        if (!mustGrantAll) {
            checkAndRequestNextMissingPermission()
        } else {
            // If they dismiss a required rationale, we can't proceed with the feature.
            pendingFeatureAction = null
        }
    }

    fun handlePermissionResult(type: PermissionType, activity: Activity) {
        _requestPermissionTrigger.value = null
        val context = getApplication<Application>()
        val status = PermissionHelper.getStatus(context, type)
        
        if (status == PermissionStatus.GRANTED) {
            sessionDeniedPermissions.remove(type)
            checkAndRequestNextMissingPermission()
        } else {
            sessionDeniedPermissions.add(type)
            // Check if it's permanently denied now
            val permissions = PermissionHelper.getPermissionsForType(type)
            val shouldShowRationale = permissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            
            if (!shouldShowRationale) {
                // Permanently denied
                _showSettingsDialog.value = type
            } else if (!mustGrantAll) {
                // Not all must be granted, so move to next instead of stopping
                checkAndRequestNextMissingPermission()
            } else {
                // Just denied, and mustGrantAll is true, so stop
                pendingFeatureAction = null
            }
        }
        updatePermissionStates()
    }

    fun dismissSettingsDialog() {
        val type = _showSettingsDialog.value
        if (type != null) {
            sessionDeniedPermissions.add(type)
        }
        _showSettingsDialog.value = null
        if (!mustGrantAll) {
            checkAndRequestNextMissingPermission()
        } else {
            pendingFeatureAction = null
        }
    }
}
