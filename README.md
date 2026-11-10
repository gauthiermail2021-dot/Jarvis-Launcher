# Jarvis Bridge 1.2.26 — Session EasySet : une autorisation Android

Prototype expérimental signé, sur une branche séparée. La version stable Jarvis 1.2.21 n'est pas modifiée.

## Parcours visé sur Android

1. Ouverture de ChatGPT : le détecteur facultatif déjà existant affiche la bulle Jarvis lorsqu'il est autorisé. Une seule demande de partage d'écran est alors lancée automatiquement. Si Android bloque l'ouverture du dialogue, un élément « Autoriser la capture pour la session » reste accessible dans le menu de la bulle.
2. Si l'utilisateur accepte, un service de premier plan avec notification permanente maintient une unique session `MediaProjection`. Le `VirtualDisplay` est créé une seule fois avec une `Surface` nulle au repos : aucune image n'est lue, enregistrée ou analysée en permanence.
3. La création/expédition d'un SVG et son ouverture par Jarvis dans xTool restent identiques aux versions précédentes.
4. Lors de la demande d'ouverture du SVG vers xTool, Jarvis planifie **une seule analyse automatique, en lecture seule**, après un délai de 6,5 secondes. Il attache temporairement une `ImageReader`, attend la disparition de la bulle, reconnaît les libellés EasySet avec ML Kit hors ligne, puis enlève la `Surface`. Si le volet EasySet n'est pas encore visible, il ne masque pas le dessin avec un échec.
5. Avec le volet EasySet visible, un appui long sur la bulle → « Analyser EasySet maintenant » déclenche immédiatement une autre lecture, sans nouvelle demande de consentement, à condition que la même session Android soit toujours active.
6. « Arrêter le partage d'écran » dans le menu, « Fermer la bulle » ou l'action « Arrêter la capture » dans la notification ferment la projection et libèrent les ressources. Android peut aussi révoquer le partage (verrouillage, interruption, arrêt système). Une nouvelle session exige une nouvelle permission.

## Ce que le prototype ne fait pas

- Aucun accès au contenu de ChatGPT, aucune analyse continue des images, aucun stockage de capture.
- Aucun contrôle de xTool, aucun changement de puissance/vitesse/passages et aucun démarrage de machine.
- L'export SVG, Gmail et l'ouverture xTool sont conservés. Une demande d'ouverture ne prouve pas que xTool ou EasySet est déjà affiché.
- L'autorisation MediaProjection n'est jamais permanente : une autorisation par **session active**, pas par installation.
- Le comportement de la permission au démarrage dépend de restrictions du fabricant Android; le menu de la bulle sert de solution de repli.
- Le champ « EasySet reconnu » est une détection visuelle des libellés, pas une confirmation des paramètres de fabrication.

## Tests

La compilation GitHub Actions exécute les tests Java, produit l'APK signé avec la clé d'origine et vérifie sa signature. Tester sur Samsung Android 13 la demande automatique, la notification persistante, la fermeture et une seconde analyse dans la même session sans nouveau dialogue Android.
