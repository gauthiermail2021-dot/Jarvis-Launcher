# Jarvis Bridge 1.2.30 — Lecture du nombre de passages dans EasySet

Version expérimentale distincte. La version stable 1.2.21 et les fonctions Gmail → SVG → xTool, bulle Jarvis, capture autorisée restent inchangées.

## Problème reproduit

Sur la capture xTool fournie, Jarvis 1.2.29 a correctement lu **Puissance 63 %** et **Vitesse 134 mm/s**, mais pas la valeur de **Passer : 1**. Le petit chiffre est à l'extrémité droite, collé à un chevron.

## Amélioration ciblée

- Conserve les deux analyses de la 1.2.29 : OCR complet par lignes et éléments, puis relecture agrandie de la colonne droite.
- Si le nombre de passages est **toujours non lu**, extrait en mémoire une **bande étroite autour du libellé « Passer »** et du champ numérique situé à droite.
- Cette bande est agrandie (jusqu'à 3,5×), légèrement contrastée puis confiée une dernière fois à ML Kit avec le contexte « Passer » visible.
- Les coordonnées OCR sont replacées dans le repère de l'écran original afin d'appliquer les mêmes garde-fous de zone, de type numérique entier positif et de non-confusion avec la vitesse.
- **Aucune valeur par défaut**, aucune conversion automatique de « I » ou « l » en 1 : si le chiffre n'est pas reconnu, le résultat reste « non lus ».
- Aucune capture supplémentaire ni nouveau dialogue Android. Les images et les chiffres reconnus restent temporaires et ne sont pas conservés sur disque.

## Essai

1. Installer Jarvis **1.2.30** sur le Samsung, avec l'APK signé.
2. Dans xTool, afficher le volet EasySet avec les **trois valeurs visibles**.
3. Appui long sur la bulle → « Analyser EasySet maintenant ».
4. Vérifier que les valeurs correspondent *effectivement* à ce qui est affiché dans xTool. Si la valeur « Passer » reste non lue, envoyer une capture du résultat.

## Vérifications et limites

Des tests JVM vérifient que la bande recadre bien le champ Passer sur la géométrie vue dans la capture d'écran, sans déborder sur la vitesse, et qu'aucune flèche ou lettre n'est interprétée d'office comme un nombre. GitHub Actions exécute les tests, produit un APK signé par la clé existante et vérifie la signature.

**Diagnostic uniquement :** aucun changement de puissance, de vitesse ou du nombre de passages, aucune commande de lancement de fabrication. La validation finale des paramètres reste humaine.
