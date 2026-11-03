package com.weave.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

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
                    SetupScreen(context) { prefs.edit().putBoolean("setup_done", true).apply(); isSetupDone = true }
                } else {
                    MainLayout(currentTheme) { newTheme -> prefs.edit().putString("theme", newTheme.name).apply(); currentTheme = newTheme }
                }
            }
        }
    }
}

fun isNotificationAccessGranted(context: Context): Boolean {
    return androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
                Text("WEAVE PRO", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                DrawerItem("Dashboard", Icons.Default.Home, showScreen == 0) { showScreen = 0; scope.launch { drawerState.close() } }
                DrawerItem("Chats", Icons.Default.Person, showScreen == 1) { showScreen = 1; scope.launch { drawerState.close() } }
                DrawerItem("Groups", Icons.Default.Group, showScreen == 2) { showScreen = 2; scope.launch { drawerState.close() } }
                DrawerItem("Ghost Vault", Icons.Default.Delete, showScreen == 3) { showScreen = 3; scope.launch { drawerState.close() } }
                DrawerItem("Media Vault", Icons.Default.Photo, showScreen == 5) { showScreen = 5; scope.launch { drawerState.close() } }
                DrawerItem("DP Downloader", Icons.Default.Download, showScreen == 7) { showScreen = 7; scope.launch { drawerState.close() } }
                DrawerItem("Settings", Icons.Default.Settings, showScreen == 4) { showScreen = 4; scope.launch { drawerState.close() } }
                Spacer(modifier = Modifier.weight(1f))
                Text("Developed by Arif Ali", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Weave", fontWeight = FontWeight.Bold) },
                    navigationIcon = { 
                        if (showScreen != 0) IconButton(onClick = { showScreen = 0 }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                        else IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
                )
            },
            bottomBar = { GlassDock(selectedScreen = showScreen, onScreenSelected = { showScreen = it }) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(targetState = showScreen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { target ->
                    when (target) {
                        0 -> HomeScreen()
                        1 -> ChatScreen()
                        2 -> GroupScreen()
                        3 -> GhostVaultScreen()
                        4 -> SettingsScreen(currentTheme, onThemeChange)
                        5 -> MediaVaultScreen()
                        7 -> DPDownloaderScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassDock(selectedScreen: Int, onScreenSelected: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp))
                .background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))))
                .border(1.dp, Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "bounce")
    Column(modifier = Modifier.scale(scale).clip(RoundedCornerShape(20.dp)).background(bg).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(title) }, icon = { Icon(icon, contentDescription = null) }, selected = isSelected, onClick = onClick, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
}

@Composable
fun MessageCard(notif: NotificationEntity) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp).scale(scale).clickable(interactionSource = interactionSource, indication = null) { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (notif.isDeleted) Color(0xFF3A0A0A) else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(notif.sender, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (notif.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Delete, contentDescription = "Deleted", tint = Color.Red, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("DELETED", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = notif.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = if (notif.isDeleted) TextDecoration.LineThrough else TextDecoration.None, fontSize = 14.sp)
        }
    }
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var chats by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    // Individual chats usually don't have (Group Name) format in title
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { chats = it.filter { msg -> !msg.sender.contains("(") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(chats) { notif -> MessageCard(notif) } } }
}

@Composable
fun GroupScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var groups by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    // Groups have format: "Group Name (Sender)"
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { groups = it.filter { msg -> msg.sender.contains("(") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(groups) { notif -> MessageCard(notif) } } }
}

@Composable
fun GhostVaultScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
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
    val db = DatabaseProvider.getDatabase(context)
    var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { notifications = it } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(notifications.take(10)) { notif -> MessageCard(notif) } } }
}

@Composable
fun MediaVaultScreen() {
    val context = LocalContext.current
    val vaultDir = File(context.filesDir, "media_vault")
    var files by remember { mutableStateOf<Array<File>>(emptyArray()) }
    LaunchedEffect(Unit) { if (vaultDir.exists()) files = vaultDir.listFiles() ?: emptyArray() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Media Vault (Recovered)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No media captured yet.", color = Color.Gray) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files) { file ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.aspectRatio(1f).clickable { }) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray)
                            Text(file.name.take(6), color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DPDownloaderScreen() {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("WhatsApp DP Downloader", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Enter Number (e.g. 923001234567)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { 
            if (number.length > 9) {
                // Open WhatsApp chat directly to view DP
                val uri = Uri.parse("https://wa.me/$number")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("View Profile Picture") }
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
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(4.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Notification Access", fontWeight = FontWeight.Bold)
                    Text(if (notifAccess) "Granted ✓" else "Tap to Grant", color = if (notifAccess) Color.Green else Color.Red, fontSize = 12.sp)
                }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(4.dp)) {
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
        Text("Data Management", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(4.dp)) {
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
        Text("Welcome to WEAVE PRO", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
