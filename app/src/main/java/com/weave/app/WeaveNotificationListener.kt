package com.weave.app
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.*

class WeaveNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        
        if (sbn.packageName == "com.whatsapp" || sbn.packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE)?.trim() ?: "Unknown"
            val subText = extras.getString(Notification.EXTRA_SUB_TEXT)?.trim() ?: ""
            
            var finalMessage = ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (textLines != null && textLines.isNotEmpty()) {
                finalMessage = textLines.joinToString("\n") { it.toString() }
            } else {
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                finalMessage = if (bigText.isNotEmpty()) bigText else text
            }

            val senderName = if (subText.isNotEmpty()) "$subText ($title)" else title

            if (finalMessage.isNotEmpty() && !finalMessage.contains("END-TO-END")) {
                val lowerMsg = finalMessage.lowercase()
                val isDeleteEvent = lowerMsg.contains("deleted") || lowerMsg.contains("delete")

                val db = DatabaseProvider.getDatabase(applicationContext)

                if (isDeleteEvent) {
                    serviceScope.launch { db.notificationDao().markLastAsDeleted(senderName) }
                } else {
                    serviceScope.launch {
                        db.notificationDao().insert(NotificationEntity(appName = "WhatsApp", sender = senderName, message = finalMessage, timestamp = System.currentTimeMillis()))
                    }
                }
            }
        }
    }
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }
}
