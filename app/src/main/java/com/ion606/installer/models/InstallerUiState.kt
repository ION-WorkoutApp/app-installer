package com.ion606.installer.models

data class InstallerUiState(
    val installedVersion: String? = null,
    val latestVersion: String? = null,
    val changelog: String? = null,
    val isLoading: Boolean = false,
    val installProgress: Float? = null,
    val isInstalled: Boolean = false,
    val updateAvailable: Boolean = false,
    val errorMessage: String? = null,
    val downloadURL: String? = null,
    val showUninstallWarning: Boolean = false,
    val targetPackageName: String
)