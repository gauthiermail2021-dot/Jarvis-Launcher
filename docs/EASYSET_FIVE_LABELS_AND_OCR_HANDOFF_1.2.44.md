# Jarvis 1.2.44 — libellés EasySet exacts et transfert fiable vers l'OCR

## Observation Samsung (11 novembre 2026)

Capture du diagnostic 1.2.43 montrant :
- Service d'accessibilité actif, capture Android autorisée.
- Dernier événement xTool `TYPE_WINDOW_CONTENT_CHANGED` il y a 35 s.
- Fenêtre EasySet **ouverte et visible**.
- 102 nœuds inspectés, **5 libellés** détectés.
- Aucun affichage OCR automatique malgré la reconnaissance du volet.

## Cause de la défaillance silencieuse

Dans 1.2.43, lorsqu'Android reconnaissait l'ouverture d'EasySet, l'accessibilité plaçait immédiatement `scannedCurrentAccessibleOpenPanel = true`, puis lançait une demande `ACTION_PANEL_TAP`. Or le service de capture rejetait sans message cette demande si :
- un précédent `autoScanTrigger` était encore programmé (notamment les 6,5 s après import SVG ou les 1,5 s sur changement de fenêtre) ;
- un scan était déjà en train de capturer une image ou d'analyser un OCR.

Le résultat pouvait être un volet considéré « déjà traité », sans capture effective, ni tentative de suivi.

## Correction 1.2.44

1. Une détection de volet ouvert **remplace un ancien minuteur d'analyse**, au lieu d'être rejetée. Si une capture ou OCR est en cours, la demande est conservée pour **une seule** reprise après l'analyse actuelle si celle-ci ne reconnaît pas déjà EasySet. Pas de boucle de captures.
2. Le service mémorise **uniquement les étapes du transfert** : demande transmise, capture programmée, capture démarrée, image reçue, OCR terminé/reconnu ou erreur. Aucun détail numérique, aucune image ou contenu OCR n'est enregistré dans ce journal.
3. « Diagnostic EasySet (connexion) » affiche maintenant les **libellés exacts et leur provenance** (`Texte` ou `Description`). Les doublons sont conservés pour comprendre pourquoi cinq entrées ont été relevées. Seules les courtes chaînes commençant par les libellés EasySet autorisés sont collectées ; les chaînes contenant des chiffres sont masquées pour ce diagnostic.
4. La fenêtre de diagnostic devient **défilable**, avec un bouton de fermeture accessible en permanence.
5. Conserve le système de lecture OCR passif, la bulle SVG silencieuse, les animations 500 ms et la prise en charge de xTool depuis 1.2.43. **Aucune commande ni aucun réglage laser automatisé.**

## Vérification pratique

- Installer 1.2.44 par-dessus 1.2.43 sans effacer les préférences.
- Avec la session de capture Android déjà autorisée, ouvrir manuellement `Volet EasySet` dans xTool.
- Si le panneau OCR ne s'affiche pas automatiquement, appui long sur la bulle Jarvis > `Diagnostic EasySet (connexion)`.
- Envoyer une capture : les **5 libellés exacts** et la ligne **OCR automatique : ...** indiqueront si le blocage est avant la demande, pendant la capture ou pendant la reconnaissance.

## Tests

- `EasySetDiagnosticHeadingsTest` : noms et doublons exacts, masquage numérique, liste vide.
- `EasySetScanHandoffTest` : remplacement des anciens minuteurs vs reprise après un scan en cours.
- Tests Android/JVM et APK signé par GitHub Actions.
- Tests physiques du déclencheur sur Samsung à confirmer ; ne pas confondre succès CI et preuve terrain.

Branche `fix/easyset-labelled-ocr-handoff-1.2.44`, versionName 1.2.44, versionCode 63.
