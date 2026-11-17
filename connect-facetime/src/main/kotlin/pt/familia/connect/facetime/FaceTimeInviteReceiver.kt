package pt.familia.connect.facetime

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.FamilyContacts
import pt.familia.connect.core.IncomingVideoCall
import java.security.MessageDigest

/**
 * Sentido iPhone → tablet: o familiar inicia a chamada FaceTime no iPhone, partilha o link por
 * SMS, e o tablet mostra o ecrã ATENDER / REJEITAR.
 *
 * Regras:
 * - vem **desligado** no manifesto; o cuidador liga-o na configuração (permissão RECEIVE_SMS);
 * - só aceita links oficiais facetime.apple.com, de remetentes que estejam nos **favoritos** (a
 *   família). Links de desconhecidos são ignorados;
 * - não guarda nem regista o conteúdo das SMS.
 */
class FaceTimeInviteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
            val link = FaceTimeLink.findIn(parts.joinToString("") { it.messageBody.orEmpty() }) ?: return@forEach
            handle(context, sender, link)
        }
    }

    private fun handle(context: Context, sender: String, link: FaceTimeLink) {
        val contact = FamilyContacts.byPhoneNumber(context, sender) ?: return
        if (FamilyContacts.favorites(context).none { it.lookupKey == contact.lookupKey }) return
        val callId = sha256(link.url)
        if (isDuplicate(context, callId)) return
        ConnectServices.presentIncoming(
            context,
            IncomingVideoCall(
                callId = callId,
                providerId = FaceTimeProvider.ID,
                callerName = contact.name,
                callerLookupKey = contact.lookupKey,
                acceptIntent = FaceTimeCallActivity.intent(context, link, contact.name, contact.lookupKey),
            ),
        )
    }

    /** A mesma SMS pode chegar repetida; ignora-se o mesmo link durante 2 minutos. */
    private fun isDuplicate(context: Context, callId: String): Boolean {
        val prefs = context.getSharedPreferences("connect_facetime_invites", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val duplicate = prefs.getString("last_id", null) == callId && now - prefs.getLong("last_at", 0) < DEDUP_MS
        prefs.edit().putString("last_id", callId).putLong("last_at", now).apply()
        return duplicate
    }

    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        private const val DEDUP_MS = 2 * 60 * 1000L

        fun isEnabled(context: Context) = context.packageManager.getComponentEnabledSetting(component(context)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        fun setEnabled(context: Context, enabled: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                component(context),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        private fun component(context: Context) = ComponentName(context, FaceTimeInviteReceiver::class.java)
    }
}
