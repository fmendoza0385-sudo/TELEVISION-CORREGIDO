package com.grmconexion.lukka.subscription

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import com.grmconexion.lukka.BuildConfig
import com.grmconexion.lukka.R
import com.grmconexion.lukka.data.ActivationRepository
import com.grmconexion.lukka.data.ApiResult
import com.grmconexion.lukka.data.LicenseApi

/** Resultado de un intento de activación. [messageRes] es el texto que se muestra al usuario. */
data class ActivationOutcome(
    val state: SubscriptionState,
    @StringRes val messageRes: Int?
) {
    val success: Boolean get() = state.allowsPlayback
}

enum class RefreshResult { UPDATED, REJECTED, OFFLINE, NOT_APPLICABLE }

/**
 * Coordina estado de suscripción, validación, dispositivo y servidor de licencias.
 * Es el ÚNICO lugar que decide si la reproducción está permitida: [isPlaybackAllowed].
 * Los métodos que usan red ([activate], [refreshOnline]) deben llamarse fuera del hilo principal.
 */
class SubscriptionManager(context: Context) {
    private val appContext = context.applicationContext
    private val repo = ActivationRepository(appContext)
    private val device = DeviceBindingManager(appContext)
    private val validator = SubscriptionValidator(BuildConfig.LICENSE_PUBLIC_KEY)
    private val api = LicenseApi(BuildConfig.LICENSE_API_URL)

    /** Evaluación local (sin red): licencia firmada guardada + hora confiable. */
    fun evaluateLocal(): SubscriptionStatus {
        val raw = repo.loadEnvelope() ?: return SubscriptionStatus(SubscriptionState.NO_ACTIVADA)
        if (!validator.isConfigured) return SubscriptionStatus(SubscriptionState.ERROR_SERVIDOR)
        val payload = validator.verifyEnvelope(raw)
        if (payload == null) {
            repo.clear() // firma inválida o datos alterados: se descarta
            return SubscriptionStatus(SubscriptionState.NO_ACTIVADA)
        }
        val now = repo.trustedNowMs()
        return SubscriptionStatus(validator.evaluate(payload, device.deviceId(), now), payload, now)
    }

    fun isPlaybackAllowed(): Boolean = evaluateLocal().state.allowsPlayback

    /** Código del usuario ya activado en este equipo (para reintentos de conexión). */
    fun activatedUserId(): String? = repo.userId()

    fun activate(userId: String, code: String): ActivationOutcome {
        if (!validator.isConfigured || !api.isConfigured) {
            return ActivationOutcome(SubscriptionState.ERROR_SERVIDOR, R.string.err_not_configured)
        }
        val deviceId = device.deviceId()
        return when (val r = api.activate(userId, code, deviceId, Build.MODEL ?: "android")) {
            is ApiResult.Signed -> {
                val p = validator.verifyEnvelope(r.rawEnvelope)
                if (p == null || p.userId != userId || p.deviceId != deviceId) {
                    ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_bad_response)
                } else {
                    repo.save(r.rawEnvelope, p.userId, p.serverTimeMs)
                    val state = validator.evaluate(p, deviceId, repo.trustedNowMs())
                    ActivationOutcome(state, if (state.allowsPlayback) null else R.string.err_code_invalid)
                }
            }
            is ApiResult.Rejected -> mapRejection(r.errorCode)
            is ApiResult.Unavailable -> ActivationOutcome(SubscriptionState.ERROR_SERVIDOR, R.string.err_network)
        }
    }

    /**
     * Consulta al servidor el estado de la licencia ya activada. Actualiza reloj confiable y datos.
     * NUNCA extiende la vigencia: solo el servidor firma la expiración.
     */
    fun refreshOnline(): RefreshResult {
        val userId = repo.userId() ?: return RefreshResult.NOT_APPLICABLE
        if (!validator.isConfigured || !api.isConfigured) return RefreshResult.NOT_APPLICABLE
        val deviceId = device.deviceId()
        return when (val r = api.status(userId, deviceId)) {
            is ApiResult.Signed -> {
                val p = validator.verifyEnvelope(r.rawEnvelope)
                if (p == null || p.userId != userId || p.deviceId != deviceId) {
                    RefreshResult.OFFLINE // respuesta alterada: no se acepta ni se borra la licencia previa
                } else {
                    repo.save(r.rawEnvelope, p.userId, p.serverTimeMs)
                    RefreshResult.UPDATED
                }
            }
            is ApiResult.Rejected -> {
                // El servidor dice que ya no hay licencia válida para este equipo.
                if (r.errorCode in HARD_REJECTIONS) repo.clear()
                RefreshResult.REJECTED
            }
            is ApiResult.Unavailable -> RefreshResult.OFFLINE
        }
    }

    private fun mapRejection(code: String): ActivationOutcome = when (code) {
        "USUARIO_NO_EXISTE" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_user_not_found)
        "CODIGO_INVALIDO", "CODIGO_FUERA_DE_ORDEN" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_code_invalid)
        "CODIGO_USADO" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_code_used)
        "CICLO_ANTERIOR" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_code_old)
        "DISPOSITIVO_NO_AUTORIZADO" -> ActivationOutcome(SubscriptionState.DISPOSITIVO_NO_AUTORIZADO, R.string.err_device)
        "LICENCIA_REVOCADA" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_revoked)
        "DEMASIADOS_INTENTOS" -> ActivationOutcome(SubscriptionState.VALIDACION_PENDIENTE, R.string.err_rate)
        "FORMATO_INVALIDO" -> ActivationOutcome(SubscriptionState.CODIGO_INVALIDO, R.string.err_format)
        else -> ActivationOutcome(SubscriptionState.ERROR_SERVIDOR, R.string.err_server)
    }

    private companion object {
        val HARD_REJECTIONS = setOf("LICENCIA_REVOCADA", "DISPOSITIVO_NO_AUTORIZADO", "NO_ACTIVADA")
    }
}
