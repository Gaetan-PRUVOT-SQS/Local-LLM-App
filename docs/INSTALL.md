# Installation sur appareil

Guide pour installer l'APK sur un téléphone Android et comprendre le premier lancement.

## Prérequis appareil

| Critère | Recommandation |
|---------|----------------|
| Android | 8.0+ (API 26 minimum) |
| Architecture | arm64-v8a |
| RAM | 8 Go+ recommandé |
| Stockage libre | ~3 Go (extraction du modèle) |
| Pixel Tensor G5 | NPU optionnel (Pixel 10) |

## 1. Activer le débogage USB

1. **Paramètres → À propos du téléphone** — appuyez 7 fois sur « Numéro de build »
2. **Paramètres → Options pour les développeurs** — activez « Débogage USB »
3. Connectez le téléphone en USB et acceptez l'autorisation ADB

Vérifiez la connexion :

```bash
adb devices
```

## 2. Installer l'APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Options utiles :

```bash
# Réinstaller en conservant les données
adb install -r -d app/build/outputs/apk/debug/app-debug.apk

# Désinstaller avant réinstallation propre
adb uninstall com.gaetan.localllmapp
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Installation sans câble (optionnel)

```bash
adb install -r --streaming app/build/outputs/apk/debug/app-debug.apk
```

Ou transférez l'APK sur l'appareil et ouvrez-le (autorisez les sources inconnues si demandé).

## 3. Premier lancement — extraction du modèle

L'APK embarque le modèle découpé en chunks (~2,4 Go compressés dans l'APK). **Au premier lancement**, l'application :

1. Lit `manifest.json` depuis les assets
2. Assemble les chunks `000.bin` … `003.bin`
3. Écrit `gemma-4-E2B-it.litertlm` dans le stockage interne (`files/models/`)
4. Affiche une barre de progression

> Cette étape ne se produit **qu'une seule fois**. Les lancements suivants chargent directement le modèle extrait.

Durée typique : 1 à 3 minutes selon la vitesse de stockage.

## 4. Chargement du moteur

Après l'extraction, LiteRT-LM initialise le moteur (~10 secondes au premier chargement). L'écran de chat s'affiche une fois prêt.

### Backends disponibles

| Appareil | Backend par défaut | Alternatives |
|----------|-------------------|--------------|
| Pixel Tensor G5 | NPU | GPU, CPU |
| Pixel Tensor G3/G4 | GPU | CPU |
| Autre arm64 | GPU | CPU |

Changez le backend en touchant l'indicateur dans l'en-tête (NPU → GPU → CPU).

## 5. Permissions

L'application demande à l'usage :

- **Microphone** — enregistrement vocal
- **Photos / médias** — sélection d'images et fichiers audio

L'inférence fonctionne hors-ligne. La permission `INTERNET` sert uniquement au chargement des polices Google Fonts (Manrope) au premier affichage.

## 6. Logs et débogage

```bash
# Logs en temps réel
adb logcat -s LlmEngine

# Effacer les données (force une nouvelle extraction)
adb shell pm clear com.gaetan.localllmapp
```

## Dépannage

### « Manifest des chunks absent dans l'APK »

L'APK a été compilé sans chunks. Recompilez avec `bash scripts/build_bundled_apk.sh`.

### « Taille incorrecte après assemblage »

Données corrompues. Dans l'app : **Réinstaller le modèle**, ou :

```bash
adb shell pm clear com.gaetan.localllmapp
```

Puis relancez l'application.

### L'application se ferme au chargement

- Vérifiez la RAM disponible (fermez les apps en arrière-plan)
- Consultez `adb logcat` pour l'erreur LiteRT-LM
- Essayez le backend CPU depuis l'en-tête

### NPU indisponible sur Pixel G5

1. Vérifiez que `bash scripts/fetch_npu_libs.sh` a été exécuté avant la compilation
2. Recompilez et réinstallez l'APK