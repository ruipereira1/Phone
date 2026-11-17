package pt.familia.connect.facetime

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject

/**
 * Ecrã de videochamada FaceTime para o idoso.
 *
 * A chamada é a página oficial da Apple (facetime.apple.com) dentro de uma WebView. Por cima,
 * botões enormes que não dependem da página:
 * - DESLIGAR: fecha a página, e com isso sai da chamada;
 * - Microfone: silencia o microfone do sistema (AudioManager), sem mexer na página;
 * - Volume − / +: volume do canal de chamada.
 *
 * Ligar e desligar a câmara fica nos botões da própria página da Apple. Controlá-la por fora
 * obrigava a automatizar a página, e isso parte sempre que a Apple a muda.
 *
 * Segurança: a WebView só navega em domínios da Apple, e câmara/micro só são concedidos à origem
 * da Apple e apenas se o Android já tiver dado essas permissões à app.
 */
class FaceTimeCallActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var micButton: Button
    private lateinit var statusText: TextView
    private lateinit var audio: AudioManager
    private var link: FaceTimeLink? = null
    private var micWasMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = FaceTimeLink.parse(intent.getStringExtra(EXTRA_LINK))
        if (link == null) {
            finish()
            return
        }
        showOverLockScreen()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        audio = getSystemService(AudioManager::class.java)
        micWasMuted = audio.isMicrophoneMute

        setContentView(R.layout.connect_facetime_call)
        statusText = findViewById(R.id.connect_facetime_status)
        statusText.text = getString(R.string.connect_facetime_status_joining, intent.getStringExtra(EXTRA_NAME) ?: "")
        micButton = findViewById(R.id.connect_facetime_mic)
        micButton.setOnClickListener { toggleMic() }
        findViewById<View>(R.id.connect_facetime_hangup).setOnClickListener { hangUp() }
        findViewById<View>(R.id.connect_facetime_vol_down).setOnClickListener { adjustVolume(AudioManager.ADJUST_LOWER) }
        findViewById<View>(R.id.connect_facetime_vol_up).setOnClickListener { adjustVolume(AudioManager.ADJUST_RAISE) }
        updateMicButton()

        webView = findViewById(R.id.connect_facetime_web)
        configureWebView()

        val missing = REQUIRED_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) load() else requestPermissions(missing.toTypedArray(), REQ_PERMISSIONS)
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

    @SuppressLint("SetJavaScriptEnabled") // A página da Apple precisa de JavaScript; a navegação está limitada à Apple.
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                !FaceTimeLink.isAppleOrigin(request.url.toString())

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (!FaceTimeLink.isAppleOrigin(url)) view.stopLoading()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                maybeAutoFillName(view)
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                finish() // o processo da WebView morreu: sai de forma limpa, sem a app ir abaixo
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (!FaceTimeLink.isAppleOrigin(request.origin.toString())) {
                    request.deny()
                    return
                }
                grantAllowed(request)
            }
        }
    }

    private fun grantAllowed(request: PermissionRequest) {
        val allowed = request.resources.filter { res ->
            when (res) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> has(Manifest.permission.CAMERA)
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> has(Manifest.permission.RECORD_AUDIO)
                else -> false
            }
        }
        if (allowed.isEmpty()) request.deny() else request.grant(allowed.toTypedArray())
    }

    private fun load() {
        webView.loadUrl(requireNotNull(link).url)
    }

    /**
     * EXPERIMENTAL: a página da Apple pede um nome antes de entrar e o idoso não deve ter de o
     * escrever. Preenche o primeiro campo de texto visível, se existir, e não carrega em nenhum
     * botão. Se a Apple mudar a página, isto deixa de fazer efeito sem partir nada.
     */
    private fun maybeAutoFillName(view: WebView) {
        val store = FaceTimeStore(this)
        val name = store.displayName ?: return
        if (!store.autoFillName) return
        val js = """
            (function(n){var t=0;var i=setInterval(function(){t++;
              var f=Array.prototype.find.call(document.querySelectorAll('input[type="text"],input:not([type])'),
                function(e){return e.offsetParent!==null&&!e.value;});
              if(f){var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;
                s.call(f,n);f.dispatchEvent(new Event('input',{bubbles:true}));
                f.dispatchEvent(new Event('change',{bubbles:true}));clearInterval(i);}
              if(t>40)clearInterval(i);},500);})(${JSONObject.quote(name)});
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun toggleMic() {
        audio.isMicrophoneMute = !audio.isMicrophoneMute
        updateMicButton()
    }

    private fun updateMicButton() {
        micButton.setText(
            if (audio.isMicrophoneMute) R.string.connect_facetime_mic_off else R.string.connect_facetime_mic_on
        )
    }

    private fun adjustVolume(direction: Int) {
        audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun hangUp() {
        webView.loadUrl("about:blank")
        finish()
    }

    @Deprecated("Activity.onBackPressed")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // De propósito: o botão "voltar" não desliga a chamada. Desliga-se no botão vermelho.
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) load()
    }

    override fun onDestroy() {
        if (::audio.isInitialized) audio.isMicrophoneMute = micWasMuted
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun has(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val EXTRA_LINK = "link"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_LOOKUP = "lookup"
        private const val REQ_PERMISSIONS = 41
        private val REQUIRED_PERMISSIONS = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        fun intent(context: Context, link: FaceTimeLink, name: String, lookupKey: String?): Intent =
            Intent(context, FaceTimeCallActivity::class.java)
                .putExtra(EXTRA_LINK, link.url)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_LOOKUP, lookupKey)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
