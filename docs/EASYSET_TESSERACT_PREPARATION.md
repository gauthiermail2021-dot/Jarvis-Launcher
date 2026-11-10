# Tesseract 5 — travail préparatoire vers Jarvis 1.2.32

**BRANCHE EXPÉRIMENTALE NON INSTALLABLE EN VERSION OFFICIELLE.**

- Basée sur `main` du dépôt **Jarvis-Launcher** après la migration canonique du 10 novembre 2026.
- Base Android **1.2.31** inchangée : `versionCode 50`, package `ca.jarvis.bridge`. La prochaine vraie version 1.2.32 devra avoir un `versionCode` supérieur à 50.
- `EasySetPassesTesseract.java` a été récupéré de `Jarvis-Bridge-Android/feature/easyset-tesseract-staging` **sans reprendre le reste de la base obsolète 0.7**.
- Le module Tesseract est ajouté à la compilation mais **n'est volontairement pas encore raccordé au diagnostic EasySet**. Un APK compilé ne prouve donc pas que la lecture du chiffre est corrigée.
- Le build spécial `build-tesseract-staging.yml` compile uniquement la variante **debug** `ca.jarvis.bridge.dev`. Il ne publie aucun APK, n'utilise pas de clé privée release et ne modifie pas l'app installée.

## Étapes restantes pour rendre une future 1.2.32 réelle

1. Cadrer avec précision la case **numérique** à droite du libellé `Passer` avec la géométrie de la capture originale. L'ancienne classe `EasySetPassesRegion` renvoie actuellement **toute la ligne**, ce qui est trop large pour le module de lecture chiffre seul.
2. Brancher `EasySetPassesTesseract.read` en repli sur l'algorithme réel `EasySetVisionAnalyzer` uniquement lorsque « Passages : non lus » ; la lecture doit s'exécuter hors du fil d'interface, tout en conservant le bitmap le temps nécessaire et en recyclant le résultat.
3. Ne jamais accepter un nombre absent, hors zone ou contradictoire avec l'observation ML Kit, et ne jamais forcer `1`.
4. Ajouter tests sur des captures d'écran xTool représentatives, en conservant le caractère éphémère des informations OCR.
5. Tests de non-régression : Gmail → SVG → xTool, bulle, sons, sessions de capture et signature release. Aucun démarrage laser.
6. Quand tout est validé : `versionCode 51`, `versionName '1.2.32'`, compilation release signée, conservation durable de l'APK et des notes de test.

## Source de vérité
`main` du dépôt **Jarvis-Launcher**. Aucune modification fonctionnelle n'a été faite sur l'ancienne base 0.7 durant cette migration du module.
