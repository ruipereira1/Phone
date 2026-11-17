package pt.familia.connect.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone

/**
 * Leitura direta da agenda do Android. A lista da família são os contactos marcados com
 * estrela (favoritos). É o mesmo conceito de "Favoritos" da Fossify Phone, por isso o
 * cuidador gere-a sem aprender nada de novo.
 *
 * Só lê. Devolve listas vazias ou null se a permissão READ_CONTACTS não tiver sido dada.
 */
object FamilyContacts {

    fun favorites(context: Context): List<FamilyContact> {
        if (!canRead(context)) return emptyList()
        val result = mutableListOf<FamilyContact>()
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            CONTACT_PROJECTION,
            "${ContactsContract.Contacts.STARRED} = 1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                result += FamilyContact(
                    contactId = id,
                    lookupKey = c.getString(1) ?: continue,
                    name = c.getString(2) ?: "",
                    photoUri = c.getString(3)?.let(Uri::parse),
                    phoneNumber = if (c.getInt(4) > 0) primaryNumber(context, id) else null,
                )
            }
        }
        return result
    }

    fun byLookupKey(context: Context, lookupKey: String): FamilyContact? {
        if (!canRead(context)) return null
        val uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(lookupKey).build()
        return readSingle(context, uri)
    }

    /** Usa o PhoneLookup do sistema, que trata dos formatos (+351, espaços, etc.). */
    fun byPhoneNumber(context: Context, number: String): FamilyContact? {
        if (!canRead(context) || number.isBlank()) return null
        val lookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val lookupKey = context.contentResolver.query(
            lookup, arrayOf(ContactsContract.PhoneLookup.LOOKUP_KEY), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: return null
        return byLookupKey(context, lookupKey)
    }

    fun canRead(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun readSingle(context: Context, uri: Uri): FamilyContact? {
        val cursor = context.contentResolver.query(uri, CONTACT_PROJECTION, null, null, null) ?: return null
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            val id = c.getLong(0)
            val lookupKey = c.getString(1) ?: return null
            return FamilyContact(
                contactId = id,
                lookupKey = lookupKey,
                name = c.getString(2) ?: "",
                photoUri = c.getString(3)?.let(Uri::parse),
                phoneNumber = if (c.getInt(4) > 0) primaryNumber(context, id) else null,
            )
        }
    }

    /** Prefere o número marcado como predefinido e, a seguir, o telemóvel. */
    private fun primaryNumber(context: Context, contactId: Long): String? =
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.IS_SUPER_PRIMARY, Phone.TYPE),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { c ->
            var best: String? = null
            var bestScore = -1
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                val score = when {
                    c.getInt(1) > 0 -> 3
                    c.getInt(2) == Phone.TYPE_MOBILE -> 2
                    else -> 1
                }
                if (score > bestScore) {
                    best = number
                    bestScore = score
                }
            }
            best
        }

    private val CONTACT_PROJECTION = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        ContactsContract.Contacts.PHOTO_URI,
        ContactsContract.Contacts.HAS_PHONE_NUMBER,
    )
}
