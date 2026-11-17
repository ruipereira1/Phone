package pt.familia.connect.core

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Cada módulo connect-* declara no manifesto uma subclasse disto, com `exported="false"`.
 * O Android cria-a no arranque do processo, e é aí que o módulo se regista em [ConnectServices].
 *
 * Com isto a `:app` não precisa de conhecer os módulos: basta declará-los como dependências.
 * Funciona como o androidx.startup, mas sem acrescentar uma dependência.
 */
abstract class ModuleInitializer : ContentProvider() {
    abstract fun initialize(context: Context)

    final override fun onCreate(): Boolean {
        initialize(requireNotNull(context).applicationContext)
        return true
    }

    final override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor? = null

    final override fun getType(uri: Uri): String? = null
    final override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    final override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    final override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?,
    ) = 0
}
