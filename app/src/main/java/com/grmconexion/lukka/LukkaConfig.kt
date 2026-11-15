package com.grmconexion.lukka

/** Modelos alineados con LUKKA_Banco_Codigos_Activacion_TEMPORAL.json. */
data class LukkaConfig(
    val schema: String?,
    val schema_version: String?,
    val generated_at_utc: String?,
    val environment: String?,
    val timezone: String?,
    val subscription_policy: SubscriptionPolicy?,
    val plans: List<LukkaPlan>?,
    val summary: LukkaSummary?,
    val users: List<LukkaUser>?
)

data class SubscriptionPolicy(
    val monthly_cutoff: MonthlyCutoff?,
    val grace_period: String?,
    val offline_policy: OfflinePolicy?
)

data class MonthlyCutoff(
    val day: Int?,
    val time: String?,
    val timezone: String?
)

data class OfflinePolicy(
    val retry_interval_seconds: Int?,
    val use_cached_validated_license: Boolean?,
    val offline_access_must_not_extend_past_signed_license_expiration: Boolean?,
    val block_at_cutoff_if_not_renewed: Boolean?
)

data class LukkaPlan(
    val plan_id: String?,
    val name: String?,
    val channels: Int?
)

data class LukkaSummary(
    val users: Int?,
    val codes_per_user: Int?,
    val total_codes: Int?,
    val unique_code_count: Int?,
    val user_id_start: String?,
    val user_id_end: String?
)

data class LukkaUser(
    val user_id: String?,
    val plan_id: String?,
    val plan_name: String?,
    val monthly_codes: List<ActivationCode>?
)

data class ActivationCode(
    val activation_code: String?,
    val renewal_number: Int?,
    val type: String?,
    val status: String?
)
