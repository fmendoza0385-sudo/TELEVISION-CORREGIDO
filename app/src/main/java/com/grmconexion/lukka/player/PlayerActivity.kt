package com.grmconexion.lukka.player

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.PlayerView
import com.grmconexion.lukka.BuildConfig
import com.grmconexion.lukka.R
import com.grmconexion.lukka.blocking.BlockingActivity
import com.grmconexion.lukka.data.Channel
import com.grmconexion.lukka.data.PlaylistRepository
import com.grmconexion.lukka.subscription.RefreshResult
import com.grmconexion.lukka.subscription.SubscriptionManager
import com.grmconexion.lukka.subscription.SubscriptionState
import com.grmconexion.lukka.util.Immersive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Reproductor estilo decodificador. REGLA: el reproductor solo se crea si
 * SubscriptionManager.isPlaybackAllowed() es verdadero, y se vuelve a comprobar periódicamente.
 */
@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var manager: SubscriptionManager
    private lateinit var playerView: PlayerView
    private lateinit var tvNumber: TextView
    private lateinit var tvName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvNotice: TextView
    private lateinit var osd: View

    private var player: ExoPlayer? = null
    private var channels: List<Channel> = emptyList()
    private var planId: String = ""
    private var index = 0
    private var retries = 0
    private var triedAlternate = false
    private var numberBuffer = ""
    private lateinit var gestures: GestureDetector

    private val ui = Handler(Looper.getMainLooper())

    // Fábricas de fuentes (HLS / flujo progresivo MPEG-TS) con banderas de compatibilidad TS.
    private val tsFlags = DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
        DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS

    private val hideOsd = Runnable { osd.visibility = View.GONE }
    private val applyNumber = Runnable { commitNumber() }

    /** Comprobación periódica del criterio central: si deja de ser válido, se detiene y se bloquea. */
    private val gateTick = object : Runnable {
        override fun run() {
            if (!manager.isPlaybackAllowed()) { goBlocking(); return }
            ui.postDelayed(this, GATE_INTERVAL_MS)
        }
    }

    /** Sincronización con el servidor: cada 60 s si no hay conexión, cada 30 min si todo va bien. */
    private val onlineTick = object : Runnable {
        override fun run() {
            thread(name = "lukka-refresh") {
                val result = manager.refreshOnline()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (!manager.isPlaybackAllowed()) { goBlocking(); return@runOnUiThread }
                    val next = if (result == RefreshResult.OFFLINE) RETRY_OFFLINE_MS else REFRESH_OK_MS
                    ui.postDelayed(this, next)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Immersive.apply(this)
        manager = SubscriptionManager(this)

        playerView = findViewById(R.id.player_view)
        osd = findViewById(R.id.osd)
        tvNumber = findViewById(R.id.tv_channel_number)
        tvName = findViewById(R.id.tv_channel_name)
        tvStatus = findViewById(R.id.tv_status)
        tvNotice = findViewById(R.id.tv_notice)

        // Smartphone: deslizar arriba/abajo cambia de canal; toque muestra información.
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { showOsd(); return true }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                val dx = e2.x - e1.x
                if (abs(dy) > 120 && abs(dy) > abs(dx)) { changeChannel(if (dy < 0) +1 else -1); return true }
                return false
            }
        })
    }

    override fun onStart() {
        super.onStart()
        // El reproductor NO se inicializa sin licencia válida.
        if (!manager.isPlaybackAllowed()) { goBlocking(); return }

        val status = manager.evaluateLocal()
        planId = status.planId.orEmpty()
        channels = PlaylistRepository.load(this, planId)
        if (channels.isEmpty()) {
            showStatus(getString(R.string.player_no_playlist), persistent = true)
        } else {
            index = prefs().getInt("last_$planId", 0).coerceIn(0, channels.size - 1)
            initPlayer()
            playCurrent()
        }
        ui.postDelayed(gateTick, GATE_INTERVAL_MS)
        ui.postDelayed(onlineTick, 2_000)
    }

    override fun onStop() {
        super.onStop()
        ui.removeCallbacksAndMessages(null)
        releasePlayer()
    }

    // ---------------------------------------------------------------- reproductor

    private fun initPlayer() {
        releasePlayer()
        val renderers = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true) // prueba otro decodificador si el primero falla
        val exo = ExoPlayer.Builder(this, renderers).build()
        exo.addListener(listener)
        playerView.player = exo
        player = exo
    }

    private fun releasePlayer() {
        player?.let {
            it.removeListener(listener)
            it.release()
        }
        player = null
        playerView.player = null
    }

    private fun buildSource(url: String, alternate: Boolean): MediaSource {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent("LUKKA-TV/${BuildConfig.VERSION_NAME}")
        val dataSources = DefaultDataSource.Factory(this, http)
        val item = MediaItem.fromUri(url)
        // La extensión es solo una pista: si el formato no se reconoce se prueba el otro tipo de fuente.
        val useHls = url.contains(".m3u8", ignoreCase = true) xor alternate
        return if (useHls) {
            HlsMediaSource.Factory(dataSources)
                .setExtractorFactory(DefaultHlsExtractorFactory(tsFlags, false))
                .createMediaSource(item)
        } else {
            val extractors = DefaultExtractorsFactory().setTsExtractorFlags(tsFlags)
            ProgressiveMediaSource.Factory(dataSources, extractors).createMediaSource(item)
        }
    }

    private fun playCurrent() {
        if (channels.isEmpty()) return
        prefs().edit().putInt("last_$planId", index).apply()
        retries = 0
        triedAlternate = false
        prepareCurrent()
        showOsd()
    }

    private fun prepareCurrent() {
        val exo = player ?: return
        val ch = channels[index]
        showStatus(getString(R.string.player_loading))
        exo.setMediaSource(buildSource(ch.url, triedAlternate))
        exo.prepare()
        exo.playWhenReady = true
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> { retries = 0; if (tvStatus.tag != STATUS_PERSISTENT) hideStatus() }
                Player.STATE_BUFFERING -> showStatus(getString(R.string.player_loading))
                else -> Unit
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            var hasVideo = false
            var videoPlayable = false
            var hasAudio = false
            for (group in tracks.groups) {
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        hasVideo = true
                        if (group.isSelected && group.isSupported) videoPlayable = true
                    }
                    C.TRACK_TYPE_AUDIO -> hasAudio = true
                }
            }
            if (tracks.groups.isEmpty()) return
            PlaybackDiagnostics.logTracks(channels.getOrNull(index), tracks)
            if (hasAudio && !hasVideo) showStatus(getString(R.string.player_no_video), persistent = true)
            else if (hasVideo && !videoPlayable) showStatus(getString(R.string.player_video_unsupported), persistent = true)
        }

        override fun onPlayerError(error: PlaybackException) {
            val ch = channels.getOrNull(index)
            PlaybackDiagnostics.logError(ch, error)
            val exo = player ?: return
            when {
                error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                    exo.seekToDefaultPosition(); exo.prepare()
                }
                // Formato no reconocido por la fuente elegida: se prueba una sola vez la otra (HLS <-> TS).
                !triedAlternate && (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED) -> {
                    triedAlternate = true
                    prepareCurrent()
                }
                isDecoderError(error) -> showStatus(getString(R.string.player_video_unsupported), persistent = true)
                retries < MAX_RETRIES -> {
                    retries++
                    ui.postDelayed({ if (!isFinishing) prepareCurrent() }, 1_500L * retries)
                }
                else -> showStatus(getString(R.string.player_error), persistent = true)
            }
        }
    }

    private fun isDecoderError(e: PlaybackException) = e.errorCode in setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
    )

    // ---------------------------------------------------------------- zapping / OSD

    private fun changeChannel(delta: Int) {
        if (channels.isEmpty()) return
        index = (index + delta + channels.size) % channels.size
        hideStatus()
        playCurrent()
    }

    private fun commitNumber() {
        val n = numberBuffer.toIntOrNull()
        numberBuffer = ""
        if (n != null && n in 1..channels.size) {
            index = n - 1
            hideStatus()
            playCurrent()
        } else {
            showOsd()
        }
    }

    private fun showOsd() {
        val ch = channels.getOrNull(index)
        tvNumber.text = if (numberBuffer.isNotEmpty()) numberBuffer else (index + 1).toString()
        tvName.text = if (numberBuffer.isNotEmpty()) "" else ch?.name.orEmpty()

        // Aviso no intrusivo cuando se acerca el corte.
        val status = manager.evaluateLocal()
        if (status.state == SubscriptionState.POR_VENCER) {
            val fmt = SimpleDateFormat("d 'de' MMMM, h:mm a", Locale("es", "CO"))
            fmt.timeZone = java.util.TimeZone.getTimeZone("America/Bogota")
            tvNotice.text = getString(R.string.player_notice_expiring, fmt.format(Date(status.validUntilMs)))
            tvNotice.visibility = View.VISIBLE
        } else {
            tvNotice.visibility = View.GONE
        }
        osd.visibility = View.VISIBLE
        ui.removeCallbacks(hideOsd)
        ui.postDelayed(hideOsd, 4_000)
    }

    private fun showStatus(text: String, persistent: Boolean = false) {
        tvStatus.text = text
        tvStatus.tag = if (persistent) STATUS_PERSISTENT else null
        tvStatus.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        tvStatus.tag = null
        tvStatus.visibility = View.GONE
    }

    // ---------------------------------------------------------------- entradas

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> { changeChannel(+1); return true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> { changeChannel(-1); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (numberBuffer.isNotEmpty()) { ui.removeCallbacks(applyNumber); commitNumber() } else showOsd()
                return true
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> { appendDigit(keyCode - KeyEvent.KEYCODE_0); return true }
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> { appendDigit(keyCode - KeyEvent.KEYCODE_NUMPAD_0); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun appendDigit(d: Int) {
        if (numberBuffer.length >= 3) numberBuffer = ""
        numberBuffer += d.toString()
        showOsd()
        ui.removeCallbacks(applyNumber)
        ui.postDelayed(applyNumber, 1_500)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ---------------------------------------------------------------- utilidades

    private fun goBlocking() {
        ui.removeCallbacksAndMessages(null)
        releasePlayer()
        startActivity(
            Intent(this, BlockingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun prefs() = getSharedPreferences("lukka_prefs", MODE_PRIVATE)

    private companion object {
        const val GATE_INTERVAL_MS = 15_000L
        const val RETRY_OFFLINE_MS = 60_000L
        const val REFRESH_OK_MS = 30 * 60_000L
        const val MAX_RETRIES = 3
        const val STATUS_PERSISTENT = "persistent"
    }
}
