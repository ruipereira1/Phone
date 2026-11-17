package pt.familia.connect.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import pt.familia.connect.core.IncomingCallPresenter
import pt.familia.connect.core.IncomingVideoCall

/**
 * Mostra uma videochamada recebida como notificação de chamada com full-screen intent:
 * - com o ecrã bloqueado ou apagado, o Android abre logo o [IncomingCallActivity];
 * - com o tablet em uso, aparece uma notificação destacada que abre o mesmo ecrã.
 *
 * A partir do Android 14, o full-screen intent só é concedido a apps de chamadas e despertadores.
 * Como o Fossify Connect é o marcador predefinido, deve tê-lo. Fica POR VALIDAR por versão do Android.
 */
internal object IncomingCallNotifier : IncomingCallPresenter {
    private const val CHANNEL = "connect_incoming_video"
    private const val TIMEOUT_MS = 60_000L

    override fun present(context: Context, call: IncomingVideoCall) {
        val nm = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context, nm)
        val screen = IncomingCallActivity.intent(context, call)
        val pending = PendingIntent.getActivity(
            context, call.callId.hashCode(), screen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(context.getString(R.string.connect_ui_incoming_title, call.callerName))
            .setContentText(context.getString(R.string.connect_ui_incoming_text))
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MS)
            .build()
        try {
            nm.notify(notificationId(call.callId), notification)
        } catch (e: SecurityException) {
            // Sem POST_NOTIFICATIONS (Android 13+): tenta abrir o ecrã diretamente. Só resulta em primeiro plano.
            context.startActivity(screen)
        }
    }

    fun cancel(context: Context, callId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(callId))
    }

    private fun notificationId(callId: String) = 0x4F0000 or (callId.hashCode() and 0xFFFF)

    private fun ensureChannel(context: Context, nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(
            CHANNEL, context.getString(R.string.connect_ui_incoming_channel), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            // O toque é feito pelo IncomingCallActivity, para não haver dois sons ao mesmo tempo.
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    /** Android 14+: indica se o sistema deixa mostrar a chamada em ecrã inteiro. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}
