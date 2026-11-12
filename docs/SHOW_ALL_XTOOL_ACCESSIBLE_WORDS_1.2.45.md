# Jarvis 1.2.45 — Voir tous les mots/accessibility labels exposés par xTool

## Demande utilisateur

« Je veux voir les mots qu'il découvre ». Après la 1.2.44 qui n'affichait que les libellés filtrés EasySet (5 correspondances parmi 102 nœuds), l'utilisateur demande de **voir tout le texte accessible** que le service Android reçoit réellement de xTool.

## Modification

- Nouvelle commande dans le menu (pression longue de la bulle) : **« Voir les mots détectés (xTool) »**.
- Lors de la même lecture d'accessibilité **déjà active** et limitée au paquet `com.makeblock.xcs`, conserver les textes de `AccessibilityNodeInfo.getText()` et les descriptions de `getContentDescription()`, **sans les filtrer par nom EasySet**.
- Les lignes montrent leur source (`Texte` ou `Description`), le libellé exact reçu, dans l'ordre des nœuds parcourus, avec les doublons. Affichage limité aux premiers 100 textes/descriptions et aux 240 nœuds déjà parcourus ; les textes de plus de 100 caractères sont coupés avec une ellipse. Le décompte et la limite sont visibles.
- Le diagnostic mentionne l'âge de la dernière inspection pour éviter de confondre un ancien résultat avec l'écran actuel.
- La fenêtre déjà défilable affiche cette nouvelle liste complète. La dernière lecture existe **uniquement en RAM**, est remplacée au prochain événement xTool et disparaît à l'arrêt du service.
- **Aucun changement** de l'analyse OCR, des clics (toujours désactivés), des paramètres laser, des envois Gmail ou de la bulle SVG. L'écran xTool n'est pas capturé en continu.
- Le résultat inclut seulement les textes/déscriptions **effectivement exposés par Android**. Les zones peintes à l'écran mais sans nœud accessible n'apparaissent pas. Ce n'est pas une extraction OCR exhaustive des pixels.

## Test sur Samsung

1. Installer 1.2.45 par-dessus 1.2.44 (pas de désinstallation).
2. Avec le service d'accessibilité et la capture déjà autorisés, ouvrir xTool puis le volet EasySet.
3. Appui long sur Jarvis, puis **« Voir les mots détectés (xTool) »**.
4. Faire défiler la fenêtre et examiner les lignes telles que « Texte : … » et « Description : … ».
5. Si la fenêtre indique « Aucune inspection xTool », vérifier le service Android d'accessibilité dans les réglages puis revenir à xTool.

`XtoolAccessibleWordsTest` couvre l'absence de filtre EasySet, les doublons, l'affichage de la source, la normalisation et les limites de longueur/nombre.

Version `1.2.45`, `versionCode 64`. Test automatique et APK signé GitHub Actions à vérifier.
