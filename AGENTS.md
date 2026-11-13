# Instructions pour toute intervention dans Jarvis

## Source unique et identité

- Dépôt **unique et officiel** : `gauthiermail2021-dot/Jarvis-Launcher`.
- Application Android : **Jarvis Bridge** ; son `applicationId` reste **`ca.jarvis.bridge`**. Ne pas le renommer sans migration Android explicite.
- L'autre dépôt, `gauthiermail2021-dot/Jarvis-Bridge-Android`, est un **prototype historique interdit de build**. Ne jamais y créer ni y chercher la version actuelle de l'APK.
- Le nom `Jarvis-Launcher` désigne le **dépôt GitHub** et le lanceur Web associé, pas une autre application Android.

## État au 13 novembre 2026 : vérifier avant toute modification

- `main` : source Android **1.2.31**, donc **non actuelle**, conservée en attendant la consolidation.
- `fix/layers-dismiss-after-ungroup-1.2.69` : dernière base **fonctionnellement testée** sur le Samsung. Conception et Dégrouper OK ; Calques reste inachevé.
- `fix/audit-stability-1.2.70` : maintenance basée sur 1.2.69, CI réussie ; **non validée sur téléphone** au moment de cette note.
- Les versions et statuts sont des instantanés, pas des vérités permanentes. Vérifier **branche, SHA, `versionName`, `versionCode`, résultats CI et test téléphone** avant d'agir.

## Procédure obligatoire

1. Consulter `README.md`, `docs/SOURCE_OF_TRUTH.md`, le `build.gradle` et le commit de la **branche de travail**, pas seulement de `main`.
2. Pour une évolution du comportement, créer une branche depuis la dernière base **confirmée sur l'appareil**. Ne jamais redémarrer depuis `main` tant qu'il demeure sur 1.2.31. Les ajustements documentaires peuvent être proposés séparément sur `main`.
3. Préserver les fonctions Gmail → SVG → xTool, la bulle, les animations et sons, la validation SVG et EasySet. Toute régression exige un retour arrière facile.
4. Ne pas toucher à la signature permanente, aux secrets GitHub ou au client OAuth sans nécessité validée. Ne publier aucun secret.
5. Les gestes d'interface (Conception, Dégrouper, Calques) ne prouvent pas à eux seuls que xTool a appliqué l'action. Donner un état réel ou « non confirmé ».
6. Ne pas automatiser le déclenchement du laser ni ses paramètres. OCR EasySet en lecture seule.
7. Ouvrir une PR, comparer les fichiers, exécuter tests + build release signé, vérifier `apksigner`, puis faire un **essai sur Samsung** avant de déclarer une version stable.
8. Ne jamais supprimer l'ancien historique ou les branches de restauration sans sauvegarde. Un APK GitHub Actions peut expirer.

Voir `docs/SOURCE_OF_TRUTH.md` pour l'état détaillé et les références de récupération.
