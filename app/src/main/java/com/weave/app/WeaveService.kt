package com.weave.app
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
class WeaveService : Service() {
    private val CHANNEL_ID = "weave_service_channel"
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Weave Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
        startForeground(1, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Weave is Active").setContentText("Protecting your messages...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details).setPriority(NotificationCompat.PRIORITY_LOW).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
