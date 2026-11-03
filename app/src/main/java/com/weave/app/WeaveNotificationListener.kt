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
            
            // Safe extraction for all Android versions
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            
            var finalMessage = ""
            if (textLines != null && textLines.isNotEmpty()) {
                finalMessage = textLines.joinToString("\n") { it.toString() }
            } else if (bigText.isNotEmpty()) {
                finalMessage = bigText
            } else {
                finalMessage = text
            }

            if (finalMessage.isNotEmpty() && !finalMessage.contains("END-TO-END")) {
                val lowerMsg = finalMessage.lowercase()
                val isDeleteEvent = lowerMsg == "this message was deleted" || 
                                    lowerMsg == "you deleted this message" || 
                                    lowerMsg.contains("message was deleted")

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
