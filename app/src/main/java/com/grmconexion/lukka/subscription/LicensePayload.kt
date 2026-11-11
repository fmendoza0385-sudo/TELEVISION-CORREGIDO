package com.grmconexion.lukka.subscription

import org.json.JSONObject

/** Contenido firmado por el servidor de licencias (ver docs/FORMATO_LICENCIAS.md). */
data class LicensePayload(
    val schemaVersion: Int,
    val result: String,
    val userId: String,
    val licenseId: String,
    val planId: String,
    val renewalNumber: Int,
    val validFromMs: Long,
    val validUntilMs: Long,
    val deviceId: String,
    val serverTimeMs: Long
) {
    companion object {
        const val SUPPORTED_SCHEMA = 1

        /** Devuelve null si falta algún campo o el tipo no es el esperado. */
        fun parse(json: String): LicensePayload? = try {
            val o = JSONObject(json)
            LicensePayload(
                schemaVersion = o.getInt("schema_version"),
                result = o.getString("result"),
                userId = o.getString("user_id"),
                licenseId = o.getString("license_id"),
                planId = o.getString("plan_id"),
                renewalNumber = o.getInt("renewal_number"),
                validFromMs = o.getLong("valid_from_ms"),
                validUntilMs = o.getLong("valid_until_ms"),
                deviceId = o.getString("device_id"),
                serverTimeMs = o.getLong("server_time_ms")
            )
        } catch (e: Exception) {
            null
        }
    }
}
