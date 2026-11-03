package com.weave.app
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.FileObserver
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
class WeaveService : Service() {
    private val CHANNEL_ID = "weave_service_channel"
    private val observers = mutableListOf<FileObserver>()
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Weave Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        startForeground(1, NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Weave is Active").setContentText("Protecting your messages and media...").setSmallIcon(android.R.drawable.ic_menu_info_details).setPriority(NotificationCompat.PRIORITY_LOW).build())
        setupObservers()
    }
    private fun setupObservers() {
        val paths = listOf("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/", "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp View Once/", "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/")
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                val observer = object : FileObserver(dir, FileObserver.CREATE) {
                    override fun onEvent(event: Int, file: String?) {
                        if (file != null && event == FileObserver.CREATE) copyMediaToVault(File(dir, file))
                    }
                }
                observer.startWatching(); observers.add(observer)
            }
        }
    }
    private fun copyMediaToVault(sourceFile: File) {
        try {
            val vaultDir = File(applicationContext.filesDir, "media_vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()
            val destFile = File(vaultDir, "W_${System.currentTimeMillis()}_${sourceFile.name}")
            val input = FileInputStream(sourceFile); val output = FileOutputStream(destFile)
            input.copyTo(output); input.close(); output.close()
        } catch (e: Exception) { e.printStackTrace() }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { observers.forEach { it.stopWatching() }; super.onDestroy() }
}
