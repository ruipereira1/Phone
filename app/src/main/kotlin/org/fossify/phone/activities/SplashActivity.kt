package org.fossify.phone.activities

import android.content.Intent
import org.fossify.commons.activities.BaseSplashActivity
import pt.familia.connect.core.ConnectServices
import pt.familia.connect.core.ConnectSettings

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        // Fossify Connect: com o modo simplificado ligado, abre o ecrã de fotografias.
        // Sem o módulo de interface, ou com o modo desligado, abre a Fossify Phone original.
        val simpleHome = if (ConnectSettings.isSimpleModeEnabled(this)) ConnectServices.homeIntent(this) else null
        startActivity(simpleHome ?: Intent(this, MainActivity::class.java))
        finish()
    }
}
