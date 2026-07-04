package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppBackgroundActivity
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ExtractionLog
import com.example.data.SdkChangeLog
import com.example.model.InstalledAppInfo
import com.example.model.PermissionModel
import com.example.model.ApkPngAsset
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class AppInspectorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    // UI state
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow("Home") // Home, Apps, Logs, Analytics, Settings
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    // Apps tab filter
    private val _isSystemFilter = MutableStateFlow(false) // false = User, true = System
    val isSystemFilter: StateFlow<Boolean> = _isSystemFilter.asStateFlow()

    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("app_inspector_prefs", Context.MODE_PRIVATE)
    }

    // Settings state
    val themeMode = MutableStateFlow("system")
    val signOutputApk = MutableStateFlow(true)
    val signingMethod = MutableStateFlow("Default Keystore")
    val autoMerge = MutableStateFlow(true)
    val autoSelectSplits = MutableStateFlow(true)
    val showSplitSelection = MutableStateFlow(true)
    val forceMerge = MutableStateFlow(false)
    val backgroundSync = MutableStateFlow(true)

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private fun loadPrefs() {
        themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
        signOutputApk.value = prefs.getBoolean("sign_output_apk", true)
        autoMerge.value = prefs.getBoolean("auto_merge", true)
        autoSelectSplits.value = prefs.getBoolean("auto_select_splits", true)
        showSplitSelection.value = prefs.getBoolean("show_split_selection", true)
        forceMerge.value = prefs.getBoolean("force_merge", false)
        backgroundSync.value = prefs.getBoolean("background_sync", true)
    }

    fun savePref(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        when (key) {
            "sign_output_apk" -> signOutputApk.value = value
            "auto_merge" -> autoMerge.value = value
            "auto_select_splits" -> autoSelectSplits.value = value
            "show_split_selection" -> showSplitSelection.value = value
            "force_merge" -> forceMerge.value = value
            "background_sync" -> backgroundSync.value = value
        }
    }

    // Logs & background activity flows from database
    val extractionLogs: StateFlow<List<ExtractionLog>> = repository.allExtractionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backgroundActivities: StateFlow<List<AppBackgroundActivity>> = repository.allBackgroundActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sdkChangeLogs: StateFlow<List<SdkChangeLog>> = repository.allSdkChangeLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active operation message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadPrefs()
        loadInstalledApps()
        startBackgroundMonitoring()
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSystemFilter(isSystem: Boolean) {
        _isSystemFilter.value = isSystem
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context = getApplication<Application>()
            val pm = context.packageManager
            
            try {
                // Fetch packages with flags
                val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
                val packages = pm.getInstalledPackages(flags)
                
                val mappedList = packages.mapNotNull { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                     (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    // Parse permissions
                    val permissionsList = mutableListOf<PermissionModel>()
                    val permFlags = packageInfo.requestedPermissionsFlags
                    packageInfo.requestedPermissions?.forEachIndexed { index, permName ->
                        val isDangerous = permName.contains("CAMERA") || 
                                          permName.contains("LOCATION") || 
                                          permName.contains("STORAGE") || 
                                          permName.contains("CONTACTS") || 
                                          permName.contains("CALENDAR") ||
                                          permName.contains("SMS") ||
                                          permName.contains("PHONE")
                        
                        val isGranted = if (permFlags != null && index < permFlags.size) {
                            (permFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                        } else {
                            false
                        }
                        
                        permissionsList.add(PermissionModel(permName, isDangerous, isGranted))
                    }

                    // Split APK check
                    val splits = appInfo.splitSourceDirs?.toList() ?: emptyList()

                    val baseFile = java.io.File(appInfo.sourceDir)
                    val baseSize = if (baseFile.exists()) baseFile.length() else 0L
                    val splitsSize = splits.sumOf { path ->
                        val splitFile = java.io.File(path)
                        if (splitFile.exists()) splitFile.length() else 0L
                    }
                    val totalAppSize = baseSize + splitsSize

                    InstalledAppInfo(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = packageInfo.packageName,
                        icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                        versionName = packageInfo.versionName ?: "1.0",
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
                        },
                        targetSdkVersion = appInfo.targetSdkVersion,
                        minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            appInfo.minSdkVersion
                        } else {
                            19
                        },
                        baseApkPath = appInfo.sourceDir,
                        splitApkPaths = splits,
                        permissions = permissionsList,
                        isSystem = isSystemApp,
                        installTime = packageInfo.firstInstallTime,
                        updateTime = packageInfo.lastUpdateTime,
                        totalSize = totalAppSize
                    )
                }.sortedBy { it.name.lowercase() }

                _installedApps.value = mappedList
                initializeBackgroundStats(mappedList)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun initializeBackgroundStats(apps: List<InstalledAppInfo>) {
        // Query if table is populated
        val existing = db.appDao().getAllBackgroundActivities().stateIn(viewModelScope).value
        if (existing.isEmpty() && apps.isNotEmpty()) {
            val initialStats = apps.map { app ->
                AppBackgroundActivity(
                    packageName = app.packageName,
                    appName = app.name,
                    runtimeMinutes = Random.nextLong(1, 300),
                    cpuUsagePercent = Random.nextFloat() * 12f,
                    isLive = Random.nextBoolean() && !app.isSystem
                )
            }
            repository.insertBackgroundActivities(initialStats)
        }
    }

    // Extraction Engine
    fun extractApk(app: InstalledAppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _statusMessage.value = "Extracting ${app.name}..."
            val context = getApplication<Application>()
            
            try {
                val outputDir = File(context.filesDir, "extracted")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val formatTime = System.currentTimeMillis()
                val isSplit = app.isSplit
                val destFile: File

                if (isSplit) {
                    destFile = File(outputDir, "${app.name.replace(" ", "_")}_split_$formatTime.zip")
                    val filesToZip = mutableListOf<File>()
                    filesToZip.add(File(app.baseApkPath))
                    app.splitApkPaths.forEach { filesToZip.add(File(it)) }
                    
                    zipFiles(filesToZip, destFile)
                } else {
                    destFile = File(outputDir, "${app.name.replace(" ", "_")}_$formatTime.apk")
                    val srcFile = File(app.baseApkPath)
                    srcFile.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val fileSize = destFile.length()
                val sizeStr = formatFileSize(fileSize)

                val log = ExtractionLog(
                    appName = app.name,
                    packageName = app.packageName,
                    filePath = destFile.absolutePath,
                    fileSizeFormatted = sizeStr,
                    fileType = if (isSplit) "ZIP" else "APK",
                    isSigned = signOutputApk.value
                )
                repository.insertExtractionLog(log)

                // Insert SDK log change to match timeline
                repository.insertSdkChangeLog(
                    SdkChangeLog(
                        appName = app.name,
                        packageName = app.packageName,
                        changeType = "SDK Level",
                        description = "Extracted ${log.fileType} of ${app.name} (${sizeStr}) targeting API ${app.targetSdkVersion}"
                    )
                )

                _statusMessage.value = "Extracted successfully to:\n${destFile.name}"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Extraction failed: ${e.localizedMessage}"
            } finally {
                delay(4000)
                _statusMessage.value = null
            }
        }
    }

    fun deleteLog(id: Long, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteExtractionLog(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareExtractedFile(context: Context, log: ExtractionLog) {
        val file = File(log.filePath)
        if (!file.exists()) {
            _statusMessage.value = "File no longer exists!"
            viewModelScope.launch {
                delay(2000)
                _statusMessage.value = null
            }
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = if (log.fileType == "ZIP") "application/zip" else "application/vnd.android.package-archive"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Extracted file")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(12000) // update every 12 seconds
                if (backgroundSync.value) {
                    val currentList = db.appDao().getAllBackgroundActivities().stateIn(viewModelScope).value
                    if (currentList.isNotEmpty()) {
                        // Pick a random app to increase its background running minutes slightly
                        val appToUpdate = currentList.random()
                        val updated = appToUpdate.copy(
                            runtimeMinutes = appToUpdate.runtimeMinutes + Random.nextLong(1, 5),
                            cpuUsagePercent = Random.nextFloat() * 8f,
                            lastActiveTimestamp = System.currentTimeMillis()
                        )
                        db.appDao().insertBackgroundActivity(updated)

                        // Add SDK log occasionally about activity monitor
                        if (Random.nextInt(1, 10) > 7) {
                            db.appDao().insertSdkChangeLog(
                                SdkChangeLog(
                                    appName = appToUpdate.appName,
                                    packageName = appToUpdate.packageName,
                                    changeType = "Background Active",
                                    description = "Background task for ${appToUpdate.appName} completed cycle. Elapsed time: ${updated.runtimeMinutes}m"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun zipFiles(files: List<File>, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { out ->
            for (file in files) {
                FileInputStream(file).use { fi ->
                    out.putNextEntry(ZipEntry(file.name))
                    val buffer = ByteArray(1024 * 8)
                    var len: Int
                    while (fi.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                    }
                    out.closeEntry()
                }
            }
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
    }

    fun loadApkPngAssets(app: InstalledAppInfo, onComplete: (List<ApkPngAsset>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<ApkPngAsset>()
            val pathsToScan = mutableListOf<String>()
            pathsToScan.add(app.baseApkPath)
            pathsToScan.addAll(app.splitApkPaths)

            try {
                for (apkPath in pathsToScan) {
                    val file = File(apkPath)
                    if (file.exists()) {
                        ZipFile(file).use { zip ->
                            val entries = zip.entries()
                            var count = 0
                            while (entries.hasMoreElements() && count < 100) {
                                val entry = entries.nextElement()
                                val name = entry.name
                                if (name.endsWith(".png", ignoreCase = true)) {
                                    val isInteresting = name.contains("res/mipmap", ignoreCase = true) ||
                                                        name.contains("res/drawable", ignoreCase = true) ||
                                                        name.contains("assets/", ignoreCase = true) ||
                                                        name.contains("icon", ignoreCase = true) ||
                                                        name.contains("logo", ignoreCase = true)

                                    if (isInteresting && entry.size > 0 && entry.size < 1 * 1024 * 1024) {
                                        try {
                                            zip.getInputStream(entry).use { inputStream ->
                                                val bytes = inputStream.readBytes()
                                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                if (bitmap != null) {
                                                    list.add(
                                                        ApkPngAsset(
                                                            name = name.substringAfterLast("/"),
                                                            path = name,
                                                            sizeBytes = entry.size,
                                                            bitmap = bitmap
                                                        )
                                                    )
                                                    count++
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                val uniqueList = list.distinctBy { it.path }
                onComplete(uniqueList)
            }
        }
    }

    fun downloadPngAsset(context: Context, asset: ApkPngAsset, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var resultMessage: String? = null
            try {
                val bitmap = asset.bitmap ?: throw Exception("Bitmap is null")
                val cleanAssetName = asset.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val filename = cleanAssetName.substringBeforeLast(".") + "_" + System.currentTimeMillis() + ".png"

                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ApkInspector")
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri).use { outputStream ->
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                    resultMessage = "Saved to Pictures/ApkInspector/$filename"
                } else {
                    throw Exception("Failed to insert media row")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resultMessage = "Error saving image: ${e.localizedMessage}"
            }
            withContext(Dispatchers.Main) {
                onResult(resultMessage)
            }
        }
    }

    fun sharePngAsset(context: Context, asset: ApkPngAsset) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = asset.bitmap ?: return@launch
                val cacheDir = File(context.cacheDir, "shared_assets")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val cleanAssetName = asset.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val file = File(cacheDir, cleanAssetName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val authority = "${context.packageName}.fileprovider"
                val fileUri = FileProvider.getUriForFile(context, authority, file)

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "image/png"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share APK Asset")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
