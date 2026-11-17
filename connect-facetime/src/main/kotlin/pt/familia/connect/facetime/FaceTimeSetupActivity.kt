package pt.familia.connect.facetime

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.FamilyContacts

/**
 * Configurar o FaceTime de um familiar (Figma: "M6 · Configurar FaceTime"). Ecrã do cuidador.
 *
 * 1. No iPhone do familiar: FaceTime → "Criar link" → enviar para este aparelho.
 * 2. "Colar link": o link é validado e **guardado de imediato** (não há botão Guardar).
 *    Também se pode escrever à mão; guarda-se ao sair do campo ou do ecrã.
 * 3. "Testar videochamada" abre a chamada.
 */
class FaceTimeSetupActivity : Activity() {

    private lateinit var store: FaceTimeStore
    private lateinit var contact: FamilyContact
    private lateinit var linkInput: EditText
    private lateinit var ownerInput: EditText
    private lateinit var stateView: TextView
    private lateinit var notifySwitch: CompoundButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lookupKey = intent.getStringExtra(EXTRA_LOOKUP) ?: return finish()
        contact = FamilyContacts.byLookupKey(this, lookupKey) ?: return finish()
        store = FaceTimeStore(this)
        setContentView(R.layout.connect_facetime_setup)

        findViewById<TextView>(R.id.connect_facetime_setup_title).text =
            getString(R.string.connect_facetime_setup_title, contact.name)
        findViewById<TextView>(R.id.connect_facetime_setup_step1_title).text =
            getString(R.string.connect_facetime_setup_step1, contact.name)

        stateView = findViewById(R.id.connect_facetime_setup_state)
        linkInput = findViewById(R.id.connect_facetime_setup_link)
        linkInput.setText(store.link(contact.lookupKey)?.url.orEmpty())
        linkInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) saveTyped(showError = true)
            false
        }
        linkInput.setOnFocusChangeListener { _, focused -> if (!focused) saveTyped(showError = false) }

        findViewById<Button>(R.id.connect_facetime_setup_paste).setOnClickListener { pasteFromClipboard() }
        findViewById<Button>(R.id.connect_facetime_setup_test).setOnClickListener {
            saveTyped(showError = false)
            val link = store.link(contact.lookupKey)
            if (link == null) {
                toast(R.string.connect_facetime_setup_need_link)
            } else {
                startActivity(FaceTimeCallActivity.intent(this, link, contact.name, contact.lookupKey))
            }
        }
        findViewById<TextView>(R.id.connect_facetime_setup_remove).setOnClickListener {
            store.setLink(contact.lookupKey, null)
            linkInput.setText("")
            updateState()
            toast(R.string.connect_facetime_setup_removed)
        }

        notifySwitch = findViewById(R.id.connect_facetime_setup_notify)
        notifySwitch.isChecked = store.notifyBySms(contact.lookupKey) && SmsNotifier.canSend(this)
        notifySwitch.setOnCheckedChangeListener { _, checked ->
            store.setNotifyBySms(contact.lookupKey, checked)
            if (checked && !SmsNotifier.canSend(this)) requestPermissions(arrayOf(Manifest.permission.SEND_SMS), REQ_SEND_SMS)
        }

        ownerInput = findViewById(R.id.connect_facetime_setup_owner)
        ownerInput.setText(store.displayName.orEmpty())

        findViewById<CompoundButton>(R.id.connect_facetime_setup_autofill).apply {
            isChecked = store.autoFillName
            setOnCheckedChangeListener { _, checked -> store.autoFillName = checked }
        }

        findViewById<TextView>(R.id.connect_facetime_setup_status).text =
            FaceTimeProvider().capabilities().joinToString("\n\n") { "• ${it.requirement}: ${it.status.label}\n   ${it.note}" }

        updateState()
    }

    override fun onPause() {
        if (::ownerInput.isInitialized) store.displayName = ownerInput.text.toString()
        if (::linkInput.isInitialized) saveTyped(showError = false)
        super.onPause()
    }

    private fun pasteFromClipboard() {
        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        val link = FaceTimeLink.findIn(text) ?: FaceTimeLink.parse(text)
        if (link == null) {
            toast(R.string.connect_facetime_setup_invalid)
            return
        }
        linkInput.setText(link.url)
        store.setLink(contact.lookupKey, link)
        updateState()
        toast(R.string.connect_facetime_setup_saved)
    }

    /** Guarda o que foi escrito à mão, se for um link válido e diferente do guardado. */
    private fun saveTyped(showError: Boolean) {
        val typed = linkInput.text.toString().trim()
        if (typed.isEmpty()) return
        val link = FaceTimeLink.parse(typed)
        if (link == null) {
            if (showError) toast(R.string.connect_facetime_setup_invalid)
            return
        }
        if (store.link(contact.lookupKey)?.url != link.url) {
            store.setLink(contact.lookupKey, link)
            toast(R.string.connect_facetime_setup_saved)
        }
        updateState()
    }

    private fun updateState() {
        val ok = store.link(contact.lookupKey) != null
        stateView.setText(if (ok) R.string.connect_facetime_setup_state_ok else R.string.connect_facetime_setup_state_missing)
        stateView.setBackgroundResource(if (ok) R.drawable.connect_bg_chip_ok else R.drawable.connect_bg_chip_neutro)
        stateView.setTextColor(getColor(if (ok) R.color.connect_chip_ok_texto else R.color.connect_chip_neutro_texto))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_SEND_SMS && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            notifySwitch.isChecked = false
            toast(R.string.connect_facetime_setup_sms_denied)
        }
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()

    companion object {
        private const val EXTRA_LOOKUP = "lookup"
        private const val REQ_SEND_SMS = 51

        fun intent(context: Context, lookupKey: String): Intent =
            Intent(context, FaceTimeSetupActivity::class.java).putExtra(EXTRA_LOOKUP, lookupKey)
    }
}
