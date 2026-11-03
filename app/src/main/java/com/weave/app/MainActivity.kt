package com.weave.app

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
            var isLocked by remember { mutableStateOf(prefs.getBoolean("is_locked", false)) }
            var currentTheme by remember { mutableStateOf(WeaveTheme.valueOf(prefs.getString("theme", "GOLDEN") ?: "GOLDEN")) }

            MaterialTheme(colorScheme = getThemeColors(currentTheme)) {
                if (!isSetupDone) {
                    SetupScreen(context) { prefs.edit().putBoolean("setup_done", true).apply(); isSetupDone = true }
                } else if (isLocked) {
                    LockScreen(onUnlock = { isLocked = false }, currentPin = prefs.getString("app_pin", "1234") ?: "1234")
                } else {
                    MainLayout(currentTheme, onThemeChange = { newTheme -> prefs.edit().putString("theme", newTheme.name).apply(); currentTheme = newTheme })
                }
            }
        }
    }
}

val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult -> if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) ShizukuManager.isGranted = true }
fun isNotificationAccessGranted(context: Context): Boolean = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
fun isBatteryOptimizationDisabled(context: Context): Boolean = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)

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
                        0 -> DashboardScreen()
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

@Composable
fun BottomNavBar(selectedScreen: Int, onScreenSelected: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home", fontSize = 10.sp) }, selected = selectedScreen == 0, onClick = { onScreenSelected(0) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        NavigationBarItem(icon = { Icon(Icons.Default.Person, contentDescription = "Chats") }, label = { Text("Chats", fontSize = 10.sp) }, selected = selectedScreen == 1, onClick = { onScreenSelected(1) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        NavigationBarItem(icon = { Icon(Icons.Default.Group, contentDescription = "Groups") }, label = { Text("Groups", fontSize = 10.sp) }, selected = selectedScreen == 2, onClick = { onScreenSelected(2) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        NavigationBarItem(icon = { Icon(Icons.Default.Delete, contentDescription = "Ghost") }, label = { Text("Ghost", fontSize = 10.sp) }, selected = selectedScreen == 3, onClick = { onScreenSelected(3) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        NavigationBarItem(icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }, label = { Text("Settings", fontSize = 10.sp) }, selected = selectedScreen == 4, onClick = { onScreenSelected(4) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
    }
}

