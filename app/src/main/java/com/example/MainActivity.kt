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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            MyApplicationTheme {
                val factory = remember {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AppInspectorViewModel(application) as T
                        }
                    }
                }
                val viewModel: AppInspectorViewModel = viewModel(factory = factory)
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

    var selectedAppDetails by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

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
                    viewModel = viewModel
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
                    }
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
@Composable
fun AllAppsScreen(
    viewModel: AppInspectorViewModel,
    onAppClicked: (InstalledAppInfo) -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSystemFilter by viewModel.isSystemFilter.collectAsStateWithLifecycle()

    val filteredApps = remember(apps, searchQuery, isSystemFilter) {
        apps.filter { app ->
            val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = app.isSystem == isSystemFilter
            matchesSearch && matchesFilter
        }
    }

    val userAppsCount = remember(apps) { apps.count { !it.isSystem } }
    val systemAppsCount = remember(apps) { apps.count { it.isSystem } }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTabButton(
                text = "User ($userAppsCount)",
                isSelected = !isSystemFilter,
                onClick = { viewModel.setSystemFilter(false) },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                text = "System ($systemAppsCount)",
                isSelected = isSystemFilter,
                onClick = { viewModel.setSystemFilter(true) },
                modifier = Modifier.weight(1f)
            )
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
    viewModel: AppInspectorViewModel
) {
    val signOutput by viewModel.signOutputApk.collectAsStateWithLifecycle()
    val autoMergeOpt by viewModel.autoMerge.collectAsStateWithLifecycle()
    val autoSelectSplt by viewModel.autoSelectSplits.collectAsStateWithLifecycle()
    val showSplitSel by viewModel.showSplitSelection.collectAsStateWithLifecycle()
    val forceMrg by viewModel.forceMerge.collectAsStateWithLifecycle()
    val bgSync by viewModel.backgroundSync.collectAsStateWithLifecycle()

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
                color = TextPrimary,
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
                            text = "Support Split APKs Extractor",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Developed securely for Android devices. Tap to check latest rules.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        item {
            PreferenceHeader(title = "SIGNING")
        }

        item {
            PreferenceToggle(
                title = "Sign Output APK",
                subtitle = "Applies V1 + V2 signatures after extraction automatically",
                checked = signOutput,
                onCheckedChange = { viewModel.signOutputApk.value = it }
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
                onCheckedChange = { viewModel.autoMerge.value = it }
            )
        }

        item {
            PreferenceToggle(
                title = "Auto-Select Splits",
                subtitle = "Pre-select architecture and compatible system splits",
                checked = autoSelectSplt,
                onCheckedChange = { viewModel.autoSelectSplits.value = it }
            )
        }

        item {
            PreferenceToggle(
                title = "Show Split Selection Dialog",
                subtitle = "Always ask for split components before packaging",
                checked = showSplitSel,
                onCheckedChange = { viewModel.showSplitSelection.value = it }
            )
        }

        item {
            PreferenceToggle(
                title = "Force Extraction",
                subtitle = "Bypass signature mismatch errors on deep packages",
                checked = forceMrg,
                onCheckedChange = { viewModel.forceMerge.value = it }
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
                onCheckedChange = { viewModel.backgroundSync.value = it }
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
    onExtract: () -> Unit
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

            Spacer(modifier = Modifier.height(20.dp))

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
fun AppListItem(
    app: InstalledAppInfo,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
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
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentPurple,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = BorderColor
                )
            )
        }
    }
}

@Composable
fun PreferenceValue(
    title: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
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
                    color = TextPrimary
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
