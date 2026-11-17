package pt.familia.connect.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.StartResult

/**
 * As duas ações do cartão de cada familiar, cada uma com um só toque.
 *
 * - Ligar: chamada telefónica normal, pela Telecom do Android e pelo ecrã de chamada da Fossify.
 *   Nunca é substituída por VoIP.
 * - Vídeo: o primeiro fornecedor de vídeo configurado para este contacto (por exemplo, FaceTime).
 */
internal object CallActions {
    const val REQ_CALL_PHONE = 71

    /** Contacto à espera da permissão CALL_PHONE (a chamada faz-se quando a permissão for dada). */
    private var pending: FamilyContact? = null

    fun phoneCall(activity: Activity, contact: FamilyContact) {
        val number = contact.phoneNumber ?: return
        if (activity.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pending = contact
            activity.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL_PHONE)
            return
        }
        // ACTION_CALL com o nosso pacote: a DialerActivity da Fossify trata da SIM e da Telecom.
        activity.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)).setPackage(activity.packageName))
    }

    fun onPermissionResult(activity: Activity, requestCode: Int, grantResults: IntArray) {
        if (requestCode != REQ_CALL_PHONE) return
        val contact = pending.also { pending = null } ?: return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) phoneCall(activity, contact)
    }

    fun hasVideo(activity: Activity, contact: FamilyContact) =
        ConnectServices.videoProviderFor(activity, contact) != null

    fun videoCall(activity: Activity, contact: FamilyContact) {
        val provider = ConnectServices.videoProviderFor(activity, contact)
        when (provider?.startVideoCall(activity, contact)) {
            StartResult.Started -> Unit
            is StartResult.Failed -> toast(activity, activity.getString(R.string.connect_ui_video_failed))
            StartResult.NotConfigured, null ->
                toast(activity, activity.getString(R.string.connect_ui_video_not_configured, contact.name))
        }
    }

    private fun toast(activity: Activity, text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
