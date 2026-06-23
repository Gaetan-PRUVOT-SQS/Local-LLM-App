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

## 3. Modèle — téléchargé au 1er lancement (pas d'étape de build)

Le modèle n'est **plus embarqué dans l'APK** : l'application le télécharge au premier
lancement depuis l'URL définie dans `app/.../data/ModelVariant.kt` (`downloadUrl`).

- Par défaut, l'URL pointe sur la résolution Hugging Face
  (`https://huggingface.co/<repo>/resolve/main/<fichier>`).
- Le repo HF source étant soumis à la **licence Gemma** (gated), fournissez une **URL directe
  non protégée** (votre propre miroir HF public, un bucket, un CDN) pour un téléchargement
  sans friction depuis l'app.

Pour préparer un tel miroir, récupérez le fichier en local :

```bash
export HF_TOKEN=hf_votre_token   # si le repo source est gated
bash scripts/download_model.sh   # → models/gemma-4-E2B-it.litertlm (~2,4 Go)
```

…puis ré-hébergez ce fichier à une URL directe et reportez-la dans `ModelVariant.downloadUrl`.

## 4. Compiler l'APK

```bash
./gradlew assembleDebug
```

APK généré (~70 Mo, **sans modèle**) :

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
| Lib NPU (optionnel) | `ls app/src/main/jniLibs/arm64-v8a/` |
| APK compilé | `ls -lh app/build/outputs/apk/debug/app-debug.apk` |

Le modèle se télécharge dans le stockage interne de l'appareil au 1er lancement
(`filesDir/models/`) — il n'est ni dans le repo ni dans l'APK.

## 7. Build de production

Le build release applique **R8** (minification + `shrinkResources`) et est **signé**.

### Signature (secrets hors dépôt)

Les identifiants de signature sont résolus dans cet ordre : propriété Gradle → variable
d'environnement → `keystore.properties` local (dev). Pour la CI / la publication :

```bash
export RELEASE_STORE_FILE=/chemin/vers/release.jks
export RELEASE_STORE_PASSWORD=********
export RELEASE_KEY_ALIAS=localllmapp
export RELEASE_KEY_PASSWORD=********
./gradlew assembleRelease   # ou bundleRelease pour un .aab Play Store
```

> **Play App Signing recommandé** : publiez un `.aab` ; Google gère la clé d'app, vous ne
> conservez que la clé d'upload. Ne committez jamais le keystore ni les mots de passe.

### URL du modèle (miroir non-gated)

Le repo HF source est gated (licence Gemma). Pour une distribution publique, hébergez le
`.litertlm` sur une **URL directe non protégée** et passez-la au build :

```bash
./gradlew assembleRelease -PMODEL_URL_OVERRIDE=https://votre-miroir/gemma-4-E2B-it.litertlm
```

L'app vérifie ensuite l'**intégrité SHA-256** du fichier téléchargé (cf. `ModelVariant.sha256`)
et exige l'**acceptation de la licence Gemma** in-app avant le téléchargement.

### Tests

```bash
./gradlew testDebugUnitTest
```

## 7bis. Dépannage

### Téléchargement du modèle refusé (HTTP 401/403) au 1er lancement

Le repo HF source est gated. Buildez avec `-PMODEL_URL_OVERRIDE=<url>` pointant vers un
miroir public (voir §7), ou définissez la variable d'env `MODEL_URL_OVERRIDE`.

### Erreur mémoire Gradle

Augmentez la heap dans `gradle.properties` :

```properties
org.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8
```

### NPU non disponible

L'application bascule automatiquement sur GPU puis CPU. Vérifiez que `fetch_npu_libs.sh` a bien été exécuté sur un Pixel Tensor G5.