package com.ion606.installer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ion606.installer.components.AppHeader
import com.ion606.installer.components.ChangelogSection
import com.ion606.installer.components.ErrorMessage
import com.ion606.installer.components.InstallSection
import com.ion606.installer.components.VersionInfoSection
import com.ion606.installer.models.InstallerViewModel

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MainScreen() {
    val viewModel: InstallerViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AppHeader() }
        item { VersionInfoSection(uiState) }
        item { ChangelogSection(uiState) }
        item { InstallSection(uiState, viewModel) }
        if (uiState.errorMessage != null) {
            item { ErrorMessage(uiState.errorMessage!!) }
        }
    }
}