# Jarvis Bridge 1.2.32 — candidat signé, à tester sur Samsung

> **ÉTAT : APK de TEST produit, pas encore validé en conditions réelles.**
> Dépôt officiel : **Jarvis-Launcher**. La branche `main` demeure sur 1.2.31 ; la version candidate 1.2.32 est isolée dans `experiment/easyset-tesseract-1.2.32-preparation`.

## Traçabilité de la version test

- Source exacte du build : commit `9bdf30e9ce8f28f4a0fa571070ea0f2c687cdc7e` (snapshot `snapshot/jarvis-1.2.32-candidate-signed-2026-11-10`).
- `applicationId ca.jarvis.bridge`, `versionCode 51`, `versionName 1.2.32`.
- GitHub Actions : [compilation signée 1.2.32 #42812323145](https://github.com/gauthiermail2021-dot/Jarvis-Launcher/actions/runs/42812323145) — tests JVM, compilation Release et contrôle de signature **réussis**.
- Artefact temporaire GitHub Actions : `Jarvis-Bridge-1.2.32-tesseract-signed`, ID `13763176020`.
- Copie livrée dans la conversation : `Jarvis-Bridge-1.2.32-EasySet-Tesseract-signed.apk`, SHA-256 `0082b36a6a90b0bb18dd5bc53d2286ef63e66c6f1776751ee7cf0f65701e5a96`, 77 071 752 octets.
- Le certificat de signature APK v2 a le **même SHA-256** que l'APK signé 1.2.31 : `68989EDC69061CCC06D0D6BC0CB0E3FE5BDE157AB7ADE5111F06E867A74413B7`. Il devrait s'installer comme mise à jour, sans désinstallation.
- L'APK intègre `assets/tessdata/eng.traineddata` et les bibliothèques Tesseract natives.

## Modifications du candidat

1. **ML Kit conservé** pour l'écran complet, la colonne droite et la zone « Passer » agrandie.
2. Si toutes les étapes ML Kit renvoient `Passages : non lus`, un moteur **Tesseract4Android** lancé sur un thread de travail examine seulement la case numérique près du libellé `Passer`, à partir de la **même capture Android autorisée**.
3. Le champ est cadré relativement à l'écran et à la hauteur du libellé. Les pixels foncés sont isolés, agrandis ×6, bordés de blanc, puis analysés avec PSM_SINGLE_WORD et chiffres uniquement.
4. Le diagnostic indique la valeur, la confiance ou l'échec. En cas de doute, le texte **reste « Passages : non lus »** ; aucune valeur par défaut `1` n'est inventée.
5. La capture n'est pas publiée sur GitHub, les Bitmap sont recyclés, la clé de signature reste dans les secrets GitHub. Aucun accès réseau OCR, aucune modification xTool, aucune commande laser.

## Tests réalisés

- [x] Test avec la capture reçue de xTool (**869×1536**, 69 %, 152 mm/s, Passer **1**) sur le moteur **Tesseract CLI local** : reconnaissance `1`, confiance environ **67 %** avec le bon cadrage.
- [x] Test JVM de géométrie du cadrage ciblé et tests hérités de Jarvis.
- [x] Compilation Android de développement, puis compilation **Release** signée, avec modèle OCR inclus.
- [x] Vérification de la signature et du même certificat de release que la 1.2.31.
- [ ] Installation et essai de l'APK 1.2.32 sur Samsung (lecture réelle de `Passer`).
- [ ] Essais supplémentaires sur différentes valeurs, résolutions et thèmes, et vérification fonctionnelle Gmail → SVG → xTool et bulle.
- [ ] Après validation, publier une release officielle durable (l'artefact Actions expire).

## Test sur téléphone

Mettre à jour Jarvis avec l'APK signé **sans désinstaller l'app 1.2.31**. Ouvir xTool EasySet avec les 3 valeurs visibles, puis appui long sur la bulle → **Analyser EasySet maintenant**. Contrôler que l'analyse présente Puissance, Vitesse et le chiffre `Passer`, ou une raison d'échec Tesseract. La lecture seule ne doit rien changer dans xTool. Partager le résultat textuel ou une capture pour finaliser le diagnostic.

## Historique et source canonique

Le dépôt **Jarvis-Bridge-Android 0.7** reste une archive et ne doit pas compiler cette version. `main` du dépôt **Jarvis-Launcher** reste la référence de développement ; la branche candidate sera fusionnée uniquement après essai réel et vérifications. L'APK signé de la 1.2.31 et son snapshot restent disponibles pour revenir à une version antérieure.
