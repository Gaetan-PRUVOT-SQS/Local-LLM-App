# Politique de confidentialité

**Application : Gemma Chat (Local-LLM-App)** · Éditeur : SigmaQuantSystems
Dernière mise à jour : 2026-06-23

## En résumé

Cette application exécute le modèle d'IA **entièrement sur votre appareil**. Vos
conversations, images et enregistrements audio **ne sont jamais transmis** à l'éditeur ni à
un tiers. Il n'y a **ni compte, ni publicité, ni traceur, ni analytics**.

## Données traitées et où elles restent

| Donnée | Traitement | Stockage |
|--------|-----------|----------|
| Messages texte | Inférence **locale** (LiteRT-LM) | Appareil uniquement |
| Images jointes | Analyse **locale** | Appareil uniquement |
| Audio enregistré | Transcription **locale** ; fichier temporaire | Appareil uniquement |
| Historique des discussions | Sauvegarde **locale** (JSON dans le stockage privé de l'app) | Appareil uniquement |
| Préférences (backend, Wi-Fi, licence) | Local | Appareil uniquement |
| Journal de crash | Local (`last_crash.txt`), non transmis | Appareil uniquement |

Le stockage interne de l'application est **privé (bac à sable Android)** et **chiffré au
niveau du système** par Android (chiffrement basé sur fichiers).

## Connexions réseau

La seule connexion réseau a lieu **au premier lancement**, pour **télécharger le fichier
modèle** (~2,4 Go) depuis Hugging Face (ou un miroir configuré). Cette requête est un
simple téléchargement : **aucune donnée personnelle n'est envoyée**. Après installation du
modèle, l'application fonctionne **hors-ligne** (l'inférence ne requiert aucun réseau).

Vous pouvez aussi fournir le fichier modèle **localement** (import / `adb`), sans aucune
connexion.

## Permissions

- **Internet / état réseau** — uniquement le téléchargement du modèle au 1er lancement.
- **Micro** — enregistrement vocal (à votre demande), traité localement.
- **Photos / médias** — sélection d'images/audio que vous joignez, traités localement.

## Vos droits (RGPD)

Comme aucune donnée n'est collectée ni transmise à l'éditeur, il n'y a pas de traitement
côté serveur. Vous gardez le contrôle total : **« Effacer les discussions »** et **« Vider
le cache modèle »** (dans Paramètres) suppriment vos données ; la désinstallation efface
tout.

## Contact

SigmaQuantSystems — Responsable : Gaetan PRUVOT — <sigmaquantsystems@gmail.com>.

---

*Gemma est une marque de Google LLC ; cette application n'est pas affiliée à Google.*
