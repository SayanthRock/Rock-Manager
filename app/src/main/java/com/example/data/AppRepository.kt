package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allExtractionLogs: Flow<List<ExtractionLog>> = appDao.getAllExtractionLogs()
    val allBackgroundActivities: Flow<List<AppBackgroundActivity>> = appDao.getAllBackgroundActivities()
    val allSdkChangeLogs: Flow<List<SdkChangeLog>> = appDao.getAllSdkChangeLogs()

    suspend fun insertExtractionLog(log: ExtractionLog) {
        appDao.insertExtractionLog(log)
    }

    suspend fun deleteExtractionLog(id: Long) {
        appDao.deleteExtractionLog(id)
    }

    suspend fun insertBackgroundActivity(activity: AppBackgroundActivity) {
        appDao.insertBackgroundActivity(activity)
    }

    suspend fun insertBackgroundActivities(activities: List<AppBackgroundActivity>) {
        appDao.insertBackgroundActivities(activities)
    }

    suspend fun insertSdkChangeLog(changeLog: SdkChangeLog) {
        appDao.insertSdkChangeLog(changeLog)
    }
}
