# Jarvis 1.2.38 — Vitesse à un chiffre dans EasySet

## Capture Samsung 10 novembre 2026

La nouvelle capture de xTool Creative Space, prise après installation de Jarvis 1.2.37, montre les **trois champs réglés visuellement à 1** :
- `Puissance (%)` : **1 %** ;
- `Vitesse (mm/s)` : **1 mm/s** ;
- `Passer` : **1**.

Jarvis affiche `Puissance : 1 % ; Vitesse : non lue ; Passages : 1`. Les deux lectures Tesseract « Puissance » et « Passer » indiquent une confiance OCR de **95 %** chacune. Le correctif 1.2.37 pour Puissance est donc **validé sur le Samsung** avec un chiffre isolé, alors que la lecture numérique de Vitesse manque encore.

La capture n'établit pas à elle seule si l'appui sur la bulle SVG a effectivement envoyé et ouvert un nouveau fichier. Le fonctionnement du déclenchement reste à distinguer du résultat OCR.

## Correctif candidat 1.2.38

- Ajouter `EasySetSpeedRegion.find` : zone OCR **étroite**, à droite, **sous le libellé Vitesse**, sans chevaucher la ligne « Passer ». La position est dérivée des limites du libellé OCR dans la capture Android autorisée, pas d'une valeur supposée.
- Cloner la zone numérique avant recyclage de la capture originale.
- Si ML Kit (plein écran, colonne droite et zone Passer) n'a toujours pas lu la vitesse, utiliser `EasySetPassesTesseract.readSpeed` sur cette **zone numérique uniquement**, en conservant exactement les prétraitements Tesseract qui fonctionnent déjà pour les chiffres isolés de Puissance et Passer.
- Contrôler strictement le résultat : entier 1..3000, pas de 0, chiffres étrangers, préfixe `0` ou score OCR insuffisant. Vérifier que les seuils Tesseract de 120 et 165 ne produisent pas deux lectures incompatibles.
- Ne jamais écraser une valeur Vitesse que ML Kit a déjà reconnue. Tout échec conserve « non lue ».
- Ne pas changer les chemins OCR de puissance (validée à 1 %) ni de passages (validés à 1).
- Ne modifier aucun dessin, paramètre xTool ou équipement.

## Tests / publication

- Tests `EasySetSpeedRegionTest` : géométrie Samsung, chiffre isolé sous le libellé, exclusion des voisins, contrôle des valeurs 1, 20, 152, 3000, refus de 0, 3001, 01 et caractères ambigus. Contrôle que les plages Puissance et Passer restent différentes.
- Branche : `fix/easyset-speed-small-digit-1.2.38`; version Android `1.2.38`, `versionCode 57`.
- L'APK signé est une **version expérimentale**. Les tests automatiques ne constituent pas encore une validation d'OCR sur le Samsung; demander une nouvelle capture de l'interface avec « 1, 1, 1 » et les trois valeurs Jarvis.
