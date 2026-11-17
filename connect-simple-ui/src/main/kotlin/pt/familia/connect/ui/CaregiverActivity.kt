package pt.familia.connect.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ConnectSettings
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.FamilyContacts

/**
 * Configuração para o cuidador (Figma: "M5 · Configuração"). Abre-se com um toque longo de 3 s
 * no relógio do ecrã inicial ou, na primeira utilização, com o botão "Configurar família".
 *
 * - Avisos no topo, só quando é preciso fazer alguma coisa (contactos, telefone predefinido,
 *   notificações, ecrã inteiro). Cada aviso resolve-se com um toque.
 * - Família: uma linha por familiar, com o estado do vídeo. Tocar na linha abre a configuração do
 *   vídeo; o ✕ tira-o da família (só desmarca o favorito, o contacto fica na agenda).
 *   "＋ Adicionar familiar" abre o seletor de contactos e marca o escolhido como favorito.
 * - Utilização: abrir sempre no ecrã simplificado; usar como ecrã principal do aparelho.
 * - Telefone completo: a Fossify Phone original, com todas as funções.
 */
class CaregiverActivity : Activity() {

    private lateinit var alerts: LinearLayout
    private lateinit var familyList: LinearLayout
    private var pendingStar: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_ui_config)
        alerts = findViewById(R.id.connect_ui_config_alerts)
        familyList = findViewById(R.id.connect_ui_config_family)
        findViewById<View>(R.id.connect_ui_config_add).setOnClickListener { pickContact() }
        findViewById<View>(R.id.connect_ui_config_full_phone).setOnClickListener {
            startActivity(HomeActivity.fullPhoneIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        renderAlerts()
        renderFamily()
        findViewById<Switch>(R.id.connect_ui_config_simple).apply {
            setOnCheckedChangeListener(null)
            isChecked = ConnectSettings.isSimpleModeEnabled(this@CaregiverActivity)
            setOnCheckedChangeListener { _, checked -> ConnectSettings.setSimpleModeEnabled(this@CaregiverActivity, checked) }
        }
        findViewById<Switch>(R.id.connect_ui_config_launcher).apply {
            setOnCheckedChangeListener(null)
            isChecked = isHomeLauncherEnabled()
            setOnCheckedChangeListener { _, checked -> setHomeLauncherEnabled(checked) }
        }
    }

    // ── Avisos ────────────────────────────────────────────────────────────────────────────────
    private fun renderAlerts() {
        alerts.removeAllViews()
        if (!FamilyContacts.canRead(this)) alert(R.string.connect_ui_config_alert_contacts) { requestContacts() }
        if (getSystemService(TelecomManager::class.java)?.defaultDialerPackage != packageName) {
            alert(R.string.connect_ui_config_alert_dialer) { requestDialerRole() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            alert(R.string.connect_ui_config_alert_notifications) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !IncomingCallNotifier.canUseFullScreen(this)) {
            alert(R.string.connect_ui_config_alert_fullscreen) {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
            }
        }
        alerts.visibility = if (alerts.childCount == 0) View.GONE else View.VISIBLE
    }

    private fun alert(textRes: Int, onClick: () -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.connect_ui_config_alert, alerts, false) as TextView
        view.setText(textRes)
        view.setOnClickListener { onClick() }
        alerts.addView(view)
    }

    // ── Família ───────────────────────────────────────────────────────────────────────────────
    private fun renderFamily() {
        familyList.removeAllViews()
        val family = FamilyContacts.favorites(this)
        if (family.isEmpty()) {
            familyList.addView(TextView(this).apply {
                setText(R.string.connect_ui_caregiver_empty)
                textSize = 18f
                setTextColor(getColor(R.color.connect_texto_suave))
                setPadding(0, 0, 0, dp(12))
            })
        }
        family.forEach { familyList.addView(familyRow(it)) }
    }

    private fun familyRow(contact: FamilyContact): View {
        val row = LayoutInflater.from(this).inflate(R.layout.connect_ui_config_row, familyList, false)
        row.findViewById<TextView>(R.id.connect_ui_config_row_name).text = contact.name
        bindAvatar(
            this, contact,
            row.findViewById<ImageView>(R.id.connect_ui_config_row_photo),
            row.findViewById(R.id.connect_ui_config_row_initials), dp(56),
        )
        val providers = ConnectServices.videoProviders()
        val configured = providers.firstOrNull { it.isConfigured(this, contact) }
        row.findViewById<TextView>(R.id.connect_ui_config_row_status).apply {
            when {
                configured != null -> chip(this, R.string.connect_ui_config_video_on, R.drawable.connect_bg_chip_ok, R.color.connect_chip_ok_texto)
                contact.phoneNumber.isNullOrBlank() -> chip(this, R.string.connect_ui_config_no_number, R.drawable.connect_bg_chip_aviso, R.color.connect_chip_aviso_texto)
                else -> chip(this, R.string.connect_ui_config_calls_only, R.drawable.connect_bg_chip_neutro, R.color.connect_chip_neutro_texto)
            }
        }
        // Tocar na linha: configurar o vídeo (o fornecedor já configurado ou o primeiro disponível).
        val setup = (configured ?: providers.firstOrNull())?.setupIntent(this, contact)
        row.contentDescription = getString(R.string.connect_ui_config_row_description, contact.name)
        row.setOnClickListener {
            if (setup != null) startActivity(setup)
            else Toast.makeText(this, R.string.connect_ui_caregiver_no_providers, Toast.LENGTH_LONG).show()
        }
        row.findViewById<View>(R.id.connect_ui_config_row_remove).apply {
            contentDescription = getString(R.string.connect_ui_caregiver_remove, contact.name)
            setOnClickListener { confirmRemove(contact) }
        }
        return row
    }

    private fun chip(view: TextView, textRes: Int, bgRes: Int, colorRes: Int) {
        view.setText(textRes)
        view.setBackgroundResource(bgRes)
        view.setTextColor(getColor(colorRes))
    }

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

    private fun confirmRemove(contact: FamilyContact) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.connect_ui_caregiver_remove_confirm, contact.name))
            .setPositiveButton(R.string.connect_ui_caregiver_remove_yes) { _, _ ->
                setStarred(ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(contact.lookupKey).build(), false)
            }
            .setNegativeButton(R.string.connect_ui_caregiver_cancel, null)
            .show()
    }

    /** Marca ou desmarca o contacto como favorito na agenda. A família são os favoritos. */
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

    private fun requestDialerRole() {
        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        }
        startActivityForResult(request, REQ_DIALER)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == REQ_WRITE_CONTACTS) {
            pendingStar.also { pendingStar = null }?.let { if (granted) setStarred(it, true) }
        }
        render()
    }

    // ── Ecrã principal (launcher) do aparelho dedicado ───────────────────────────────────────
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REQ_PICK = 91
        const val REQ_WRITE_CONTACTS = 92
        const val REQ_CONTACTS = 93
        const val REQ_NOTIFICATIONS = 94
        const val REQ_DIALER = 95
    }
}
