package pt.familia.connect.facetime

import android.content.Context
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ModuleInitializer

/** Regista o fornecedor FaceTime no arranque do processo (declarado no manifesto deste módulo). */
class FaceTimeInitializer : ModuleInitializer() {
    override fun initialize(context: Context) {
        ConnectServices.registerVideoProvider(FaceTimeProvider())
    }
}
