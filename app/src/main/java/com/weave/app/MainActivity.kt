package com.weave.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("weave_prefs", Context.MODE_PRIVATE)
            var isSetupDone by remember { mutableStateOf(prefs.getBoolean("setup_done", false)) }
            var currentTheme by remember { mutableStateOf(WeaveTheme.valueOf(prefs.getString("theme", "DARK") ?: "DARK")) }

            val colorScheme = when (currentTheme) {
                WeaveTheme.DARK -> DarkColors
                WeaveTheme.LIGHT -> LightColors
                WeaveTheme.OCEAN -> OceanColors
                WeaveTheme.SUNSET -> SunsetColors
            }

            MaterialTheme(colorScheme = colorScheme) {
                if (!isSetupDone) {
                    SetupScreen(context) {
                        prefs.edit().putBoolean("setup_done", true).apply()
                        isSetupDone = true
                    }
                } else {
                    MainLayout(currentTheme) { newTheme ->
                        prefs.edit().putString("theme", newTheme.name).apply()
                        currentTheme = newTheme
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
fun MainLayout(currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    var showScreen by remember { mutableStateOf(0) } 
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = showScreen != 0) { showScreen = 0 }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text("WEAVE MENU", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                DrawerItem("Dashboard", Icons.Default.Home, showScreen == 0) { showScreen = 0; scope.launch { drawerState.close() } }
                DrawerItem("Chats", Icons.Default.Person, showScreen == 1) { showScreen = 1; scope.launch { drawerState.close() } }
                DrawerItem("Groups", Icons.Default.Group, showScreen == 2) { showScreen = 2; scope.launch { drawerState.close() } }
                DrawerItem("Ghost Vault", Icons.Default.Delete, showScreen == 3) { showScreen = 3; scope.launch { drawerState.close() } }
                DrawerItem("Status & Media", Icons.Default.Photo, showScreen == 5) { showScreen = 5; scope.launch { drawerState.close() } }
                DrawerItem("Settings", Icons.Default.Settings, showScreen == 4) { showScreen = 4; scope.launch { drawerState.close() } }
                Spacer(modifier = Modifier.weight(1f))
                Text("Developed by Arif Ali", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when(showScreen){0->"Dashboard";1->"Chats";2->"Groups";3->"Ghost Vault";5->"Status & Media";else->"Settings"}, fontWeight = FontWeight.Bold) },
                    navigationIcon = { 
                        if (showScreen != 0) {
                            IconButton(onClick = { showScreen = 0 }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
                )
            },
            bottomBar = {
                GlassDock(selectedScreen = showScreen, onScreenSelected = { showScreen = it })
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (showScreen) {
                    0 -> HomeScreen()
                    1 -> ChatScreen()
                    2 -> GroupScreen()
                    3 -> GhostVaultScreen()
                    4 -> SettingsScreen(currentTheme, onThemeChange)
                    5 -> StatusAndMediaScreen()
                }
            }
        }
    }
}

// 3D Animated Glass Dock
@Composable
fun GlassDock(selectedScreen: Int, onScreenSelected: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(30.dp))
                .background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(Icons.Default.Home, "Home", selectedScreen == 0) { onScreenSelected(0) }
            DockItem(Icons.Default.Person, "Chats", selectedScreen == 1) { onScreenSelected(1) }
            DockItem(Icons.Default.Group, "Groups", selectedScreen == 2) { onScreenSelected(2) }
            DockItem(Icons.Default.Delete, "Ghost", selectedScreen == 3) { onScreenSelected(3) }
            DockItem(Icons.Default.Photo, "Media", selectedScreen == 5) { onScreenSelected(5) }
            DockItem(Icons.Default.Settings, "Settings", selectedScreen == 4) { onScreenSelected(4) }
        }
    }
}

@Composable
fun DockItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    
    // Bounce Animation for selected item
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dock_bounce"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(title) }, icon = { Icon(icon, contentDescription = null) }, selected = isSelected, onClick = onClick, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
}

// 3D Animated Message Card
@Composable
fun MessageCard(notif: NotificationEntity) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Press Animation (3D Press Effect)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { },
        shape = RoundedCornerShape(20.dp), // Zyada rounded
        colors = CardDefaults.cardColors(containerColor = if (notif.isDeleted) Color(0xFF3A0A0A) else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // 3D Shadow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(notif.sender, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (notif.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = "Deleted", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETED", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = notif.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = if (notif.isDeleted) TextDecoration.LineThrough else TextDecoration.None, fontSize = 14.sp)
        }
    }
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var chats by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { chats = it.filter { msg -> !msg.sender.contains("Group") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(chats) { notif -> MessageCard(notif) } } }
}

@Composable
fun GroupScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var groups by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { groups = it.filter { msg -> msg.sender.contains("Group") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(groups) { notif -> MessageCard(notif) } } }
}

@Composable
fun GhostVaultScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var deletedMessages by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { deletedMessages = it.filter { msg -> msg.isDeleted } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { 
        if (deletedMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No deleted messages recovered yet.", color = Color.Gray) }
        } else {
            LazyColumn { items(deletedMessages) { notif -> MessageCard(notif) } } 
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db = Room.databaseBuilder(context, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { notifications = it } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(notifications.take(5)) { notif -> MessageCard(notif) } } }
}

@Composable
fun StatusAndMediaScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Statuses & Media", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(4.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Profile Picture Downloader", fontWeight = FontWeight.Bold)
                    Text("Coming soon...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Captured statuses will appear here.", color = Color.Gray)
        }
    }
}

@Composable
fun SettingsScreen(currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    val context = LocalContext.current
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    var hapticEnabled by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Appearance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("Dark", WeaveTheme.DARK, currentTheme, onThemeChange)
            ThemeChip("Light", WeaveTheme.LIGHT, currentTheme, onThemeChange)
            ThemeChip("Ocean", WeaveTheme.OCEAN, currentTheme, onThemeChange)
            ThemeChip("Sunset", WeaveTheme.SUNSET, currentTheme, onThemeChange)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Permissions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Notification Access", fontWeight = FontWeight.Bold)
                    Text(if (notifAccess) "Granted ✓" else "Tap to Grant", color = if (notifAccess) Color.Green else Color.Red, fontSize = 12.sp)
                }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Battery Optimization", fontWeight = FontWeight.Bold)
                    Text(if (batteryOpt) "Disabled ✓" else "Tap to Disable", color = if (batteryOpt) Color.Green else Color.Red, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Customization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Haptic Feedback (Vibration)", fontWeight = FontWeight.Bold)
                Switch(checked = hapticEnabled, onCheckedChange = { hapticEnabled = it })
            }
        }
    }
}

@Composable
fun ThemeChip(label: String, theme: WeaveTheme, selectedTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    val isSelected = theme == selectedTheme
    Card(
        modifier = Modifier.clickable { onThemeChange(theme) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
    }
}

@Composable
fun SetupScreen(context: Context, onFinish: () -> Unit) {
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome to WEAVE", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (notifAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)) {
            Text(if (notifAccess) "1. Notification Access: Granted ✓" else "1. Allow Notification Access")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (batteryOpt) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant)) {
            Text(if (batteryOpt) "2. Battery Optimization: Disabled ✓" else "2. Disable Battery Optimization", color = if (batteryOpt) Color.White else MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Continue to App") }
    }
}
