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
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
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
                    MainLayout(currentTheme, onThemeChange = { newTheme -> prefs.edit().putString("theme", newTheme.name).apply(); currentTheme = newTheme })
                }
            }
        }
    }
}

val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        ShizukuManager.isGranted = true
    }
}

fun isNotificationAccessGranted(context: Context): Boolean = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
fun isBatteryOptimizationDisabled(context: Context): Boolean = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainLayout(currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    var showScreen by remember { mutableStateOf(0) } // 0=Home, 1=Chats, 2=Groups, 3=Ghost, 4=Settings, 5=Media, 6=DP
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = showScreen != 0) { showScreen = 0 }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(32.dp))
                Text("WEAVE PRO", modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                DrawerItem("Dashboard", Icons.Default.Home, showScreen == 0) { showScreen = 0; scope.launch { drawerState.close() } }
                DrawerItem("Chats", Icons.Default.Person, showScreen == 1) { showScreen = 1; scope.launch { drawerState.close() } }
                DrawerItem("Groups", Icons.Default.Group, showScreen == 2) { showScreen = 2; scope.launch { drawerState.close() } }
                DrawerItem("Ghost Vault", Icons.Default.Delete, showScreen == 3) { showScreen = 3; scope.launch { drawerState.close() } }
                DrawerItem("Media Vault", Icons.Default.Photo, showScreen == 5) { showScreen = 5; scope.launch { drawerState.close() } }
                DrawerItem("DP Downloader", Icons.Default.Download, showScreen == 6) { showScreen = 6; scope.launch { drawerState.close() } }
                DrawerItem("Settings", Icons.Default.Settings, showScreen == 4) { showScreen = 4; scope.launch { drawerState.close() } }
                Spacer(modifier = Modifier.weight(1f))
                Text("Developed by Arif Ali", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when(showScreen){0->"Dashboard";1->"Chats";2->"Groups";3->"Ghost Vault";5->"Media Vault";6->"DP Downloader";else->"Settings"}, fontWeight = FontWeight.Bold) },
                    navigationIcon = { 
                        if (showScreen != 0) IconButton(onClick = { showScreen = 0 }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                        else IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
                )
            },
            bottomBar = { BottomNavBar(selectedScreen = showScreen, onScreenSelected = { showScreen = it }) }
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
                        6 -> DPDownloaderScreen()
                    }
                }
            }
        }
    }
}

// PROFESSIONAL BOTTOM NAVIGATION (Edge to Edge)
@Composable
fun BottomNavBar(selectedScreen: Int, onScreenSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            selected = selectedScreen == 0,
            onClick = { onScreenSelected(0) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Chats") },
            label = { Text("Chats", fontSize = 10.sp) },
            selected = selectedScreen == 1,
            onClick = { onScreenSelected(1) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
            label = { Text("Groups", fontSize = 10.sp) },
            selected = selectedScreen == 2,
            onClick = { onScreenSelected(2) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Delete, contentDescription = "Ghost") },
            label = { Text("Ghost", fontSize = 10.sp) },
            selected = selectedScreen == 3,
            onClick = { onScreenSelected(3) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp) },
            selected = selectedScreen == 4,
            onClick = { onScreenSelected(4) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(title, fontWeight = FontWeight.Medium) }, 
        icon = { Icon(icon, contentDescription = null) }, 
        selected = isSelected, 
        onClick = onClick, 
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

// PROFESSIONAL CHAT CARD (With Bottom Sheet Actions)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactChatCard(senderName: String, lastMessage: String, onClick: () -> Unit) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(senderName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(senderName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(lastMessage, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

// HOME SCREEN (List of Contacts)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var groupedChats by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) { 
        db.notificationDao().getAllNotifications().collectLatest { allMsgs -> 
            // Group by sender and get last message
            val map = mutableMapOf<String, String>()
            for (msg in allMsgs) {
                if (!map.containsKey(msg.sender)) {
                    map[msg.sender] = msg.message
                }
            }
            groupedChats = map
        } 
    }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) { 
        LazyColumn { 
            items(groupedChats.toList()) { (sender, lastMsg) -> 
                ContactChatCard(sender, lastMsg) {
                    val intent = Intent(context, ChatDetailActivity::class.java)
                    intent.putExtra("contact_name", sender)
                    context.startActivity(intent)
                }
            } 
        } 
    }
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var chats by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { chats = it.filter { msg -> !msg.sender.contains("(") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(chats) { notif -> MessageCard(notif) } } }
}

