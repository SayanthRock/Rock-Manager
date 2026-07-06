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
    val sdkLabel: String = getSdkName(targetSdkVersion)
    val isSplit: Boolean = splitApkPaths.isNotEmpty()

    companion object {
        fun getSdkName(apiLevel: Int): String {
            return when (apiLevel) {
                37 -> "Android 17"
                36 -> "Baklava / 16"
                35 -> "V / 15"
                34 -> "U / 14"
                33 -> "T / 13"
                32 -> "S_V2 / 12L"
                31 -> "S / 12"
                30 -> "R / 11"
                29 -> "Q / 10"
                28 -> "Pie / 9"
                26, 27 -> "Oreo / 8"
                else -> "Lollipop+"
            }
        }
    }
}

data class ApkPngAsset(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val bitmap: android.graphics.Bitmap?
)
