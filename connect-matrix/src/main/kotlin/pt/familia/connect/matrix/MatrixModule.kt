package pt.familia.connect.matrix

/**
 * Reservado. Videochamada Android↔Android com o Matrix como **sinalização** e WebRTC
 * (Element Call/LiveKit ou outro) como **transporte**. Os dois são distintos (ver
 * docs/RELATORIO-FASE1.md, secções 4 e 9).
 *
 * Não é usado para FaceTime: o Rustpush-Matrix só dá avisos de FaceTime, não chamadas.
 *
 * De propósito, este módulo não regista nenhum fornecedor. Uma implementação só entra depois da
 * PoC-D, com as dependências justificadas em FORK-CHANGES.md.
 */
internal object MatrixModule {
    const val STATUS = "NÃO IMPLEMENTADO"
}
