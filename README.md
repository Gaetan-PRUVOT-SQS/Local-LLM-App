# GemmaChat

Application Android de chat avec **Gemma 4 E2B**, fonctionnant **100 % en local** sur l'appareil — aucune donnée ne quitte le téléphone, fonctionne hors‑ligne.

Construite en Kotlin / Jetpack Compose, inférence via **LiteRT‑LM**.

## Fonctionnalités

- 💬 **Chat texte** en streaming, rendu **Markdown** (gras, listes, titres, blocs de code avec copie)
- 🖼️ **Image** : analyse / description d'une photo
- 🎙️ **Audio** : enregistrement vocal (WAV PCM 16 kHz) envoyé au modèle
- 🗂️ **Conversations multiples** persistées (tiroir : naviguer, renommer, supprimer)
- ⚙️ Bascule de **backend** d'inférence : NPU (Tensor G5) → GPU → CPU, avec repli automatique
- 🔒 100 % local / hors‑ligne, sélection/copie des réponses, arrêt de la génération

## Prérequis

- Android Studio (AGP 8.7+), JDK 17
- Android SDK (définir `sdk.dir` dans `local.properties`)
- Un appareil/émulateur **arm64‑v8a**, `minSdk 26`
- Accès au modèle sur Hugging Face : `litert-community/gemma-4-E2B-it-litert-lm`
  (export `HF_TOKEN` si nécessaire)

## Préparer le modèle (non versionné)

Le modèle (~2,4 Go) et les bibliothèques natives ne sont pas dans le dépôt ; des scripts les récupèrent :

```bash
# Télécharge le modèle puis le découpe en chunks (< limite 2 Go d'Android)
./scripts/download_model.sh
./scripts/split_model.sh

# (optionnel) accélérateurs natifs
./scripts/fetch_npu_libs.sh      # dispatch NPU Google Tensor (Pixel G5)
./scripts/fetch_gpu_sampler.sh   # sampling top‑K sur GPU (~2x le débit de décodage)
```

Le modèle est embarqué dans l'APK sous forme de chunks et réassemblé au premier lancement.

## Construire

```bash
./gradlew assembleDebug      # APK debug
./gradlew installDebug       # installer sur l'appareil connecté
```

Ou tout en un :

```bash
./scripts/build_bundled_apk.sh
```

## Architecture

```
app/src/main/java/com/gaetan/gemmchat/
├─ llm/        moteur LiteRT‑LM (init, backends, conversation, streaming)
├─ data/       modèle bundlé (chunks), préférences, persistance des conversations
├─ audio/      enregistrement WAV (AudioRecord)
├─ device/     détection SoC / génération Tensor
└─ ui/         écrans Compose (chat, chargement, tiroir, thème)
```

## Notes

- **Vidéo non supportée** : le modèle traite le texte, l'image et l'audio uniquement.
- Audio : LiteRT‑LM décode via miniaudio → format **WAV/FLAC/MP3** requis (l'app enregistre en WAV).
- La variante NPU (Tensor G5) n'est pas embarquée par défaut (seul le Q4 GPU/CPU l'est).
