package com.zionchat.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.zionchat.app.BuildConfig

object RuntimeShellPlugin {
    private const val fallbackPackageName = "com.zionchat.runtime.core_runtime_shell"
    private const val fallbackDownloadUrl =
        "https://github.com/king0929zion/core/releases/latest/download/runtime-release-unsigned.apk"

    fun packageName(): String {
        return BuildConfig.RUNTIME_SHELL_PLUGIN_PACKAGE.trim().ifBlank { fallbackPackageName }
    }

    fun downloadUrl(): String {
        return BuildConfig.RUNTIME_SHELL_PLUGIN_DOWNLOAD_URL.trim().ifBlank { fallbackDownloadUrl }
    }

    fun isInstalled(context: Context): Boolean {
        val targetPackage = packageName()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    targetPackage,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(targetPackage, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openDownloadPage(context: Context): Boolean {
        val uri = runCatching { Uri.parse(downloadUrl()) }.getOrNull() ?: return false
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrElse { false }
    }
}
