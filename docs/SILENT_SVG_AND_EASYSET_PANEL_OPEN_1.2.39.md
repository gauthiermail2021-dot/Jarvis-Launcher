# Jarvis 1.2.39 — Envoi SVG discret et détection du volet EasySet

Date : 2026-11-10.

## Retour utilisateur

Après validation OCR EasySet sur Galaxy Note20 Ultra (1 / 1 / 1 puis 5 / 9 / 10), l'utilisateur a indiqué que la ligne **« Volet EasySet »** doit être touchée dans xTool pour faire apparaître les paramètres. Il a demandé la détection automatique de ce changement, puis a explicitement ajouté : **« J'aimerais que la fenêtre n'apparaisse pas quand je clique sur l'envoie d'un svg »**.

## Correction 1 : bulle SVG sans fenêtre Jarvis

En 1.2.38, la bulle flottante SVG ouvrait `JarvisTestShortcutActivity` avec sa fenêtre visible « TEST SVG JARVIS », et l'activité restait ouverte après envoi.

En 1.2.39 :
- La bulle SVG lance uniquement `JarvisSilentSvgSendActivity`, activité Android transparente **sans aucune vue affichée**, sans animation ni atténuation de l'arrière-plan.
- L'icône Android `TEST SVG JARVIS` demeure inchangée avec sa fenêtre normale pour un test manuel et son diagnostic.
- Les autorisations Google restent soumises à consentement : si l'accès Gmail a expiré ou manque, **l'écran officiel de Google peut toujours apparaître**, conformément à la sécurité Android.
- L'envoi utilise exactement le même SVG numéroté, le même consentement, la protection contre les doublons et le réarmement explicite de session xTool que 1.2.38.
- Après succès Gmail et ajout du libellé `Jarvis SVG`, Jarvis crée une nouvelle `MainActivity` de contrôle Gmail **transparente**, même si la préférence de surveillance Gmail automatique est désactivée. La fenêtre visible de paramètres Jarvis ne doit pas surgir pendant le transfert.
- Les erreurs d'envoi affichent seulement un message Toast et ferment l'activité transparente. L'icône manuelle continue d'afficher les erreurs dans son écran.
- Android/xTool garde le droit de demander quoi faire d'un document déjà ouvert. Jarvis **ne ferme ni ne jette automatiquement** un dessin xTool.

## Correction 2 : EasySet fermé → ouvert, sans cliquer dans xTool

- Nouvelle classification OCR pure Java `EasySetPanelState` depuis une seule capture Android autorisée.
- **Fermé** : zone `Volet EasySet` reconnue dans les **Paramètres / Réglages** de xTool, sans libellés `Puissance`, `Vitesse`, `Passer` ; instruction `Cliquer sur le volet EasySet...` reconnue lorsqu'elle est visible.
- **Ouvert** : libellés des trois paramètres reconnus dans le panneau. Pas de supposition à partir d'un chiffre manquant.
- Si fermé, Jarvis affiche une consigne courte « Touche Volet EasySet dans xTool » et n'essaie pas d'inventer des valeurs. Un état interne `awaitingPanelOpen` est posé.
- Si l'utilisateur ouvre le volet, le service Accessibilité **observe uniquement l'événement xTool de changement de contenu** (sans lire le contenu de la fenêtre) et demande **une** nouvelle analyse par `MediaProjection` déjà consenti ; seuil anti-rafale de 1,2 s, dédoublonnage des scans en cours.
- Si un nouveau scan reconnaît les valeurs EasySet, la demande de reprise est arrêtée et les résultats OCR s'affichent.
- Hors attente d'ouverture, Jarvis ignore tous les événements de contenu xTool. Aucune capture permanente, aucune OCR en continu, aucune modification de paramètres, aucun geste sur xTool et aucun démarrage du laser.
- Le service Accessibilité existant doit être actif pour recevoir ces changements; il reste **facultatif**, désactivable par l'utilisateur, et le manuel « Analyser EasySet maintenant » reste disponible.

## Validations attendues sur Samsung

1. Installer 1.2.39 **par-dessus** 1.2.38 (ne pas désinstaller), avec la bulle SVG active et Gmail déjà autorisé.
2. Depuis xTool, toucher une fois la bulle `SVG`. **Aucune fenêtre « TEST SVG JARVIS »** ne doit apparaître. Le prochain numéro SVG doit être envoyé à Gmail puis demandé à xTool.
3. Avec xTool ouvert dans **Paramètres**, laisser **Volet EasySet fermé** : Jarvis doit reconnaître cet état et afficher la courte consigne, pas un faux résultat OCR.
4. Toucher personnellement `Volet EasySet` dans xTool. Après l'événement de changement d'interface, Jarvis doit demander une seule lecture en lecture seule et afficher les trois valeurs visibles.
5. Si xTool ne produit pas d'événement Accessibilité sur ce téléphone, le mode manuel par appui long sur Jarvis reste nécessaire; ne pas promettre que l'automatisme fonctionne avant le test terrain.
6. Une demande d'autorisation Google officielle, une confirmation xTool du dessin existant ou une capture Android révoquée restent des écrans système qui peuvent demander l'accord de l'utilisateur.

## Version et traçabilité

- `ca.jarvis.bridge` — `versionName 1.2.39`, `versionCode 58`.
- Branche isolée `feature/easyset-open-panel-trigger-1.2.39`, basée sur la version signée 1.2.38.
- Tests Java `EasySetPanelStateTest` et `EasySetForegroundScanPolicyTest`, en plus des tests OCR existants.
- Compilation GitHub signée, avec les mêmes secrets Android permanents ; branche `main` non modifiée.
- Le comportement Android/Samsung est **à vérifier sur appareil réel** ; une réussite CI ne suffit pas.
