package com.grmconexion.lukka.blocking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.grmconexion.lukka.R
import com.grmconexion.lukka.activation.ActivationActivity
import com.grmconexion.lukka.player.PlayerActivity
import com.grmconexion.lukka.subscription.SubscriptionManager
import com.grmconexion.lukka.util.Immersive

/** Pantalla de SERVICIO SUSPENDIDO. Estado principal mientras la licencia no sea válida. */
class BlockingActivity : AppCompatActivity() {

    private lateinit var manager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocking)
        Immersive.apply(this)
        manager = SubscriptionManager(this)

        // La tecla Atrás NO regresa al reproductor mientras exista bloqueo.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* bloqueado a propósito */ }
        })

        val support = getString(R.string.support_contact)
        findViewById<TextView>(R.id.tv_support).apply {
            text = support
            visibility = if (support.isBlank()) View.GONE else View.VISIBLE
        }

        val button = findViewById<Button>(R.id.btn_activate)
        button.setOnClickListener { startActivity(Intent(this, ActivationActivity::class.java)) }
        button.requestFocus() // foco inicial en Android TV
    }

    override fun onResume() {
        super.onResume()
        Immersive.apply(this)
        // Si la licencia volvió a ser válida (p. ej. tras renovar), se continúa al reproductor.
        if (manager.isPlaybackAllowed()) {
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }
}
