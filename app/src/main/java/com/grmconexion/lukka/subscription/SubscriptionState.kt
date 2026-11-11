package com.grmconexion.lukka.subscription

/** Estados definidos en el Documento Maestro (sección 6.3). */
enum class SubscriptionState {
    NO_ACTIVADA,
    ACTIVA,
    POR_VENCER,
    VENCIDA,
    CODIGO_INVALIDO,
    DISPOSITIVO_NO_AUTORIZADO,
    VALIDACION_PENDIENTE,
    ERROR_SERVIDOR;

    /** ÚNICO criterio que habilita la reproducción. */
    val allowsPlayback: Boolean
        get() = this == ACTIVA || this == POR_VENCER
}

data class SubscriptionStatus(
    val state: SubscriptionState,
    val license: LicensePayload? = null,
    val nowMs: Long = 0L
) {
    val validUntilMs: Long get() = license?.validUntilMs ?: 0L
    val planId: String? get() = license?.planId
}
