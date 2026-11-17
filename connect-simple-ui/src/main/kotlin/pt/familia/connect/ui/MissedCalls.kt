package pt.familia.connect.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import pt.familia.connect.core.FamilyContact
import java.util.Calendar

/**
 * O aviso "A Ana ligou-lhe · Ligar de volta" do ecrã inicial (Figma: "aviso · chamada perdida").
 *
 * Regra simples, sem guardar nada: olha para o registo de chamadas das últimas 48 horas e, para
 * cada familiar, só conta a chamada **mais recente**. Se essa foi perdida, aparece o aviso. Quando
 * o idoso liga de volta, a chamada mais recente passa a ser a de saída e o aviso desaparece sozinho.
 */
internal object MissedCalls {
    data class Missed(val contact: FamilyContact, val timeMillis: Long)

    private const val WINDOW_MS = 48L * 60 * 60 * 1000
    private const val MAX_ROWS = 200

    fun latest(context: Context, family: List<FamilyContact>): Missed? {
        if (family.isEmpty()) return null
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return null
        val since = System.currentTimeMillis() - WINDOW_MS
        val seen = HashSet<String>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            "${CallLog.Calls.DATE} > ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            var rows = 0
            while (c.moveToNext() && rows++ < MAX_ROWS) {
                val number = c.getString(0) ?: continue
                val contact = family.firstOrNull { samePhone(it.phoneNumber, number) } ?: continue
                if (!seen.add(contact.lookupKey)) continue // só a chamada mais recente de cada familiar
                if (c.getInt(1) == CallLog.Calls.MISSED_TYPE) return Missed(contact, c.getLong(2))
            }
        }
        return null
    }

    /** Compara pelos últimos 9 dígitos (números portugueses com ou sem +351). */
    internal fun samePhone(a: String?, b: String?): Boolean {
        val da = a?.filter(Char::isDigit) ?: return false
        val db = b?.filter(Char::isDigit) ?: return false
        if (da.length < 6 || db.length < 6) return da == db
        return da.takeLast(9) == db.takeLast(9)
    }

    fun isToday(timeMillis: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }
}
