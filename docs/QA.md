# Assurance qualité du dépôt

Guide pour valider le dépôt avant publication GitHub ou après modifications importantes.

## Objectif

Le script `scripts/qa_repo.sh` exécute un **smoke test** automatisé :

- vérifier l'intégrité du dépôt (fichiers requis, absence de binaires lourds) ;
- valider la syntaxe des scripts shell ;
- confirmer que Gradle fonctionne ;
- compiler l'APK debug (APK léger — **le modèle est téléchargé au 1er lancement**, plus
  aucune préparation de modèle/chunks n'est nécessaire).

Ce test ne remplace pas les tests manuels sur appareil (onboarding, chat, multimodal, NPU).
Il garantit que le dépôt est **clonable, buildable et prêt à être versionné**.

## Prérequis

| Outil | Détail |
|-------|--------|
| Python 3 | Pour `bootstrap_repo.py` (auto-appelé si besoin) |
| JDK | 17 recommandé (17–26 testé) |
| Android SDK | API 35 — détecté via `ANDROID_HOME` ou `~/Library/Android/sdk` |

## Lancer la QA

```bash
cd Local-LLM-App
bash scripts/setup_repo.sh
bash scripts/qa_repo.sh          # structure + syntaxe + assembleDebug
./gradlew testDebugUnitTest      # tests unitaires JVM (ModelVariant, Skills)
```

Résultat attendu : **15 PASS, 0 FAIL**, `BUILD SUCCESSFUL`, APK (~70 Mo) dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

> L'APK ne contient **pas** le modèle : il est téléchargé au premier lancement. La
> compilation ne dépend donc plus de chunks locaux.

## Checks exécutés

| # | Check | Description |
|---|-------|-------------|
| 1 | `LICENSE` | Fichier Apache-2.0 présent |
| 2 | `gradle-wrapper.jar` | Wrapper Gradle bootstrapé |
| 3 | `gradlew executable` | Binaire Gradle exécutable |
| 4 | `git initialized` | Dépôt Git local initialisé |
| 5–9 | `bash -n scripts/*.sh` | Syntaxe valide de tous les scripts |
| 10 | `no litertlm in repo` | Aucun fichier `.litertlm` versionné |
| 11 | `no chunk bins in repo` | Aucun `*.bin` dans `models/chunks/` |
| 12 | `gradlew --version` | Gradle répond correctement |
| 13 | `assembleDebug` | Compilation de l'APK (léger) |

(Le nombre exact de lignes `bash -n` suit le nombre de scripts présents.)

## Critères de sortie

Le script retourne le code de sortie **0** si `FAIL == 0`, sinon **1**.

## Fichiers exclus du dépôt Git

La QA vérifie que ces éléments **ne sont pas commités** :

```
models/*.litertlm
models/chunks
models/.cache/
app/src/main/jniLibs/
app/build/
.gradle/
local.properties
*.apk
```

## Dépannage

### `SDK location not found`

```bash
bash scripts/setup_repo.sh
# ou manuellement :
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### `assembleDebug` échoue avec JDK 26+

Repasser sur JDK 17 :

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
bash scripts/qa_repo.sh
```

## QA manuelle complémentaire (parcours complet)

Après le smoke test automatisé, valider sur appareil :

1. Installation APK (`docs/INSTALL.md`)
2. **1er lancement** — Compatibilité → Choix modèle → **Licence Gemma** → **Téléchargement**
   (progression, pause/reprise, vérif SHA-256) → Hub
3. **Import local** — « Charger un modèle déjà présent » (ou `adb push`) → auto-détecté au lancement
4. **2e lancement** (modèle présent) — accès direct au **Hub**
5. **Hub** — cartes Discuter / **Analyser** (ouvre la galerie) / **Transcrire** (démarre l'enregistrement) / **Skills**
6. **Chat** — streaming + télémétrie (tok/s · °C · batterie) ; rendu markdown ; Copier / **Régénérer** / Partager ; menu ⋮
7. **Skills** — activer un preset (ex. Traduction) → le message est traité selon le style
8. **Paramètres** — backend, Wi-Fi, effacer discussions, vider cache modèle, à propos
9. **Historique** — ouvrir une discussion archivée (restaurée), supprimer, effacer tout
10. Envoi image + audio ; bascule backend NPU / GPU / CPU
11. **Offline** — mode avion : l'inférence fonctionne toujours

### Build de production

```bash
./gradlew testDebugUnitTest                 # tests verts
./gradlew assembleRelease                   # R8 + signé (cf. docs/BUILD.md §7)
```
Vérifier que l'APK release **minifié (R8)** se lance et fait de l'inférence sur appareil
(les keep rules JNI litertlm doivent tenir à l'exécution).

## Scripts associés

| Script | Rôle |
|--------|------|
| `scripts/qa_repo.sh` | Smoke test QA (ce document) |
| `scripts/bootstrap_repo.py` | Wrapper Gradle, SDK, git init, permissions |
| `scripts/setup_repo.sh` | Raccourci vers `bootstrap_repo.py` |
| `scripts/download_model.sh` | Récupère le `.litertlm` (préparer un miroir de téléchargement) |
| `scripts/fetch_npu_libs.sh` | Lib NPU Pixel Tensor G5 |
