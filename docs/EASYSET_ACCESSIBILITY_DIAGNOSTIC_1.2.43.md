# Jarvis 1.2.43 — vérification de connexion Android avec EasySet

## Situation

La version 1.2.42 est compilée, mais le test sur le Samsung indique que l'ouverture manuelle du volet EasySet **ne déclenche pas** l'analyse.

## Ce que la version 1.2.42 fait déjà

- Le service d'accessibilité écoute les événements d'interface provenant de `com.makeblock.xcs`.
- Lorsqu'une session de capture Android est déjà autorisée, Jarvis appelle `getRootInActiveWindow()`, cherche des libellés précis (`Puissance`, `Vitesse`, `Passer`) et demande une capture OCR si le volet paraît ouvert.
- La reconnaissance des trois nombres à partir d'une capture fonctionne dans des essais précédents, mais cela ne confirme **pas** que la fenêtre xTool expose ces champs à Android.
- Aucun geste, aucun réglage laser et aucun lancement laser ne sont programmés.

## Diagnostic ciblé 1.2.43

La bulle propose désormais **« Diagnostic EasySet (connexion) »**. Cette fonction affiche uniquement :

- si le service d'accessibilité Jarvis est connecté ;
- si la session de capture Android est active ;
- l'âge et le type du dernier événement d'interface xTool observé ;
- si la racine de fenêtre active est accessible et identifiée comme xTool ;
- le nombre de nœuds examinés et le nombre de libellés EasySet reconnus.

Aucun texte de la fenêtre, aucune valeur de paramètre ni aucune capture d'écran ne sont conservés par le diagnostic. Les règles de lecture OCR existantes demeurent inchangées.

## Essai utile sur le Samsung

1. Installer la 1.2.43 par-dessus la version présente.
2. Vérifier le service Android « Jarvis Bridge — ChatGPT et xTool » dans Accessibilité (le réactiver si nécessaire).
3. Autoriser la capture Android comme auparavant.
4. Ouvrir xTool, toucher personnellement « Volet EasySet », puis ouvrir le menu de la bulle Jarvis par pression longue et choisir « Diagnostic EasySet (connexion) ».
5. Envoyer une capture du diagnostic. Les états permettent de distinguer le blocage : service désactivé, capture désactivée, aucun événement xTool ou arborescence xTool non lisible.

Statut : code de diagnostic ajouté ; compilation GitHub Actions à vérifier et validation physique encore nécessaire.
