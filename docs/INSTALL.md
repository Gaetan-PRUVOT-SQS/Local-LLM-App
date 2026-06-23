# Installation sur appareil

Guide pour installer l'APK sur un téléphone Android et comprendre le premier lancement.

## Prérequis appareil

| Critère | Recommandation |
|---------|----------------|
| Android | 8.0+ (API 26 minimum) |
| Architecture | arm64-v8a |
| RAM | 6 Go+ recommandé |
| Stockage libre | ~3 Go (téléchargement du modèle) |
| Pixel Tensor G5 | NPU optionnel (Pixel 10) |

## Méthode simple — sans ordinateur (recommandée)

1. Sur ton téléphone, ouvre la page **[Releases](https://github.com/Gaetan-PRUVOT-SQS/Local-LLM-App/releases/latest)** et télécharge `app-release.apk`.
2. Ouvre le fichier depuis **Téléchargements**.
3. Android demande d'autoriser l'installation depuis cette source → **Paramètres → autoriser** « installer des applications inconnues », puis reviens et installe.
4. Ouvre **Gemma Chat**.

Le reste de ce document décrit l'installation **via `adb`** (développeurs).

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

## 3. Premier lancement — parcours d'onboarding

L'APK est **léger (~70 Mo) et ne contient pas le modèle**. Au premier lancement, l'app
déroule l'onboarding :

1. **Compatibilité** — scan RAM / processeur / stockage / NPU
2. **Choix du modèle** — taille, version texte-seule, Wi-Fi uniquement
3. **Téléchargement** — récupération du `.litertlm` (~2,4 Go) dans le stockage interne
   (`files/models/`), avec progression, pause et **reprise automatique** si la connexion coupe
4. **Hub** une fois le modèle prêt

> Le téléchargement ne se produit **qu'une seule fois**. Les lancements suivants vont
> directement au Hub. La permission `INTERNET` ne sert qu'à cette étape : l'inférence est
> ensuite 100 % hors-ligne.

Durée typique : quelques minutes selon le débit réseau.

### Utiliser un modèle déjà présent sur le téléphone (sans téléchargement)

Deux possibilités si le `.litertlm` est déjà sur l'appareil :

1. **Import via l'app** — sur l'écran « Choix du modèle », bouton **« Charger un modèle déjà
   présent (.litertlm) »** : sélectionnez le fichier (Téléchargements, stockage, etc.), il est
   copié dans l'app puis chargé. Aucun réseau.
2. **Push direct via adb** — déposez le fichier dans le stockage interne de l'app ; il est
   **détecté au lancement** et l'onboarding est sauté :

   ```bash
   adb push gemma-4-E2B-it.litertlm \
     /sdcard/Android/data/com.gaetan.localllmapp/files/models/gemma-4-E2B-it.litertlm
   ```

   (Le nom de fichier doit correspondre à `ModelVariant.fileName`.)

## 4. Chargement du moteur

Après le téléchargement, LiteRT-LM initialise le moteur (~10 secondes au premier chargement). Le Hub s'affiche une fois prêt.

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

L'inférence fonctionne hors-ligne. La permission `INTERNET` sert uniquement au
**téléchargement du modèle** au premier lancement.

## 6. Logs et débogage

```bash
# Logs en temps réel
adb logcat -s LlmEngine

# Effacer les données (force un nouveau téléchargement au prochain lancement)
adb shell pm clear com.gaetan.localllmapp
```

## Dépannage

### Téléchargement refusé (HTTP 401/403)

Le repo HF source est gated. L'URL de téléchargement (`ModelVariant.downloadUrl`) doit
pointer vers un **miroir public non protégé**. Voir `docs/BUILD.md` §3.

### Téléchargement interrompu

La reprise est automatique : relancez/Reprenez depuis l'écran de téléchargement, le fichier
`.partial` reprend là où il s'était arrêté. En dernier recours :

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