# Gemma 4 E2B — Chat Android hors-ligne

**Auteur :** Gaetan Pruvot

Application Android de chat multimodal en inférence locale, propulsée par [Gemma 4 E2B](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) et [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM). **L'exécution du LLM est 100 % hors-ligne** : une fois le modèle installé, prompts et médias ne quittent jamais l'appareil.

## 📥 Installation (utilisateurs)

**[➜ Télécharger la dernière version (APK)](https://github.com/Gaetan-PRUVOT-SQS/Local-LLM-App/releases/latest)**

1. Sur ton **téléphone Android**, télécharge `Local-LLM-App-release.apk` depuis la page Releases.
2. Ouvre le fichier (*Téléchargements*) → autorise « **installer des applications inconnues** » si demandé.
3. Lance **Gemma Chat**. Au 1er démarrage : vérification de compatibilité → acceptation de la
   licence Gemma → **téléchargement du modèle (~2,4 Go, Wi-Fi conseillé)**. Ensuite, **tout est hors-ligne**.

| | |
|---|---|
| Android | 8.0+ (API 26), **arm64-v8a** |
| RAM | 6 Go+ recommandé |
| Stockage libre | ~3 Go (modèle) |

> Astuce : tu as déjà le `.litertlm` ? Utilise **« Charger un modèle déjà présent »** dans l'app — aucun téléchargement.

Guide détaillé : [docs/INSTALL.md](docs/INSTALL.md).

## Parcours

Onboarding complet, du premier lancement au chat :

1. **Compatibilité** — scan RAM / processeur / stockage / NPU (multi-vendeur)
2. **Choix du modèle** — carte modèle, version texte-seule, Wi-Fi uniquement, import local
3. **Licence Gemma** — acceptation des *Gemma Terms of Use* avant téléchargement
4. **Téléchargement** — progression résumable (pause / reprise auto) + vérification SHA-256
5. **Hub** — lanceur multimodal (Discuter / Analyser / Transcrire / Skills) + discussions récentes
6. **Chat** — accueil, streaming live + télémétrie (tok/s · °C · batterie), actions Copier / Régénérer / Partager, menu ⋮
7. **Paramètres** — backend, Wi-Fi, effacer données, à propos
8. **Historique** — toutes les discussions (ouvrir / supprimer / effacer tout)

## Fonctionnalités

- **Chat multimodal** — texte, images et audio (enregistrement ou fichier), rendu markdown
- **Inférence 100 % locale** via LiteRT-LM (`litertlm-android`) — fonctionne en mode avion
- **Modèle téléchargé au 1er lancement** — APK léger ; le `.litertlm` (~2,4 Go) est récupéré depuis l'URL Hugging Face **publique** puis stocké en interne. Téléchargement **résumable**, Wi-Fi en option, **intégrité SHA-256** vérifiée
- **Modèle local accepté** — « Charger un modèle déjà présent » (sélecteur de fichiers) ou push `adb` : un `.litertlm` déjà sur le téléphone est utilisé sans téléchargement (auto-détecté)
- **Universel — pas seulement Pixel** — modèle Q4 par défaut sur **tout** appareil arm64 ; détection multi-vendeur du SoC (Qualcomm Snapdragon, MediaTek Dimensity, Samsung Exynos, Google Tensor)
- **Accélération matérielle** — GPU/CPU partout ; backend **NPU** sur Tensor G5 (repli GPU automatique sinon)
- **Skills** — presets de prompt (Résumé, Traduction, Correction, Code, Brainstorm, Explication simple) appliqués en un tap
- **Persistance** — discussions sauvegardées et restaurables ; préférences (backend, Wi-Fi, licence)
- **Interface Jetpack Compose** — thème sombre, polices embarquées (Manrope / JetBrains Mono), edge-to-edge

## Production

- **Build release** : R8 (minification + `shrinkResources`), signé (secrets hors dépôt via variables d'env)
- **Vérification d'intégrité** SHA-256 du modèle ; **garde-fou** stockage ; **crash logger** local
- **Tests unitaires** JVM (`./gradlew testDebugUnitTest`)
- Détails : [docs/BUILD.md §7](docs/BUILD.md)

## Prérequis

| Outil | Version minimale |
|-------|------------------|
| Android Studio | Ladybug ou plus récent |
| JDK | 17 |
| Android SDK | API 35 (compileSdk) |
| Appareil cible | Android 8.0+ (API 26), arm64-v8a recommandé |
| Espace disque (build) | ~1 Go (APK léger, sans modèle) |
| Stockage appareil | ~3 Go libres (le modèle est téléchargé au 1er lancement) |

Le modèle est **téléchargé au premier lancement** depuis Hugging Face. Le lien direct
(`resolve/main/…`) est **public** (téléchargement anonyme, aucun token requis) — aucun
hébergement à fournir. Un **miroir** reste possible en option via `-PMODEL_URL_OVERRIDE=<url>`
au build (indépendance vis-à-vis de HF). L'utilisateur accepte les *Gemma Terms of Use*
in-app avant le téléchargement.

## Démarrage rapide

```bash
git clone <url-du-repo> Local-LLM-App
cd Local-LLM-App

# Première fois : wrapper Gradle + git local
bash scripts/setup_repo.sh   # télécharge/copie gradle-wrapper.jar + git init

# Lib NPU (optionnel, Pixel Tensor G5)
bash scripts/fetch_npu_libs.sh

# Compiler l'APK (léger — aucun modèle à préparer)
bash scripts/qa_repo.sh      # QA + assembleDebug
# ou directement :
./gradlew assembleDebug

# Installer sur l'appareil — le modèle se télécharge au 1er lancement
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Consultez la documentation détaillée :

- [Compilation](docs/BUILD.md)
- [Installation sur appareil](docs/INSTALL.md)
- [Assurance qualité](docs/QA.md)

## Architecture du projet

```
Local-LLM-App/
├── app/src/main/java/com/gaetan/localllmapp/
│   ├── ui/onboarding/      # Compatibilité, choix modèle, licence, téléchargement
│   ├── ui/hub/             # Hub (lanceur)
│   ├── ui/settings/        # Paramètres
│   ├── ui/history/         # Historique des discussions
│   ├── ui/components/      # Composants Compose réutilisables
│   ├── ui/theme/           # Couleurs, typo, thème
│   ├── data/               # ModelDownloader, ModelVariant, ConversationStore, Skill…
│   ├── device/             # DeviceInfo (détection SoC / NPU / RAM / stockage)
│   ├── llm/                # LlmEngine, ChatRepository (LiteRT-LM)
│   └── audio/              # Enregistrement audio
├── app/src/test/           # Tests unitaires JVM
├── models/                 # Modèle .litertlm (local, non versionné)
├── scripts/                # Outils (download, NPU, QA)
├── docs/                   # Documentation (BUILD / INSTALL / QA)
└── gradle/                 # Wrapper Gradle
```

Le modèle n'est **ni versionné ni embarqué dans l'APK**. Au 1er lancement, l'app le
**télécharge** (`data/ModelDownloader.kt`, HTTP résumable + SHA-256) vers `filesDir/models/`,
ou l'**importe** depuis un fichier local. Sur Pixel Tensor G5, l'accélération NPU utilise
`libLiteRtDispatch_GoogleTensor.so` (via `bash scripts/fetch_npu_libs.sh`, non versionnée).

## Licence

- **Code source** de cette application : [Apache-2.0](LICENSE)
- **Modèle Gemma 4 E2B** : [Gemma Terms of Use](https://ai.google.dev/gemma/terms) — téléchargement via [Hugging Face](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) après acceptation de la licence
- **LiteRT-LM** : [Apache-2.0](https://github.com/google-ai-edge/LiteRT-LM)