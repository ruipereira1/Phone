package pt.familia.connect.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.FamilyContacts
import pt.familia.connect.core.StartResult

/**
 * Fotografia grande, nome e duas ações enormes: CHAMADA e VIDEOCHAMADA.
 *
 * - A chamada é uma chamada telefónica normal: passa pela Telecom do Android e pelo ecrã de
 *   chamada da Fossify Phone. Nunca é substituída por VoIP.
 * - A videochamada usa o primeiro fornecedor configurado para este contacto (por exemplo, FaceTime
 *   por link). Se nenhum estiver configurado, o botão aparece desativado e com essa indicação.
 */
class ContactActivity : Activity() {

    private lateinit var contact: FamilyContact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lookupKey = intent.getStringExtra(EXTRA_LOOKUP) ?: return finish()
        contact = FamilyContacts.byLookupKey(this, lookupKey) ?: return finish()
        setContentView(R.layout.connect_ui_contact)

        findViewById<TextView>(R.id.connect_ui_contact_name).text = contact.name
        val size = resources.displayMetrics.widthPixels.coerceAtMost(resources.displayMetrics.heightPixels) / 2
        bindAvatar(this, contact, findViewById<ImageView>(R.id.connect_ui_contact_photo), findViewById(R.id.connect_ui_contact_initials), size)

        findViewById<Button>(R.id.connect_ui_contact_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.connect_ui_contact_call).apply {
            isEnabled = !contact.phoneNumber.isNullOrBlank()
            if (!isEnabled) setText(R.string.connect_ui_no_number)
            setOnClickListener { placePhoneCall() }
        }
    }

    override fun onResume() {
        super.onResume()
        val provider = ConnectServices.videoProviderFor(this, contact)
        findViewById<Button>(R.id.connect_ui_contact_video).apply {
            isEnabled = provider != null
            setText(if (provider != null) R.string.connect_ui_video else R.string.connect_ui_video_not_configured)
            setOnClickListener {
                val result = provider?.startVideoCall(this@ContactActivity, contact)
                if (result is StartResult.Failed) toast(getString(R.string.connect_ui_video_failed))
                if (result == StartResult.NotConfigured) toast(getString(R.string.connect_ui_video_not_configured))
            }
        }
    }

    private fun placePhoneCall() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL)
            return
        }
        val number = contact.phoneNumber ?: return
        // ACTION_CALL com o nosso pacote: a DialerActivity da Fossify trata da SIM e da Telecom.
        startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)).setPackage(packageName))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CALL && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) placePhoneCall()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    companion object {
        private const val EXTRA_LOOKUP = "lookup"
        private const val REQ_CALL = 71

        fun intent(context: Context, lookupKey: String): Intent =
            Intent(context, ContactActivity::class.java).putExtra(EXTRA_LOOKUP, lookupKey)
    }
}
