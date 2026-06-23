# Assurance qualité du dépôt

Guide pour valider le dépôt avant publication GitHub ou après modifications importantes.

## Objectif

Le script `scripts/qa_repo.sh` exécute un **smoke test** automatisé :

- vérifier l'intégrité du dépôt (fichiers requis, absence de binaires lourds) ;
- valider la syntaxe des scripts shell ;
- confirmer que Gradle fonctionne ;
- compiler l'APK debug si les chunks du modèle sont disponibles localement.

Ce test ne remplace pas les tests manuels sur appareil (chat, multimodal, NPU). Il garantit que le dépôt est **clonable, buildable et prêt à être versionné**.

## Prérequis

| Outil | Détail |
|-------|--------|
| Python 3 | Pour `bootstrap_repo.py` (auto-appelé si besoin) |
| JDK | 17 recommandé (17–26 testé) |
| Android SDK | API 35 — détecté via `ANDROID_HOME` ou `~/Library/Android/sdk` |
| Chunks (optionnel) | `models/chunks/manifest.json` pour le test `assembleDebug` |

## Lancer la QA

### QA minimale (clone frais)

```bash
cd Local-LLM-App
bash scripts/setup_repo.sh
bash scripts/qa_repo.sh
```

Résultat attendu : **14 PASS, 0 FAIL**, avec `SKIP: assembleDebug` (normal sans chunks).

### QA complète (avec compilation APK)

```bash
cd Local-LLM-App
bash scripts/setup_repo.sh
bash scripts/fetch_npu_libs.sh

# Chunks locaux (symlink ou générés par build_bundled_apk.sh)
ln -sf /chemin/vers/models/chunks models/chunks

bash scripts/qa_repo.sh
```

Résultat attendu : **15 PASS, 0 FAIL**, `BUILD SUCCESSFUL`, APK dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

## Checks exécutés

| # | Check | Description |
|---|-------|-------------|
| 1 | `LICENSE` | Fichier Apache-2.0 présent |
| 2 | `gradle-wrapper.jar` | Wrapper Gradle bootstrapé |
| 3 | `gradlew executable` | Binaire Gradle exécutable |
| 4 | `git initialized` | Dépôt Git local initialisé |
| 5–11 | `bash -n scripts/*.sh` | Syntaxe valide de tous les scripts |
| 12 | `no litertlm in repo` | Aucun fichier `.litertlm` versionné |
| 13 | `no chunk bins in repo` | Aucun `*.bin` dans `models/chunks/` |
| 14 | `gradlew --version` | Gradle répond correctement |
| 15 | `assembleDebug` | Compilation APK (si chunks présents) |

## Critères de sortie

| Scénario | PASS | FAIL |
|----------|------|------|
| Clone frais (sans chunks) | 14 PASS, 0 FAIL | Tout échec bloquant |
| Avec chunks + SDK | 15 PASS, 0 FAIL | Build ou check structure |

Le script retourne le code de sortie **0** si `FAIL == 0`, sinon **1**.

## Dernier run validé

Environnement de référence (juin 2026) :

| Paramètre | Valeur |
|-----------|--------|
| OS | macOS 26.5.1 aarch64 |
| JDK | Temurin 26.0.1 |
| Gradle | 9.3.1 |
| SDK | `~/Library/Android/sdk` |
| Résultat | **15 PASS, 0 FAIL** — build en ~29 s |

Warnings non bloquants observés :

- `statusBarColor` / `navigationBarColor` dépréciés dans `Theme.kt`
- `stripDebugDebugSymbols` sur les libs LiteRT (normal)
- Avertissements de compatibilité Gradle 10 (plugins)

## Fichiers exclus du dépôt Git

La QA vérifie que ces éléments **ne sont pas commités** :

```
models/*.litertlm
models/chunks/
models/.cache/
app/src/main/jniLibs/
app/build/
.gradle/
local.properties
*.apk
```

## Dépannage

### `permission denied: ./scripts/qa_repo.sh`

```bash
bash scripts/qa_repo.sh
# ou
chmod +x scripts/*.sh gradlew
```

### `SDK location not found`

```bash
bash scripts/setup_repo.sh
# ou manuellement :
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### `Chunks du modèle manquants` (preBuild)

Normal sur un clone frais. Pour tester la compilation :

```bash
bash scripts/download_model.sh
bash scripts/split_model.sh
# ou symlink vers un dossier chunks existant
```

### `assembleDebug` échoue avec JDK 26+

Repasser sur JDK 17 :

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
bash scripts/qa_repo.sh
```

### Symlink `models/chunks` détecté par `git status`

Ne pas committer le symlink ni les chunks. Vérifier :

```bash
git status
# models/chunks ne doit pas apparaître comme fichier à ajouter
```

## QA manuelle complémentaire

Après le smoke test automatisé, valider sur appareil :

1. Installation APK (`docs/INSTALL.md`)
2. Premier lancement — extraction des chunks (~1–3 min)
3. Chat texte + streaming
4. Envoi image + audio
5. Bascule backend NPU / GPU / CPU (en-tête)
6. Pixel Tensor G5 — NPU si `fetch_npu_libs.sh` a été exécuté avant build

## Scripts associés

| Script | Rôle |
|--------|------|
| `scripts/qa_repo.sh` | Smoke test QA (ce document) |
| `scripts/bootstrap_repo.py` | Wrapper Gradle, SDK, git init, permissions |
| `scripts/setup_repo.sh` | Raccourci vers `bootstrap_repo.py` |
| `scripts/build_bundled_apk.sh` | Pipeline complet modèle + APK |