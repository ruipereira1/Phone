package pt.familia.connect.ui

import android.content.Context
import android.content.Intent
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ModuleInitializer

/** Regista o ecrã principal e o ecrã de chamadas recebidas no arranque do processo. */
class SimpleUiInitializer : ModuleInitializer() {
    override fun initialize(context: Context) {
        ConnectServices.registerHome { Intent(it, HomeActivity::class.java) }
        ConnectServices.registerIncomingPresenter(IncomingCallNotifier)
    }
}
