# Jarvis — source unique et protection contre les régressions

Mise à jour : **13 novembre 2026**. Ce document remplace les indications devenues fausses qui désignaient systématiquement `main` 1.2.31 comme la dernière version de Jarvis.

## Identité : dépôt, application, versions

- **Dépôt Android officiel :** `gauthiermail2021-dot/Jarvis-Launcher` — et **aucun autre**.
- **Application installée :** Jarvis Bridge, identifiant Android **`ca.jarvis.bridge`**. Le changement de nom du dépôt ne nécessite pas de changer l'application, ses données ou son certificat.
- **Lanceur Web :** `index.html` dans le dépôt officiel, indépendant de l'APK.
- **Dépôt précédent :** `gauthiermail2021-dot/Jarvis-Bridge-Android`, réservé à l'histoire du projet. Sa CI ne produit aucun APK ; il ne doit jamais redevenir une source de développement.

## Référence précise et état de validation

| Branche / référence | Code | Preuve et limites |
| --- | --- | --- |
| `main` | 1.2.31, versionCode 50 | Branche GitHub par défaut **en retard** ; non utilisable comme point de départ pour une évolution moderne |
| `fix/layers-dismiss-after-ungroup-1.2.69` | 1.2.69, versionCode 88 | Baseline testée sur le Samsung : SVG, Conception et Dégrouper fonctionnent ; **Calques pas encore automatique** |
| `fix/audit-stability-1.2.70` | 1.2.70, versionCode 89 | Basée sur 1.2.69 ; libération d'objets d'accessibilité et workflow OCR épinglé ; CI/test/signature OK ; **validation téléphone à faire** |
| `snapshot/jarvis-1.2.31-build-verified` | 1.2.31 historique | Préserver pour restauration ancienne, **ne pas utiliser pour la prochaine mise à jour** |
| `feature/jarvis-rollback-stable-1.2.7` | 1.2.7 historique | Branche de retour arrière antérieure, ne pas supprimer |

Une compilation réussie, même signée, n'est **pas** une confirmation du dégroupement, des calques ni d'EasySet sur le téléphone.

## Développement — choix de la base

1. Chercher uniquement dans **Jarvis-Launcher**. Identifier **branche, SHA et version** avant toute lecture ou modification de code.
2. Pour corriger la fonction sur le téléphone : repartir de la dernière **base validée sur le Samsung** (actuellement 1.2.69). Conserver et examiner les changements isolés de maintenance 1.2.70 avant d'y ajouter d'autres fonctions.
3. Pour les documents de gouvernance : possibilité de corriger `main` sans mettre à jour son code Android, en indiquant honnêtement que sa version reste 1.2.31.
4. Ne pas copier de fichiers de l'archive vers l'app Android actuelle. Ne pas supposer que `main` est plus récent qu'une branche nommée.
5. Réaliser les changements sur une branche dédiée, relire le diff, ouvrir une PR ; ne fusionner la vraie version fonctionnelle vers `main` qu'après décision et validation des régressions.

## Sécurité, compilation et sauvegardes

- Préserver le package `ca.jarvis.bridge`, le certificat Android de release, les secrets hors GitHub et les autorisations Google Gmail.
- Utiliser le workflow **de la branche/version ciblée** et un `versionCode` croissant ; vérifier les tests JVM, le build release, le certificat `apksigner` et les fichiers réellement présents dans l'APK.
- Tester sur appareil réel : ouverture d'un SVG Gmail et depuis la bulle, affichage de Conception, dégroupement, Calques, effets/sons, notifications, absence de doublons, EasySet et fermeture de session.
- Les paramètres laser et le déclenchement de la machine restent sous contrôle humain ; l'OCR d'EasySet est en lecture seule.
- Garder un commit précis, le résultat CI, le numéro de version, un APK signé sauvegardé durablement et une procédure de retour arrière. Les artefacts GitHub Actions expirent.

## Historique documentaire

La réorganisation initiale date du **10 novembre 2026**. L'archive contient le prototype **0.7** ; les anciennes notes 1.2.31 restent consultables dans l'historique Git, mais n'indiquent plus l'état actuel. Ni l'ancien dépôt, ni son numéro de version, ni le `main` en retard ne doivent se substituer à une référence de branche précise.
