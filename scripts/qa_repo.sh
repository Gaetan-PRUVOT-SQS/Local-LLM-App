#!/usr/bin/env bash
# QA smoke test du dépôt — à lancer avant publication GitHub.
# Documentation : docs/QA.md
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
PASS=0
FAIL=0

if [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then
  echo "gradle-wrapper.jar absent — bootstrap..."
  python3 "$ROOT/scripts/bootstrap_repo.py"
fi

check() {
  local name="$1"
  shift
  if "$@"; then
    echo "PASS: $name"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $name"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== QA Local-LLM-App ==="

check "LICENSE" test -f LICENSE
check "gradle-wrapper.jar" test -f gradle/wrapper/gradle-wrapper.jar
check "gradlew executable" test -x gradlew
check "git initialized" test -d .git

for s in scripts/*.sh; do
  check "bash -n $(basename "$s")" bash -n "$s"
done

check "no litertlm in repo" bash -c '! find . -name "*.litertlm" ! -path "./.git/*" | grep -q .'
check "no chunk bins in repo" bash -c '! find models/chunks -name "*.bin" 2>/dev/null | grep -q .'

if [[ -f gradle/wrapper/gradle-wrapper.jar ]]; then
  ./gradlew --version | head -3
  check "gradlew --version" ./gradlew --version
fi

if [[ -f models/chunks/manifest.json ]]; then
  if [[ ! -f local.properties ]]; then
    echo "local.properties absent — bootstrap SDK..."
    python3 "$ROOT/scripts/bootstrap_repo.py"
  fi
  if [[ ! -f local.properties ]]; then
    echo "FAIL: assembleDebug — Android SDK non configuré"
    echo "  Créez local.properties : sdk.dir=$HOME/Library/Android/sdk"
    echo "  Ou : export ANDROID_HOME=$HOME/Library/Android/sdk"
    FAIL=$((FAIL + 1))
  elif ./gradlew assembleDebug; then
    check "assembleDebug" test -f app/build/outputs/apk/debug/app-debug.apk
  else
    echo "FAIL: assembleDebug"
    FAIL=$((FAIL + 1))
  fi
else
  echo "SKIP: assembleDebug (models/chunks/manifest.json absent — normal pour clone frais)"
fi

echo ""
echo "=== Résultat: $PASS PASS, $FAIL FAIL ==="
[[ "$FAIL" -eq 0 ]]