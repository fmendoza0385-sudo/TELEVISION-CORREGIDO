package com.grmconexion.lukka.subscription

import java.util.Calendar
import java.util.TimeZone

/**
 * Reglas de fecha de corte (Documento Maestro v1.2, sección 21.1):
 * día 5 de cada mes, 12:00:00, zona horaria America/Bogota, sin período de gracia.
 */
object SubscriptionPolicy {
    const val CUTOFF_DAY = 5
    const val CUTOFF_HOUR = 12
    const val ZONE_ID = "America/Bogota"
    const val NEAR_EXPIRY_DAYS = 3

    fun zone(): TimeZone = TimeZone.getTimeZone(ZONE_ID)

    /** Primer instante de corte ESTRICTAMENTE posterior a [timeMs]. */
    fun nextCutoffAfter(timeMs: Long): Long {
        val cal = Calendar.getInstance(zone())
        cal.timeInMillis = timeMs
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val thisMonthCutoff = cutoffOf(year, month)
        return if (timeMs < thisMonthCutoff) thisMonthCutoff else cutoffOf(year, month + 1)
    }

    /** Instante del corte para un mes (month 0-11; 12 pasa al enero siguiente). */
    fun cutoffOf(year: Int, month0: Int): Long {
        val cal = Calendar.getInstance(zone())
        cal.clear()
        cal.set(year, month0, CUTOFF_DAY, CUTOFF_HOUR, 0, 0)
        return cal.timeInMillis
    }

    fun isExpired(nowMs: Long, validUntilMs: Long): Boolean = nowMs >= validUntilMs

    fun isNearExpiry(nowMs: Long, validUntilMs: Long): Boolean =
        validUntilMs - nowMs <= NEAR_EXPIRY_DAYS * 24L * 3600L * 1000L
}
