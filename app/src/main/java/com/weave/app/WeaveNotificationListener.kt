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
        ).build()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Filhal WhatsApp ke liye filter
        if (sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val sender = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
            val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (message.isNotEmpty()) {
                val entity = NotificationEntity(
                    appName = "WhatsApp",
                    sender = sender,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
                CoroutineScope(Dispatchers.IO).launch {
                    database.notificationDao().insert(entity)
                }
            }
        }
    }
}
