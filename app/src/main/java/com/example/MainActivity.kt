package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppBackgroundActivity
import com.example.data.ExtractionLog
import com.example.data.SdkChangeLog
import com.example.model.InstalledAppInfo
import com.example.model.PermissionModel
import com.example.ui.theme.*
import com.example.viewmodel.AppInspectorViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val factory = remember {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AppInspectorViewModel(application) as T
                    }
                }
            }
            val viewModel: AppInspectorViewModel = viewModel(factory = factory)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = remember(themeMode, isSystemDark) {
                when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemDark
                }
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                AppInspectorApp(viewModel)
            }
        }
    }
}

@Composable
fun AppInspectorApp(viewModel: AppInspectorViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var selectedAppDetails by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var appToExploreAssets by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "Home" -> HomeScreen(
                    viewModel = viewModel,
                    onAppClicked = { selectedAppDetails = it },
                    onPermissionsClicked = { showPermissionsDialog = true }
                )
                "Apps" -> AllAppsScreen(
                    viewModel = viewModel,
                    onAppClicked = { selectedAppDetails = it }
                )
                "Logs" -> LogsScreen(
                    viewModel = viewModel
                )
                "Analytics" -> AnalyticsScreen(
                    viewModel = viewModel
                )
                "Settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onAppearanceClick = { showThemeDialog = true }
                )
            }

            // Global Operation Overlay Message
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                statusMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OnHeaderViolet),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Detailed App Inspector Bottom Sheet / Dialog
            selectedAppDetails?.let { app ->
                AppDetailsBottomSheet(
                    app = app,
                    onDismiss = { selectedAppDetails = null },
                    onExtract = {
                        viewModel.extractApk(app)
                        selectedAppDetails = null
                    },
                    onExploreAssets = {
                        appToExploreAssets = app
                        selectedAppDetails = null
                    }
                )
            }

            // APK Asset Explorer Bottom Sheet (PNG icons / images inside)
            appToExploreAssets?.let { app ->
                ApkAssetsBottomSheet(
                    app = app,
                    viewModel = viewModel,
                    onDismiss = { appToExploreAssets = null }
                )
            }

            // System Permissions Overview Sheet
            if (showPermissionsDialog) {
                PermissionsExplorerDialog(
                    apps = apps,
                    onDismiss = { showPermissionsDialog = false },
                    onAppClicked = { app ->
                        selectedAppDetails = app
                        showPermissionsDialog = false
                    }
                )
            }

            // Theme Selection Dialog (Appearance Selector)
            if (showThemeDialog) {
                ThemeSelectionDialog(
                    themeMode = themeMode,
                    onThemeSelected = { mode ->
                        viewModel.setThemeMode(mode)
                        showThemeDialog = false
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }
        }
    }
}

