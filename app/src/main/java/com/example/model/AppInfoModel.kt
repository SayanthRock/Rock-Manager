package com.example.model

import android.graphics.drawable.Drawable

data class PermissionModel(
    val name: String,
    val isDangerous: Boolean,
    val isGranted: Boolean,
    val group: String = ""
)

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val baseApkPath: String,
    val splitApkPaths: List<String>,
    val permissions: List<PermissionModel>,
    val isSystem: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val totalSize: Long,
    val isLaunchable: Boolean = false
) {
    val sdkLabel: String = AndroidSdkLabels.getName(targetSdkVersion)
    val isSplit: Boolean = splitApkPaths.isNotEmpty()

    companion object {
        fun getSdkName(apiLevel: Int): String {
            return AndroidSdkLabels.getName(apiLevel)
        }
    }
}

data class ApkPngAsset(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val bitmap: android.graphics.Bitmap?
)
