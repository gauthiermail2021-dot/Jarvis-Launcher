# Jarvis 1.2.33 — analyse automatique d'un dessin xTool déjà ouvert

## Problème constaté

Dans Jarvis 1.2.32, la lecture des trois valeurs fonctionne sur le Samsung (Puissance 69 %, Vitesse 152 mm/s, Passer 1 avec Tesseract 95 %). Toutefois, l'analyse automatique n'est programmée qu'après un **nouvel envoi SVG** vers xTool, à délai fixe. L'utilisateur a indiqué que xTool Creative Space était **déjà ouvert avec un dessin**, ce qui ne garantit pas qu'une nouvelle importation soit terminée et ne déclenche pas la lecture lors d'un retour ordinaire dans xTool.

## Correction expérimentale 1.2.33 (code 52)

- Le service d'accessibilité Jarvis **facultatif et activé explicitement** n'écoute que les transitions de fenêtres de **ChatGPT** et de **xTool Creative Space** (`com.makeblock.xcs`).
- Aucune lecture de contenu de fenêtre, aucune action tactile ou geste n'est demandée par le service d'accessibilité.
- Lorsque xTool revient au premier plan, le service de capture **déjà autorisé par Android** peut lancer une seule lecture EasySet après stabilisation de l'interface, même si le dessin a été ouvert auparavant.
- Le chemin existant **Gmail → SVG → xTool → analyse EasySet** est conservé. Si la lecture programmée après transfert SVG est déjà en attente, une transition de fenêtre ne la duplique pas.
- Après une lecture reconnue, Jarvis ne répète pas l'analyse à chaque changement de fenêtre. Un nouvel envoi SVG réarme l'analyse.
- L'OCR reste **strictement passif** : aucune sauvegarde/fermeture du dessin, aucune modification des réglages xTool, aucun lancement laser.

## Contraintes honnêtes

- **Android doit autoriser la session de capture** au départ de Jarvis. Un écran ne peut pas être lu silencieusement sans ce consentement système.
- Le service d'accessibilité « Jarvis Bridge — ChatGPT et xTool » doit être activé dans les paramètres Android pour ce nouveau déclenchement. La portée surveillée est limitée à ces deux applications.
- Si xTool affiche une demande **Enregistrer / Jeter** pour l'ancien dessin, Jarvis **ne choisit pas à la place de l'utilisateur**. Il n'y a aucune garantie que l'OCR s'exécute avant la fermeture de cette boîte.
- L'analyse au retour de xTool nécessite que le **volet EasySet soit visible** pour que les valeurs soient reconnues. Sinon, elle peut ne produire aucun panneau de résultat.
- Le nouveau déclenchement est un candidat de test : compilation et tests automatiques ne prouvent pas encore le comportement du Samsung.

## Tests automatisés

- Filtre exact du paquet xTool.
- Aucun déclenchement sans projection Android autorisée.
- Aucun déclenchement sans bulle Jarvis visible.
- Aucune surveillance active des autres applications.
- Limitation des déclenchements successifs.

## Test de validation pratique (sans mettre la machine en marche)

1. Mettre à jour l'application vers l'APK 1.2.33 **si la signature et les tests sont corrects**.
2. Vérifier que le service d'accessibilité Jarvis limité à ChatGPT et xTool est activé et que la capture Android est autorisée.
3. Laisser un dessin existant affiché dans xTool **avec EasySet visible**. Revenir à ChatGPT, puis réafficher xTool.
4. Vérifier que le résultat EasySet s'affiche automatiquement, sans appui long, avec les trois valeurs.
5. Refaire une vérification du circuit Gmail → SVG → xTool ; ne pas utiliser la fonction de découpe.
