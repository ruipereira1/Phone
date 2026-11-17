# Alterações relativamente à Fossify Phone

Base: Fossify Phone **1.11.1**, commit `4044183603b41d928ccd9391fbcf6a52e68a8eb6` (16-11-2026).

Este ficheiro lista **todas** as alterações a ficheiros que vieram do upstream. Os módulos
`connect-*`, `docs/`, `scripts/`, `FORK.md`, `FORK-CHANGES.md`, `NOTICE` e
`.github/workflows/connect-build.yml` são novos, e o upstream nunca lhes toca.

Regra: cada alteração a um ficheiro do upstream é mínima e leva um comentário `Fossify Connect`
no próprio código, para ser fácil de encontrar num conflito de merge.

| Ficheiro do upstream | Alteração | Porquê | Risco no sync |
|---|---|---|---|
| `gradle.properties` | Novo `CONNECT_APP_ID=pt.familia.connect`; o `APP_ID` original mantém-se | `applicationId` próprio sem mudar o namespace | Baixo |
| `app/build.gradle.kts` | `applicationId` passa a vir de `CONNECT_APP_ID`; placeholder `baseAppId`; APK `connect-*`; dependências dos módulos `connect-*` | Instalação lado a lado com a original; o namespace `org.fossify.phone` mantém R/BuildConfig e evita conflitos | Médio (o ficheiro muda com frequência no upstream) |
| `app/src/main/AndroidManifest.xml` | Os 19 `activity-alias` `.activities.SplashActivity.<Cor>` passam a `${baseAppId}.activities.SplashActivity.<Cor>`; rótulo da aplicação `@string/app_name` | O Commons (`toggleAppIconColor`) monta o nome a partir do applicationId | Médio |
| `app/src/main/res/values/donottranslate.xml` | `app_name` = "Fossify Connect" | Identidade própria | Baixo |
| `app/src/main/kotlin/org/fossify/phone/activities/SplashActivity.kt` | Abre o ecrã simplificado se estiver ligado; caso contrário, a `MainActivity` | É o único ponto de entrada; um ícone só | Baixo |
| `settings.gradle.kts` | `rootProject.name`; `include` dos módulos `connect-*` | Estrutura modular | Baixo |
| `build.gradle.kts` | Plugin `com.android.library` declarado (`apply false`) | Módulos `connect-*` | Baixo |
| `gradle/libs.versions.toml` | Alias `androidLibrary` (mesma versão do AGP); `junit` 4.13.2, **apenas para testes** | Módulos e testes JVM | Baixo |

## Dependências acrescentadas

| Dependência | Onde | Justificação |
|---|---|---|
| `junit:junit:4.13.2` | `testImplementation` de `connect-facetime` | Testes da validação de links. Não vai no APK. |

Não há dependências de execução novas. Os módulos usam só a plataforma Android: `Activity`,
`WebView`, `Keystore`, `ContactsContract`, `Telecom`.

## Comportamento que muda para o utilizador

- Ao abrir a app, aparece o ecrã de fotografias. O cuidador pode desligar isto em
  Configuração → "Abrir sempre no ecrã de fotografias".
- A Fossify Phone completa (marcador, histórico, favoritos, definições, bloqueio, marcação
  rápida) está sempre no botão **Telefone**. Não foi removida nenhuma funcionalidade.
- O ecrã de chamada **telefónica** (GSM) continua a ser o da Fossify. Adaptá-lo aos idosos
  está pendente e vai precisar de um ponto de extensão no `CallActivity` (ver `docs/TESTES.md`,
  pendentes).

## Avisos conhecidos herdados do Commons

- `BaseSimpleActivity` mostra de vez em quando um aviso de "app modificada" quando o
  packageName não começa por `org.fossify.`. Só aparece nos ecrãs originais da Fossify
  (botão Telefone), nunca nos ecrãs simplificados, que não herdam de `BaseSimpleActivity`.
  Não foi alterado, para não mexer no Commons.
- `appLaunched()`, no ramo de migração de ícones de versões antigas, monta
  `<applicationId>.activities.SplashActivity` sem `try/catch`. Numa instalação nova esse ramo
  nunca corre (`appRunCount == 0`). Fica registado para os testes de atualização.
