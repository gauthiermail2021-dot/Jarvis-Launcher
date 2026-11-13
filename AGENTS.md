# Jarvis — exigences qualité pour toute modification

## Dépôt et application

- **Seul dépôt de développement :** `gauthiermail2021-dot/Jarvis-Launcher`.
- **Application Android :** `ca.jarvis.bridge` (Jarvis Bridge). L'identifiant Android n'est **pas** remplacé par le nom du dépôt.
- Le dépôt `gauthiermail2021-dot/Jarvis-Bridge-Android` est une archive GitHub en lecture seule ; ne jamais y compiler ni copier son ancien code comme base courante.
- Le lanceur Web `index.html` est préservé ; il n'est pas l'application Android.

## Référence de qualité au 2026-11-13

- Branche `main` : **1.2.69**, `versionCode 88`, référence fonctionnelle **testée sur Samsung** pour Gmail/SVG, Conception, Dégrouper. **Calques reste non automatique.**
- Branche `fix/audit-stability-1.2.70` : nettoyage ressources d'accessibilité + modèle OCR épinglé, tests/compilation/signature réussis, **pas encore confirmée sur téléphone**.
- Ces indications sont des instantanés : toujours vérifier version, commit, branches, résultats CI et tests réels avant de prendre une décision.

## Procédure obligatoire

1. Vérifier `main` et les branches de maintenance ; lire `README.md`, `docs/SOURCE_OF_TRUTH.md`, `android/app/build.gradle` et les services affectés.
2. Isoler les corrections sur une branche. Faire une PR avec liste des fichiers touchés et un changement de comportement clairement identifié.
3. Ne modifier ni signature release, `applicationId`, ni scopes Gmail sans nécessité et tests de migration documentés. Ne publier aucun secret.
4. Exécuter les tests JVM, compiler `assembleRelease`, vérifier `apksigner` et l'APK produit. Prévoir des cas de régression pour SVG Gmail, SVG bulle, session xTool, EasySet, notifications et animations.
5. La réussite de `dispatchGesture()` ou `ACTION_CLICK` n'établit **pas** que le dégroupement ou le panneau Calques a effectivement changé d'état. Afficher « non confirmé » si aucun état probant n'est disponible.
6. Les gestes automatiques doivent cibler uniquement des contrôles xTool identifiés, autorisés et visibles ; pas de positions devinées quand la fenêtre est incertaine. Une action ne doit pas être répétée à cause d'un événement Android.
7. Aucun déclenchement laser ni changement de paramètres machine. OCR EasySet en lecture seule, captures seulement avec autorisation Android.
8. Tester sur Samsung avant toute déclaration « stable » ou promotion d'une nouvelle version dans `main` ; conserver sauvegardes et possibilité de retour arrière.

La CI de `main` produit des **artefacts de contrôle**, non une publication automatique.
