package com.grmconexion.lukka

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.grmconexion.lukka.activation.ActivationActivity
import com.grmconexion.lukka.blocking.BlockingActivity
import com.grmconexion.lukka.player.PlayerActivity
import com.grmconexion.lukka.subscription.SubscriptionManager
import com.grmconexion.lukka.subscription.SubscriptionState
import com.grmconexion.lukka.util.Immersive
import kotlin.concurrent.thread

/**
 * Puerta de entrada (splash). Consulta el estado local, intenta sincronizar con el servidor
 * (hora confiable / revocaciones) y decide la pantalla. Nunca inicia reproducción por sí misma.
 */
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_launcher)
            Immersive.apply(this)
        } catch (t: Throwable) {
            showCrashDialog("Error en LauncherActivity.onCreate", t)
        }
    }

    override fun onResume() {
        super.onResume()
        thread(name = "lukka-gate") {
            try {
                val manager = SubscriptionManager(this)
                // Sincronización breve con el servidor si ya hay un usuario activado (falla en silencio sin red).
                if (manager.activatedUserId() != null) {
                    manager.refreshOnline()
                }
                val status = manager.evaluateLocal()
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    try {
                        val target = when {
                            status.state.allowsPlayback -> PlayerActivity::class.java
                            status.state == SubscriptionState.NO_ACTIVADA -> ActivationActivity::class.java
                            else -> BlockingActivity::class.java
                        }
                        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    } catch (t: Throwable) {
                        showCrashDialog("Error al abrir pantalla destino", t)
                    }
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    showCrashDialog("Error en SubscriptionManager / Gate", t)
                }
            }
        }
    }

    private fun showCrashDialog(title: String, throwable: Throwable) {
        if (isFinishing || isDestroyed) return
        val details = buildString {
            append(throwable.javaClass.name)
            append(": ")
            append(throwable.message ?: "Sin mensaje")
            append("\n\n")
            throwable.stackTrace.take(8).forEach { element ->
                append("at ")
                append(element.toString())
                append("\n")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(details)
            .setCancelable(false)
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .show()
    }
}
