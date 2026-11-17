package pt.familia.connect.facetime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import pt.familia.connect.core.FamilyContact

/**
 * Avisa o familiar por SMS de que o idoso está à espera na chamada FaceTime dele.
 * Como o Android não consegue fazer tocar o iPhone, isto substitui o toque.
 *
 * Só funciona se o cuidador tiver dado a permissão SEND_SMS. Sem ela, não faz nada e a chamada
 * continua normalmente.
 */
internal object SmsNotifier {
    private const val TAG = "ConnectFaceTime"

    fun canSend(context: Context) =
        context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun notifyWaiting(context: Context, contact: FamilyContact, ownerName: String?) {
        val number = contact.phoneNumber
        if (number.isNullOrBlank() || !canSend(context)) return
        val who = ownerName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.connect_facetime_default_owner)
        val text = context.getString(R.string.connect_facetime_sms_waiting, who)
        try {
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            sms.sendMultipartTextMessage(number, null, sms.divideMessage(text), null, null)
        } catch (e: RuntimeException) {
            // Não se regista o número nem o texto (privacidade).
            Log.w(TAG, "Não foi possível enviar a SMS de aviso: ${e.javaClass.simpleName}")
        }
    }
}
