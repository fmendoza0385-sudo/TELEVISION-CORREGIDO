package com.grmconexion.lukka

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader
import java.util.Calendar
import java.util.TimeZone

class LukkaAuthManager(private val context: Context) {

    private var lukkaData: LukkaConfig? = null

    init {
        loadJsonData()
    }

    private fun loadJsonData() {
        try {
            val inputStream = context.assets.open("LUKKA_Banco_Codigos_Activacion_TEMPORAL.json")
            val reader = InputStreamReader(inputStream)
            lukkaData = Gson().fromJson(reader, LukkaConfig::class.java)
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Valida si el código de activación ingresado es correcto para el usuario.
     */
    fun validateCode(userId: String, inputCode: String): Boolean {
        val user = lukkaData?.users?.find { it.user_id == userId } ?: return false
        val codeEntry = user.codes.find { it.code == inputCode } ?: return false

        // Verifica que el estado sea DISPONIBLE
        if (codeEntry.status != "DISPONIBLE") return false

        return true
    }
    
    /**
     * Obtiene el plan asignado al usuario (ej. PLAN-50, PLAN-80, PLAN-120).
     */
    fun getUserPlan(userId: String): String? {
        val user = lukkaData?.users?.find { it.user_id == userId }
        val plan = lukkaData?.plans?.find { it.id == user?.plan_id }
        return plan?.name
    }
}