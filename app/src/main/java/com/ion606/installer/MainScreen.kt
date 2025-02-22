package com.ion606.installer.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.ion606.installer.components.*
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