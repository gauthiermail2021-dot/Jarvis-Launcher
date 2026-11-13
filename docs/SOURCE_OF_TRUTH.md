# Jarvis — source officielle et protocole de validation

État établi : **13 novembre 2026**.

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
| [`fix/audit-stability-1.2.70`](../../tree/fix/audit-stability-1.2.70) | 1.2.70, `versionCode 89` | Ressources d'accessibilité nettoyées, modèle OCR épinglé. Build + tests JVM + signature validés. **Test téléphone non confirmé** |
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
