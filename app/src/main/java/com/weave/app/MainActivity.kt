package com.weave.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete // Trash Icon Import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val serviceIntent = Intent(this, WeaveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("weave_prefs", Context.MODE_PRIVATE)
            var isSetupDone by remember { mutableStateOf(prefs.getBoolean("setup_done", false)) }
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }

            MaterialTheme(colorScheme = if (isDarkTheme) DarkColors else LightColors) {
                if (!isSetupDone) {
                    SetupScreen(context) {
                        prefs.edit().putBoolean("setup_done", true).apply()
                        isSetupDone = true
                    }
                } else {
                    MainLayout(isDarkTheme) {
                        prefs.edit().putBoolean("dark_theme", !isDarkTheme).apply()
                        isDarkTheme = !isDarkTheme
                    }
                }
            }
        }
    }
}

fun isNotificationAccessGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var showGhostVault by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("WEAVE MENU", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                NavigationDrawerItem(
                    label = { Text("Ghost Vault (Deleted)") },
                    selected = showGhostVault,
                    onClick = { showGhostVault = true; scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = selectedTab == 2 && !showGhostVault,
                    onClick = { selectedTab = 2; showGhostVault = false; scope.launch { drawerState.close() } }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("Developed by Arif Ali", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (showGhostVault) "Ghost Vault" else if (selectedTab == 0) "Dashboard" else if (selectedTab == 1) "History" else "Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Text(
                            text = if (isDarkTheme) "🌙" else "☀️",
                            fontSize = 24.sp,
                            modifier = Modifier.clickable { onThemeToggle() }.padding(16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            },
            bottomBar = {
                if (!showGhostVault) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.List, contentDescription = "History") },
                            label = { Text("History") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (showGhostVault) {
                    GhostVaultScreen()
                } else {
                    when (selectedTab) {
                        0 -> HomeScreen()
                        1 -> HistoryScreen()
                        2 -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun GhostVaultScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var deletedMessages by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.notificationDao().getAllNotifications().collectLatest { 
            deletedMessages = it.filter { msg -> msg.isDeleted }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A0A0A)).padding(16.dp)) {
        Text("Recovered Deleted Messages", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (deletedMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No deleted messages recovered yet.", color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(deletedMessages) { notif ->
                    MessageCard(notif)
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.notificationDao().getAllNotifications().collectLatest { notifications = it }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Recent Activity", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Waiting for messages...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn {
                items(notifications.take(5)) { notif -> MessageCard(notif) }
            }
        }
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.notificationDao().getAllNotifications().collectLatest { notifications = it }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("All Messages", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(notifications) { notif -> MessageCard(notif) }
        }
    }
}

@Composable
fun MessageCard(notif: NotificationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notif.isDeleted) Color(0xFF3A0A0A) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(notif.sender, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                
                // Yahan Trash Icon aur Text add kiya hai
                if (notif.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Deleted",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETED", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text = notif.message,
                color = MaterialTheme.colorScheme.onBackground,
                textDecoration = if (notif.isDeleted) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Settings", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (notifAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (notifAccess) "Notification Access: Granted ✓" else "Manage Notification Access")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (batteryOpt) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surface)
        ) {
            Text(if (batteryOpt) "Battery Optimization: Disabled ✓" else "Disable Battery Optimization", color = if (batteryOpt) Color.White else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SetupScreen(context: Context, onFinish: () -> Unit) {
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to WEAVE", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Let's setup your app", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, 
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (notifAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (notifAccess) "1. Notification Access: Granted ✓" else "1. Allow Notification Access")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            }, 
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (batteryOpt) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surface)
        ) {
            Text(if (batteryOpt) "2. Battery Optimization: Disabled ✓" else "2. Disable Battery Optimization", color = if (batteryOpt) Color.White else MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Continue to App")
        }
    }
}
