# Gemma 4 E2B — Chat Android hors-ligne

**Auteur :** Gaetan Pruvot

Application Android de chat multimodal en inférence locale, propulsée par [Gemma 4 E2B](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) et [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM). Les prompts et médias ne quittent pas l'appareil pendant l'inférence.

## Fonctionnalités

- **Chat multimodal** — texte, images et audio (enregistrement ou fichier)
- **Inférence locale** via LiteRT-LM (`litertlm-android`)
- **Modèle Q4 embarqué** — Gemma 4 E2B quantifié (~2,4 Go), découpé en chunks pour contourner la limite Android de 2 Go par asset
- **Accélération matérielle** — GPU/CPU sur tous les appareils compatibles ; **NPU Tensor G5** sur Pixel 10 (backend NPU + modèle Q4 embarqué)
- **Interface Jetpack Compose** — thème sombre, streaming des réponses, sélection du backend (NPU / GPU / CPU)

## Prérequis

| Outil | Version minimale |
|-------|------------------|
| Android Studio | Ladybug ou plus récent |
| JDK | 17 |
| Android SDK | API 35 (compileSdk) |
| Appareil cible | Android 8.0+ (API 26), arm64-v8a recommandé |
| Espace disque | ~5 Go (modèle + build) |

Pour le modèle Gemma, acceptez la [licence Gemma](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) sur Hugging Face. Un token `HF_TOKEN` peut être nécessaire.

## Démarrage rapide

```bash
git clone <url-du-repo> gemma-4-e2b-android
cd gemma-4-e2b-android

# Première fois : wrapper Gradle + git local
bash scripts/setup_repo.sh   # télécharge/copie gradle-wrapper.jar + git init

# QA avant publication
bash scripts/qa_repo.sh

# Lib NPU (optionnel, Pixel Tensor G5)
bash scripts/fetch_npu_libs.sh

# Télécharger le modèle, découper en chunks et compiler
bash scripts/build_bundled_apk.sh

# Installer sur l'appareil
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Consultez la documentation détaillée :

- [Compilation](docs/BUILD.md)
- [Installation sur appareil](docs/INSTALL.md)
- [Assurance qualité](docs/QA.md)

## Architecture du projet

```
gemma-4-e2b-android/
├── app/                    # Module Android (Kotlin + Compose)
├── models/                 # Modèle .litertlm (local, non versionné)
├── scripts/                # Téléchargement, découpage, build
├── docs/                   # Documentation
└── gradle/                 # Wrapper Gradle
```

Le modèle n'est **pas** inclus dans le dépôt Git. Les scripts le téléchargent depuis Hugging Face puis le découpent en chunks de 700 Mo embarqués dans l'APK. Au premier lancement, l'application réassemble le fichier `gemma-4-E2B-it.litertlm` (Q4, 2 588 147 712 octets) dans le stockage interne.

L'APK embarque **uniquement le modèle Q4 universel**. Sur Pixel Tensor G5, l'accélération NPU utilise ce même fichier avec `libLiteRtDispatch_GoogleTensor.so` — pas un second modèle séparé dans l'APK.

La bibliothèque native NPU est récupérée via `bash scripts/fetch_npu_libs.sh` et n'est pas versionnée.

## Licence

- **Code source** de cette application : [Apache-2.0](LICENSE)
- **Modèle Gemma 4 E2B** : [Gemma Terms of Use](https://ai.google.dev/gemma/terms) — téléchargement via [Hugging Face](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) après acceptation de la licence
- **LiteRT-LM** : [Apache-2.0](https://github.com/google-ai-edge/LiteRT-LM)