package com.grmconexion.lukka.activation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.grmconexion.lukka.R
import com.grmconexion.lukka.player.PlayerActivity
import com.grmconexion.lukka.subscription.SubscriptionManager
import com.grmconexion.lukka.util.Immersive
import kotlin.concurrent.thread

/** Captura ID de usuario y código mensual. La decisión de validez la toma SubscriptionManager. */
class ActivationActivity : AppCompatActivity() {

    private lateinit var manager: SubscriptionManager
    private lateinit var etUser: EditText
    private lateinit var etCode: EditText
    private lateinit var tvMessage: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnValidate: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)
        Immersive.apply(this)
        manager = SubscriptionManager(this)

        etUser = findViewById(R.id.et_user_id)
        etCode = findViewById(R.id.et_code)
        tvMessage = findViewById(R.id.tv_message)
        progress = findViewById(R.id.progress)
        btnValidate = findViewById(R.id.btn_validate)
        btnBack = findViewById(R.id.btn_back)

        manager.activatedUserId()?.let { etUser.setText(it) } // renovación: ID prellenado
        etCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        btnValidate.setOnClickListener { submit() }
        // Atrás (botón o tecla) vuelve a la pantalla anterior (bloqueo), nunca al reproductor.
        btnBack.setOnClickListener { finish() }

        (if (etUser.text.isNullOrEmpty()) etUser else etCode).requestFocus()
    }

    private fun submit() {
        val userId = etUser.text.toString().trim()
        val code = etCode.text.toString().trim()
        if (!Regex("\\d{4,20}").matches(userId)) return showError(R.string.err_user_format)
        if (!Regex("\\d{16}").matches(code)) return showError(R.string.err_code_format)

        setBusy(true)
        thread(name = "lukka-activate") {
            val outcome = manager.activate(userId, code)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                setBusy(false)
                if (outcome.success) {
                    startActivity(
                        Intent(this, PlayerActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                } else {
                    etCode.text.clear()
                    showError(outcome.messageRes ?: R.string.err_server)
                }
            }
        }
    }

    private fun showError(resId: Int) {
        tvMessage.setText(resId)
        tvMessage.visibility = View.VISIBLE
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        btnValidate.isEnabled = !busy
        btnBack.isEnabled = !busy
        if (busy) tvMessage.visibility = View.GONE
    }
}
