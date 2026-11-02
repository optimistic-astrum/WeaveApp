package com.weave.app
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.*

class WeaveNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return // Skip duplicates
        
        if (sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val sender = extras.getString(Notification.EXTRA_TITLE)?.trim() ?: "Unknown"
            
            var finalMessage = ""
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                val lastMsg = android.app.Notification.MessagingStyle.Message.getMessageFromBundleArray(messages)?.lastOrNull()
                finalMessage = lastMsg?.text?.toString() ?: ""
            } else {
                finalMessage = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            }

            if (finalMessage.isNotEmpty() && !finalMessage.contains("END-TO-END")) {
                val lowerMsg = finalMessage.lowercase()
                // Exact match for deleted messages only
                val isDeleteEvent = lowerMsg == "this message was deleted" || lowerMsg == "you deleted this message" || lowerMsg.contains("message was deleted")

                val db = DatabaseProvider.getDatabase(applicationContext)

                if (isDeleteEvent) {
                    serviceScope.launch { db.notificationDao().markLastAsDeleted(sender) }
                } else {
                    serviceScope.launch {
                        db.notificationDao().insert(NotificationEntity(appName = "WhatsApp", sender = sender, message = finalMessage, timestamp = System.currentTimeMillis()))
                    }
                }
            }
        }
    }
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }
}
