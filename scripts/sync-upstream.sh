#!/usr/bin/env bash
# Integra uma versão do upstream (Fossify Phone) de forma controlada.
# Uso: scripts/sync-upstream.sh <tag-ou-commit-do-upstream>
set -euo pipefail

REF="${1:?Uso: $0 <tag-ou-commit-do-upstream>}"
cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Erro: há alterações por gravar. Faz commit ou stash primeiro." >&2
  exit 1
fi

git fetch upstream --tags
git rev-parse --verify --quiet "$REF^{commit}" >/dev/null || REF="upstream/$REF"
git rev-parse --verify "$REF^{commit}" >/dev/null

# 1. Espelho só de leitura.
git branch -f upstream-main upstream/main

# 2. Ramo de integração, sem commit automático.
BRANCH="sync/${1//\//-}"
git checkout -b "$BRANCH" connect/main
if ! git merge --no-ff --no-commit "$REF"; then
  echo ">> Há conflitos. Resolve-os neste ramo ($BRANCH)."
fi

# 3. Os módulos connect-* nunca vêm do upstream.
TOUCHED="$(git diff --cached --name-only | grep -E '^connect-' || true)"
if [[ -n "$TOUCHED" ]]; then
  echo ">> ATENÇÃO: o merge mexeu em módulos personalizados. Estas alterações vão ser repostas:" >&2
  echo "$TOUCHED" >&2
  git checkout connect/main -- $(echo "$TOUCHED" | cut -d/ -f1 | sort -u)
fi

# 4. Ficheiros alterados pelo fork que também mudaram no upstream.
echo ">> Ficheiros do fork alterados também no upstream (rever um a um):"
grep -oE '`(app|gradle|settings|build)[^`]*`' FORK-CHANGES.md | tr -d '`' | sort -u | while read -r f; do
  if git diff --cached --name-only | grep -qx "$f"; then echo "   - $f"; fi
done

echo ">> Próximo passo: rever, compilar, correr docs/TESTES.md e depois:"
echo "   git commit && git checkout connect/main && git merge --no-ff $BRANCH"
