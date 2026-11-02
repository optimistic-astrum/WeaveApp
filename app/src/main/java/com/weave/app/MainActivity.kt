package com.weave.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weave_database").build()

        setContent {
            var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }

            LaunchedEffect(Unit) {
                db.notificationDao().getAllNotifications().collectLatest {
                    notifications = it
                }
            }

            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WEAVE", color = Color(0xFF00E5FF), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Text("Access", color = Color(0xFF00E5FF))
                    }
                }

                // List
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Waiting for messages...", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(notif.sender, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    Text(notif.message, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
