package pt.familia.connect.core

import android.content.Context
import android.content.Intent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Uma videochamada recebida, à espera de que o idoso a atenda ou rejeite.
 *
 * @property acceptIntent o que se abre quando o idoso toca em ATENDER. É criado pelo fornecedor.
 */
data class IncomingVideoCall(
    val callId: String,
    val providerId: String,
    val callerName: String,
    val callerLookupKey: String?,
    val acceptIntent: Intent,
)

/** Mostra uma chamada recebida, por exemplo num ecrã inteiro com ATENDER e REJEITAR. */
fun interface IncomingCallPresenter {
    fun present(context: Context, call: IncomingVideoCall)
}

/**
 * Ponto de encontro entre módulos. Nenhum módulo conhece outro: cada um regista aqui o que oferece,
 * através de um [ModuleInitializer] declarado no seu manifesto. O Android cria estes
 * inicializadores no arranque do processo, antes de qualquer Activity.
 */
object ConnectServices {
    private val providers = CopyOnWriteArrayList<VideoCallProvider>()

    @Volatile
    private var incomingPresenter: IncomingCallPresenter? = null

    @Volatile
    private var homeIntentFactory: ((Context) -> Intent)? = null

    fun registerVideoProvider(provider: VideoCallProvider) {
        if (providers.none { it.id == provider.id }) providers += provider
    }

    fun videoProviders(): List<VideoCallProvider> = providers.toList()

    /** O primeiro fornecedor configurado para este contacto, pela ordem de registo. */
    fun videoProviderFor(context: Context, contact: FamilyContact): VideoCallProvider? =
        providers.firstOrNull { it.isConfigured(context, contact) }

    fun registerIncomingPresenter(presenter: IncomingCallPresenter) {
        incomingPresenter = presenter
    }

    /** @return false se nenhum módulo de interface estiver instalado para mostrar a chamada. */
    fun presentIncoming(context: Context, call: IncomingVideoCall): Boolean {
        val presenter = incomingPresenter ?: return false
        presenter.present(context, call)
        return true
    }

    /** Ecrã principal simplificado, registado pelo módulo de interface. */
    fun registerHome(factory: (Context) -> Intent) {
        homeIntentFactory = factory
    }

    /** @return null se o módulo de interface simplificada não estiver incluído na compilação. */
    fun homeIntent(context: Context): Intent? = homeIntentFactory?.invoke(context)
}
