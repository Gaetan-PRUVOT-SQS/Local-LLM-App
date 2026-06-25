# GemmaChat — État du projet

> Dernière mise à jour : 2026-06-25 · Document de reprise / handoff.

## 1. Vue d'ensemble

Application **Android** de chat avec **Gemma 4 E2B**, fonctionnant **100 % en local** (hors-ligne) sur l'appareil. Kotlin / Jetpack Compose, inférence via **LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android:0.13.1`).

- **Package** : `com.gaetan.gemmchat`
- **Dossier local** : `~/Desktop/gemma-chat` (Android Studio / Gradle)
- **Repo GitHub** : https://github.com/Gaetan-PRUVOT-SQS/Local-LLM-App (branche `main`)
- **Release** : v1.0.0 avec `GemmaChat-v1.0.0.apk` (~72 Mo, debug-signed)
- **Cible testée** : Pixel 8 Pro (Tensor G3 → backend GPU), Android 16, arm64-v8a, minSdk 26

> ⚠️ Le repo s'appelle `Local-LLM-App` (ancien nom) mais contient désormais GemmaChat. Une autre app `com.gaetan.localllmapp` (plus riche : onboarding/skills) a été abandonnée/supprimée au profit de celle-ci.

## 2. Statut

| Aspect | État |
|---|---|
| Fonctionnel (chat, multimodal, persistance) | ✅ Stable, 0 crash en campagne QA |
| Usage perso / beta sideloadée | ✅ Prêt |
| Distribution Play Store | ❌ Pas prêt (signature release + privacy/légal) |

## 3. Fonctionnalités

- 💬 Chat texte **streaming** + rendu **Markdown** maison (gras, italique, code inline, **blocs de code** monospace avec bouton copier, titres, listes) — robuste au streaming (markdown incomplet géré).
- 🖼️ **Image** : analyse d'une photo (picker `image/*`). Image copiée en stockage privé pour survivre au redémarrage.
- 🎙️ **Audio** : enregistrement **WAV PCM 16 kHz mono** (via `AudioRecord`) envoyé au modèle.
- 🗂️ **Conversations multiples** persistées (JSON par conversation dans `filesDir/conversations/`), tiroir : ouvrir / renommer / supprimer (avec confirmation). Restaurées au redémarrage.
- ⚙️ Bascule **backend** NPU → GPU → CPU avec **repli automatique** (NPU réservé Tensor G5).
- ⏹️ **Arrêt** de la génération (garde le texte partiel), sélection/copie des réponses, horodatage, UI responsive.
- 📥 **Modèle téléchargé au 1er lancement** (~2,4 Go, HF public, sans token), avec progression + reprise → APK léger.

## 4. Architecture

```
app/src/main/java/com/gaetan/gemmchat/
├─ llm/
│   ├─ LlmEngine.kt        init moteur, ordre des backends, conversation, config sampler, startNewConversation()
│   ├─ ChatRepository.kt   construit les Content (texte/image/audio), streaming Flow<String>
│   └─ EngineMode.kt       MULTIMODAL/TEXT_ONLY, BackendChoice, EngineStatus
├─ data/
│   ├─ ModelVariant.kt     Q4 (GPU/CPU) + PIXEL_TENSOR_G5 (NPU), downloadUrl (HF resolve)
│   ├─ ModelRepository.kt  fichiers modèle dans filesDir/models/, isModelReady()
│   ├─ ModelDownloader.kt  download HTTP resumable (Range) + ExtractProgress/InstallResult
│   ├─ ModelPreferences.kt variant sélectionné + lastActiveConversationId
│   ├─ ConversationStore.kt persistance JSON (suspend/IO), listSummaries/load/save/delete
│   └─ StoredConversation.kt  modèles sérialisés (kotlinx.serialization)
├─ audio/AudioRecorder.kt  enregistrement WAV (AudioRecord) + en-tête WAV manuel
├─ device/DeviceInfo.kt    détection SoC / génération Tensor (G3/G4/G5), supportsGemma4Npu
└─ ui/
    ├─ MainActivity.kt, AppViewModel.kt, AppUiState.kt
    ├─ ChatScreen.kt, LoadingScreen.kt, ConversationDrawer.kt
    ├─ components/GemmaComponents.kt, MarkdownText.kt
    └─ theme/ (GemmaColors, Type, Theme)
```

## 5. Décisions techniques NON évidentes (pièges)

