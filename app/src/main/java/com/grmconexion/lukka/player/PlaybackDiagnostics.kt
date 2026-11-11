package com.grmconexion.lukka.player

import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.grmconexion.lukka.data.Channel
import java.net.URI

/**
 * Registro técnico para investigar "audio sin imagen" (Documento Maestro, sección 20.4).
 * Las URL se REDACTAN: solo se registra el host; nunca rutas, usuarios, claves ni tokens.
 */
@UnstableApi
object PlaybackDiagnostics {
    private const val TAG = "LukkaPlayer"

    fun logTracks(channel: Channel?, tracks: Tracks) {
        val sb = StringBuilder("pistas canal=${channel?.name} host=${host(channel)} ")
        sb.append("equipo=${Build.MANUFACTURER} ${Build.MODEL} android=${Build.VERSION.SDK_INT} | ")
        for (g in tracks.groups) {
            val kind = when (g.type) {
                C.TRACK_TYPE_VIDEO -> "VIDEO"
                C.TRACK_TYPE_AUDIO -> "AUDIO"
                else -> continue
            }
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                sb.append("[$kind mime=${f.sampleMimeType} codecs=${f.codecs} ${f.width}x${f.height} ")
                sb.append("fps=${f.frameRate} sel=${g.isTrackSelected(i)} soportada=${g.isTrackSupported(i)}] ")
            }
        }
        Log.i(TAG, sb.toString())
    }

    fun logError(channel: Channel?, e: PlaybackException) {
        Log.w(
            TAG,
            "error canal=${channel?.name} host=${host(channel)} codigo=${e.errorCodeName} " +
                "equipo=${Build.MANUFACTURER} ${Build.MODEL} android=${Build.VERSION.SDK_INT} detalle=${e.cause?.javaClass?.simpleName}"
        )
    }

    private fun host(channel: Channel?): String = try {
        URI(channel?.url ?: "").host ?: "?"
    } catch (e: Exception) {
        "?"
    }
}
