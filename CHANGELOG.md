# Changelog

## v1.0.0

Première version publique de **Gemma 4 E2B — Chat Android hors-ligne**.

### Parcours complet
- Onboarding : **compatibilité** (scan RAM / SoC / stockage / NPU), **choix du modèle**,
  **acceptation de la licence Gemma**, **téléchargement** résumable (pause / reprise auto)
  avec **vérification d'intégrité SHA-256**.
- **Hub** : lanceur multimodal — Discuter, **Analyser** (ouvre la galerie en 1 tap),
  **Transcrire** (démarre l'enregistrement en 1 tap), **Skills** — + discussions récentes.
- **Chat** : streaming live, **télémétrie** (tok/s · °C · batterie), **rendu markdown**,
  actions **Copier / Régénérer / Partager**, menu ⋮.
- **Paramètres** (backend, Wi-Fi, données, à propos) et **Historique** complet
  (ouvrir / supprimer / effacer tout) avec **persistance** des discussions.

### Modèle
- **Téléchargé au 1er lancement** depuis l'URL Hugging Face publique, ou **importé** depuis un
  fichier local, ou poussé via `adb` (auto-détecté). APK léger, modèle jamais embarqué.
- URL **configurable** au build (`-PMODEL_URL_OVERRIDE`).

### Matériel
- **Universel** : modèle Q4 par défaut sur tout appareil arm64 ; détection multi-vendeur
  (Qualcomm / MediaTek / Exynos / Google Tensor). NPU sur Tensor G5, repli GPU/CPU sinon.

### Skills
- Presets de prompt : Résumé, Traduction, Correction, Code, Brainstorm, Explication simple.

### Qualité / production
- Build release **R8** (minification + `shrinkResources`), **signé** (secrets hors dépôt).
- **Crash logger** local, **garde-fou stockage**, **URI image persistable**.
- **Tests unitaires** JVM.

### Vérifié sur appareil
- Pixel (Google Tensor G3) : parcours complet, inférence **hors-ligne** (mode avion),
  build R8 minifié fonctionnel — 0 crash.
