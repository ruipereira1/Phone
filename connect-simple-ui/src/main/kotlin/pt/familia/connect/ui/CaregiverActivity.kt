package pt.familia.connect.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
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
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ConnectSettings
import pt.familia.connect.core.FamilyContacts

/**
 * Configuração para o cuidador (familiar ou técnico), aberta com um toque longo de 3 s na
 * engrenagem do ecrã principal. Permite:
 * - escolher a família (os favoritos da agenda, com a app de contactos);
 * - configurar a videochamada de cada familiar, um botão por fornecedor instalado;
 * - ligar ou desligar o modo simplificado e o uso como ecrã principal do tablet;
 * - abrir a Fossify Phone completa, sem perder nenhuma funcionalidade original.
 */
class CaregiverActivity : Activity() {

    private lateinit var list: LinearLayout

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
        button(R.string.connect_ui_caregiver_edit_favorites) {
            startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        }
        val providers = ConnectServices.videoProviders()
        val family = FamilyContacts.favorites(this)
        if (family.isEmpty()) text(getString(R.string.connect_ui_home_empty))
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
}
