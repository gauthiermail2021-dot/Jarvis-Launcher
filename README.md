# JARVIS — projet officiel

> **Un seul dépôt pour le développement Android : [Jarvis-Launcher](https://github.com/gauthiermail2021-dot/Jarvis-Launcher).**
> Le logiciel installé sur le téléphone s'appelle **Jarvis Bridge** et conserve volontairement l'identifiant Android `ca.jarvis.bridge`. Le nom du dépôt et celui de l'application sont différents, mais il ne s'agit pas de deux applications concurrentes.

## État vérifié le 13 novembre 2026

| Élément | Référence | État |
| --- | --- | --- |
| Dépôt officiel | `Jarvis-Launcher` | **Seul emplacement pour les nouvelles modifications** |
| Base fonctionnelle testée sur Samsung | [`fix/layers-dismiss-after-ungroup-1.2.69`](../../tree/fix/layers-dismiss-after-ungroup-1.2.69) | **1.2.69**, versionCode **88** ; SVG, Conception et Dégrouper fonctionnent ; **Calques non automatique** |
| Maintenance issue de cette base | [`fix/audit-stability-1.2.70`](../../tree/fix/audit-stability-1.2.70) | **1.2.70**, versionCode **89** ; nettoyage des ressources d'accessibilité ; compilation/test/signature réussis, **essai sur téléphone non confirmé** |
| Branche GitHub par défaut | `main` | **Code Android 1.2.31, versionCode 50 : EN RETARD**, à ne pas choisir pour construire une nouvelle version actuelle |
| Ancien dépôt | [`Jarvis-Bridge-Android`](https://github.com/gauthiermail2021-dot/Jarvis-Bridge-Android) | **Historique seulement. Ne pas y modifier ou compiler Jarvis.** |

**Attention :** `main` désigne encore la branche par défaut de GitHub ; cela ne garantit pas qu'elle contienne le dernier code Android. En attendant une intégration explicitement validée, toujours utiliser le nom exact de la branche fonctionnelle, son commit et sa version. Ne pas fusionner aveuglément un ancien `main` vers une branche récente.

## Règles pour chaque développement

1. Travailler **uniquement dans ce dépôt**. Lire `docs/SOURCE_OF_TRUTH.md` et `android/app/build.gradle` sur **la branche ciblée**.
2. Partir de la dernière version **validée sur le Samsung** pour toute modification fonctionnelle. La base reconnue est actuellement **1.2.69** ; les corrections 1.2.70 peuvent servir de base de maintenance après revue.
3. Conserver `applicationId 'ca.jarvis.bridge'`, le certificat de signature release et les autorisations Google Gmail ; ne pas créer une deuxième application pour résoudre une confusion de noms.
4. Ne pas réactiver les workflows ni reconstruire les anciennes versions de l'autre dépôt. Leurs fichiers subsistent uniquement pour la traçabilité.
5. Exécuter les tests, compiler la bonne variante, vérifier la signature et **faire confirmer le comportement sur le téléphone** avant d'appeler une version « stable ».
6. Préserver Gmail → SVG → xTool, la bulle, les sons, la validation SVG, les sessions et l'OCR. Aucun départ automatique du laser.

## Organisation

- `android/` : application **Jarvis Bridge** pour Android.
- `android/app/build.gradle` : nom et numéro de version de la branche.
- `android/app/src/test/` : tests JVM.
- `.github/workflows/` : anciens et nouveaux workflows **spécifiques à leur branche/version**. Utiliser seulement le workflow correspondant à la branche en cours.
- `index.html` : lanceur Web historique, **distinct** de l'APK Android.
- `docs/SOURCE_OF_TRUTH.md` : politique de branchement, sauvegarde et validation.

## Historique

Le développement avait commencé dans un autre dépôt avec un prototype **0.7**, désormais abandonné pour tout développement. Le détail de la série 1.2.31 et des versions plus anciennes est conservé dans l'historique Git, sans l'afficher comme l'état actuel du projet.
