package com.weave.app
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuManager {
    var isGranted = false
        private set

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
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) { "Error: ${e.message}" }
    }
}
