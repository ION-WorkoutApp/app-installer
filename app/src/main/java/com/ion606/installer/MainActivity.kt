package com.ion606.installer

import IONWorkoutAppInstallerTheme
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class PermissionState(
    val hasManageExternalStorage: Boolean,
    val hasInstallPackages: Boolean,
    val hasNotification: Boolean
)

class MainActivity : ComponentActivity() {
    private val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1000
    private val INSTALL_PERMISSION_REQUEST_CODE = 1001
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002

    private var permissionState by mutableStateOf(
        PermissionState(
            hasManageExternalStorage = false,
            hasInstallPackages = false,
            hasNotification = false
        )
    )

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermissionState()

        setContent {
            IONWorkoutAppInstallerTheme {
                if (permissionState.hasManageExternalStorage &&
                    permissionState.hasInstallPackages &&
                    permissionState.hasNotification
                ) {
                    MainScreen()
                } else {
                    PermissionsExplanationScreen(
                        hasManageExternalStorage = permissionState.hasManageExternalStorage,
                        hasInstallPackages = permissionState.hasInstallPackages,
                        hasNotification = permissionState.hasNotification,
                        onRequestManageExternalStorage = { requestManageExternalStoragePermission() },
                        onRequestInstallPackages = { requestInstallPackagesPermission() },
                        onRequestNotification = { requestNotificationPermission() }
                    )
                }
            }
        }
    }

    private fun updatePermissionState() {
        permissionState = PermissionState(
            hasManageExternalStorage = hasManageExternalStoragePermission(),
            hasInstallPackages = hasInstallPackagesPermission(),
            hasNotification = hasNotificationPermission()
        )
    }

    // Check functions
    private fun hasManageExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasInstallPackagesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Request functions
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MANAGE_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }
    }

    private fun requestInstallPackagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MANAGE_EXTERNAL_STORAGE_REQUEST_CODE,
            NOTIFICATION_PERMISSION_REQUEST_CODE -> updatePermissionState()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }
}

@Composable
fun PermissionsExplanationScreen(
    hasManageExternalStorage: Boolean,
    hasInstallPackages: Boolean,
    hasNotification: Boolean,
    onRequestManageExternalStorage: () -> Unit,
    onRequestInstallPackages: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        PermissionSection(
            title = "External File Access",
            description = "The installer requires file access to properly install and update files. Without this, installation may fail.",
            buttonText = if (hasManageExternalStorage) "File Access Granted" else "Grant File Access",
            onClick = onRequestManageExternalStorage,
            isEnabled = !hasManageExternalStorage
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionSection(
            title = "Installation & Notification Permissions",
            description = "The installer requires permission to request package installations and to post notifications to keep you informed about the process.",
            buttonText = if (hasInstallPackages && hasNotification)
                "Permissions Granted"
            else
                "Grant Permissions",
            onClick = {
                onRequestInstallPackages()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    onRequestNotification()
                }
            },
            isEnabled = !(hasInstallPackages && hasNotification)
        )
    }
}

@Composable
private fun PermissionSection(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(buttonText)
            }
        }
    }
}