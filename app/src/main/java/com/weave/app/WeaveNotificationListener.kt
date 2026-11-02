package com.weave.app
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.room.Room
import kotlinx.coroutines.*
class WeaveNotificationListener : NotificationListenerService() {
    private lateinit var database: AppDatabase
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weave_database").fallbackToDestructiveMigration().build()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
            var finalMessage = ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (textLines != null && textLines.isNotEmpty()) {
                finalMessage = textLines.joinToString("\n") { it.toString() }
            } else {
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                finalMessage = if (bigText.isNotEmpty()) bigText else text
            }
            if (finalMessage.isNotEmpty() && !finalMessage.contains("END-TO-END")) {
                val lowerMsg = finalMessage.lowercase()
                val isDeleteEvent = lowerMsg.contains("deleted") || lowerMsg.contains("delete")
                if (isDeleteEvent) {
                    CoroutineScope(Dispatchers.IO).launch { database.notificationDao().markLastAsDeleted(sender) }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.notificationDao().insert(NotificationEntity(appName = "WhatsApp", sender = sender, message = finalMessage, timestamp = System.currentTimeMillis()))
                    }
                }
            }
        }
    }
}
