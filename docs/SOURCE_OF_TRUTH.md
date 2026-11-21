# Jarvis — source officielle et protocole de validation

## Proposition 1.2.71 — désélection avant Calques (non validée)

- Branche candidate : `feature/deselect-before-layers-1.2.71`, issue de `fix/audit-stability-1.2.70-clean` (version installée/testée par l'utilisateur : 1.2.70).
- [PR #45 — désélection autonome du canevas avant Calques](https://github.com/gauthiermail2021-dot/Jarvis-Launcher/pull/45) : correction ciblée, brouillon en attente des tests GitHub Actions et d'une validation sur Samsung.
- Comportement ajouté : un toucher distinct, borné et unique sur une zone de canevas contrôlée, après fermeture du menu Dégrouper, avant le toucher Calques. Si la zone sûre n'est pas identifiable, la séquence s'arrête sans réglages ni commandes machine.
- Versions de la candidate : `versionName 1.2.71`, `versionCode 90` ; ne pas déclarer stable sans APK signé, tests et vérification visuelle que **les formes sont effectivement désélectionnées**.
- Étapes non implémentées à ce stade : sélectionner Rouge, fermer Calques, ouvrir Paramètres, ouvrir EasySet ; aucune écriture de Puissance/Vitesse/Passes ni lancement laser.

## Retour de test 1.2.70 sur Samsung — 21 novembre 2026

L'utilisateur précise que son essai récent de Jarvis est effectué avec **la version 1.2.70**, non 1.2.69. Résultat observé : **Dégrouper fonctionne mais l'ensemble des formes demeure sélectionné**. La prochaine correction doit partir du code **1.2.70** (`fix/audit-stability-1.2.70-clean`, `versionCode 89`), sans revenir à 1.2.69 ni perdre les corrections de maintenance. Le test utilisateur ne confirme pas encore que toutes les autres étapes xTool sont validées sous 1.2.70.

**Workflow à développer et vérifier dans cet ordre :** après Dégrouper, désélectionner les formes; ouvrir Calques; sélectionner Rouge; fermer Calques; ouvrir l'onglet Paramètres; ouvrir Volet EasySet. Les valeurs Puissance, Vitesse et Passes ne sont pour l'instant qu'à lire/vérifier : pas de modification automatique ni de mise en marche du laser.

## Mise à jour du 21 novembre 2026 — sélection des calques

**Séquence xTool confirmée par l'utilisateur le 21 novembre 2026 (ordre exact) :**

1. Désélectionner toutes les formes après le dégroupement.
2. Ouvrir « Calques ».
3. Choisir une couleur, par exemple « Rouge ».
4. **Fermer « Calques »** en gardant le calque choisi.
5. Aller dans l'onglet « Paramètres ».
6. Ouvrir le « Volet EasySet ».
7. Accéder aux champs **Puissance**, **Vitesse** et **Passes** pour le calque choisi ; la validation/saisie des paramètres de traitement et le lancement du laser restent sous contrôle humain.

**État :** workflow décrit par l'utilisateur, pas encore implémenté ni vérifié par test automatique. Ne pas sauter l'étape de fermeture du panneau Calques. L'OCR EasySet existant reste en lecture seule tant que l'évolution des règles de sécurité et sa validation n'ont pas été examinées.

**Constat de test communiqué par l'utilisateur :** après « Dégrouper » dans xTool Creative Space, toutes les formes restent sélectionnées. Le dégroupement fonctionne; cette sélection globale empêche de cibler sans ambiguïté les paramètres d'une seule couleur.

**Séquence de prochaine implémentation (non validée / non codée à ce stade) :** désélectionner les formes par une zone vide sûre du canevas; afficher Calques; sélectionner une couleur précise (ex. rouge); ouvrir « Paramètres » pour cette couleur; permettre la modification des valeurs de cette couleur et confirmer l'effet à l'écran. Répéter la sélection de couleur seulement pour les calques présents. Éviter toute modification de tous les objets par erreur.

**Critères de validation Android :** vérifier après le clic de désélection que la sélection globale est réellement annulée; vérifier que le panneau Calques est visible; confirmer que seule la couleur visée est active avant d'accéder à ses paramètres; vérifier la prise en compte des réglages et l'absence de clics multiples. Ne pas considérer qu'un `dispatchGesture()` réussi confirme un changement d'état. Les paramètres de traitement et le lancement du laser restent sous contrôle humain.

État établi : **13 novembre 2026**.

## Point de reprise interconversations — 21 novembre 2026

**Dernier état communiqué par l'utilisateur (à distinguer de la validation du code sur `main`)** : le parcours Jarvis Bridge → Gmail/SVG → xTool → Conception → Dégrouper → panneau **Calques** est atteint. Ne pas lui demander de réexpliquer ces étapes déjà couvertes.

**Étape suivante demandée** : depuis le panneau Calques, ouvrir **Paramètres** pour le calque concerné, puis prévoir l'association **couleur du calque → paramètres correspondants**. Cette association est un objectif à concevoir et vérifier ; elle n'est **pas attestée comme implémentée ou fonctionnelle** dans l'APK. Aucun barème de valeurs ni aucune couleur précise n'a été communiqué dans ce point de reprise : ne pas les inventer.

**Restrictions inchangées** : le lancement du laser et ses paramètres de traitement restent sous contrôle humain ; aucune action automatique de démarrage, de gravure, de découpe ou de réglage machine. Ne pas confondre accès à un panneau, clic réussi et réglages réellement appliqués. Toute évolution de l'interface doit être testée sur Samsung et garder une preuve de résultat avant validation.

**Continuité** : avant chaque reprise après limite de conversation, consulter d'abord ce fichier, `README.md`, `AGENTS.md`, les branches/commits actuels et la dernière validation utilisateur. Le dépôt GitHub constitue la référence durable ; les comptes rendus de chat peuvent être incomplets.

## Dépôt unique et application stable

- **Dépôt officiel :** `gauthiermail2021-dot/Jarvis-Launcher`.
- **Nom de l'application :** Jarvis Bridge ; `applicationId` **`ca.jarvis.bridge`**. Ne pas changer l'identité pour renommer le projet.
- **Lanceur Web :** `index.html`, conservé séparément de l'application Android.
- **Ancien dépôt :** `gauthiermail2021-dot/Jarvis-Bridge-Android`, désormais **archivé** ; aucun code, workflow ou APK n'y fait foi pour une version actuelle.

## Branche de référence et versions

| Référence | Version | Statut |
| --- | --- | --- |
| **`main`** | **1.2.69**, `versionCode 88` | **Référence fonctionnelle testée sur Samsung** ; Gmail/SVG, Conception et Dégrouper fonctionnent ; Calques reste à corriger |
| [`fix/layers-dismiss-after-ungroup-1.2.69`](../../tree/fix/layers-dismiss-after-ungroup-1.2.69) | 1.2.69 | Branche historique précise du code promu vers `main`, à conserver comme sauvegarde |
| [`fix/audit-stability-1.2.70-clean`](../../tree/fix/audit-stability-1.2.70-clean) | 1.2.70, `versionCode 89` | Ressources d'accessibilité nettoyées, modèle OCR épinglé. Tests JVM, compilation et signature réussis sur la branche propre. **Test téléphone non confirmé** |
| `snapshot/jarvis-1.2.31-build-verified` | 1.2.31 | Archive historique de restauration, non utilisable comme nouvelle base |

La branche `main` conserve l'histoire du lanceur et des versions précédentes. La source Android actuelle a été remplacée intégralement par l'arbre exact de 1.2.69, sans copier d'anciennes classes isolées.

## Contrôles indispensables

1. Vérifier branche, SHA, `versionName`, `versionCode` et tâches CI avant la compilation.
2. Brancher les corrections futures depuis la dernière base **réellement approuvée sur Samsung** ; comparer la branche de maintenance 1.2.70 et sélectionner uniquement ses changements testés.
3. Maintenir les tests pour la validation SVG, les recherches Gmail, la protection anti-doublons, la session xTool, la bulle, les permissions et l'OCR.
4. Exécuter `testReleaseUnitTest assembleRelease`, confirmer les métadonnées d'APK, le certificat de signature et la présence des assets OCR hors ligne.
5. Avant d'appeler un APK « stable », le tester sur appareil réel : SVG Gmail, SVG depuis la bulle, Conception, Dégrouper, Calques, notification unique, bulles, sons et EasySet.
6. Les commandes du laser et ses paramètres sont exclusivement sous contrôle humain.

## Publication et retour arrière

- Toute nouveauté arrive sur une branche, en PR, avec un diff vérifié. Ne pas fusionner des fonctionnalités non vérifiées vers `main`.
- `.github/workflows/build-jarvis-android.yml` vérifie l'APK sur `main` sans le publier comme mise à jour officielle.
- Les artefacts GitHub Actions sont **expirables**. Pour une vraie release : conserver le SHA du commit, un APK signé durable, le résultat des tests, le hash du certificat public et une procédure de restauration.
- Ne jamais enregistrer de keystore, mot de passe ou jeton OAuth dans GitHub.
- Les branches historiques et les anciennes versions restent dans l'historique Git ; aucun effacement de données locales Android ne doit être nécessaire pour consolider la branche.
