#!/usr/bin/env bash
set -euo pipefail

# Récupère le plugin de sampling top-K GPU (OpenCL) officiel de LiteRT-LM et le
# rend chargeable avec l'AAR Maven litertlm 0.13.1.
#
# Pourquoi : sans cette lib, le sampler top-K retombe sur CPU (un aller-retour
# GPU->CPU des logits ~256k par token), ce qui plombe le débit de décodage.
# Avec, le sampling reste sur GPU (~2x tok/s mesuré sur Pixel 8 Pro / Tensor G3).
#
# Le binaire est distribué en git-LFS dans le repo google-ai-edge/LiteRT-LM.
# Il n'a pas de DT_NEEDED vers libLiteRt.so et s'attend à résoudre
# LiteRtCreateEnvironment depuis le scope global ; avec l'AAR ce symbole vit dans
# libLiteRt.so chargé en scope local -> dlopen échoue. On ajoute donc un
# DT_NEEDED libLiteRt.so via patchelf pour que le linker le résolve.

VERSION="v0.13.1"          # doit correspondre à la version de l'AAR litertlm
LIB="libLiteRtTopKOpenClSampler.so"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET_DIR="$ROOT/app/src/main/jniLibs/arm64-v8a"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for tool in git git-lfs patchelf; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "Outil manquant: $tool"
    echo "  macOS : brew install $tool"
    exit 1
  }
done

echo "Clone sparse de LiteRT-LM $VERSION (git-LFS)…"
GIT_LFS_SKIP_SMUDGE=1 git clone --filter=blob:none --no-checkout --depth 1 \
  --branch "$VERSION" https://github.com/google-ai-edge/LiteRT-LM.git "$TMP/src"
git -C "$TMP/src" sparse-checkout set --no-cone prebuilt/android_arm64
git -C "$TMP/src" checkout "$VERSION"

SRC="$TMP/src/prebuilt/android_arm64/$LIB"
[ -f "$SRC" ] || { echo "Introuvable: $SRC"; exit 1; }

echo "Ajout du DT_NEEDED libLiteRt.so…"
patchelf --add-needed libLiteRt.so "$SRC"

mkdir -p "$TARGET_DIR"
cp "$SRC" "$TARGET_DIR/$LIB"
echo "Installé $TARGET_DIR/$LIB"
patchelf --print-needed "$TARGET_DIR/$LIB" | head -1
