#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="$ROOT/models/gemma-4-E2B-it.litertlm"
CHUNKS_DIR="$ROOT/models/chunks"
MANIFEST="$CHUNKS_DIR/manifest.json"
CHUNK_SIZE=$((700 * 1024 * 1024)) # 700 Mo — sous la limite Java 2 Go

if [[ ! -f "$SOURCE" ]]; then
  echo "Modèle source introuvable: $SOURCE" >&2
  echo "Lancez d'abord: ./scripts/download_model.sh" >&2
  exit 1
fi

python3 - <<PY
import json
import os
from pathlib import Path

source = Path("$SOURCE")
chunks_dir = Path("$CHUNKS_DIR")
chunk_size = $CHUNK_SIZE

chunks_dir.mkdir(parents=True, exist_ok=True)
for old in chunks_dir.glob("*.bin"):
    old.unlink()

total = source.stat().st_size
chunk_names = []
index = 0
written = 0

with source.open("rb") as src:
    while written < total:
        name = f"{index:03d}.bin"
        path = chunks_dir / name
        remaining = total - written
        to_read = min(chunk_size, remaining)
        with path.open("wb") as out:
            while to_read > 0:
                block = src.read(min(1024 * 1024, to_read))
                if not block:
                    break
                out.write(block)
                to_read -= len(block)
                written += len(block)
        chunk_names.append(name)
        print(f"Chunk {name}: {path.stat().st_size} bytes")
        index += 1

manifest = {
    "fileName": source.name,
    "expectedSizeBytes": total,
    "chunkCount": len(chunk_names),
    "chunks": chunk_names,
}
manifest_path = Path("$MANIFEST")
manifest_path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(f"Manifest: {manifest_path}")
print(f"Total chunks: {len(chunk_names)}, total bytes: {total}")
PY