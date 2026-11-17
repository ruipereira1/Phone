package pt.familia.connect.facetime

import android.app.Activity
import android.content.Context
import android.content.Intent
import pt.familia.connect.core.Capability
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.StartResult
import pt.familia.connect.core.SupportStatus.NAO_SUPORTADO
import pt.familia.connect.core.SupportStatus.POR_VALIDAR
import pt.familia.connect.core.VideoCallProvider

/**
 * FaceTime através dos links oficiais da Apple ("Entrar numa chamada FaceTime a partir de um
 * dispositivo Android"). O familiar com iPhone cria uma vez um link persistente no FaceTime e o
 * cuidador cola-o na configuração deste contacto.
 *
 * Limitação de fundo, documentada pela Apple: o Android só pode *entrar* numa chamada criada por
 * um dispositivo Apple. O iPhone não recebe um toque FaceTime nativo. Recebe uma SMS de aviso,
 * se estiver configurada, e tem de admitir a entrada do tablet.
 */
class FaceTimeProvider : VideoCallProvider {
    override val id = ID
    override val displayName = "FaceTime (link oficial)"

    override fun isConfigured(context: Context, contact: FamilyContact) =
        FaceTimeStore(context).link(contact.lookupKey) != null

    override fun startVideoCall(activity: Activity, contact: FamilyContact): StartResult {
        val store = FaceTimeStore(activity)
        val link = store.link(contact.lookupKey) ?: return StartResult.NotConfigured
        if (store.notifyBySms(contact.lookupKey)) {
            SmsNotifier.notifyWaiting(activity, contact, store.displayName)
        }
        activity.startActivity(FaceTimeCallActivity.intent(activity, link, contact.name, contact.lookupKey))
        return StartResult.Started
    }

    override fun setupIntent(context: Context, contact: FamilyContact): Intent =
        FaceTimeSetupActivity.intent(context, contact.lookupKey)

    override fun capabilities() = listOf(
        Capability("Android → iPhone com toque FaceTime", NAO_SUPORTADO,
            "A Apple não permite iniciar chamadas FaceTime fora dos seus dispositivos. O tablet entra no link e o familiar é avisado por SMS."),
        Capability("iPhone → Android", POR_VALIDAR,
            "O familiar envia o link por SMS e o tablet mostra ATENDER/REJEITAR. Precisa da opção de convites por SMS ligada."),
        Capability("Câmara e microfone", POR_VALIDAR, "Página web da Apple dentro da WebView (Chromium)."),
        Capability("Ecrã bloqueado", POR_VALIDAR, "A atividade da chamada abre por cima do ecrã de bloqueio."),
        Capability("Atender sem intervenção", NAO_SUPORTADO, "O iPhone que criou o link tem de admitir quem entra."),
        Capability("Wi-Fi e dados móveis", POR_VALIDAR, "Não há restrições do lado da app."),
        Capability("Associação a contactos", POR_VALIDAR, "Um link por contacto da agenda; não se usam IDs Apple."),
        Capability("Depois de reiniciar", POR_VALIDAR, "Os links ficam guardados cifrados e não há sessão a manter."),
    )

    companion object {
        const val ID = "facetime-link"
    }
}
