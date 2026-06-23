#!/usr/bin/env bash
# Première configuration du dépôt : wrapper Gradle + git init.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 "$ROOT/scripts/bootstrap_repo.py"