package pt.familia.connect.core

import android.net.Uri

/**
 * Um familiar mostrado no ecrã principal.
 *
 * A fonte é sempre a agenda do Android (contactos marcados como favoritos). O Fossify Connect
 * não tem uma lista de contactos própria, por isso nada é recolhido nem copiado para fora da agenda.
 *
 * @property lookupKey chave estável do Android (`ContactsContract.Contacts.LOOKUP_KEY`). É a
 *   chave com que os módulos guardam a sua configuração por contacto.
 */
data class FamilyContact(
    val contactId: Long,
    val lookupKey: String,
    val name: String,
    val photoUri: Uri?,
    val phoneNumber: String?,
)
