# GemmaChat (Local-LLM-App)

[Français](#français) · [English](#english)

Chat Android **100 % local** avec Gemma multimodal (LiteRT-LM).

**Fully offline** on-device multimodal Android chat with Gemma (LiteRT-LM).

---

## Français

### Présentation

Application Android de chat avec **Gemma**, fonctionnant **entièrement sur l’appareil** — aucune donnée ne quitte le téléphone, usage hors ligne possible.

Stack : **Kotlin / Jetpack Compose**, inférence via **LiteRT-LM**.

### Fonctionnalités

- Chat texte en streaming, rendu Markdown (gras, listes, titres, code avec copie)
- Image : analyse / description d’une photo
- Audio : enregistrement vocal (WAV PCM 16 kHz) envoyé au modèle
- Conversations multiples persistées (tiroir : naviguer, renommer, supprimer)
- Bascule de backend d’inférence : NPU → GPU → CPU, avec repli automatique
- 100 % local / hors ligne, sélection/copie des réponses, arrêt de la génération

### Prérequis

- Android Studio (AGP 8.7+), JDK 17
- Android SDK (`sdk.dir` dans `local.properties`)
- Appareil ou émulateur **arm64-v8a**, `minSdk 26`

### Build

```bash
# ouvrir le projet dans Android Studio
# ou
./gradlew assembleDebug
```

---

## English

### Overview

Android chat app powered by **Gemma**, running **fully on-device** — no data leaves the phone; works offline.

Stack: **Kotlin / Jetpack Compose**, inference via **LiteRT-LM**.

### Features

- Streaming text chat with Markdown rendering (bold, lists, headings, code + copy)
- Image: analyze / describe a photo
- Audio: voice recording (WAV PCM 16 kHz) sent to the model
- Multiple persisted conversations (drawer: browse, rename, delete)
- Inference backend switching: NPU → GPU → CPU with automatic fallback
- Fully local / offline, select/copy answers, stop generation

### Prerequisites

- Android Studio (AGP 8.7+), JDK 17
- Android SDK (`sdk.dir` in `local.properties`)
- **arm64-v8a** device/emulator, `minSdk 26`

### Build

```bash
# open the project in Android Studio
# or
./gradlew assembleDebug
```
