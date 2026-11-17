package pt.familia.connect.facetime

import java.net.URI
import java.net.URISyntaxException

/**
 * Link oficial de chamada FaceTime, no formato `https://facetime.apple.com/join#v=1&p=...&k=...`.
 *
 * Só se aceitam links do domínio oficial da Apple, por HTTPS. O fragmento (`#...`) leva a chave da
 * chamada e nunca é enviado para o servidor, por isso tem de estar presente e não vazio.
 *
 * Kotlin puro (java.net.URI), para poder ser testado na JVM sem Android.
 */
@JvmInline
value class FaceTimeLink private constructor(val url: String) {

    override fun toString() = "FaceTimeLink(facetime.apple.com/join#…)" // nunca registar a chave nos logs

    companion object {
        const val HOST = "facetime.apple.com"
        private const val PATH = "/join"

        /** @return o link normalizado, ou null se não for um link FaceTime oficial válido. */
        fun parse(raw: String?): FaceTimeLink? {
            val text = raw?.trim()?.trimEnd('.', ',', ';', ')', ']', '>', '"', '\'') ?: return null
            if (text.isEmpty() || text.length > MAX_LENGTH) return null
            val uri = try {
                URI(text)
            } catch (e: URISyntaxException) {
                return null
            }
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (!uri.host.equals(HOST, ignoreCase = true)) return null
            if (uri.port != -1 && uri.port != 443) return null
            if (uri.rawUserInfo != null) return null
            if (uri.rawPath != PATH) return null
            val fragment = uri.rawFragment
            if (fragment.isNullOrBlank()) return null
            return FaceTimeLink("https://$HOST$PATH#$fragment")
        }

        /** Encontra o primeiro link FaceTime válido num texto (por exemplo, numa SMS). */
        fun findIn(text: String?): FaceTimeLink? {
            if (text.isNullOrEmpty()) return null
            return CANDIDATE.findAll(text).firstNotNullOfOrNull { parse(it.value) }
        }

        /** Confirma que um endereço carregado na WebView pertence à Apple (navegação permitida). */
        fun isAppleOrigin(url: String?): Boolean {
            if (url == null) return false
            val rawHost: String? = try {
                URI(url).host
            } catch (e: URISyntaxException) {
                null
            }
            val host = rawHost?.lowercase() ?: return false
            return host == "apple.com" || host.endsWith(".apple.com") ||
                host == "icloud.com" || host.endsWith(".icloud.com")
        }

        private const val MAX_LENGTH = 2048
        private val CANDIDATE = Regex("""https://facetime\.apple\.com/join#\S+""", RegexOption.IGNORE_CASE)
    }
}
