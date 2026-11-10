# Source officielle et protection contre les régressions — Jarvis

Date de réorganisation : **2026-11-10**.

## 1. Dépôts, sans ambiguïté

| Dépôt | Fonction | Ce qu'il ne faut pas faire |
| --- | --- | --- |
| **Jarvis-Launcher** → `main` | **Seule source canonique du Jarvis Android actuel** (base 1.2.31 ; l'index.html du lanceur Web est préservé) | Ne pas travailler sur un ancien prototype comme base d'une nouvelle version |
| **Jarvis-Bridge-Android** | Archive historique v0.7, puis essais Tesseract non intégrés | Ne plus compiler/distribuer d'APK à partir de ce dépôt |
| `snapshot/jarvis-1.2.31-build-verified` | Instantané immuable par convention du commit source qui a passé la CI | Ne pas déplacer ni réécrire cette référence |
| `archive/pre-canonical-main-2026-11-10` | Ancien main du lanceur Web avant réorganisation | Ne pas supprimer |
| `feature/jarvis-rollback-stable-1.2.7` | Ancienne branche ayant servi au retour au fonctionnement fiable | Ne pas supprimer |
| `experiment/easyset-diagnostic-1.2.31` | Code original du test diagnostique 1.2.31 | Ne pas réécrire ; snapshot conservé |

## 2. État actuel exact

- Base Android **1.2.31**, `versionCode 50`, package `ca.jarvis.bridge`, `minSdk 26`, `targetSdk 28`.
- Compilation signée 1.2.31 et tests automatisés réussis dans **GitHub Actions #42800834402**, commit `6c7a3fd3767e134cefb2d2598895d963edc78810`.
- L'APK de ce workflow est un artefact GitHub Actions **expirable**, pas une version archivée à vie.
- ML Kit lit des paramètres EasySet mais la valeur du champ **Passer** reste non lue dans un test réel : **ne pas inventer la valeur 1**.
- 1.2.32 = version **future**, pas livrée ; toute compilation expérimentale sur une nouvelle branche ne constitue pas la release officielle.

## 3. Démarrage obligatoire de chaque tâche

1. Lister les dépôts accessibles. Vérifier que l'on travaille dans **Jarvis-Launcher**.
2. Inspecter `main`, ses derniers commits et `android/app/build.gradle` pour l'identité et la version.
3. Lire l'implémentation actuelle, les services Android, le manifeste, les tests et le workflow.
4. Créer une branche d'essai à partir de **main**, à SHA précisé. Ne jamais remplacer `main` par un ancien code.
5. Pour un changement OCR, revoir en particulier `EasySetVisionAnalyzer.java`, `EasySetPassesDiagnostic.java`, `EasySetPassesRegion.java`, `EasySetValues.java`, `EasySetSessionService.java`.
6. Soumettre les changements sous forme de PR ; vérifier la différence de fichiers, les tests et le workflow.
7. Identifier honnêtement les limites testées/les limites non testées : **compilation OK ≠ OCR fiable sur le téléphone**.

## 4. Contrôles avant publication d'un APK

- Code actuel de la bonne branche + versionCode supérieur au dernier code installé.
- Signature Android de **release** réutilisant le certificat officiel.
- OAuth Gmail et libellé **Jarvis SVG** non modifiés sans nécessité.
- Régression Gmail → SVG → xTool, bulle flottante, notifications, sons, session EasySet et sécurité SVG.
- Captures écran seulement après consentement Android ; OCR passif. Aucune modification des paramètres laser, aucun déclenchement du laser.
- Nouveau test OCR sur **capture xTool réelle**, comparé aux valeurs visibles ; échecs = « non lus » plutôt que chiffres présumés.
- Archiver durablement le **code exact**, **APK signé**, résultats des tests, fichier CHANGELOG, SHA de signature **public** et notes de restauration ; conserver la **clé privée ailleurs**, jamais dans le repo.

## 5. Numérotation et statut

- 1.2.31 / versionCode **50** : référence pour poursuivre le développement, diagnostics EasySet **expérimentaux**.
- 1.2.32 / versionCode **51** : prochaine release **possible**, après intégration et vérifications. Ne pas numéroter d'APK 1.2.32 tant que cela n'est pas prêt.
- Le succès des tests GitHub ne signifie ni validation réelle, ni installation réussie.
- Le prototype 0.7 n'est jamais une base de restauration de la série 1.2.x.

## 6. Sauvegardes et restauration

Ne jamais supprimer les branches historiques sans sauvegarde confirmée hors GitHub. Les ZIP privés contenant des clés doivent rester **hors du dépôt public**. Les artefacts Actions expirent ; archiver un APK officiel sous une Release GitHub ou une autre sauvegarde pérenne. En cas de panne, restaurer depuis un snapshot connu et vérifier le certificat, sans réinitialiser silencieusement les données locales ni supprimer l'application utilisateur.

## 7. Journal des erreurs corrigées

- **2026-11-10** : découverte que le vrai code Android 1.2.31 est stocké dans *Jarvis-Launcher* sur la branche `experiment/easyset-diagnostic-1.2.31`, et non dans *Jarvis-Bridge-Android*.
- **2026-11-10** : `main` de *Jarvis-Launcher* avancé sans réécrire l'historique à partir de l'ancien `main` vers le commit confirmé de 1.2.31.
- **2026-11-10** : préservation de l'ancien `main` et d'un snapshot exact 1.2.31.
- Le prototype historique et les essais Tesseract de l'ancien dépôt ne doivent pas être fusionnés dans `main` par inadvertance.

## 8. Limites et vérifications humaines restantes

La présence de code et la réussite CI ne prouvent pas que la reconnaissance du petit chiffre de « Passer » fonctionne sur le Samsung. Il faut la capture et le test d'intégration. Le responsable de l'app vérifie la version installée et le comportement réel après livraison ; aucune commande de découpe n'est automatisée.
