#!/usr/bin/env bash
set -euo pipefail

TARGET_DIR="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$TARGET_DIR"

curl -fsSL \
  "https://raw.githubusercontent.com/jegly/Box/main/Android/src/app/src/main/jniLibs/arm64-v8a/libLiteRtDispatch_GoogleTensor.so" \
  -o "$TARGET_DIR/libLiteRtDispatch_GoogleTensor.so"

echo "Installed $TARGET_DIR/libLiteRtDispatch_GoogleTensor.so"