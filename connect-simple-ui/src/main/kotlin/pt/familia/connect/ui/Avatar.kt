package pt.familia.connect.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import pt.familia.connect.core.FamilyContact

/** Fotografia grande, ou as iniciais em fundo de alto contraste quando não há fotografia. */
internal fun bindAvatar(context: Context, contact: FamilyContact, photo: ImageView, initials: TextView, sizePx: Int) {
    initials.visibility = View.GONE
    photo.visibility = View.VISIBLE
    initials.text = initialsOf(contact.name)
    initials.setBackgroundColor(AVATAR_COLORS[(contact.contactId % AVATAR_COLORS.size).toInt()])
    PhotoLoader.load(context, contact.contactId, photo, sizePx) {
        photo.visibility = View.GONE
        initials.visibility = View.VISIBLE
    }
}

internal fun initialsOf(name: String): String =
    name.split(' ', '-').filter { it.isNotBlank() }.let { parts ->
        when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2)
            else -> "${parts.first().first()}${parts.last().first()}"
        }
    }.uppercase()

/** Cores escuras, todas com contraste ≥ 4,5:1 com texto branco (WCAG AA; a maioria ≥ 7:1). */
private val AVATAR_COLORS = intArrayOf(
    0xFF0D47A1.toInt(), 0xFF1B5E20.toInt(), 0xFF4A148C.toInt(),
    0xFFB71C1C.toInt(), 0xFF004D40.toInt(), 0xFF3E2723.toInt(),
)
