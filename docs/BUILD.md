# Compilation de l'APK

Guide pas à pas pour préparer le modèle, récupérer les bibliothèques natives et compiler l'application.

## 1. Préparer l'environnement

```bash
# Vérifier Java 17
java -version

# Configurer le SDK Android (Android Studio ou variable ANDROID_HOME)
export ANDROID_HOME=~/Library/Android/sdk   # macOS

# Première configuration du dépôt (wrapper Gradle + git init local)
bash scripts/setup_repo.sh
# ou : python3 scripts/bootstrap_repo.py
```

`local.properties` est généré automatiquement par `bash scripts/setup_repo.sh` s'il détecte le SDK à `~/Library/Android/sdk` ou via `ANDROID_HOME`.

Sinon, créez-le manuellement (non versionné) :

```properties
sdk.dir=/Users/VOTRE_USER/Library/Android/sdk
```

## 2. Récupérer la bibliothèque NPU (Pixel Tensor)

Sur les appareils Pixel équipés d'un SoC Tensor, l'accélération NPU nécessite une bibliothèque de dispatch LiteRT :

```bash
bash scripts/fetch_npu_libs.sh
```

Ce script télécharge `libLiteRtDispatch_GoogleTensor.so` dans :

```
app/src/main/jniLibs/arm64-v8a/
```

> **Note :** Ce fichier n'est pas versionné dans Git. Relancez le script après un clone frais si vous ciblez le NPU Tensor G5.

## 3. Télécharger le modèle Gemma 4 E2B Q4

### Option A — Script tout-en-un (recommandé)

```bash
bash scripts/build_bundled_apk.sh
```

Ce script enchaîne : vérification HF → téléchargement → découpage → compilation.

### Option B — Étapes manuelles

#### 3a. Téléchargement

```bash
bash scripts/download_model.sh
```

Le modèle est stocké dans `models/gemma-4-E2B-it.litertlm` (~2,4 Go).

**Authentification Hugging Face** (si le dépôt est gated) :

```bash
export HF_TOKEN=hf_votre_token
bash scripts/download_model.sh
```

Méthodes supportées (par ordre de préférence) :
- CLI `hf`
- `huggingface_hub` (Python)
- `curl` avec reprise

#### 3b. Découpage en chunks

Android impose une limite de **2 Go** par fichier dans les assets. Le modèle (~2,59 Go) doit être découpé :

```bash
bash scripts/split_model.sh
```

Résultat dans `models/chunks/` :

```
000.bin
001.bin
002.bin
003.bin
manifest.json
```

Chaque chunk fait ~700 Mo. Le `manifest.json` décrit l'assemblage au premier lancement.

## 4. Compiler l'APK

```bash
./gradlew assembleDebug
```

APK généré :

```
app/build/outputs/apk/debug/app-debug.apk
```

### Build release (optionnel)

```bash
./gradlew assembleRelease
```

Configurez une clé de signature dans `app/build.gradle.kts` avant de publier.

## 5. QA automatisée

Avant de committer ou publier sur GitHub :

```bash
bash scripts/qa_repo.sh
```

Détails des checks, critères PASS/FAIL et dépannage : [docs/QA.md](QA.md).

## 6. Vérifications manuelles

| Vérification | Commande |
|--------------|----------|
| Modèle présent | `ls -lh models/gemma-4-E2B-it.litertlm` |
| Chunks générés | `ls -lh models/chunks/` |
| Lib NPU (optionnel) | `ls app/src/main/jniLibs/arm64-v8a/` |
| APK compilé | `ls -lh app/build/outputs/apk/debug/app-debug.apk` |

Taille attendue du modèle : **2 588 147 712 octets**.

## 7. Dépannage

### `Chunks du modèle Gemma 4 E2B Q4 manquants`

Lancez `bash scripts/split_model.sh` avant la compilation.

### Modèle trop petit après téléchargement

- Acceptez la licence Gemma sur Hugging Face
- Vérifiez `HF_TOKEN`
- Supprimez le fichier partiel et relancez `bash scripts/download_model.sh`

### Erreur mémoire Gradle

Augmentez la heap dans `gradle.properties` :

```properties
org.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8
```

### NPU non disponible

L'application bascule automatiquement sur GPU puis CPU. Vérifiez que `fetch_npu_libs.sh` a bien été exécuté sur un Pixel Tensor G5.