#!/usr/bin/env python3
"""Bootstrap local repo: gradle wrapper jar, permissions, git init."""
from __future__ import annotations

import os
import shutil
import stat
import subprocess
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WRAPPER_JAR = ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"
LEGACY = Path.home() / "Desktop" / "gemma-chat" / "gradle" / "wrapper" / "gradle-wrapper.jar"
WRAPPER_URL = (
    "https://github.com/gradle/gradle/raw/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
)
LOCAL_PROPERTIES = ROOT / "local.properties"
LEGACY_LOCAL_PROPERTIES = Path.home() / "Desktop" / "gemma-chat" / "local.properties"
DEFAULT_SDK = Path.home() / "Library" / "Android" / "sdk"


def ensure_wrapper_jar() -> None:
    WRAPPER_JAR.parent.mkdir(parents=True, exist_ok=True)
    if WRAPPER_JAR.exists() and WRAPPER_JAR.stat().st_size > 1000:
        print(f"OK: {WRAPPER_JAR} already present")
        return
    if LEGACY.exists():
        shutil.copy2(LEGACY, WRAPPER_JAR)
        print(f"OK: copied from {LEGACY}")
        return
    print(f"Downloading {WRAPPER_URL} ...")
    urllib.request.urlretrieve(WRAPPER_URL, WRAPPER_JAR)
    size = WRAPPER_JAR.stat().st_size
    if size < 1000:
        WRAPPER_JAR.unlink(missing_ok=True)
        raise RuntimeError(f"Downloaded wrapper jar too small ({size} bytes)")
    print(f"OK: downloaded to {WRAPPER_JAR} ({size} bytes)")


def chmod_scripts() -> None:
    gradlew = ROOT / "gradlew"
    if gradlew.exists():
        gradlew.chmod(gradlew.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    for script in (ROOT / "scripts").glob("*.sh"):
        script.chmod(script.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    py_bootstrap = ROOT / "scripts" / "bootstrap_repo.py"
    if py_bootstrap.exists():
        py_bootstrap.chmod(py_bootstrap.stat().st_mode | stat.S_IXUSR)
    print("OK: executable bits set")


def detect_android_sdk() -> Path | None:
    env = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if env:
        path = Path(env).expanduser()
        if (path / "platforms").is_dir():
            return path
    if DEFAULT_SDK.is_dir() and (DEFAULT_SDK / "platforms").is_dir():
        return DEFAULT_SDK
    if LEGACY_LOCAL_PROPERTIES.exists():
        for line in LEGACY_LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                path = Path(line.split("=", 1)[1].strip())
                if path.is_dir():
                    return path
    return None


def ensure_local_properties() -> None:
    if LOCAL_PROPERTIES.exists():
        print(f"OK: {LOCAL_PROPERTIES} already present")
        return
    sdk = detect_android_sdk()
    if sdk is None:
        print(
            "WARN: Android SDK introuvable — créez local.properties avec sdk.dir=...",
            file=sys.stderr,
        )
        return
    LOCAL_PROPERTIES.write_text(f"sdk.dir={sdk}\n", encoding="utf-8")
    print(f"OK: wrote {LOCAL_PROPERTIES} (sdk.dir={sdk})")


def git_init() -> None:
    git_dir = ROOT / ".git"
    if git_dir.exists():
        print("OK: .git already exists")
        return
    subprocess.run(["git", "init"], cwd=ROOT, check=True)
    subprocess.run(["git", "config", "user.name", "Gaetan Pruvot"], cwd=ROOT, check=True)
    subprocess.run(
        ["git", "config", "user.email", "pruvot.gaetan@gmail.com"], cwd=ROOT, check=True
    )
    print("OK: git initialized")


def main() -> int:
    os.chdir(ROOT)
    ensure_wrapper_jar()
    chmod_scripts()
    ensure_local_properties()
    git_init()
    size = WRAPPER_JAR.stat().st_size if WRAPPER_JAR.exists() else 0
    print(f"Bootstrap complete. gradle-wrapper.jar: {size} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())