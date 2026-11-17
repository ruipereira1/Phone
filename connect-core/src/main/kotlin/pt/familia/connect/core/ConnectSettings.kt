package pt.familia.connect.core

import android.content.Context

/**
 * Preferências do Fossify Connect, separadas das preferências da Fossify para não se misturarem.
 * Aqui não se guarda nenhum dado sensível: os segredos ficam em [SecureStore].
 */
object ConnectSettings {
    private const val FILE = "connect_settings"
    private const val KEY_SIMPLE_MODE = "simple_mode"

    /** Quando está ligado, a app abre no ecrã de fotografias e não na Fossify Phone completa. */
    fun isSimpleModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SIMPLE_MODE, true)

    fun setSimpleModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SIMPLE_MODE, enabled).apply()
    }

    fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
