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
        lukkaData = try {
            context.assets.open("LUKKA_Banco_Codigos_Activacion_TEMPORAL.json").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    Gson().fromJson(reader, LukkaConfig::class.java)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Valida que el código mensual pertenezca al usuario y esté disponible. */
    fun validateCode(userId: String, inputCode: String): Boolean {
        val normalizedUserId = userId.trim()
        val normalizedCode = inputCode.trim()
        if (normalizedUserId.isEmpty() || normalizedCode.isEmpty()) return false

        val user = lukkaData?.users
            ?.firstOrNull { it.user_id == normalizedUserId }
            ?: return false

        val codeEntry = user.monthly_codes
            ?.firstOrNull { it.activation_code == normalizedCode }
            ?: return false

        return codeEntry.status?.equals("DISPONIBLE", ignoreCase = true) == true
    }

    /** Obtiene el nombre del plan asignado al usuario. */
    fun getUserPlan(userId: String): String? {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isEmpty()) return null

        val user = lukkaData?.users
            ?.firstOrNull { it.user_id == normalizedUserId }
            ?: return null

        val planId = user.plan_id
        val planName = if (planId.isNullOrBlank()) {
            null
        } else {
            lukkaData?.plans?.firstOrNull { it.plan_id == planId }?.name
        }

        return planName ?: user.plan_name
    }
}
