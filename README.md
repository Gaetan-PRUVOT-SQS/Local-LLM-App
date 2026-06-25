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

## Modèle (téléchargé au premier lancement)

Le modèle (~2,4 Go) **n'est pas embarqué** dans l'APK : l'application le télécharge
automatiquement au premier démarrage depuis Hugging Face
(`litert-community/gemma-4-E2B-it-litert-lm`, **public, sans token**), avec
barre de progression et **reprise** en cas de coupure. Wi‑Fi conseillé.
→ APK léger (~70 Mo).

Bibliothèques natives optionnelles (accélération), non versionnées :

```bash
./scripts/fetch_npu_libs.sh      # dispatch NPU Google Tensor (Pixel G5)
./scripts/fetch_gpu_sampler.sh   # sampling top‑K sur GPU (~2x le débit de décodage)
```

## Construire

```bash
./gradlew assembleDebug      # APK debug (~70 Mo)
./gradlew installDebug       # installer sur l'appareil connecté
```

## Architecture

```
app/src/main/java/com/gaetan/gemmchat/
├─ llm/        moteur LiteRT‑LM (init, backends, conversation, streaming)
├─ data/       téléchargement du modèle, préférences, persistance des conversations
├─ audio/      enregistrement WAV (AudioRecord)
├─ device/     détection SoC / génération Tensor
└─ ui/         écrans Compose (chat, chargement, tiroir, thème)
```

## Notes

- **Vidéo non supportée** : le modèle traite le texte, l'image et l'audio uniquement.
- Audio : LiteRT‑LM décode via miniaudio → format **WAV/FLAC/MP3** requis (l'app enregistre en WAV).
- La variante NPU (Tensor G5) n'est pas embarquée par défaut (seul le Q4 GPU/CPU l'est).

## Licence

Distribué sous licence **Apache 2.0** — voir [LICENSE](LICENSE).

Le modèle Gemma et les bibliothèques tierces (LiteRT‑LM, polices) restent soumis à
leurs licences respectives (Gemma Terms of Use, Apache 2.0, OFL).
