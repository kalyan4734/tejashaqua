package com.tejashaqua.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.PermissionType

@Composable
fun PermissionRationaleDialog(
    type: PermissionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when (type) {
        PermissionType.LOCATION -> stringResource(R.string.location_rationale)
        PermissionType.CAMERA -> stringResource(R.string.camera_rationale)
        PermissionType.PHOTOS -> stringResource(R.string.photo_rationale)
        PermissionType.NOTIFICATIONS -> stringResource(R.string.notification_rationale)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_required_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AppLaunchPermissionsDialog(
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // App launch mandatory check, don't allow dismiss without action
        title = { Text(stringResource(R.string.permission_required_title)) },
        text = { Text(stringResource(R.string.permission_required_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
fun SettingsRedirectDialog(
    type: PermissionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val message = when (type) {
        PermissionType.LOCATION -> stringResource(R.string.location_settings_desc)
        PermissionType.CAMERA -> stringResource(R.string.camera_settings_desc)
        PermissionType.PHOTOS -> stringResource(R.string.storage_settings_desc)
        PermissionType.NOTIFICATIONS -> stringResource(R.string.notification_settings_desc)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_required_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
