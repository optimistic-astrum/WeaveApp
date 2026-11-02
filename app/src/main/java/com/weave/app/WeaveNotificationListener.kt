package com.weave.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeaveNotificationListener : NotificationListenerService() {

    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "weave_database"
        ).fallbackToDestructiveMigration().build()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
            
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            
            val finalMessage = when {
                bigText.isNotEmpty() -> bigText
                lines != null && lines.isNotEmpty() -> lines.joinToString("\n") { it.toString() }
                else -> text
            }

            if (finalMessage.isNotEmpty()) {
                val isDeleteEvent = finalMessage.contains("message was deleted") || 
                                    finalMessage.contains("deleted this message") ||
                                    finalMessage.contains("You deleted this message")

                if (isDeleteEvent) {
                    CoroutineScope(Dispatchers.IO).launch {
                        database.notificationDao().markLastAsDeleted(sender)
                    }
                } else {
                    val entity = NotificationEntity(
                        appName = "WhatsApp",
                        sender = sender,
                        message = finalMessage,
                        timestamp = System.currentTimeMillis(),
                        isDeleted = false
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        database.notificationDao().insert(entity)
                    }
                }
            }
        }
    }
}