// Custom Navigation Bar designed exactly to the M3 and Vibrant Palette parameters
@Composable
fun AppNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Column {
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavBackground)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                NavigationItem("Home", "Dashboard", Icons.Default.Home),
                NavigationItem("Apps", "All Apps", Icons.Default.List),
                NavigationItem("Logs", "Logs", Icons.Default.PlayArrow),
                NavigationItem("Analytics", "Analytics", Icons.Default.Info),
                NavigationItem("Settings", "Settings", Icons.Default.Settings)
            )

            tabs.forEach { item ->
                val isSelected = selectedTab == item.id
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(item.id) }
                        .padding(vertical = 8.dp)
                        .testTag("nav_tab_${item.id.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ActionZipsBg else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) ActionZipsText else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ActionZipsText else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD / HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(
    viewModel: AppInspectorViewModel,
    onAppClicked: (InstalledAppInfo) -> Unit,
    onPermissionsClicked: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val activities by viewModel.backgroundActivities.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Find app with highest background duration
    val highestActivity = remember(activities) {
        activities.maxByOrNull { it.runtimeMinutes }
    }

    val highestAppInfo = remember(highestActivity, apps) {
        apps.find { it.packageName == highestActivity?.packageName }
    }

    val maxAppSize = remember(apps) {
        apps.maxOfOrNull { it.totalSize }?.coerceAtLeast(1L) ?: 1L
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HeaderViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = OnHeaderViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "App Inspector",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.selectTab("Apps") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                }
                IconButton(
                    onClick = { viewModel.selectTab("Settings") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ⚡ OPTIMIZATION CONTROL CENTER
            item {
                OptimizationControlCenter(viewModel = viewModel)
            }

            // Highest Activity Feature Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HeaderViolet),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("highest_activity_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RUNTIME ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnHeaderViolet,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Highest Activity",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OnHeaderViolet
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(OnHeaderViolet)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        highestActivity?.let { activity ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AndroidLogoCanvas(size = 32.dp, color = Color(0xFF4CAF50))
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = activity.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnHeaderViolet,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatMinutes(activity.runtimeMinutes),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = OnHeaderViolet,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (activity.runtimeMinutes.toFloat() / 300f).coerceIn(0.1f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = AccentPurple,
                                        trackColor = AccentPurpleLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    highestAppInfo?.let { viewModel.extractApk(it) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("extract_active_apk_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Extract Active APK",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentPurple)
                            }
                        }
                    }
                }
            }

            // Grid Actions (Manage Zips & Permissions)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Manage Zips Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ActionZipsBg),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab("Logs") }
                            .testTag("manage_zips_grid_item")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ActionZipsText,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Manage Zips",
                                fontWeight = FontWeight.Bold,
                                color = ActionZipsText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Permissions Explorer Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ActionPermsBg),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPermissionsClicked() }
                            .testTag("permissions_grid_item")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ActionPermsText,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Permissions",
                                fontWeight = FontWeight.Bold,
                                color = ActionPermsText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Recently Analyzed Section
            item {
                Text(
                    text = "Recently Analyzed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPurple)
                    }
                }
            } else {
                val recentApps = apps.take(3)
                items(recentApps) { app ->
                    AppListItem(
                        app = app,
                        maxAppSize = maxAppSize,
                        onClick = { onAppClicked(app) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. ALL INSTALLED APPS SCREEN
// ==========================================
enum class SortOption {
    NAME,
    INSTALL_DATE
}

@Composable
fun AllAppsScreen(
    viewModel: AppInspectorViewModel,
    onAppClicked: (InstalledAppInfo) -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val smartFilter by viewModel.smartFilter.collectAsStateWithLifecycle()
    val selectedPermissionFilter by viewModel.selectedPermissionFilter.collectAsStateWithLifecycle()

    var sortBy by remember { mutableStateOf(SortOption.NAME) }

    val filteredApps = remember(apps, searchQuery, smartFilter, selectedPermissionFilter, sortBy) {
        val filtered = apps.filter { app ->
            val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true) ||
                    app.permissions.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesFilter = when (smartFilter) {
                "user" -> !app.isSystem
                "launchable" -> app.isLaunchable
                "system" -> app.isSystem
                else -> true
            }
            val matchesPermission = selectedPermissionFilter == null || app.permissions.any { perm ->
                perm.name.contains(selectedPermissionFilter!!, ignoreCase = true)
            }
            matchesSearch && matchesFilter && matchesPermission
        }
        when (sortBy) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.INSTALL_DATE -> filtered.sortedByDescending { it.installTime }
        }
    }

    val userAppsCount = remember(apps) { apps.count { !it.isSystem } }
    val launchableAppsCount = remember(apps) { apps.count { it.isLaunchable } }
    val systemAppsCount = remember(apps) { apps.count { it.isSystem } }

    val maxAppSize = remember(apps) {
        apps.maxOfOrNull { it.totalSize }?.coerceAtLeast(1L) ?: 1L
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Installed Split Apps",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search apps...", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("app_search_field"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true
        )

        val permissionsList = remember {
            listOf(
                Pair(null, "🔑 All"),
                Pair("CAMERA", "📷 Camera"),
                Pair("LOCATION", "📍 Location"),
                Pair("CONTACTS", "👤 Contacts"),
                Pair("STORAGE", "📁 Storage"),
                Pair("MICROPHONE", "🎤 Mic"),
                Pair("PHONE", "📞 Phone"),
                Pair("SMS", "💬 SMS"),
                Pair("CALENDAR", "📅 Calendar")
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(permissionsList) { (permKey, label) ->
                val isSelected = selectedPermissionFilter == permKey
                val bg = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                val textColor = if (isSelected) Color.White else TextPrimary
                val borderCol = if (isSelected) AccentPurple else BorderColor

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                        .clickable { viewModel.setSelectedPermissionFilter(permKey) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("permission_chip_${permKey ?: "all"}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTabButton(
                text = "User ($userAppsCount)",
                isSelected = smartFilter == "user",
                onClick = { viewModel.setSmartFilter("user") },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = "Launchable ($launchableAppsCount)",
                isSelected = smartFilter == "launchable",
                onClick = { viewModel.setSmartFilter("launchable") },
                modifier = Modifier.weight(1.2f)
            )
            FilterTabButton(
                text = "System ($systemAppsCount)",
                isSelected = smartFilter == "system",
                onClick = { viewModel.setSmartFilter("system") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filteredApps.size} apps found",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Box {
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Sort Options",
                        tint = AccentPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (sortBy) {
                            SortOption.NAME -> "Name (A-Z)"
                            SortOption.INSTALL_DATE -> "Install Date"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                    Text(
                        text = "▼",
                        fontSize = 10.sp,
                        color = AccentPurple
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)", color = TextPrimary) },
                        onClick = {
                            sortBy = SortOption.NAME
                            expanded = false
                        },
                        leadingIcon = {
                            if (sortBy == SortOption.NAME) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Installation Date", color = TextPrimary) },
                        onClick = {
                            sortBy = SortOption.INSTALL_DATE
                            expanded = false
                        },
                        leadingIcon = {
                            if (sortBy == SortOption.INSTALL_DATE) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPurple)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No apps found",
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredApps) { app ->
                    AppListItem(
                        app = app,
                        maxAppSize = maxAppSize,
                        onClick = { onAppClicked(app) },
                        modifier = Modifier.testTag("app_item_${app.packageName}")
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) OnHeaderViolet else Color.White,
            contentColor = if (isSelected) Color.White else TextPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) null else BorderStroke(1.dp, BorderColor),
        modifier = modifier.height(44.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ==========================================
// 3. LOGS / EXTRACTIONS SCREEN
// ==========================================
@Composable
fun LogsScreen(
    viewModel: AppInspectorViewModel
) {
    val logs by viewModel.extractionLogs.collectAsStateWithLifecycle()
    val sdkLogs by viewModel.sdkChangeLogs.collectAsStateWithLifecycle()
    var selectedLogTab by remember { mutableStateOf("Recent") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Logs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedLogTab = "Recent" }
                    .padding(vertical = 12.dp)
                    .testTag("logs_tab_recent"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recent Extractions",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedLogTab == "Recent") AccentPurple else TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth(0.6f)
                        .background(if (selectedLogTab == "Recent") AccentPurple else Color.Transparent)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedLogTab = "SDK" }
                    .padding(vertical = 12.dp)
                    .testTag("logs_tab_sdk"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SDK Changes",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedLogTab == "SDK") AccentPurple else TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth(0.6f)
                        .background(if (selectedLogTab == "SDK") AccentPurple else Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedLogTab == "Recent") {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent extractions.\nGo to All Apps and tap on an app to extract its APK!",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(logs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.appName,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = log.packageName,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(ActionZipsBg)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (log.isSigned) "Signed" else "Unsigned",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ActionZipsText
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteLog(log.id, log.filePath) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Format: ${log.fileType}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Size: ${log.fileSizeFormatted}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                    }

                                    Text(
                                        text = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Copied Path", log.filePath)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Path copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Path", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.shareExtractedFile(context, log) },
                                        colors = ButtonDefaults.buttonColors(containerColor = HeaderViolet, contentColor = OnHeaderViolet),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = OnHeaderViolet)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install / Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (sdkLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Monitoring SDK and background events.\nActivity logs will appear here automatically.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sdkLogs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.appName,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (log.changeType.contains("Active")) ActionPermsBg else ActionZipsBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.changeType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.changeType.contains("Active")) ActionPermsText else ActionZipsText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = SimpleDateFormat("hh:mm:ss a • MMM d", Locale.getDefault()).format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ANALYTICS / SDK DISTRIBUTION SCREEN
// ==========================================
@Composable
fun AnalyticsScreen(
    viewModel: AppInspectorViewModel
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredApps = remember(apps, selectedFilter) {
        when (selectedFilter) {
            "User" -> apps.filter { !it.isSystem }
            "System" -> apps.filter { it.isSystem }
            else -> apps
        }
    }

    val sdkDistribution = remember(filteredApps) {
        filteredApps.groupBy { it.targetSdkVersion }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.first }
    }

    val totalApps = filteredApps.size
    val totalUniqueSdks = sdkDistribution.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "SDK Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("User", "System", "All").forEach { filter ->
                val isSelected = selectedFilter == filter
                Button(
                    onClick = { selectedFilter = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) OnHeaderViolet else Color.White,
                        contentColor = if (isSelected) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(text = filter, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ActionZipsBg),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalApps",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = ActionZipsText
                            )
                            Text(
                                text = "Total Apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = ActionZipsText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ActionPermsBg),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalUniqueSdks",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = ActionPermsText
                            )
                            Text(
                                text = "Unique SDKs",
                                style = MaterialTheme.typography.labelMedium,
                                color = ActionPermsText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SDK Distribution",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (sdkDistribution.isEmpty()) {
                            Text("No SDK distribution data available.")
                        } else {
                            SdkBarChartCompose(sdkData = sdkDistribution)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SDK Legend",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sdkDistribution.forEach { (apiLevel, count) ->
                                val pct = if (totalApps > 0) (count.toFloat() / totalApps.toFloat() * 100f).toInt() else 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(getChartColorForSdk(apiLevel))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "API $apiLevel (${InstalledAppInfo.getSdkName(apiLevel).split(" / ")[0]})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "$count apps",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "$pct%",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = AccentPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SdkBarChartCompose(sdkData: List<Pair<Int, Int>>) {
    val maxCount = remember(sdkData) { sdkData.maxOfOrNull { it.second } ?: 1 }
    val displayData = remember(sdkData) { sdkData.take(6) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        displayData.forEach { (api, count) ->
            val ratio = count.toFloat() / maxCount.toFloat()
            val barHeightFraction = ratio.coerceIn(0.1f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$count",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(barHeightFraction)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(getChartColorForSdk(api))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "API $api",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun getChartColorForSdk(apiLevel: Int): Color {
    return when (apiLevel) {
        37 -> Color(0xFF00C853)
        36 -> Color(0xFF00B0FF)
        35 -> Color(0xFFFF9100)
        34 -> Color(0xFFFF3D00)
        33 -> Color(0xFFD500F9)
        else -> Color(0xFF7C4DFF)
    }
}

// ==========================================
// 5. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: AppInspectorViewModel,
    onAppearanceClick: () -> Unit
) {
    val signOutput by viewModel.signOutputApk.collectAsStateWithLifecycle()
    val autoMergeOpt by viewModel.autoMerge.collectAsStateWithLifecycle()
    val autoSelectSplt by viewModel.autoSelectSplits.collectAsStateWithLifecycle()
    val showSplitSel by viewModel.showSplitSelection.collectAsStateWithLifecycle()
    val forceMrg by viewModel.forceMerge.collectAsStateWithLifecycle()
    val bgSync by viewModel.backgroundSync.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnHeaderViolet),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HeaderViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = OnHeaderViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "App Inspector v1.2.0 (Build 12)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Developed securely for local Android audits. Privacy prioritized.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        item {
            PreferenceHeader(title = "GENERAL")
        }

        item {
            PreferenceValue(
                title = "Appearance",
                value = when (themeMode) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System"
                },
                onClick = onAppearanceClick
            )
        }

        item {
            PreferenceValue(
                title = "Language",
                value = "English (US)",
                onClick = { showLanguageDialog = true }
            )
        }

        item {
            PreferenceValue(
                title = "Privacy & Data Safety",
                value = "View policies & details",
                onClick = { showPrivacyDialog = true }
            )
        }

        item {
            PreferenceHeader(title = "SIGNING")
        }

        item {
            PreferenceToggle(
                title = "Sign Output APK",
                subtitle = "Applies V1 + V2 signatures after extraction automatically",
                checked = signOutput,
                onCheckedChange = { viewModel.savePref("sign_output_apk", it) }
            )
        }

        item {
            PreferenceValue(
                title = "Signing Method",
                value = "Default Debug Keystore"
            )
        }

        item {
            PreferenceValue(
                title = "Signing Scheme",
                value = "V1 + V2 (current default)"
            )
        }

        item {
            PreferenceHeader(title = "PROCESS & OUTPUT")
        }

        item {
            PreferenceToggle(
                title = "Auto Extract Split Pack",
                subtitle = "Extract base and device splits instantly to ZIP",
                checked = autoMergeOpt,
                onCheckedChange = { viewModel.savePref("auto_merge", it) }
            )
        }

        item {
            PreferenceToggle(
                title = "Auto-Select Splits",
                subtitle = "Pre-select architecture and compatible system splits",
                checked = autoSelectSplt,
                onCheckedChange = { viewModel.savePref("auto_select_splits", it) }
            )
        }

        item {
            PreferenceToggle(
                title = "Show Split Selection Dialog",
                subtitle = "Always ask for split components before packaging",
                checked = showSplitSel,
                onCheckedChange = { viewModel.savePref("show_split_selection", it) }
            )
        }

        item {
            PreferenceToggle(
                title = "Force Extraction",
                subtitle = "Bypass signature mismatch errors on deep packages",
                checked = forceMrg,
                onCheckedChange = { viewModel.savePref("force_merge", it) }
            )
        }

        item {
            PreferenceHeader(title = "SDK & PERFORMANCE MONITORING")
        }

        item {
            PreferenceToggle(
                title = "Background Monitor",
                subtitle = "Query running processes and track background active times dynamically",
                checked = bgSync,
                onCheckedChange = { viewModel.savePref("background_sync", it) }
            )
        }
    }
}

// ==========================================
// 6. DETAILED BOTTOM SHEET / EXPLORER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsBottomSheet(
    app: InstalledAppInfo,
    onDismiss: () -> Unit,
    onExtract: () -> Unit,
    onExploreAssets: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconView(
                    drawable = app.icon,
                    size = 56.dp,
                    fallbackColor = AccentPurple
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ActionPermsBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = app.sdkLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActionPermsText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoBlock(
                    title = "Target SDK",
                    value = "${app.targetSdkVersion}",
                    modifier = Modifier.weight(1f)
                )
                InfoBlock(
                    title = "Min SDK",
                    value = "${app.minSdkVersion}",
                    modifier = Modifier.weight(1f)
                )
                InfoBlock(
                    title = "Type",
                    value = if (app.isSplit) "Split (${app.splitApkPaths.size + 1})" else "Single APK",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permissions Details (${app.permissions.size})",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                if (app.permissions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No permissions requested",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(app.permissions) { perm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (perm.isDangerous) ActionPermsBg.copy(alpha = 0.4f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = perm.name.substringAfterLast("."),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = perm.name,
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (perm.isDangerous) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ActionPermsBg)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "DANGEROUS",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ActionPermsText
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (perm.isGranted) ActionZipsBg else Color(0xFFEEEEEE))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (perm.isGranted) "GRANTED" else "DENIED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (perm.isGranted) ActionZipsText else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // App Info button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${app.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "App Info",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play Store button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("market://details?id=${app.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")
                                }
                                context.startActivity(webIntent)
                            } catch (e2: Exception) {
                                // ignore
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play Store",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExtract,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("extract_app_sheet_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (app.isSplit) "Extract Split APKs (ZIP)" else "Extract Base APK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onExploreAssets,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AccentPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("explore_assets_sheet_button")
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = AccentPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explore & Download PNG Icons",
                    color = AccentPurple,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ==========================================
// 7. SYSTEM PERMISSIONS OVERVIEW EXPLORER
// ==========================================
@Composable
fun PermissionsExplorerDialog(
    apps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onAppClicked: (InstalledAppInfo) -> Unit
) {
    val permissionCategories = listOf(
        "CAMERA",
        "LOCATION",
        "STORAGE",
        "CONTACTS",
        "CALENDAR"
    )

    var selectedCategory by remember { mutableStateOf("CAMERA") }

    val categoryApps = remember(apps, selectedCategory) {
        apps.filter { app ->
            app.permissions.any { perm ->
                perm.name.contains(selectedCategory, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentPurple, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "System Permissions",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    permissionCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) OnHeaderViolet else ActionZipsBg)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ActionZipsText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Apps asking for $selectedCategory (${categoryApps.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    if (categoryApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No apps request this permission.", fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(categoryApps) { app ->
                                val isGranted = app.permissions.find { it.name.contains(selectedCategory, ignoreCase = true) }?.isGranted == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAppClicked(app) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconView(
                                        drawable = app.icon,
                                        size = 36.dp,
                                        fallbackColor = AccentPurple
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isGranted) ActionZipsBg else ActionPermsBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isGranted) "GRANTED" else "REVOKED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGranted) ActionZipsText else ActionPermsText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = BackgroundColor
    )
}

// ==========================================
// CUSTOM CANVAS GRAPHICS
// ==========================================
@Composable
fun AndroidLogoCanvas(
    size: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val headRadius = w * 0.35f

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w / 2 - headRadius, h / 2 - headRadius + 4.dp.toPx()),
            size = Size(headRadius * 2, headRadius * 2)
        )

        val eyeRadius = w * 0.04f
        drawCircle(
            color = Color.White,
            radius = eyeRadius,
            center = Offset(w / 2 - headRadius * 0.4f, h / 2 - headRadius * 0.3f + 4.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = eyeRadius,
            center = Offset(w / 2 + headRadius * 0.4f, h / 2 - headRadius * 0.3f + 4.dp.toPx())
        )

        drawLine(
            color = color,
            start = Offset(w / 2 - headRadius * 0.4f, h / 2 - headRadius * 0.8f),
            end = Offset(w / 2 - headRadius * 0.7f, h / 2 - headRadius * 1.2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w / 2 + headRadius * 0.4f, h / 2 - headRadius * 0.8f),
            end = Offset(w / 2 + headRadius * 0.7f, h / 2 - headRadius * 1.2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

data class NavigationItem(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppIconView(
    drawable: android.graphics.drawable.Drawable?,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    fallbackColor: Color = AccentPurple,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(ActionZipsBg),
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.widget.ImageView(context).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(drawable)
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(drawable)
                },
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        } else {
            AndroidLogoCanvas(size = size * 0.6f, color = fallbackColor)
        }
    }
}

@Composable
fun SwipeableAppListItem(
    app: InstalledAppInfo,
    maxAppSize: Long,
    onClick: () -> Unit,
    onSwipeToExtract: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 300f // Swipe threshold in pixels

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF3E5F5)) // Beautiful Lavender/Purple underlay
    ) {
        // Underlay extraction indicators
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Extract APK",
                    tint = AccentPurple
                )
                Text(
                    text = "Release to Extract",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AccentPurple
                )
            }
        }

        // Foreground: The actual AppListItem content
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (java.lang.Math.abs(offsetX) > swipeThreshold) {
                                onSwipeToExtract()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // Clamp swipe limits to prevent excessive drag
                            offsetX = (offsetX + dragAmount).coerceIn(-400f, 400f)
                        }
                    )
                }
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(
                    drawable = app.icon,
                    size = 44.dp,
                    fallbackColor = if (app.isSplit) AccentPurple else Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = app.packageName,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "v${app.versionName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${app.permissions.size} Perms",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (app.isSplit) "${app.splitApkPaths.size + 1} splits" else "Single APK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (app.isSplit) AccentPurple else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatFileSize(app.totalSize),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                        LinearProgressIndicator(
                            progress = { (app.totalSize.toFloat() / maxAppSize.toFloat()).coerceIn(0.01f, 1.0f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp)),
                            color = AccentPurple,
                            trackColor = ActionZipsBg
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (app.targetSdkVersion >= 35) ActionZipsBg else Color(0xFFE2E2E2))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "API ${app.targetSdkVersion}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (app.targetSdkVersion >= 35) ActionZipsText else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: InstalledAppInfo,
    maxAppSize: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(
                drawable = app.icon,
                size = 44.dp,
                fallbackColor = if (app.isSplit) AccentPurple else Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = app.packageName,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${app.versionName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${app.permissions.size} Perms",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (app.isSplit) "${app.splitApkPaths.size + 1} splits" else "Single APK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (app.isSplit) AccentPurple else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatFileSize(app.totalSize),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                    LinearProgressIndicator(
                        progress = { (app.totalSize.toFloat() / maxAppSize.toFloat()).coerceIn(0.01f, 1.0f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp)),
                        color = AccentPurple,
                        trackColor = ActionZipsBg
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (app.targetSdkVersion >= 35) ActionZipsBg else Color(0xFFE2E2E2))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "API ${app.targetSdkVersion}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (app.targetSdkVersion >= 35) ActionZipsText else TextSecondary
                )
            }
        }
    }
}

fun formatMinutes(minutes: Long): String {
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val rem = minutes % 60
    return if (rem == 0L) "${hours}h" else "${hours}h ${rem}m"
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun PreferenceHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AccentPurple,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun PreferenceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentPurple,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun PreferenceValue(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .let { modifier ->
                if (onClick != null) {
                    modifier.clickable(onClick = onClick)
                } else {
                    modifier
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentPurple
            )
        }
    }
}

@Composable
fun InfoBlock(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AccentPurple,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkAssetsBottomSheet(
    app: InstalledAppInfo,
    viewModel: AppInspectorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var assets by remember { mutableStateOf<List<com.example.model.ApkPngAsset>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(app) {
        viewModel.loadApkPngAssets(app) { loadedAssets ->
            assets = loadedAssets
            isLoading = false
        }
    }

    val filteredAssets = remember(assets, searchQuery) {
        assets.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIconView(
                        drawable = app.icon,
                        size = 48.dp,
                        fallbackColor = AccentPurple
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "APK Asset Explorer",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${app.name} (${app.packageName})",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_asset_explorer")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search PNG icons...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_png_icons_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderColor
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentPurple)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Scanning APK for PNG resources...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else if (filteredAssets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (assets.isEmpty()) "No PNG assets found in this APK." else "No assets match search query.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Found ${filteredAssets.size} PNG images inside the package:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filteredAssets) { asset ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Display PNG thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ActionZipsBg)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (asset.bitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = asset.bitmap.asImageBitmap(),
                                                contentDescription = asset.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = asset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = asset.path.substringBeforeLast("/").ifEmpty { "root" },
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = viewModel.formatFileSize(asset.sizeBytes),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AccentPurple,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Download button
                                        IconButton(
                                            onClick = {
                                                viewModel.downloadPngAsset(context, asset) { result ->
                                                    coroutineScope.launch {
                                                        if (result != null) {
                                                            snackbarHostState.showSnackbar(result)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .background(ActionZipsBg, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Save to Pictures",
                                                tint = AccentPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Share button
                                        IconButton(
                                            onClick = {
                                                viewModel.sharePngAsset(context, asset)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .background(ActionPermsBg, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Image",
                                                tint = ActionPermsText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExclusionListDialog(
    viewModel: AppInspectorViewModel,
    onDismiss: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val exclusions by viewModel.exclusions.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filteredApps = remember(apps, query) {
        apps.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "🛡️ Exclusion Whitelist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Whitelist important apps to keep them running during optimization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps) { app ->
                        val isExcluded = exclusions.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleExclusion(app.packageName) }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isExcluded,
                                onCheckedChange = { viewModel.toggleExclusion(app.packageName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizationControlCenter(viewModel: AppInspectorViewModel) {
    val activities by viewModel.backgroundActivities.collectAsStateWithLifecycle()
    val isOptimizing by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val killerMode by viewModel.killerMode.collectAsStateWithLifecycle()
    val batterySaver by viewModel.batterySaver.collectAsStateWithLifecycle()
    val performanceBoost by viewModel.performanceBoost.collectAsStateWithLifecycle()
    val exclusions by viewModel.exclusions.collectAsStateWithLifecycle()

    var showExclusionsDialog by remember { mutableStateOf(false) }

    val liveAppsCount = remember(activities) { activities.count { it.isLive } }
    val totalCpuLoad = remember(activities) {
        activities.filter { it.isLive }.sumOf { it.cpuUsagePercent.toDouble() }.toFloat()
    }

    if (showExclusionsDialog) {
        ExclusionListDialog(
            viewModel = viewModel,
            onDismiss = { showExclusionsDialog = false }
        )
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag("optimization_control_center")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OPTIMIZATION SYSTEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Task Controller",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (liveAppsCount > 0) ActionPermsBg else ActionZipsBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$liveAppsCount Active",
                        color = if (liveAppsCount > 0) ActionPermsText else ActionZipsText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("CPU LOAD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", totalCpuLoad)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalCpuLoad > 30f) Color(0xFFD32F2F) else Color(0xFF388E3C)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable { showExclusionsDialog = true }
                        .padding(12.dp)
                ) {
                    Column {
                        Text("WHITELIST", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${exclusions.size} Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentPurple,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Whitelist",
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.optimizeOneClick() },
                enabled = !isOptimizing,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("quick_optimize_button")
            ) {
                if (isOptimizing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Optimize",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Run One-Click Optimize",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🛡️ KILLER TERMINATION MODE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    Triple("default", "Standard", "Default API force stop"),
                    Triple("root", "Root SU", "Superuser automation"),
                    Triple("shizuku", "Shizuku", "AADB shell termination")
                )

                modes.forEach { (mode, label, description) ->
                    val isSelected = killerMode == mode
                    val modeBg = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val modeText = if (isSelected) Color.White else TextPrimary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(modeBg)
                            .clickable { viewModel.setKillerMode(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = modeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🔋 Battery Saver",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-hibernates long-running background tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = batterySaver,
                    onCheckedChange = { viewModel.savePref("battery_saver", it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🚀 Performance Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Free up CPU limits and clear active memory",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = performanceBoost,
                    onCheckedChange = { viewModel.savePref("performance_boost", it) }
                )
            }
        }
    }
}

// ==========================================
// 8. THEME & GENERAL DIALOGS
// ==========================================

@Composable
fun ThemeSelectionDialog(
    themeMode: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val options = listOf(
                    Triple("light", "Light", "Light"),
                    Triple("dark", "Dark", "Dark"),
                    Triple("system", "System (Default)", "System")
                )

                options.forEach { (mode, label, testTagSuffix) ->
                    val isSelected = themeMode == mode
                    val rowBg = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(rowBg)
                            .clickable { onThemeSelected(mode) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onThemeSelected(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentPurple,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("theme_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Privacy & Data Safety",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "App Inspector operates 100% locally on your device. We do not have servers, and we never collect, transmit, or share any of your personal details, list of installed applications, extracted APK packages, or asset directories.\n\nAll operations—including APK packaging, code signature generation, and binary analysis—happen purely on-device securely and without internet intervention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Dismiss",
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Language Selection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = true,
                        onClick = {},
                        colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "English (US) - Active",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Support for multilingual translations is currently being compiled and will be available in the upcoming dynamic release cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
            }
        }
    }
}
