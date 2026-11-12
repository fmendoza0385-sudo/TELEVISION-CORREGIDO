package com.grmconexion.lukka

data class LukkaConfig(
    val schema: String?,
    val version: String?,
    val generated_at: String?,
    val configuration: LukkaConfiguration?,
    val plans: List<LukkaPlan>?,
    val users: List<LukkaUser>?
)

data class LukkaConfiguration(
    val timezone: String?,
    val monthly_cutoff_day: Int,
    val monthly_cutoff_time: String?,
    val offline_policy: OfflinePolicy?
)

data class OfflinePolicy(
    val cached_validation: Boolean,
    val block_on_cutoff_exceeded: Boolean,
    val retry_interval_seconds: Int
)

data class LukkaPlan(
    val id: String?,
    val name: String?,
    val channels: Int
)

data class LukkaUser(
    val user_id: String?,
    val plan_id: String?,
    val codes: List<ActivationCode>?
)

data class ActivationCode(
    val renewal_number: Int,
    val code: String?,
    val type: String?,
    val status: String?
)
