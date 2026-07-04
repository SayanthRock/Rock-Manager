package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extraction_logs")
data class ExtractionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val fileType: String, // APK or ZIP (split)
    val timestamp: Long = System.currentTimeMillis(),
    val isSigned: Boolean = true
)

@Entity(tableName = "background_activities")
data class AppBackgroundActivity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val runtimeMinutes: Long,
    val cpuUsagePercent: Float,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = false
)

@Entity(tableName = "sdk_change_logs")
data class SdkChangeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val changeType: String, // "SDK Level", "Permissions Added", "Background Active"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
