# Fossify Connect — fork independente da Fossify Phone

O Fossify Connect é um fork da [Fossify Phone](https://github.com/FossifyOrg/Phone),
adaptado a pessoas idosas: um telefone simples, com fotografias grandes, que liga
à família por chamada normal ou videochamada.

- **Licença:** GPL-3.0, igual à original (ver `LICENSE` e `NOTICE`).
- **Base:** Fossify Phone 1.11.1, commit `4044183603b41d928ccd9391fbcf6a52e68a8eb6`.
- **Histórico:** o histórico completo do projeto original foi mantido.
- **Instalação:** convive com a Fossify Phone original, porque usa outro `applicationId`.

## Regras do fork

1. O repositório original nunca é modificado. O remote `upstream` tem o push desativado.
2. O fork tem repositório Git próprio, no remote `origin`, que é teu.
3. Identidade visual própria: nome "Fossify Connect", ícone e cores do módulo `connect-simple-ui`.
4. `applicationId` próprio (`CONNECT_APP_ID` em `gradle.properties`). O `namespace`
   Kotlin mantém-se `org.fossify.phone`, para que as atualizações vindas do upstream
   não entrem em conflito.
5. Pode instalar-se lado a lado com a Fossify Phone original.
6. O histórico de origem e a licença GPL-3.0 são mantidos.
7. Todas as alterações ao código original estão em [`FORK-CHANGES.md`](FORK-CHANGES.md).
8. Não depende de alterações manuais à Fossify instalada no aparelho.
9. As atualizações do upstream são integradas de forma controlada (ver abaixo).
10. Os módulos `connect-facetime`, `connect-matrix` e `connect-simple-ui` são independentes:
    cada um depende apenas de `connect-core` e nunca de outro módulo.

## Ramos

| Ramo | Função |
|---|---|
| `upstream-main` | Espelho só de leitura de `upstream/main`. Nunca recebe commits nossos. |
| `connect/main` | Ramo principal do Fossify Connect. |
| `sync/<versão>` | Ramo temporário para integrar uma versão do upstream, depois de revista e testada. |

## Atualizar a partir do upstream

```bash
scripts/sync-upstream.sh 1.12.0   # tag ou commit do upstream
```

O script:

1. atualiza `upstream-main`;
2. cria `sync/<versão>` a partir de `connect/main` e faz merge da versão pedida, **sem commit automático**;
3. avisa se o merge mexeu em ficheiros dos módulos `connect-*`. Esses ficheiros nunca vêm do upstream;
4. lista os ficheiros de [`FORK-CHANGES.md`](FORK-CHANGES.md) que mudaram no upstream, para rever um a um.

Depois de rever e testar (ver `docs/TESTES.md`), faz-se o merge de `sync/<versão>` para `connect/main`.
Os módulos personalizados nunca são substituídos automaticamente.

## Módulos

```
:app                    Fossify Phone (chamadas, contactos, histórico, favoritos, Telecom); alterações mínimas
:connect-core           Interfaces e modelos partilhados. Sem dependências externas
:connect-facetime       Videochamadas FaceTime através dos links oficiais da Apple
:connect-matrix         Reservado à videochamada Android↔Android (opção D). Ainda vazio
:connect-simple-ui      Interface para idosos: ecrã de fotografias, ações, chamada recebida, configuração
```

Se um módulo for retirado da compilação, os outros continuam a compilar. Os módulos não se
conhecem: cada um declara no seu manifesto um `ModuleInitializer`, que o regista em
`ConnectServices` (`connect-core`) quando o processo arranca. A `:app` limita-se a declarar os
módulos como dependências.

## CI

`.github/workflows/connect-build.yml` corre os testes, gera o APK `foss` debug, confirma que
cada módulo compila sozinho e verifica o `applicationId`. Os outros workflows da pasta vêm do
upstream e chamam workflows privados da Fossify. Não se apagam (para não criar conflitos no
sync); desligam-se uma vez no GitHub do fork:

```bash
for w in image-minimizer no-response pr-labeler pr prepare-release-pr release testing-build \
         update-commons update-lint-baselines validate-fastlane-metadata; do
  gh workflow disable "$w.yml"
done
```

## Estado do FaceTime

Ver `docs/RELATORIO-FASE1.md`. Resumo: o caminho implementado (links oficiais da Apple) permite
ao tablet **entrar** numa chamada FaceTime. Não faz tocar o iPhone como uma chamada nativa.
Nenhuma funcionalidade FaceTime está classificada como FUNCIONA enquanto não for testada com
equipamentos reais.
