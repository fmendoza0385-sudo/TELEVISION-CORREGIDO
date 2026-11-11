package com.grmconexion.lukka.subscription

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Identificador del dispositivo (distinto del ID de usuario/subcuenta).
 * Se envía al servidor como huella SHA-256 (32 hex), nunca el ANDROID_ID en claro.
 */
class DeviceBindingManager(private val context: Context) {

    fun deviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val seed = if (androidId.isNullOrBlank() || androidId == BAD_ANDROID_ID) fallbackInstallId() else androidId
        val digest = MessageDigest.getInstance("SHA-256").digest("lukka|$seed".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.substring(0, 32)
    }

    private fun fallbackInstallId(): String {
        val prefs = context.getSharedPreferences("lukka_device", Context.MODE_PRIVATE)
        var id = prefs.getString("install_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("install_id", id).apply()
        }
        return id
    }

    private companion object {
        const val BAD_ANDROID_ID = "9774d56d682e549c"
    }
}