- **Modèle téléchargé au runtime** (plus de bundling en chunks). HF `litert-community/gemma-4-E2B-it-litert-lm` est **public, non-gated** → download anonyme. `ModelDownloader` suit les redirections **manuellement** pour préserver l'en-tête `Range` (HttpURLConnection le perd sinon) → reprise OK.
- **Audio = WAV obligatoire.** LiteRT-LM décode via **miniaudio** = WAV/FLAC/MP3 uniquement, **pas AAC/M4A** (sinon « Failed to initialize miniaudio decoder, error code: -10 »). D'où `AudioRecord` + WAV, pas `MediaRecorder`.
- **Polices bundlées en TTF variables** (Manrope, JetBrains Mono) dans `res/font/`. Ne PAS utiliser les Google Fonts téléchargeables (GMS) → crash `bad base-64` au démarrage et contraire à l'offline.
- **Sampler GPU** : `libLiteRtTopKOpenClSampler.so` (git-LFS du repo google-ai-edge/LiteRT-LM, tag v0.13.1) **patché** `patchelf --add-needed libLiteRt.so` (sinon `dlopen` échoue sur `LiteRtCreateEnvironment`). Gain ~2× décodage. Récupérable via `scripts/fetch_gpu_sampler.sh`.
- **Insets** : `safeDrawingPadding()` partout (sinon barre de nav latérale en paysage / taskbar grand écran recouvrent le contenu).
- **Persistance hors thread UI** : `ConversationStore` suspend + `Dispatchers.IO` (évite ANR quand le nb de conversations grandit).
- **Speculative decoding** déjà actif (vrai draft MTP dans le bundle modèle).
- **Vidéo non supportée** par le modèle (texte/image/audio uniquement) — vérifié dans l'API litert-lm (pas de Content vidéo ni videoBackend).

## 6. Performance (Pixel 8 Pro, GPU)

- ~**13–18 tok/s** en décodage (après activation du sampler GPU ; ~8 tok/s avant).
- Plafond essentiellement borné par la bande passante mémoire (modèle 2B 4-bit). Gain x2-3 supplémentaire = NPU (G5) ou modèle plus léger.

## 7. Travaux réalisés (historique)

- **Features** : historique multi-conversations + tiroir + persistance.
- **Perf** : bundling sampler GPU OpenCL (patchelf) → ~2×.
- **Audit UX** → corrigé : rendu markdown, sélection/copie, bouton Stop, scroll conditionnel, clé de liste stable, confirmation suppression, cibles tactiles, contrastes WCAG AA, labels TalkBack, messages d'erreur, horodatage, disclaimer persistant.
- **Audit responsive** → corrigé : insets `safeDrawingPadding`, largeur de contenu max 760 dp centrée, accueil scrollable, boutons `heightIn`, tiroir adaptatif.
- **Audit QA (ISTQB)** → défauts corrigés : I/O hors thread UI (DEF-1), stop avant 1er token (DEF-2), tiroir se ferme à la suppression courante (DEF-3), image persistée localement (DEF-4), audio en filesDir (DEF-5), entrée plafonnée 8000 car. (DEF-6). 0 crash sur 2 campagnes.
- **Audio** : bug AAC/miniaudio → réécrit en WAV.
- **Distribution** : modèle sorti de l'APK (2,6 Go → 72 Mo), download runtime, repo + release mis à jour, licence Apache 2.0.

## 8. Build & run

```bash
# Prérequis : Android SDK (local.properties: sdk.dir=...), JDK 17, appareil arm64-v8a
./gradlew assembleDebug        # APK debug ~72 Mo
./gradlew installDebug         # installe sur l'appareil connecté

# Libs natives optionnelles (accélération), non versionnées :
./scripts/fetch_npu_libs.sh    # dispatch NPU Google Tensor (Pixel G5)
./scripts/fetch_gpu_sampler.sh # sampling top-K GPU (~2× décodage) — nécessite git-lfs + patchelf
```

Le modèle se télécharge tout seul au premier lancement (Wi-Fi conseillé, ~2,4 Go).
Scripts legacy de bundling encore présents (`download_model.sh`, `split_model.sh`, `build_bundled_apk.sh`) — plus utilisés par le build.

## 9. Limites connues / risque résiduel

- **APK debug-signed** (sideload OK ; signature release à configurer pour Play Store).
- **Pas de tests automatisés** (régression manuelle uniquement).
- Robustesse mémoire sur appareils bas de gamme non testée (modèle 2,4 Go + largeHeap).
- Pas de **PRIVACY.md** / politique de confidentialité (requise Play Store, permissions micro/photos).
- Image `content://` : persistée localement à l'envoi (sinon non ré-affichable après redémarrage).

## 10. Prochaines étapes possibles

1. **Signature release** (`signingConfig` + `assembleRelease`) → APK distribuable proprement.
2. **PRIVACY.md** + conformité licences (Gemma Terms of Use).
3. **Tests instrumentés** (Compose UI tests) sur persistance / génération.
4. Sélecteur de variante Q4 ↔ G5 (NPU) avec download dédié.
5. Tableaux / liens markdown, écran réglages, export de conversations.
