package com.ion606.installer.models

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ion606.installer.GitHubAPI
import com.ion606.installer.components.formatVersion
import com.ion606.installer.util.SemanticVersion
import com.ion606.installer.util.installApk
import com.ion606.installer.util.uninstallExisting
import com.ion606.installer.util.verifySignature
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@SuppressLint("StaticFieldLeak")
@RequiresApi(Build.VERSION_CODES.P)
class InstallerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val targetPackageName = "com.ion606.workoutapp"
    private val _uiState = MutableStateFlow(InstallerUiState(targetPackageName = targetPackageName))
    val uiState: StateFlow<InstallerUiState> = _uiState.asStateFlow()

    init {
        checkInstalledVersion()
        fetchLatestRelease()
    }

    protected fun getUiState() = _uiState.value.copy();

    @RequiresApi(Build.VERSION_CODES.P)
    fun handleInstall() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val downloadUrl = currentState.downloadURL ?: throw Exception("No download URL")

                _uiState.update { it.copy(installProgress = 0f) }

                if (currentState.isInstalled && !verifySignature(context, targetPackageName)) {
                    throw SecurityException("Signature mismatch")
                }

                val apkFile = downloadApk(downloadUrl)
                installApk(context, apkFile)
                checkInstalledVersion() // Refresh installation status

                _uiState.update {
                    it.copy(
                        installProgress = null, errorMessage = null
                    )
                }
            } catch (e: Exception) {
                handleInstallationError(e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkInstalledVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                targetPackageName, PackageManager.GET_ACTIVITIES
            );

            Log.d("installer", "found package: ${packageInfo.versionName}");

            _uiState.update {
                it.copy(
                    isInstalled = true,
                    installedVersion = formatVersion(packageInfo.versionName!!),
                    errorMessage = null
                )
            }

            if (!verifySignature(context, targetPackageName)) {
                throw SecurityException("Signature mismatch with existing installation")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle "not installed" case
            Log.d("installer", "package not found: $targetPackageName");
            _uiState.update { it.copy(isInstalled = false, installedVersion = null) }
        } catch (e: SecurityException) {
            Log.d("installer", "package signature mismatch: $targetPackageName");

            // Handle signature mismatch
            _uiState.update {
                it.copy(
                    errorMessage = "Signature mismatch with installed app",
                    isInstalled = true,
                    updateAvailable = true // I'm treating mismatch as an update
                )
            }
        } catch (e: Exception) {
            // Catch other unexpected errors
            Log.e("Installer", "Detection failed: ${e.message}")
            _uiState.update { it.copy(errorMessage = "Detection error: ${e.message}") }
        }
    }

    private fun fetchLatestRelease() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val releases = GitHubAPI.getLatestRelease()
                releases?.firstOrNull()?.let { release ->
                    val formattedVersion = formatVersion(release.tag_name)
                    Log.d("installer", "latest release: $formattedVersion");

                    _uiState.update {
                        it.copy(
                            latestVersion = formattedVersion,
                            changelog = release.body,
                            downloadURL = release.assets.firstOrNull()?.browser_download_url,
                            updateAvailable = isUpdateAvailable(
                                it.installedVersion, formattedVersion
                            ),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } ?: handleNoReleasesFound()
            } catch (e: Exception) {
                handleFetchError(e)
            }
        }
    }

    private fun isUpdateAvailable(installed: String?, latest: String?): Boolean {
        if (installed == null || latest == null) return false
        return try {
            val current = SemanticVersion.parse(installed)
            val available = SemanticVersion.parse(latest)
            available > current
        } catch (e: Exception) {
            latest != installed
        }
    }

    private suspend fun downloadApk(url: String): File {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to download APK: ${response.code()}")
                }

                val fileName = url.substringAfterLast("/")
                val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw IOException("Failed to access downloads directory")

                val outputFile = File(outputDir, fileName).apply {
                    if (exists()) delete()
                }

                response.body()!!.byteStream().use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        val totalBytes = response.body()!!.contentLength()
                        var downloadedBytes = 0L
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // Update progress state
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            _uiState.update { it.copy(installProgress = progress.coerceIn(0f, 1f)) }
                        }
                    }
                }

                if (!outputFile.exists() || outputFile.length() == 0L) {
                    throw IOException("Downloaded file is empty or corrupted")
                }

                outputFile
            }
        }
    }

    private fun handleInstallationError(e: Exception) {
        val errorMessage = when {
            e.message?.contains("conflicting provider") == true -> "Uninstall existing app first"

            e is PackageManager.NameNotFoundException -> "Installation failed - signature mismatch"

            else -> "Installation failed: ${e.message}"
        }

        // add a button to prompt the user to uninstall
        if (e is PackageManager.NameNotFoundException) {
            uninstallExisting(context, targetPackageName)
        }

        _uiState.update {
            it.copy(
                errorMessage = errorMessage, installProgress = null
            )
        }
    }

    private fun handleNoReleasesFound() {
        _uiState.update {
            it.copy(
                errorMessage = "No releases found", isLoading = false
            )
        }
    }

    private fun handleFetchError(e: Exception) {
        _uiState.update {
            it.copy(
                errorMessage = "Failed to fetch updates: ${e.message}", isLoading = false
            )
        }
    }

    private fun verifyPackageExists(): Boolean {
        return try {
            context.packageManager.getPackageInfo(targetPackageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun handleUninstall() {
        viewModelScope.launch {
            try {
                if (verifyPackageExists()) {
                    uninstallExisting(context, targetPackageName)

                    // Wait and verify uninstall
                    var retries = 0
                    while (retries < 5 && verifyPackageExists()) {
                        delay(500)
                        retries++
                    }

                    checkInstalledVersion()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Uninstall failed: ${e.message}")
                }
            }
        }
    }
}