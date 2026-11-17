package pt.familia.connect.facetime

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import pt.familia.connect.core.FamilyContacts

/**
 * Ecrã do cuidador para configurar o FaceTime de um familiar. Os idosos não chegam aqui: o
 * ecrã principal só o abre depois de um toque longo.
 *
 * Passos que aparecem no ecrã:
 * 1. No iPhone do familiar: FaceTime → "Criar link" → partilhar → copiar.
 * 2. Colar aqui e Guardar. Testar abre a chamada.
 */
class FaceTimeSetupActivity : Activity() {

    private lateinit var store: FaceTimeStore
    private lateinit var lookupKey: String
    private lateinit var linkInput: EditText
    private lateinit var notifyBox: CheckBox
    private lateinit var ownerInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lookupKey = intent.getStringExtra(EXTRA_LOOKUP) ?: return finish()
        val contact = FamilyContacts.byLookupKey(this, lookupKey) ?: return finish()
        store = FaceTimeStore(this)
        setContentView(R.layout.connect_facetime_setup)

        findViewById<TextView>(R.id.connect_facetime_setup_title).text =
            getString(R.string.connect_facetime_setup_title, contact.name)

        linkInput = findViewById(R.id.connect_facetime_setup_link)
        linkInput.setText(store.link(lookupKey)?.url.orEmpty())

        findViewById<Button>(R.id.connect_facetime_setup_paste).setOnClickListener {
            val clip = getSystemService(ClipboardManager::class.java).primaryClip
            val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
            val link = FaceTimeLink.findIn(text) ?: FaceTimeLink.parse(text)
            if (link == null) toast(R.string.connect_facetime_setup_invalid) else linkInput.setText(link.url)
        }
        findViewById<Button>(R.id.connect_facetime_setup_save).setOnClickListener { save(showToast = true) }
        findViewById<Button>(R.id.connect_facetime_setup_test).setOnClickListener {
            val link = save(showToast = false) ?: return@setOnClickListener
            startActivity(FaceTimeCallActivity.intent(this, link, contact.name, lookupKey))
        }
        findViewById<Button>(R.id.connect_facetime_setup_remove).setOnClickListener {
            store.setLink(lookupKey, null)
            linkInput.setText("")
            toast(R.string.connect_facetime_setup_removed)
        }

        notifyBox = findViewById(R.id.connect_facetime_setup_notify)
        notifyBox.isChecked = store.notifyBySms(lookupKey) && SmsNotifier.canSend(this)
        notifyBox.setOnCheckedChangeListener { _, checked ->
            store.setNotifyBySms(lookupKey, checked)
            if (checked) ensurePermission(Manifest.permission.SEND_SMS, REQ_SEND_SMS)
        }

        ownerInput = findViewById(R.id.connect_facetime_setup_owner)
        ownerInput.setText(store.displayName.orEmpty())

        findViewById<CheckBox>(R.id.connect_facetime_setup_autofill).apply {
            isChecked = store.autoFillName
            setOnCheckedChangeListener { _, checked -> store.autoFillName = checked }
        }

        // Convites por SMS desativados: RECEIVE_SMS faz a Play Protect bloquear a instalação.
        findViewById<CheckBox>(R.id.connect_facetime_setup_incoming).visibility = android.view.View.GONE

        findViewById<TextView>(R.id.connect_facetime_setup_status).text =
            FaceTimeProvider().capabilities().joinToString("\n\n") { "• ${it.requirement}: ${it.status.label}\n   ${it.note}" }
    }

    override fun onPause() {
        if (::ownerInput.isInitialized) store.displayName = ownerInput.text.toString()
        super.onPause()
    }

    private fun save(showToast: Boolean): FaceTimeLink? {
        val link = FaceTimeLink.parse(linkInput.text.toString())
        if (link == null) {
            toast(R.string.connect_facetime_setup_invalid)
            return null
        }
        store.setLink(lookupKey, link)
        if (showToast) toast(R.string.connect_facetime_setup_saved)
        return link
    }

    private fun ensurePermission(permission: String, requestCode: Int) {
        if (!has(permission)) requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            REQ_SEND_SMS -> if (!granted) {
                notifyBox.isChecked = false
                toast(R.string.connect_facetime_setup_sms_denied)
            }
        }
    }

    private fun has(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()

    companion object {
        private const val EXTRA_LOOKUP = "lookup"
        private const val REQ_SEND_SMS = 51

        fun intent(context: Context, lookupKey: String): Intent =
            Intent(context, FaceTimeSetupActivity::class.java).putExtra(EXTRA_LOOKUP, lookupKey)
    }
}
