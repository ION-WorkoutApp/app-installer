package com.ion606.installer.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

fun installApk(context: Context, apkFile: File) {
    val contentUri = FileProvider.getUriForFile(
        context, "${context.packageName}.provider", apkFile
    )

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(installIntent)
}

@RequiresApi(Build.VERSION_CODES.P)
fun verifySignature(context: Context, targetPackageName: String): Boolean {
    return try {
        // Only check signature if app exists
        if (!isAppInstalled(context, targetPackageName)) return true

        val existing =
            context.packageManager.getPackageInfo(targetPackageName, PackageManager.GET_SIGNATURES)
        val current = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_SIGNATURES
        )

        return existing.signingInfo?.apkContentsSigners?.any { sig1 ->
            current.signingInfo?.apkContentsSigners?.any { sig2 ->
                sig1.hashCode() == sig2.hashCode()
            } ?: false
        } ?: false
    } catch (e: Exception) {
        false
    }
}

private fun isAppInstalled(context: Context, targetPackageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(targetPackageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun uninstallExisting(context: Context, packageName: String) {
    try {
        // First verify app exists
        context.packageManager.getPackageInfo(packageName, 0)

        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent)
    } catch (e: PackageManager.NameNotFoundException) {
        throw IOException("Application not installed")
    }
}