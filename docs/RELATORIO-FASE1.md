# Relatório técnico da Fase 1 (resumo)

O relatório completo é um documento partilhado: "Fossify Connect — Relatório técnico da Fase 1"
(https://claude.ai/artifact/6CBwF3u1jkuSmecuE9ALz8). Aqui fica o essencial, para acompanhar o código.

## Conclusões

1. **Não existe** um caminho oficial e licenciável em GPL-3.0 para um Android fazer tocar um
   iPhone com uma chamada FaceTime nativa.
2. **Links FaceTime oficiais:** a Apple suporta que o Android *entre* pelo browser numa chamada
   criada por um dispositivo Apple. Este é o caminho implementado (`connect-facetime`).
3. **Rustpush-Matrix:** o FaceTime é só de notificação (não faz nem atende chamadas). Não serve.
4. **OpenBubbles / rustpush:** tem sinalização FaceTime nativa e média pela página web da Apple
   (WebView). Problemas: o rustpush está sob **SSPL-1.0**, com exceção só para o OpenBubbles, o
   que é incompatível com a GPL-3.0; exige a identidade de hardware de um Mac; há risco de
   bloqueio pela Apple (precedente Beeper Mini, 2023).
5. **Novo (análise do manifesto do OpenBubbles):** a app não exporta nenhum intent nem deep link
   para iniciar FaceTime. Os únicos filtros são para `hw.openbubbles.app`, iCloud Shared Albums,
   partilha de imagens e SMS. **Não é possível integrá-la por intents.**

## Decisões tomadas na implementação

| Tema | Decisão |
|---|---|
| Transporte FaceTime | Opção A: links oficiais. Sem rustpush e sem engenharia inversa |
| `applicationId` | `pt.familia.connect` (configurável em `gradle.properties`), fora de `org.fossify.*` |
| Namespace | Fica `org.fossify.phone`, para reduzir conflitos com o upstream |
| Nome | "Fossify Connect", como pedido. Recomenda-se confirmar o uso do nome com a Fossify (ver `NOTICE`) |
| Matrix | Reservado à opção D (Android↔Android). Sem implementação nem dependências |
