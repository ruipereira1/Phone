package pt.familia.connect.facetime

import android.content.Context
import pt.familia.connect.core.SecureStore

/**
 * Configuração FaceTime de cada contacto, guardada cifrada no Keystore.
 *
 * O link é tratado como um segredo: quem o tiver pode pedir para entrar na chamada do familiar.
 */
class FaceTimeStore(context: Context) {
    private val store = SecureStore(context, "facetime")

    fun link(lookupKey: String): FaceTimeLink? = FaceTimeLink.parse(store.get(KEY_LINK + lookupKey))

    fun setLink(lookupKey: String, link: FaceTimeLink?) = store.put(KEY_LINK + lookupKey, link?.url)

    /** Enviar uma SMS ao familiar a avisar que o idoso está à espera na chamada. */
    fun notifyBySms(lookupKey: String): Boolean = store.get(KEY_NOTIFY + lookupKey) == "1"

    fun setNotifyBySms(lookupKey: String, enabled: Boolean) =
        store.put(KEY_NOTIFY + lookupKey, if (enabled) "1" else null)

    /** Nome com que o tablet entra na chamada (a página da Apple pede um nome). */
    var displayName: String?
        get() = store.get(KEY_DISPLAY_NAME)
        set(value) = store.put(KEY_DISPLAY_NAME, value?.trim()?.takeIf { it.isNotEmpty() })

    /** Preencher automaticamente o nome na página da Apple. Experimental: depende da página. */
    var autoFillName: Boolean
        get() = store.get(KEY_AUTOFILL) != "0"
        set(value) = store.put(KEY_AUTOFILL, if (value) null else "0")

    private companion object {
        const val KEY_LINK = "link:"
        const val KEY_NOTIFY = "notify:"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AUTOFILL = "autofill_name"
    }
}
