package pt.familia.connect.core

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Classificação obrigatória do PROJECT.md. Cada fornecedor declara o estado real de cada
 * capacidade e o ecrã de configuração mostra-o, para que nada teórico apareça como concluído.
 */
enum class SupportStatus(val label: String) {
    /** Testado com dois equipamentos reais. */
    FUNCIONA("FUNCIONA"),

    /** Funciona, com limitações documentadas. */
    PARCIAL("PARCIAL"),

    /** Não existe implementação funcional. */
    NAO_SUPORTADO("NÃO SUPORTADO"),

    /** Ainda não há testes suficientes. */
    POR_VALIDAR("POR VALIDAR"),
}

/** Uma linha da matriz de capacidades de um fornecedor: requisito, estado e nota. */
data class Capability(val requirement: String, val status: SupportStatus, val note: String)

sealed interface StartResult {
    data object Started : StartResult
    data object NotConfigured : StartResult
    data class Failed(val reason: String) : StartResult
}

/**
 * Um transporte de videochamada (FaceTime por link, WebRTC, ...). As implementações vivem em
 * módulos independentes e registam-se em [ConnectServices] quando a app arranca.
 */
interface VideoCallProvider {
    /** Identificador estável, por exemplo "facetime-link". */
    val id: String

    /** Nome para o cuidador, por exemplo "FaceTime (link)". Os idosos não o veem. */
    val displayName: String

    /** Indica se este contacto já está configurado para videochamada com este fornecedor. */
    fun isConfigured(context: Context, contact: FamilyContact): Boolean

    /** Começa uma videochamada. Chama-se a partir de uma Activity em primeiro plano. */
    fun startVideoCall(activity: Activity, contact: FamilyContact): StartResult

    /** Ecrã do cuidador para configurar este contacto, ou null se não houver nada a configurar. */
    fun setupIntent(context: Context, contact: FamilyContact): Intent?

    /** Estado declarado das capacidades, mostrado ao cuidador. */
    fun capabilities(): List<Capability>
}
