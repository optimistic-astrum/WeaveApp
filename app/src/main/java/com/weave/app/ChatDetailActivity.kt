package com.weave.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import kotlinx.coroutines.flow.collectLatest

class ChatDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contactName = intent.getStringExtra("contact_name") ?: "Unknown"
        val db = DatabaseProvider.getDatabase(this)

        setContent {
            MaterialTheme(colorScheme = DarkColors) {
                var messages by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
                
                LaunchedEffect(Unit) {
                    db.notificationDao().getAllNotifications().collectLatest {
                        messages = it.filter { msg -> msg.sender == contactName }.reversed()
                    }
                }

                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(contactName, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    // Chat Messages
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.End),
                                shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (msg.isDeleted) Color(0xFF3A0A0A) else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(msg.message, color = MaterialTheme.colorScheme.onSurface)
                                    if (msg.isDeleted) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deleted", tint = Color.Red, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Deleted", color = Color.Red, fontSize = 10.sp)
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
