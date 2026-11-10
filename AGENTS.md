# Instructions de développement — Jarvis (SOURCE CANONIQUE)

IMPORTANT : ce dépôt **Jarvis-Launcher**, branche **main**, est la **seule base** du Jarvis Android actuel. Au 2026-11-10, `main` contient Jarvis 1.2.31, versionCode 50, package `ca.jarvis.bridge`. Le dépôt **Jarvis-Bridge-Android** est une **archive 0.7 obsolète** et ne doit jamais servir de base à une version Jarvis moderne.

## Pré-vol avant tout changement

- Lire le README racine, `docs/SOURCE_OF_TRUTH.md` et `android/app/build.gradle`.
- Vérifier la branche et le SHA GitHub courant ; ne pas inférer la version à partir du nom du dépôt ou de l'APK.
- Comparer la dernière branche connue, les tests et les workflows avant de créer une branche basée sur `main`.
- Travailler sur une branche `feature/` ou `experiment/` depuis `main`, puis ouvrir une PR. Ne pas fusionner sans vérification.
- Préserver toutes les fonctionnalités cumulées (Gmail → SVG → xTool, notification, bulle, sons, session de capture, validation SVG, OAuth) ; éviter une régression par code partiel.
- Ne pas modifier le package Android, la signature release permanente, les scopes Gmail ou `targetSdk` sans nécessité documentée.
- Ne jamais inclure de clé privée, jeton OAuth ni mot de passe dans le dépôt, les logs ou les réponses.
- Pour EasySet : OCR de **lecture seule**, valeurs non lues si ambiguës. Aucun réglage xTool modifié et **aucune commande laser**.
- Faire tests JVM + compilation de la bonne variante + signature release + essai réel de l'APK avant d'annoncer que la fonctionnalité fonctionne.
- Ne pas diffuser de ZIP technique quand l'utilisateur demande un APK ; ne pas nommer un artefact de staging « nouvelle version installable ».
- Préserver et citer le commit, `versionCode`, lien du build, APK signé durable et procédure de retour arrière.
- La compilation d'un outil Tesseract expérimental **sans intégration à EasySet** n'est pas une correction OCR livrée.

## Historique protégé

- Base source 1.2.31 : `snapshot/jarvis-1.2.31-build-verified` à `6c7a3fd3767e134cefb2d2598895d963edc78810`.
- Ancien main du lanceur Web : `archive/pre-canonical-main-2026-11-10`.
- Essai OCR sur la vraie base : `experiment/easyset-tesseract-1.2.32-preparation` (ne pas fusionner avant validation).
