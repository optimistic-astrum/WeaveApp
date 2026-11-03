package com.weave.app
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuManager {
    // Public setter taaki MainActivity se update ho sake
    var isGranted = false

    fun checkPermission(): Boolean {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return false
        try {
            isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            return isGranted
        } catch (e: Exception) { return false }
    }

    fun requestPermission(requestCode: Int) {
        try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun executeCommand(cmd: String): String {
        if (!isGranted) return "Error: Permission not granted"
        return try {
            // Shizuku.newProcess reflection se call hota hai
            val process = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .apply { isAccessible = true }
                .invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) { "Error: ${e.message}" }
    }
}
