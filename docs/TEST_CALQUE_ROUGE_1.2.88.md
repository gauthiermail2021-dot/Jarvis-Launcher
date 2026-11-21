# Jarvis 1.2.88 — cliquer une seule fois sur le calque rouge

## Point de départ validé par l'utilisateur

Sur son Samsung, l'utilisateur a confirmé que la version **1.2.87** effectue la séquence import SVG → Conception → Dégrouper → point de désélection cyan sur le canevas → glissement gauche du menu → Calques.

**Ne pas modifier cette séquence validée.** L'étape ajoutée commence uniquement après que l'action d'ouverture Calques a été envoyée.

## Nouvelle étape

- Attendre environ **1,1 seconde** après le résultat de Calques pour permettre au panneau de s'afficher.
- Scanner **une seule fois** la fenêtre xTool au premier plan.
- Cibler un unique nœud d'accessibilité portant **exactement « Rouge »** (majuscules et espaces tolérés) dans un panneau dont un en-tête **Calque / Calques / Layer / Layers** se trouve au-dessus de cette ligne.
- S'il existe un parent ou contrôle lui-même cliquable et de dimensions raisonnables, privilégier `ACTION_CLICK` sur cette cible.
- Sinon, tenter un seul toucher au centre du rectangle du libellé Rouge si ses dimensions sont valides.
- **Aucun toucher** si le label n'est pas visible, si plusieurs cibles Rouge existent, si le panneau est introuvable ou si les bornes sont invalides. **Aucun clic de secours** si une `ACTION_CLICK` reconnue est refusée.
- Lire à nouveau l'état d'accessibilité après 650 ms; un geste livré n'est pas une preuve de sélection. L'état `isSelected()` est cité uniquement s'il est explicitement renvoyé par xTool.
- La bulle affiche « CALQUE ROUGE : TOUCHER ENVOYÉ » ou « NON IDENTIFIÉ » sans ouvrir une grande fenêtre par-dessus le panneau.
- Un bouton « Cliquer sur calque rouge (1 fois) » a été remis dans **Jarvis → Réglages** pour un test manuel distinct, avec rapport détaillé.

## Limites

Cette étape dépend de l'accessibilité **réelle** de la couleur « Rouge » dans la version mobile de XCS. L'app peut dessiner une pastille colorée sans nom de couleur accessible : dans ce cas, Jarvis doit **s'arrêter sans clic hasardeux**. Ne pas inférer des coordonnées, ne pas cliquer une couleur d'après le seul emplacement supposé.

**Aucun changement** de vitesse, puissance, passes, paramètres machine ni démarrage du laser. `main` et la version de référence 1.2.87 restent inchangées.

## Recette Samsung

1. Installer la candidate signée **1.2.88**, même identifiant Android et signature que les releases précédentes.
2. Lancer un SVG de test avec le même dessin simple utilisé pour valider 1.2.87.
3. Vérifier que la désélection cyan, le glissement et l'ouverture de Calques restent corrects.
4. Observer l'étape supplémentaire « CALQUE ROUGE ». S'il s'agit de « NON IDENTIFIÉ », utiliser « Inspecter calques et couleurs » et partager le diagnostic ou une capture.
5. Si le toucher est envoyé, vérifier que c'est réellement la **ligne rouge** qui est sélectionnée; ne pas supposer une modification des réglages EasySet.
