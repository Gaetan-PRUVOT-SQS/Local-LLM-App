#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="$ROOT/models/gemma-4-E2B-it.litertlm"
REPO="litert-community/gemma-4-E2B-it-litert-lm"
FILE="gemma-4-E2B-it.litertlm"
URL="https://huggingface.co/$REPO/resolve/main/$FILE"
EXPECTED_MIN=2500000000

mkdir -p "$ROOT/models"

if [[ -f "$MODEL" ]]; then
  SIZE=$(stat -f%z "$MODEL" 2>/dev/null || stat -c%s "$MODEL")
  if [[ "$SIZE" -ge "$EXPECTED_MIN" ]]; then
    echo "Modèle déjà présent: $MODEL ($(numfmt --to=iec "$SIZE" 2>/dev/null || echo "${SIZE} bytes"))"
    exit 0
  fi
  echo "Fichier incomplet détecté, re-téléchargement…"
  rm -f "$MODEL"
fi

download_with_hf_cli() {
  command -v hf >/dev/null 2>&1 || return 1
  echo "Téléchargement via hf CLI…"
  hf download "$REPO" "$FILE" --local-dir "$ROOT/models"
}

download_with_python() {
  command -v python3 >/dev/null 2>&1 || return 1
  echo "Téléchargement via huggingface_hub (Python)…"
  python3 - <<PY
import os
import sys

try:
    from huggingface_hub import hf_hub_download
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "huggingface_hub"])
    from huggingface_hub import hf_hub_download

token = os.environ.get("HF_TOKEN") or os.environ.get("HUGGING_FACE_HUB_TOKEN")
path = hf_hub_download(
    repo_id="$REPO",
    filename="$FILE",
    local_dir="$ROOT/models",
    token=token,
)
print("OK:", path)
PY
}

download_with_curl() {
  command -v curl >/dev/null 2>&1 || return 1
  echo "Téléchargement via curl (~2,4 Go)…"
  local partial="$MODEL.partial"
  local headers=()
  if [[ -n "${HF_TOKEN:-}" ]]; then
    headers+=(-H "Authorization: Bearer $HF_TOKEN")
  fi
  curl -fL "${headers[@]}" -C - -o "$partial" "$URL"
  mv "$partial" "$MODEL"
}

echo "=== Téléchargement Gemma 4 E2B Q4 (.litertlm) ==="

if download_with_hf_cli; then
  :
elif download_with_python; then
  :
elif download_with_curl; then
  :
else
  cat <<EOF >&2
Échec: aucun outil de téléchargement disponible.

Installez l'un des suivants puis relancez:
  brew install curl
  pip3 install huggingface_hub
  curl -LsSf https://hf.co/cli/install.sh | bash

Si le repo est gated (licence Gemma), exportez votre token:
  export HF_TOKEN=hf_xxx
  ./scripts/download_model.sh
EOF
  exit 1
fi

if [[ ! -f "$MODEL" ]]; then
  # hf/python may place file directly in models/
  if [[ -f "$ROOT/models/$FILE" ]]; then
    :
  else
    echo "ERREUR: fichier introuvable après téléchargement" >&2
    exit 1
  fi
fi

SIZE=$(stat -f%z "$MODEL" 2>/dev/null || stat -c%s "$MODEL")
echo "Taille: $SIZE octets"
if [[ "$SIZE" -lt "$EXPECTED_MIN" ]]; then
  echo "ERREUR: fichier trop petit (licence Gemma ? token HF manquant ?)" >&2
  rm -f "$MODEL"
  exit 1
fi

echo "Modèle prêt: $MODEL"