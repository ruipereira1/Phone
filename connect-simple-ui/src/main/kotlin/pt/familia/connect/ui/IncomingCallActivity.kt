package pt.familia.connect.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import pt.familia.connect.core.FamilyContacts
import pt.familia.connect.core.IncomingVideoCall

/**
 * Videochamada recebida em ecrã inteiro: fotografia, nome e dois botões enormes, ATENDER e
 * REJEITAR. Toca o toque do telefone durante até 60 segundos.
 */
class IncomingCallActivity : Activity() {

    private var ringtone: Ringtone? = null
    private val main = Handler(Looper.getMainLooper())
    private lateinit var callId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return finish()
        val accept = acceptIntent() ?: return finish()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        showOverLockScreen()
        setContentView(R.layout.connect_ui_incoming)

        findViewById<TextView>(R.id.connect_ui_incoming_name).text = getString(R.string.connect_ui_incoming_title, name)
        intent.getStringExtra(EXTRA_LOOKUP)?.let { FamilyContacts.byLookupKey(this, it) }?.let { contact ->
            val size = resources.displayMetrics.widthPixels.coerceAtMost(resources.displayMetrics.heightPixels) / 2
            bindAvatar(this, contact, findViewById<ImageView>(R.id.connect_ui_incoming_photo), findViewById(R.id.connect_ui_incoming_initials), size)
        }

        findViewById<Button>(R.id.connect_ui_incoming_accept).setOnClickListener {
            stopRinging()
            IncomingCallNotifier.cancel(this, callId)
            startActivity(accept.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
        findViewById<Button>(R.id.connect_ui_incoming_reject).setOnClickListener { dismiss() }

        startRinging()
        main.postDelayed({ dismiss() }, RING_TIMEOUT_MS)
    }

    private fun acceptIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ACCEPT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ACCEPT)
        }

    private fun dismiss() {
        stopRinging()
        IncomingCallNotifier.cancel(this, callId)
        finish()
    }

    private fun startRinging() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE) ?: return
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Deprecated("Activity.onBackPressed")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Rejeita-se no botão vermelho, não com o "voltar".
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        stopRinging()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CALL_ID = "call_id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_LOOKUP = "lookup"
        private const val EXTRA_ACCEPT = "accept"
        private const val RING_TIMEOUT_MS = 60_000L

        fun intent(context: Context, call: IncomingVideoCall): Intent =
            Intent(context, IncomingCallActivity::class.java)
                .putExtra(EXTRA_CALL_ID, call.callId)
                .putExtra(EXTRA_NAME, call.callerName)
                .putExtra(EXTRA_LOOKUP, call.callerLookupKey)
                .putExtra(EXTRA_ACCEPT, call.acceptIntent)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
