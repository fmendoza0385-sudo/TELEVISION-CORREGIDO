package com.grmconexion.lukka

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
        setContentView(R.layout.activity_launcher)
        Immersive.apply(this)
    }

    override fun onResume() {
        super.onResume()
        val manager = SubscriptionManager(this)
        thread(name = "lukka-gate") {
            // Sincronización breve con el servidor si ya hay un usuario activado (falla en silencio sin red).
            if (manager.activatedUserId() != null) manager.refreshOnline()
            val status = manager.evaluateLocal()
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val target = when {
                    status.state.allowsPlayback -> PlayerActivity::class.java
                    status.state == SubscriptionState.NO_ACTIVADA -> ActivationActivity::class.java
                    else -> BlockingActivity::class.java
                }
                startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
    }
}
