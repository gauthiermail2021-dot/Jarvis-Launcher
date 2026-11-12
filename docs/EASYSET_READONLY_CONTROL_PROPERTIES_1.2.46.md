# Jarvis Bridge 1.2.46 — inspection en lecture seule des commandes EasySet

## Contexte vérifié sur Samsung

Avec la version 1.2.45, Jarvis a relevé 26 textes/descriptions sur 97 nœuds de l'interface xTool, dont `Volet EasySet`, `Puissance(%)`, `Vitesse(mm/s)` et `Passer`. Ces libellés sont visibles pour l'accessibilité Android, mais les valeurs numériques trouvées ne sont pas encore associées de manière fiable aux champs.

L'utilisateur a demandé : « On doit trouver comment modifier une valeur ou comment cliquer sur le volet EasySet » puis a accepté le projet d'**inspection des propriétés Android**, sans action automatique.

## Nouveauté 1.2.46

Le menu Jarvis inclut **« Inspecter les commandes xTool »**, qui affiche, **en lecture seule**, les propriétés exposées par l'arbre Android de xTool :

- libellé exact correspondant à EasySet, puissance, vitesse ou passages ;
- source du libellé : `Texte` ou `Description` d'accessibilité ;
- `className` (type du composant) ;
- `isClickable()`, `isEditable()` et `isFocusable()` ;
- `getBoundsInScreen()` — rectangle de position ;
- noms des actions disponibles selon `getActionList()` : `CLICK`, `FOCUS`, `SET_TEXT`, `SET_PROGRESS`, etc. quand ils sont effectivement exposés ;
- propriétés du **parent immédiat**, si celui-ci appartient à la fenêtre xTool.

Un texte non cliquable peut avoir un parent cliquable : ce diagnostic indique la différence, **sans essayer de cliquer**. L'inspection est limitée à 240 nœuds et 16 entrées EasySet, et utilise les événements Android déjà traités par le service. La dernière inspection est conservée en RAM seulement, supprimée à l'arrêt du service. La fenêtre d'affichage est défilable.

## Limites et sécurité

- Pas de clic automatique : aucune invocation de `performAction()` ou `dispatchGesture()`.
- Pas de modification de valeur : aucune opération de saisie ou de progression.
- Aucun démarrage ou contrôle de laser.
- Aucune capture supplémentaire, écriture de fichier, transmission réseau ni enregistrement de contenu complet du nœud.
- La présence d'une action dans le diagnostic signifie uniquement que l'interface **déclare** l'action Android ; ce n'est pas une validation de sa fiabilité réelle ni un feu vert pour piloter la machine.
- Les rectangles sont ceux du dernier état d'interface inspecté ; ils peuvent devenir obsolètes après un changement d'écran.

## Vérification sur Samsung

1. Installer Jarvis 1.2.46 par-dessus la version actuelle (sans désinstaller).
2. Garder le service d'accessibilité Jarvis actif et l'autorisation de capture Android valide, comme pour la 1.2.45.
3. Ouvrir xTool, puis le volet EasySet manuellement.
4. Faire un appui long sur la bulle Jarvis, puis choisir **« Inspecter les commandes xTool »**.
5. Faire défiler la fenêtre et observer les propriétés indiquées pour le volet et les trois champs. Une capture des résultats permettra de constater si xTool expose ces éléments comme boutons, champs modifiables ou simples textes.

## Tests

`EasySetControlPropertiesTest` couvre la sélection ciblée des quatre libellés, le formatage type/cliquable/modifiable/focus/actions/position, l'absence d'action et les diagnostics sans éléments.

Branche de travail : `feature/easyset-control-properties-1.2.46`.
VersionName : `1.2.46` ; versionCode : `65`.
Statut : compilation et tests Android/JVM signés à vérifier ; validation physique Samsung non encore réalisée.
