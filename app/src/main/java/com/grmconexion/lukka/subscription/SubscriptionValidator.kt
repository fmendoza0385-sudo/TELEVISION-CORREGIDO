package com.grmconexion.lukka.subscription

import android.util.Base64
import org.json.JSONObject

/**
 * Valida respuestas del servidor de licencias y evalúa el estado de una licencia.
 * La interfaz NUNCA decide por sí sola que un código es válido: solo cree a este componente.
 */
class SubscriptionValidator(private val publicKeyBase64: String) {

    val isConfigured: Boolean get() = publicKeyBase64.isNotBlank()

    /**
     * Comprueba la firma del sobre {"payload": base64, "signature": base64}.
     * Devuelve null si no hay clave, la firma no coincide o el contenido es incompleto/alterado.
     */
    fun verifyEnvelope(rawEnvelope: String): LicensePayload? {
        if (!isConfigured) return null
        return try {
            val env = JSONObject(rawEnvelope)
            val payloadBytes = Base64.decode(env.getString("payload"), Base64.DEFAULT)
            val signature = Base64.decode(env.getString("signature"), Base64.DEFAULT)
            val key = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            if (!LicenseVerifier.verify(payloadBytes, signature, key)) return null
            LicensePayload.parse(String(payloadBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    /** Evaluación pura (sin Android) para poder probarla con pruebas unitarias. */
    fun evaluate(payload: LicensePayload, localDeviceId: String, nowMs: Long): SubscriptionState {
        if (payload.schemaVersion != LicensePayload.SUPPORTED_SCHEMA) return SubscriptionState.VALIDACION_PENDIENTE
        if (payload.result != "VALID") return SubscriptionState.CODIGO_INVALIDO
        if (payload.deviceId != localDeviceId) return SubscriptionState.DISPOSITIVO_NO_AUTORIZADO
        if (payload.validUntilMs <= payload.validFromMs) return SubscriptionState.CODIGO_INVALIDO
        return when {
            SubscriptionPolicy.isExpired(nowMs, payload.validUntilMs) -> SubscriptionState.VENCIDA
            SubscriptionPolicy.isNearExpiry(nowMs, payload.validUntilMs) -> SubscriptionState.POR_VENCER
            else -> SubscriptionState.ACTIVA
        }
    }
}
