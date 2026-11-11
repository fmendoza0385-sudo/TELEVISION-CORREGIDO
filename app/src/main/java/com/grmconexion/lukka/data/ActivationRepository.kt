package com.grmconexion.lukka.data

import android.content.Context
import android.os.SystemClock

/**
 * Persistencia local de la licencia firmada y de los datos de reloj confiable.
 *
 * La licencia guardada NO necesita cifrado para ser segura: está FIRMADA por el servidor y la app
 * verifica la firma cada vez que la lee. Si alguien la altera, la firma falla y se descarta.
 */
class ActivationRepository(context: Context) {
    private val prefs = context.getSharedPreferences("lukka_license", Context.MODE_PRIVATE)

    fun loadEnvelope(): String? = prefs.getString(K_ENVELOPE, null)
    fun userId(): String? = prefs.getString(K_USER, null)

    fun save(envelope: String, userId: String, serverTimeMs: Long) {
        prefs.edit()
            .putString(K_ENVELOPE, envelope)
            .putString(K_USER, userId)
            .putLong(K_SYNC_SERVER, serverTimeMs)
            .putLong(K_SYNC_ELAPSED, SystemClock.elapsedRealtime())
            .putLong(K_LAST_SEEN, maxOf(serverTimeMs, prefs.getLong(K_LAST_SEEN, 0L)))
            .commit()
    }

    fun clear() {
        prefs.edit().remove(K_ENVELOPE).apply() // se conserva K_LAST_SEEN (anti-retroceso del reloj)
    }

    /**
     * Hora confiable: el reloj del dispositivo nunca autoriza nada por sí solo.
     *  - Mismo arranque desde la última sincronización: hora del servidor + tiempo monotónico transcurrido
     *    (cambiar la fecha del equipo no tiene efecto).
     *  - Tras reiniciar sin conexión: máximo entre reloj local y la última hora vista (no se puede retroceder).
     */
    fun trustedNowMs(): Long {
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val syncServer = prefs.getLong(K_SYNC_SERVER, 0L)
        val syncElapsed = prefs.getLong(K_SYNC_ELAPSED, 0L)
        val lastSeen = prefs.getLong(K_LAST_SEEN, 0L)

        val base = if (syncServer > 0L && elapsed >= syncElapsed) {
            syncServer + (elapsed - syncElapsed)
        } else {
            maxOf(wall, syncServer)
        }
        val now = maxOf(base, lastSeen)
        if (now > lastSeen) prefs.edit().putLong(K_LAST_SEEN, now).apply()
        return now
    }

    private companion object {
        const val K_ENVELOPE = "envelope"
        const val K_USER = "user_id"
        const val K_SYNC_SERVER = "sync_server_ms"
        const val K_SYNC_ELAPSED = "sync_elapsed_ms"
        const val K_LAST_SEEN = "last_seen_ms"
    }
}
