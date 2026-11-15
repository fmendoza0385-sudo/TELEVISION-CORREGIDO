package com.grmconexion.lukka

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

class LukkaAuthManager(private val context: Context) {

    private var lukkaData: LukkaConfig? = null

    init {
        loadJsonData()
    }

    private fun loadJsonData() {
        try {
            context.assets.open("LUKKA_Banco_Codigos_Activacion_TEMPORAL.json").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    lukkaData = Gson().fromJson(reader, LukkaConfig::class.java)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            lukkaData = null
        }
    }

    /** Valida que el código mensual pertenezca al usuario y esté disponible. */
    fun validateCode(userId: String, inputCode: String): Boolean {
        val user = lukkaData?.users?.firstOrNull { it.user_id == userId } ?: return false
        val codeEntry = user.monthly_codes
            ?.firstOrNull { it.activation_code == inputCode }
            ?: return false

        return codeEntry.status.equals("DISPONIBLE", ignoreCase = true)
    }

    /** Obtiene el nombre del plan asignado al usuario. */
    fun getUserPlan(userId: String): String? {
        val user = lukkaData?.users?.firstOrNull { it.user_id == userId } ?: return null
        return lukkaData?.plans?.firstOrNull { it.plan_id == user.plan_id }?.name
            ?: user.plan_name
    }
}
