# Jarvis 1.2.86 — un seul toucher de désélection avant Calques

## Constat Samsung du 21 novembre 2026

Après `Conception → Dégrouper`, le dessin demeure sélectionné et le menu contextuel empêche l’accès à la barre d’outils du bas. En 1.2.85, l’utilisateur a confirmé que **défiler vers la gauche puis cliquer sur Calques fonctionne**, mais seulement après une désélection manuelle.

Sur la capture transmise, l’utilisateur a entouré en turquoise **une portion blanche libre du plateau, vers le haut à droite, juste au-dessus de la limite de la zone grise**, loin des petites formes géométriques (position variable selon résolution/zoom).

## Modification candidate 1.2.86

- Après le retour du toucher `Dégrouper`, tenter **une fois seulement** `XtoolLayersOneShot.deselectAtMarkedBlankCanvas(...)`.
- Position calculée en fonction de la fenêtre xTool et du bord supérieur réel de l'onglet `Conception` : x environ **86 %** de sa largeur, y environ **30 %** entre le haut de la fenêtre et l'onglet. Ce sont des proportions d’un dessin-test Samsung, **pas des coordonnées fixes universelles**.
- N’autoriser le geste que lorsque la fenêtre active est xTool, `Conception` est réellement marqué sélectionné dans l’accessibilité, le rectangle reste à l'intérieur des bornes prévues, et aucun contrôle accessible compact ne recouvre le point.
- En cas de geste refusé ou annulé, stopper cette séquence automatique. Si Android confirme seulement la livraison du geste, tenter ensuite d’identifier la barre du bas et reprendre **inchangé** le glissement et le clic Calques de la 1.2.85.
- Ne jamais réessayer un autre point durant la même séquence. Ne jamais commander le laser ni modifier EasySet.

## Limite impérative

Un point blanc sur cette **capture** ne garantit pas qu'il soit vide pour **tous les futurs SVG** : xTool ne donne pas à l’application les bornes de chaque forme via l’accessibilité. Cette géométrie est un **essai à valider sur de petits SVG tests**. Un geste livré n’est pas une preuve que le dessin est désélectionné. Valider visuellement la forme et le panneau Calques sur Samsung avant d’élargir l’usage.

## Recette manuelle

1. Installer l'APK signé 1.2.86 sans toucher à `main`.
2. Ouvrir un nouveau SVG test avec les trois petites formes au centre du plateau.
3. Constater le passage Conception et le toucher Dégrouper.
4. Vérifier visuellement **un seul point de toucher** dans l’espace blanc entouré sur la capture. Vérifier que le dessin n’est plus sélectionné.
5. Vérifier que la barre du bas défile vers la gauche et que le panneau Calques s’ouvre comme en 1.2.85.
6. Si un geste touche une forme, une fenêtre ou la zone grise, **ne pas utiliser** cette fonction sur d'autres SVG ; conserver la 1.2.85 pour le test manuel.

## Version / sécurité

La base testée 1.2.85 est gardée sur sa branche propre, aucune fusion vers `main` ou la branche de consolidation. Le numéro de cette candidate est `1.2.86` / `versionCode 105`.
