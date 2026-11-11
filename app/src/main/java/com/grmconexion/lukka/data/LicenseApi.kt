package com.grmconexion.lukka.data

import com.grmconexion.lukka.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class ApiResult {
    /** Respuesta con sobre firmado (aún debe verificarse la firma). */
    data class Signed(val rawEnvelope: String) : ApiResult()
    /** El servidor rechazó la petición (código de error). Nunca concede acceso. */
    data class Rejected(val errorCode: String) : ApiResult()
    /** Sin conexión, tiempo agotado, HTTP 5xx o respuesta ilegible. Nunca concede acceso. */
    data class Unavailable(val detail: String) : ApiResult()
}

class LicenseApi(private val baseUrl: String) {

    /** En producción exige https://. http:// solo se admite en compilaciones de depuración. */
    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") || (BuildConfig.DEBUG && baseUrl.startsWith("http://"))

    fun activate(userId: String, code: String, deviceId: String, deviceModel: String): ApiResult =
        post(
            "activate.php",
            JSONObject().put("user_id", userId).put("code", code)
                .put("device_id", deviceId).put("device_model", deviceModel)
        )

    fun status(userId: String, deviceId: String): ApiResult =
        post("status.php", JSONObject().put("user_id", userId).put("device_id", deviceId))

    private fun post(endpoint: String, body: JSONObject): ApiResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(baseUrl.trimEnd('/') + "/" + endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val http = conn.responseCode
            val stream = if (http in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (http >= 500) return ApiResult.Unavailable("HTTP $http")

            val json = JSONObject(text)
            when {
                http == 200 && json.has("payload") && json.has("signature") -> ApiResult.Signed(text)
                json.has("error") -> ApiResult.Rejected(json.getString("error"))
                else -> ApiResult.Unavailable("Respuesta inesperada (HTTP $http)")
            }
        } catch (e: IOException) {
            ApiResult.Unavailable("Sin conexión")
        } catch (e: JSONException) {
            ApiResult.Unavailable("Respuesta no válida")
        } finally {
            conn?.disconnect()
        }
    }
}
