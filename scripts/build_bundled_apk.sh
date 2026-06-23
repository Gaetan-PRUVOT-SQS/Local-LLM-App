#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="$ROOT/models/gemma-4-E2B-it.litertlm"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
EXPECTED_SIZE=2588147712

cd "$ROOT"

echo "=== Step 1: HF auth (optionnel) ==="
if command -v hf >/dev/null 2>&1; then
  hf auth whoami 2>&1 || true
else
  echo "hf CLI non installé — utilisation de curl/Python"
  if [[ -z "${HF_TOKEN:-}" ]]; then
    echo "Astuce: si le téléchargement échoue, acceptez la licence Gemma sur HF puis:"
    echo "  export HF_TOKEN=hf_xxx"
  fi
fi

echo ""
echo "=== Step 2: Search HF cache ==="
find ~/.cache/huggingface -name 'gemma-4-E2B-it.litertlm' 2>/dev/null | head -3 || true

echo ""
echo "=== Step 3: Download model if missing ==="
"$ROOT/scripts/download_model.sh"

echo ""
echo "=== Step 4: Verify model ==="
ls -lh "$MODEL"
ACTUAL_SIZE=$(stat -f%z "$MODEL" 2>/dev/null || stat -c%s "$MODEL")
echo "Size bytes: $ACTUAL_SIZE (expected ~$EXPECTED_SIZE)"
if [[ "$ACTUAL_SIZE" -lt 2500000000 ]]; then
  echo "ERROR: model file too small" >&2
  exit 1
fi

echo ""
echo "=== Step 4b: Split model into chunks (<2 Go pour Android) ==="
"$ROOT/scripts/split_model.sh"
ls -lh "$ROOT/models/chunks/"

echo ""
echo "=== Step 5: Build APK ==="
./gradlew assembleDebug

echo ""
echo "=== Step 6: APK output ==="
ls -lh "$APK"
echo "APK path: $APK"