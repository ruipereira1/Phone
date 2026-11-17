package pt.familia.connect.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/**
 * Carrega as fotografias dos contactos em alta resolução, numa thread separada, com cache em memória.
 * Usa só a plataforma (sem Glide nem Coil), para não acrescentar dependências.
 */
internal object PhotoLoader {
    private val executor = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val cache = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount / 1024
    }

    /** Põe a fotografia em [target]; se o contacto não tiver fotografia, chama [onMissing]. */
    fun load(context: Context, contactId: Long, target: ImageView, maxPx: Int, onMissing: () -> Unit) {
        target.tag = contactId
        cache.get(contactId)?.let {
            target.setImageBitmap(it)
            return
        }
        target.setImageDrawable(null)
        val app = context.applicationContext
        executor.execute {
            val bitmap = decode(app, contactId, maxPx)
            main.post {
                if (target.tag != contactId) return@post // a vista já foi reutilizada para outro contacto
                if (bitmap != null) {
                    cache.put(contactId, bitmap)
                    target.setImageBitmap(bitmap)
                } else {
                    onMissing()
                }
            }
        }
    }

    private fun decode(context: Context, contactId: Long, maxPx: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val bytes = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)
            ?.use { it.readBytes() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