@Composable
fun DrawerItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(title, fontWeight = FontWeight.Medium) }, icon = { Icon(icon, contentDescription = null) }, selected = isSelected, onClick = onClick, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var totalMessages by remember { mutableStateOf(0) }
    var totalDeleted by remember { mutableStateOf(0) }
    var recentChats by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) { 
        db.notificationDao().getAllNotifications().collectLatest { allMsgs -> 
            totalMessages = allMsgs.size
            totalDeleted = allMsgs.count { it.isDeleted || it.message.lowercase().contains("delete") }
            val map = mutableMapOf<String, String>()
            for (msg in allMsgs) { if (!map.containsKey(msg.sender)) map[msg.sender] = msg.message }
            recentChats = map
        } 
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Messages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalMessages", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF3A0A0A)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Deleted", fontSize = 12.sp, color = Color.Gray)
                    Text("$totalDeleted", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Recent Chats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (recentChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No messages yet.\nWaiting for new notifications...", textAlign = TextAlign.Center, color = Color.Gray)
            }
        } else {
            recentChats.toList().take(3).forEach { (sender, lastMsg) ->
                ContactChatCard(sender, lastMsg) {
                    val intent = Intent(context, ChatDetailActivity::class.java)
                    intent.putExtra("contact_name", sender)
                    context.startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactChatCard(senderName: String, lastMessage: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(senderName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(senderName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(lastMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var chats by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { allMsgs -> chats = allMsgs.filter { msg -> !msg.sender.contains("(") }.distinctBy { it.sender } } }
    val filteredChats = chats.filter { it.sender.contains(searchQuery, ignoreCase = true) || it.message.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { 
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search Chats...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp)
        )
        LazyColumn { 
            items(filteredChats) { notif -> 
                ContactChatCard(notif.sender, notif.message) {
                    val intent = Intent(context, ChatDetailActivity::class.java)
                    intent.putExtra("contact_name", notif.sender)
                    context.startActivity(intent)
                }
            } 
        } 
    }
}

@Composable
fun GroupScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var groups by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { allMsgs -> groups = allMsgs.filter { msg -> msg.sender.contains("(") }.distinctBy { it.sender } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { LazyColumn { items(groups) { notif -> 
        ContactChatCard(notif.sender, notif.message) {
            val intent = Intent(context, ChatDetailActivity::class.java)
            intent.putExtra("contact_name", notif.sender)
            context.startActivity(intent)
        }
    } } }
}

@Composable
fun GhostVaultScreen() {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    var deletedMessages by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    LaunchedEffect(Unit) { db.notificationDao().getAllNotifications().collectLatest { allMsgs -> deletedMessages = allMsgs.filter { msg -> msg.isDeleted || msg.message.lowercase().contains("deleted") } } }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { 
        if (deletedMessages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No deleted messages recovered yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No media captured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files) { file ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.aspectRatio(1f).clickable { }) {
                        AsyncImage(model = file, contentDescription = "Captured Media", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
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
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("DP Scanner", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Scan and download profile pictures securely.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!shizukuConnected) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A0A0A)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shizuku Not Connected", fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { 
                        if (Shizuku.pingBinder()) { ShizukuManager.requestPermission(1001) } 
                        else { Toast.makeText(context, "Open Shizuku app first", Toast.LENGTH_LONG).show() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Connect Shizuku") }
                }
            }
        }

        OutlinedTextField(
            value = number, onValueChange = { number = it }, label = { Text("Enter WhatsApp Number") }, 
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp)
        )
        
        Button(
            onClick = { 
                if (number.length > 5 && shizukuConnected) {
                    isScanning = true
                    val cleanNumber = number.replace("+", "").trim()
                    val destPath = "/sdcard/Download/dp_${cleanNumber}.jpg"
                    
                    // Shizuku command to find and copy the DP image
                    val command = "cp \$(find /storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp\\ Profile\\ Photos/ -name '*${cleanNumber}*' | head -n 1) $destPath 2>/dev/null"
                    ShizukuManager.executeCommand(command)
                    
                    val file = File(destPath)
                    if (file.exists() && file.length() > 0) { 
                        previewImagePath = destPath
                        Toast.makeText(context, "DP Found!", Toast.LENGTH_SHORT).show() 
                    } else { 
                        previewImagePath = null
                        Toast.makeText(context, "DP not found.", Toast.LENGTH_LONG).show() 
                    }
                    isScanning = false
                } else { Toast.makeText(context, "Connect Shizuku or enter valid number", Toast.LENGTH_SHORT).show() }
            }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), enabled = !isScanning
        ) { 
            if (isScanning) { CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) } 
            else { Text("Scan & Download DP", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        previewImagePath?.let { path ->
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(12.dp), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = File(path), contentDescription = "DP Preview", modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Profile Picture Found!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saved automatically to /Download/ folder", color = Color.Green, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCard(notif: NotificationEntity) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Message Options", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionButton(Icons.Default.ContentCopy, "Copy") {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", notif.message))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show(); showSheet = false
                    }
                    ActionButton(Icons.Default.Share, "Share") {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, notif.message) }
                        context.startActivity(Intent.createChooser(intent, "Share Message")); showSheet = false
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp).clickable { showSheet = true },
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
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SettingsScreen(currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("weave_prefs", Context.MODE_PRIVATE)
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    var shizukuConnected by remember { mutableStateOf(ShizukuManager.checkPermission()) }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showFaq by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        onDispose { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Change PIN") },
            text = { OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 4) newPin = it }, label = { Text("Enter new 4-digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation()) },
            confirmButton = { TextButton(onClick = { prefs.edit().putString("app_pin", newPin).apply(); Toast.makeText(context, "PIN Updated!", Toast.LENGTH_SHORT).show(); showPinDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTerms) { InfoDialog("Terms & Conditions", "1. Personal Use Only.\n2. No data is uploaded to any server.\n3. Developer is not responsible for misuse.", onClose = { showTerms = false }) }
    if (showPrivacy) { InfoDialog("Privacy Policy", "1. All data is stored locally on your device.\n2. We do not collect any personal information.\n3. Permissions are used only for app functionality.", onClose = { showPrivacy = false }) }
    if (showFaq) { InfoDialog("FAQ", "Q: Why messages not capturing?\nA: Check Notification Access.\n\nQ: Why DP not downloading?\nA: Ensure Shizuku is running.\n\nQ: Where is data stored?\nA: Locally on your phone.", onClose = { showFaq = false }) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Appearance (Themes)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(300.dp)) {
            items(WeaveTheme.values()) { theme -> ThemeCard(theme, currentTheme, onThemeChange) }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Security & Privacy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard("App Lock", if (prefs.getBoolean("is_locked", false)) "Enabled ✓" else "Disabled", Icons.Default.Lock, if(prefs.getBoolean("is_locked", false)) Color.Green else Color.Red) { 
            val current = prefs.getBoolean("is_locked", false); prefs.edit().putBoolean("is_locked", !current).apply(); Toast.makeText(context, "Lock status changed. Restart app to apply.", Toast.LENGTH_SHORT).show()
        }
        SettingsCard("Change App PIN", "Tap to change 4-digit PIN", Icons.Default.Password, MaterialTheme.colorScheme.primary) { showPinDialog = true }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Permissions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard("Notification Access", if (notifAccess) "Granted ✓" else "Tap to Grant", Icons.Default.Notifications, if(notifAccess) Color.Green else Color.Red) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        SettingsCard("Battery Optimization", if (batteryOpt) "Disabled ✓" else "Tap to Disable", Icons.Default.BatteryFull, if(batteryOpt) Color.Green else Color.Red) { val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); intent.data = Uri.parse("package:${context.packageName}"); context.startActivity(intent) }
        SettingsCard("Shizuku Pro Mode", if (shizukuConnected) "Connected ✓" else "Tap to Connect", Icons.Default.Build, if(shizukuConnected) Color.Green else Color.Red) { if (Shizuku.pingBinder()) { ShizukuManager.requestPermission(1001) } else { Toast.makeText(context, "Open Shizuku app first", Toast.LENGTH_LONG).show() } }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Legal & Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard("Terms & Conditions", "Read our terms", Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant) { showTerms = true }
        SettingsCard("Privacy Policy", "How we handle your data", Icons.Default.PrivacyTip, MaterialTheme.colorScheme.onSurfaceVariant) { showPrivacy = true }
        SettingsCard("FAQ", "Common questions", Icons.Default.QuestionAnswer, MaterialTheme.colorScheme.onSurfaceVariant) { showFaq = true }
    }
}

@Composable
fun ThemeCard(theme: WeaveTheme, currentTheme: WeaveTheme, onThemeChange: (WeaveTheme) -> Unit) {
    val isSelected = theme == currentTheme
    Card(modifier = Modifier.clickable { onThemeChange(theme) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(getThemeColors(theme).primary))
            Spacer(modifier = Modifier.width(8.dp))
            Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InfoDialog(title: String, content: String, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text(title, fontWeight = FontWeight.Bold) }, text = { Text(content, fontSize = 14.sp) }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

@Composable
fun SettingsCard(title: String, subTitle: String, icon: ImageVector, subTitleColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(subTitle, color = subTitleColor, fontSize = 12.sp) }
        }
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit, currentPin: String) {
    var pin by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter PIN", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it }, label = { Text("4-Digit PIN") }, modifier = Modifier.width(200.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { if (pin == currentPin) onUnlock() else { Toast.makeText(null, "Wrong PIN", Toast.LENGTH_SHORT).show(); pin = "" } }, modifier = Modifier.width(200.dp).height(50.dp)) { Text("Unlock") }
    }
}

@Composable
fun SetupScreen(context: Context, onFinish: () -> Unit) {
    var notifAccess by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    var batteryOpt by remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome to WEAVE PRO", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Setup your app for 100% working", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
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
