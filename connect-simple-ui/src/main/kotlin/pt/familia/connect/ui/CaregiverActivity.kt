package pt.familia.connect.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ConnectSettings
import pt.familia.connect.core.FamilyContacts

/**
 * Configuração para o cuidador (familiar ou técnico). Abre-se com um toque longo de 3 s no relógio
 * do ecrã principal ou, na primeira utilização, com o botão "Configurar família". Permite:
 * - escolher a família diretamente na app: o seletor de contactos marca o escolhido como favorito
 *   (a família são os favoritos da agenda, os mesmos da Fossify Phone);
 * - configurar a videochamada de cada familiar, um botão por fornecedor instalado;
 * - ligar ou desligar o modo simplificado e o uso como ecrã principal do tablet;
 * - abrir a Fossify Phone completa, sem perder nenhuma funcionalidade original.
 */
class CaregiverActivity : Activity() {

    private lateinit var list: LinearLayout
    private var pendingStar: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.connect_ui_caregiver_title)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
        }
        setContentView(ScrollView(this).apply { addView(list) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        list.removeAllViews()

        text(getString(R.string.connect_ui_caregiver_hint))
        section(R.string.connect_ui_caregiver_family)
        if (!FamilyContacts.canRead(this)) {
            button(R.string.connect_ui_caregiver_contacts_permission) { requestContacts() }
        }
        button(R.string.connect_ui_caregiver_add) { pickContact() }
        val providers = ConnectServices.videoProviders()
        val family = FamilyContacts.favorites(this)
        if (family.isEmpty()) text(getString(R.string.connect_ui_caregiver_empty))
        family.forEach { contact ->
            text(contact.name, sizeSp = 22f, bold = true)
            if (providers.isEmpty()) text(getString(R.string.connect_ui_caregiver_no_providers))
            providers.forEach { provider ->
                val configured = provider.isConfigured(this, contact)
                val label = getString(
                    if (configured) R.string.connect_ui_caregiver_provider_on else R.string.connect_ui_caregiver_provider_off,
                    provider.displayName,
                )
                provider.setupIntent(this, contact)?.let { setup -> button(label) { startActivity(setup) } }
                    ?: text(label)
            }
            button(getString(R.string.connect_ui_caregiver_remove, contact.name)) { confirmRemove(contact.lookupKey, contact.name) }
        }
        button(R.string.connect_ui_caregiver_open_contacts) {
            startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        }

        section(R.string.connect_ui_caregiver_mode)
        check(R.string.connect_ui_caregiver_simple_mode, ConnectSettings.isSimpleModeEnabled(this)) {
            ConnectSettings.setSimpleModeEnabled(this, it)
        }
        check(R.string.connect_ui_caregiver_launcher, isHomeLauncherEnabled()) { setHomeLauncherEnabled(it) }
        text(getString(R.string.connect_ui_caregiver_launcher_note))

        if (!IncomingCallNotifier.canUseFullScreen(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            button(R.string.connect_ui_caregiver_fullscreen_permission) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:$packageName")))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            button(R.string.connect_ui_caregiver_notifications_permission) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 81)
            }
        }

        section(R.string.connect_ui_caregiver_original)
        button(R.string.connect_ui_open_full_phone) { startActivity(HomeActivity.fullPhoneIntent(this)) }
    }

    // ── Família: escolher e remover (marca/desmarca o favorito na agenda) ──────────────────────
    private fun pickContact() {
        if (!FamilyContacts.canRead(this)) return requestContacts()
        startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), REQ_PICK)
    }

    @Deprecated("Activity.onActivityResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && uri != null) setStarred(uri, true)
    }

    private fun confirmRemove(lookupKey: String, name: String) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.connect_ui_caregiver_remove_confirm, name))
            .setPositiveButton(R.string.connect_ui_caregiver_remove_yes) { _, _ ->
                setStarred(ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(lookupKey).build(), false)
            }
            .setNegativeButton(R.string.connect_ui_caregiver_cancel, null)
            .show()
    }

    private fun setStarred(contactUri: Uri, starred: Boolean) {
        if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            pendingStar = if (starred) contactUri else null
            requestPermissions(arrayOf(Manifest.permission.WRITE_CONTACTS), REQ_WRITE_CONTACTS)
            return
        }
        val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
        val changed = try {
            contentResolver.update(contactUri, values, null, null) > 0
        } catch (e: RuntimeException) {
            false
        }
        if (changed && starred) {
            val name = contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
            if (name != null) Toast.makeText(this, getString(R.string.connect_ui_caregiver_added, name), Toast.LENGTH_SHORT).show()
        }
        render()
    }

    private fun requestContacts() {
        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), REQ_CONTACTS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when (requestCode) {
            REQ_WRITE_CONTACTS -> pendingStar.also { pendingStar = null }?.let { if (granted) setStarred(it, true) }
            REQ_CONTACTS -> render()
        }
    }

    // ── Ecrã principal (launcher) do tablet dedicado ───────────────────────────────────────────
    private fun homeAlias() = ComponentName(packageName, "pt.familia.connect.ui.HomeLauncherAlias")

    private fun isHomeLauncherEnabled() =
        packageManager.getComponentEnabledSetting(homeAlias()) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    private fun setHomeLauncherEnabled(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            homeAlias(),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        if (enabled) startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    // ── Pequenos construtores de vistas (sem layouts XML para um ecrã de configuração simples) ─
    private fun section(res: Int) = text(getString(res), sizeSp = 24f, bold = true, topDp = 28)

    private fun text(value: String, sizeSp: Float = 18f, bold: Boolean = false, topDp: Int = 8) {
        list.addView(TextView(this).apply {
            text = value
            textSize = sizeSp
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(topDp), 0, 0)
        })
    }

    private fun button(res: Int, onClick: () -> Unit) = button(getString(res), onClick)

    private fun button(label: String, onClick: () -> Unit) {
        list.addView(Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 18f
            setOnClickListener { onClick() }
        })
    }

    private fun check(res: Int, value: Boolean, onChange: (Boolean) -> Unit) {
        list.addView(CheckBox(this).apply {
            setText(res)
            textSize = 18f
            isChecked = value
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REQ_PICK = 91
        const val REQ_WRITE_CONTACTS = 92
        const val REQ_CONTACTS = 93
    }
}
