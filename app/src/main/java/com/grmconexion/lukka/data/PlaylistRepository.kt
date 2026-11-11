package com.grmconexion.lukka.data

import android.content.Context

/** Selecciona la lista M3U según el plan autorizado por el código (sin configuración manual). */
object PlaylistRepository {
    private val FILES = mapOf(
        "PLAN-50" to "m3u_plan_50.m3u",
        "PLAN-80" to "m3u_plan_80.m3u",
        "PLAN-120" to "m3u_plan_120.m3u"
    )

    /** Devuelve lista vacía si el archivo no existe o no se puede leer (la app lo informa, no se cierra). */
    fun load(context: Context, planId: String): List<Channel> {
        val file = FILES[planId] ?: return emptyList()
        return try {
            context.assets.open(file).bufferedReader(Charsets.UTF_8).use { M3uParser.parse(it.readText()) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
