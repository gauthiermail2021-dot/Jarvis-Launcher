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


## Test Samsung — trois valeurs reconnues (capture fournie)

Capture EasySet suivante fournie par l'utilisateur, après la proposition de Jarvis 1.2.38 :

| Champ | Valeur visible xTool | Valeur indiquée par Jarvis | Concordance |
| --- | --- | --- | --- |
| Puissance | 5 % | 5 % | Oui |
| Vitesse | 9 mm/s | 9 mm/s | Oui |
| Passer | 10 | 10 | Oui |

Diagnostic affiché pour **Passer** : libellé repéré ; OCR « Écran » `10▸` accepté ; OCR « Colonne droite » `10▸` accepté ; zone agrandie non exécutée, car inutile ; association acceptée. Aucun secours Tesseract numérique n'est affiché sur cette capture.

**Constat terrain : 3/3 concordants pour 5 %, 9 mm/s et 10 passages.** Cela valide cette combinaison sur le téléphone, mais **ne prouve pas** encore une reconnaissance fiable de toutes les valeurs (notamment `Vitesse : 1 mm/s` qui échouait auparavant), ni le déclenchement de la bulle SVG. La capture seule ne montre pas le numéro de version installé ; éviter de l'inférer avec certitude.

## Test Samsung — valeurs 1 / 1 / 1 reconnues

Nouvelle capture utilisateur de xTool et du panneau de résultat Jarvis, transmise après publication de la version candidate 1.2.38. La capture montre :

| Champ | xTool affiché | Jarvis affiché | OCR |
| --- | --- | --- | --- |
| Puissance | 1 % | 1 % | Tesseract, confiance indiquée 95 % |
| Vitesse | 1 mm/s | 1 mm/s | Tesseract, confiance indiquée 95 % |
| Passer | 1 | 1 | Tesseract, confiance indiquée 95 % |

**Résultat : 3/3 concordants, y compris le chiffre 1 en Vitesse qui était illisible avec le candidat précédent.** Le diagnostic précise que les trois étages ML Kit n'ont pas identifié le petit chiffre Passer, ce qui explique l'utilisation du secours Tesseract.

Validation réelle du fonctionnement passif OCR pour cette combinaison; cela ne valide pas une précision universelle, les déclencheurs de bulle SVG, ni toute la chaîne automatique Gmail → xTool. L'image ne montre pas explicitement le numéro de version dans l'interface Android ; cette attribution suit le contexte de test, sans preuve directe de la version dans la capture. Pas de modification de paramètres xTool.
