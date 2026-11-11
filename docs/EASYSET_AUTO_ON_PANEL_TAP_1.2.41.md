# Jarvis 1.2.41 — EasySet : lire après un clic manuel sur le volet

## Retour Samsung — panne 1.2.40

L'utilisateur indique : « Je clique sur le volet EasySet mais l'analyse ne se fait pas ensuite. »

Vérification du code 1.2.40 :
1. Les événements de changement de contenu xTool (`TYPE_WINDOW_CONTENT_CHANGED`) ne déclenchent une nouvelle capture que si `EasySetSessionService.isAwaitingPanelOpen()` est vrai.
2. Ce drapeau est vrai **uniquement si une capture précédente a déjà reconnu le volet comme fermé**. Si Jarvis ne capture pas cet état avant que l'utilisateur ouvre le panneau, le clic dans xTool n'entraîne aucune relecture.
3. Une lecture EasySet déjà réussie posait `autoRecognizedEasySet=true`, ce qui pouvait aussi bloquer le prochain scan de premier plan.

## Correctif 1.2.41

- Le service d'accessibilité Android observe `TYPE_VIEW_CLICKED` **uniquement pour le paquet xTool `com.makeblock.xcs`**; il n'interroge pas l'arbre de fenêtres et n'effectue jamais de geste.
- Lorsqu'un événement de clic contient le texte ou la description **« Volet EasySet »**, une analyse passive à une seule capture est demandée après un bref délai de 900 ms, **sans exiger de capture préalable du volet fermé**, même si la dernière lecture EasySet avait réussi.
- Certaines interfaces Android/WebView ne donnent pas le libellé du contrôle. Dans ce cas, un clic xTool **sans aucun texte** peut autoriser un seul essai OCR si aucune lecture EasySet réussie ne l'a encore précédé; recours plafonné à un événement toutes les 8 secondes. Les clics avec un autre libellé sont ignorés.
- Le service de capture existant impose toujours que `MediaProjection` soit **déjà autorisé par Android**, que la bulle Jarvis soit active et que le paquet soit xTool. La capture de l'écran ne commence pas sans ce consentement.
- L'ancien déclencheur xTool de premier plan et la détection « volet fermé → ouvert » restent disponibles.
- Un scan précédent en cours ou déjà planifié n'est pas interrompu. Les anciens résultats OCR, la bulle SVG, l'envoi Gmail sans fenêtre et les animations visibles 500 ms de la v1.2.40 sont conservés.
- Aucun clic automatique, aucun changement des paramètres, aucune sauvegarde d'image et **aucune commande laser**.

## Tests automatiques

`EasySetPanelTapPolicyTest` vérifie :
- reconnaissance de « Volet EasySet », avec ou sans symbole chevron ;
- refus de l'instruction « Cliquer sur le volet EasySet… » et des autres textes ;
- clic nommé admissible même sans capture précédente ou après une lecture réussie ;
- recours sans texte plafonné à une tentative toutes les 8 secondes, avec permission de capture ;
- refus d'un autre paquet, d'une capture non autorisée et d'une bulle inactive.

## Validation Samsung

1. Mettre à jour Jarvis **sans désinstaller**, pour conserver les réglages et le compteur de SVG.
2. Vérifier dans **Paramètres Android → Accessibilité** que le service **« Jarvis Bridge — ChatGPT et xTool »** est activé. Si nécessaire, le désactiver puis réactiver après la mise à jour pour enregistrer le nouvel événement `TYPE_VIEW_CLICKED`.
3. Vérifier que l'autorisation Android de capture d'écran est active pour la session Jarvis.
4. Dans xTool, choisir **« Paramètres »** puis toucher personnellement **« Volet EasySet »**.
5. Résultat attendu : une lecture passive de Puissance / Vitesse / Passages et l'affichage du résultat OCR, **sans appui long sur Jarvis**.
6. Si aucune analyse : signaler si une capture manuelle via « Analyser EasySet maintenant » fonctionne, pour séparer problème d'événement Android et problème de capture.

**Statut :** correctif programmé et testé automatiquement ; le véritable événement `TYPE_VIEW_CLICKED` de xTool doit encore être confirmé sur le Samsung. Ne pas déclarer la correction validée sur appareil avant le test.
