# Jarvis 1.2.87 — correction après nouvelle capture Samsung

La capture utilisateur du 21 novembre 2026 montre un outil du volet **Conception** entouré en rouge vers le bas de l’écran, et la **bonne zone libre du canevas** entourée en turquoise vers le haut à droite.

**Important :** le cercle rouge peut indiquer le toucher précédent *Dégrouper*; la capture seule ne prouve pas qu'une tentative de désélection a réellement été livrée. La 1.2.86 ciblait de toute façon un emplacement non conforme au cercle turquoise et pouvait rester bloquée silencieusement si la cible Android n’était pas disponible.

## Modification

- Recalage du repère pour cette fenêtre Samsung, à partir du **véritable libellé Conception sélectionné** : **x ≈ 93 %** de la fenêtre, **y ≈ 48 %** entre le haut de cette fenêtre et le haut du libellé Conception, plutôt que 86 % et 30 %.
- Guard : refuser ce point si le libellé Conception sélectionné n'est pas détecté ou n'est pas au milieu de l'écran (**39–64 %** de la hauteur), si le point sort du couloir autorisé (**90–96 %** horizontal, **18–34 %** vertical de l'écran) ou si un contrôle accessible identifiable le recouvre.
- Test JVM construit avec la capture (879×1536 px) : turquoise environ **(817, 374)**, libellé Conception y ≈ 780. Refuser le **rouge (460, 1480)**, l'ancien point et la région grise.
- Marqueur de désélection désormais **cyan** seulement si Android livre cette action. Le marqueur **rouge** reste associé aux autres actions, notamment Dégrouper. La bulle indique « DÉSÉLECTION — POINT CYAN » lorsque le geste est livré; cela **ne prouve pas** que xTool a réellement désélectionné les formes.
- Si la cible ne peut pas être calculée et validée, **arrêter immédiatement** la séquence automatique au lieu de rechercher d’autres coordonnées ou de réessayer le toucher.
- Si le toucher cyan est livré, poursuivre la lecture de la barre puis la séquence de défilement + clic Calques **inchangée** depuis 1.2.85.

## Recette de test

Sur le Samsung, **avec le même SVG de petites formes uniquement** :
1. Vérifier le toucher Dégrouper (éventuel point rouge sur le bouton).
2. Chercher ensuite **un nouveau point cyan dans la zone turquoise de la capture**, jamais dans le menu.
3. Si ce point cyan n’apparaît pas, regarder l’état « DÉSÉLECTION — NON EFFECTUÉE » : l’essai a été refusé et Calques n’est pas lancé automatiquement.
4. Si le point cyan apparaît au bon endroit, vérifier visuellement la disparition de la sélection puis l’ouverture Calques via le défilement connu.

Aucun automatisme général pour les grandes formes SVG n'est autorisé par cette preuve de géométrie : le point peut rencontrer des objets dans un dessin différent. Aucun réglage EasySet ni laser déclenché. La 1.2.85 reste la référence validée pour Calques manuel. `main` reste inchangée.
