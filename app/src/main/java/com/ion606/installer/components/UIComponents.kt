package com.ion606.installer.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ion606.installer.models.InstallerUiState
import com.ion606.installer.models.InstallerViewModel

@Composable
fun AppHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.FileDownload, contentDescription = "Download")
        Spacer(Modifier.width(16.dp))
        Text(
            text = "ION Workout Installer", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun VersionInfoSection(uiState: InstallerUiState) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Version Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            VersionInfoRow("Installed Version", uiState.installedVersion ?: "Not installed")
            VersionInfoRow("Latest Version",
                uiState.latestVersion?.let { formatVersion(it) } ?: "Checking...",
                uiState.updateAvailable)
        }
    }
}

@Composable
fun VersionInfoRow(label: String, value: String, isUpdateAvailable: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isEmpty()) {
                Text(
                    "Not installed",
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    value, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary
                )
                if (isUpdateAvailable) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.NewReleases,
                        "Update available",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChangelogSection(uiState: InstallerUiState) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(
                "What's New",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            if (uiState.isLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            else Text(
                uiState.changelog ?: "No changelog available", fontSize = 14.sp, lineHeight = 18.sp
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun InstallSection(uiState: InstallerUiState, viewModel: InstallerViewModel) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        val buttonText = when {
            uiState.installProgress != null -> "${(uiState.installProgress * 100).toInt()}%"
            uiState.updateAvailable -> "Update Now"
            uiState.isInstalled -> "Reinstall"
            else -> "Install Now"
        }

        Log.d("AAAAAAAAAAAAAAA", "$uiState")

        Button(
            onClick = { viewModel.handleInstall() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.installProgress == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (uiState.installProgress != null) {
                CircularProgressIndicator(
                    progress = uiState.installProgress,
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // Add uninstall button
        if (uiState.isInstalled) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.handleUninstall()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Uninstall App")
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

fun formatVersion(version: String): String {
    return version.replace("^v".toRegex(), "").trim()
}