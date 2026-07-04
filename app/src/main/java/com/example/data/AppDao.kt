package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Extraction Logs
    @Query("SELECT * FROM extraction_logs ORDER BY timestamp DESC")
    fun getAllExtractionLogs(): Flow<List<ExtractionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtractionLog(log: ExtractionLog)

    @Query("DELETE FROM extraction_logs WHERE id = :id")
    suspend fun deleteExtractionLog(id: Long)

    // Background Activities
    @Query("SELECT * FROM background_activities ORDER BY runtimeMinutes DESC")
    fun getAllBackgroundActivities(): Flow<List<AppBackgroundActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackgroundActivity(activity: AppBackgroundActivity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackgroundActivities(activities: List<AppBackgroundActivity>)

    // SDK Change Logs
    @Query("SELECT * FROM sdk_change_logs ORDER BY timestamp DESC")
    fun getAllSdkChangeLogs(): Flow<List<SdkChangeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSdkChangeLog(changeLog: SdkChangeLog)
}