@Composable
fun GroupScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var groups by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { groups = it.filter { msg -> msg.sender.contains("(") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(groups) { notif -> MessageCard(notif) } } }
}

@Composable
fun GhostVaultScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var deletedMessages by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    
    LaunchedEffect(Unit) { 
        db.notificationDao().getAllNotifications().collectLatest { allMsgs -> 
            deletedMessages = allMsgs.filter { msg -> 
                msg.isDeleted || msg.message.lowercase().contains("delete") || msg.message.lowercase().contains("deleted")
            } 
        } 
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { 
        if (deletedMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No deleted messages recovered yet.", color = Color.Gray) }
        } else {
            LazyColumn { items(deletedMessages) { notif -> MessageCard(notif) } } 
        }
    }
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
                        // REAL IMAGE LOADING WITH COIL
                        AsyncImage(
                            model = file,
                            contentDescription = "Captured Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
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
    var shizukuConnected by remember { mutableStateOf(ShizukuManager.checkPermission()) }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("DP Downloader (Shizuku)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter number with country code. Requires Shizuku.", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!shizukuConnected) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A0A0A)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shizuku Not Connected", fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { 
                        if (Shizuku.pingBinder()) {
                            ShizukuManager.requestPermission(1001)
                        } else {
                            Toast.makeText(context, "Please open Shizuku app first", Toast.LENGTH_LONG).show()
                            try { context.startActivity(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")) } catch (e: Exception) { }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Connect Shizuku")
                    }
                }
            }
        }

        OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Enter Number (e.g. 923001234567)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            if (number.length > 9) {
                if (shizukuConnected) {
                    val cleanNumber = number.replace("+", "").trim()
                    val command = "ls /sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp\\ Profile\\ Photos/ | grep $cleanNumber"
                    val result = ShizukuManager.executeCommand(command)
                    if (result.trim().isNotEmpty() && !result.contains("Error")) {
                        Toast.makeText(context, "Profile Picture found!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No DP found for this number", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Download Profile Picture") }
    }
}

@Composable
fun MessageCard(notif: NotificationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (notif.isDeleted) Color(0xFF3A0A0A) else MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
fun SettingsScreen(currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    val context = LocalContext.current
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    var shizukuConnected by remember { mutableStateOf(ShizukuManager.checkPermission()) }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Appearance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("Dark", WeaveTheme.DARK, currentTheme, onThemeChange)
            ThemeChip("Light", WeaveTheme.LIGHT, currentTheme, onThemeChange)
            ThemeChip("Ocean", WeaveTheme.OCEAN, currentTheme, onThemeChange)
            ThemeChip("Sunset", WeaveTheme.SUNSET, currentTheme, onThemeChange)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Permissions & Pro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard("Notification Access", if (notifAccess) "Granted ✓" else "Tap to Grant", Icons.Default.Notifications, if(notifAccess) Color.Green else Color.Red) { 
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) 
        }
        
        SettingsCard("Battery Optimization", if (batteryOpt) "Disabled ✓" else "Tap to Disable", Icons.Default.BatteryFull, if(batteryOpt) Color.Green else Color.Red) { 
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) 
        }

        SettingsCard("Shizuku Pro Mode", if (shizukuConnected) "Connected ✓" else "Tap to Connect", Icons.Default.Build, if(shizukuConnected) Color.Green else Color.Red) { 
            if (Shizuku.pingBinder()) { ShizukuManager.requestPermission(1001) } else { Toast.makeText(context, "Open Shizuku app first", Toast.LENGTH_LONG).show() }
        }
    }
}

@Composable
fun SettingsCard(title: String, subTitle: String, icon: ImageVector, subTitleColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subTitle, color = subTitleColor, fontSize = 12.sp)
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
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SetupScreen(context: Context, onFinish: () -> Unit) {
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome to WEAVE PRO", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Setup your app for 100% working", color = Color.Gray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (notifAccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)) {
            Text(if (notifAccess) "1. Notification Access: Granted ✓" else "1. Allow Notification Access", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (batteryOpt) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant)) {
            Text(if (batteryOpt) "2. Battery Optimization: Disabled ✓" else "2. Disable Battery Optimization", color = if (batteryOpt) Color.White else MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("Continue to App", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}
