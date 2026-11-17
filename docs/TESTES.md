# Plano e registo de testes — Fossify Connect

Classificação obrigatória: **FUNCIONA** (testado com dois equipamentos reais) · **PARCIAL**
(funciona, com limitações documentadas) · **NÃO SUPORTADO** · **POR VALIDAR**.

Estado atual: **nenhum teste em equipamento real foi feito.**

Verificação feita no ambiente de desenvolvimento, que não chegava ao Gradle, ao Maven nem ao SDK:
- os quatro módulos `connect-*` e a `SplashActivity` alterada compilam com o Kotlin 2.0.21
  contra o `android.jar` da API 36, sem erros nem avisos (R gerado a partir dos recursos);
- os testes de `FaceTimeLink` (20 verificações) passam na JVM.

Ainda não foram verificados: o merge dos manifestos, o AAPT2/recursos, o lint, o AGP 9.4 com o
Kotlin 2.4 do projeto e o APK final. É a primeira coisa que o CI (`connect-build.yml`) faz.

## Equipamento

| Papel | Equipamento | Versão |
|---|---|---|
| Tablet do idoso | Android 10–15″ | _a preencher_ |
| Familiar Android | Smartphone Android | _a preencher_ |
| Familiar iPhone | iPhone com FaceTime | iOS _a preencher_ |
| Conta Apple | Conta dedicada aos testes | — |

## 0. Compilação e instalação

| # | Teste | Resultado esperado | Resultado |
|---|---|---|---|
| 0.1 | CI `connect-build.yml` | Testes verdes; APK gerado; applicationId `pt.familia.connect.debug` | POR VALIDAR |
| 0.2 | Instalar ao lado da Fossify Phone original | As duas apps coexistem | POR VALIDAR |
| 0.3 | Definir o Fossify Connect como telefone predefinido (banner vermelho) | O papel de marcador é concedido | POR VALIDAR |
| 0.4 | Mudar a cor do ícone (Fossify → Definições → Personalizar cores) | O ícone muda e não há erro | POR VALIDAR |

## 1. Funcionalidades originais (não podem regredir)

| # | Teste | Resultado |
|---|---|---|
| 1.1 | Chamada telefónica feita a partir do ecrã de fotografias | POR VALIDAR |
| 1.2 | Chamada telefónica recebida (ecrã da Fossify) | POR VALIDAR |
| 1.3 | Marcador, histórico, favoritos, contactos, bloqueio, marcação rápida (botão Telefone) | POR VALIDAR |
| 1.4 | Conferência, chamada em espera, escolha de SIM | POR VALIDAR |

## 2. Interface simplificada

| # | Teste | Resultado |
|---|---|---|
| 2.1 | Favoritos aparecem com fotografia grande, nome e botões Ligar/Vídeo (2 a 5 colunas, telemóvel e tablet de 10″/13″), iguais ao Figma "1 · Início" | POR VALIDAR |
| 2.1m | Telemóvel ao alto (≈ 412 dp): uma coluna, cartões como o Figma "M1", botões Ligar/Vídeo sem texto cortado; deitado: 2 colunas | POR VALIDAR |
| 2.1b | Um toque em Ligar inicia a chamada telefónica (sem ecrã intermédio) | POR VALIDAR |
| 2.1c | Chamada perdida de um familiar mostra o aviso; depois de ligar de volta, o aviso desaparece | POR VALIDAR |
| 2.2 | Sem fotografia: aparecem as iniciais | POR VALIDAR |
| 2.3 | O toque longo de 3 s no relógio abre a configuração; um toque curto não abre | POR VALIDAR |
| 2.4 | O "voltar" não sai do ecrã principal | POR VALIDAR |
| 2.5 | Uso como ecrã principal (HOME) ligado e desligado | POR VALIDAR |
| 2.6 | O TalkBack lê "Ligar a <nome>" e "Videochamada com <nome>" | POR VALIDAR |

## 3. FaceTime (links oficiais) — matriz do PROJECT.md

| # | Teste | Resultado esperado | Resultado |
|---|---|---|---|
| 3.1 | Android → iPhone | O tablet entra no link e o iPhone recebe a SMS de aviso e admite a entrada. **Não há toque FaceTime nativo** | NÃO SUPORTADO (toque nativo) · POR VALIDAR (entrada pelo link) |
| 3.2 | iPhone → Android | O familiar envia o link por SMS e o tablet mostra ATENDER/REJEITAR | POR VALIDAR |
| 3.3 | Android → Android | Fora do âmbito do FaceTime (opção D, `connect-matrix`) | NÃO SUPORTADO |
| 3.4 | Ecrã bloqueado | O ecrã ATENDER/REJEITAR e o ecrã da chamada aparecem por cima do bloqueio | POR VALIDAR |
| 3.5 | Reiniciar o tablet | Os links continuam configurados; os convites por SMS continuam a chegar | POR VALIDAR |
| 3.6 | Perda de Internet durante a chamada | A página mostra o erro; DESLIGAR funciona; consegue-se voltar a entrar | POR VALIDAR |
| 3.7 | Microfone | Áudio nos dois sentidos; o botão Microfone silencia | POR VALIDAR |
| 3.8 | Câmara | Vídeo nos dois sentidos (câmara frontal) | POR VALIDAR |
| 3.9 | Wi-Fi e dados móveis | As duas redes funcionam | POR VALIDAR |
| 3.10 | Preenchimento automático do nome | O nome aparece na página da Apple | POR VALIDAR (experimental) |
| 3.11 | Link de remetente que não está nos favoritos | É ignorado | POR VALIDAR |
| 3.12 | Android 14+: full-screen intent concedido ao marcador | O ecrã inteiro aparece com o tablet bloqueado | POR VALIDAR |

## Registo de logs

```bash
adb logcat -s ConnectFaceTime:* chromium:W AndroidRuntime:E
```

Não guardar logs com números de telefone, links FaceTime nem nomes. O código não regista nada
disso (`FaceTimeLink.toString()` esconde a chave).

## Pendentes (próximas iterações)

- Ecrã de chamada telefónica para idosos (ATENDER/REJEITAR enormes também nas chamadas GSM).
  Precisa de um ponto de extensão mínimo no `CallActivity` da Fossify.
- Modo tablet dedicado (Fase 4): arranque automático, recuperação após falhas, configuração
  remota dos contactos.
- PoC-B (OpenBubbles): **o OpenBubbles não exporta nenhum intent para iniciar FaceTime**
  (manifesto analisado). A integração por intents não é possível. Fica só a avaliação manual da
  app.
