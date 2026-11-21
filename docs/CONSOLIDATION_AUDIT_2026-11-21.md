# Plan de consolidation Jarvis — 21 novembre 2026

## Périmètre et branches

- **Base conservée :** `main` à `c516782830a31ecaf411b7b2bdac3fd3a97387ad`, version 1.2.69 (versionCode 88), confirmée sur Samsung pour Gmail/SVG, Conception et Dégrouper.
- **Branche propre :** `integration/audit-p1-from-main`, créée **directement à partir de main**. Elle ne contient pas les dizaines de commits expérimentaux des versions 1.2.71–1.2.85.
- **Candidat expérimental distinct :** `fix/1.2.85-audit-p1-hardening` (PR #59), basé sur 1.2.84. Son code Calques nécessite encore validation physique.
- **Pas de fusion ni de livraison automatique.** `main` reste inchangée et cette branche garde provisoirement le numéro 1.2.69. Ne pas installer une APK portant ce numéro par-dessus une candidate plus récente.

## Changement sélectionné pour retour vers main

1. **Sécurité du lanceur exporté** : `JarvisTestShortcutActivity` ne peut plus envoyer un courriel simplement sur `onCreate` ou `onNewIntent`, deux actions déclenchables par une application externe. Le bouton d'envoi reste accessible à l'utilisateur.
2. La petite bulle TEST conserve le transfert un-toucher en utilisant l'activité `JarvisSilentSvgSendActivity` qui est déclarée `exported=false`.
3. La règle pure `JarvisTestSendPolicy` et le test `JarvisTestSendPolicyTest` garantissent la séparation des deux chemins.

**Conséquence assumée :** le raccourci TEST SVG de l'écran d'accueil exige désormais une confirmation supplémentaire. La petite bulle Jarvis n'en exige pas.

## Changements volontairement non importés

- Le défilement/activation Calques de 1.2.85 : les gestes et la lecture du panneau ne sont pas encore validés sur Samsung.
- Les anciens essais `ACTION_CLEAR_SELECTION`, coordonnées de désélection et menus de diagnostic intermédiaires.
- Les autres variantes de workflows et ressources des PR empilées : ne pas copier un ensemble de fichiers sans dépendances, vérification des régressions et comparaison exacte avec main.

## Portes de qualité avant consolidation

1. Exécuter les tests JVM, compiler APK release, contrôler `applicationId` et **empreinte du certificat public** attendue (pas uniquement `apksigner verify`).
2. Tester sur Samsung les deux entrées : raccourci public = confirmation avant envoi, petite bulle privée = un toucher.
3. Tester Gmail/étiquette Jarvis SVG, anti-doublons, SVG valide/invalide, Conception et Dégrouper.
4. Pour Calques, confirmer une ouverture réelle après glissement et action, puis importer un diff **minimal** avec ses dépendances et tests dans une branche propre dérivée de cette base.
5. Attribuer un `versionCode` nouveau et cohérent uniquement lorsque le lot intégrable est défini.
6. Après tests, examiner chaque PR expérimentale et fermer les demandes obsolètes en conservant l'historique, **sans supprimer de branche ni de preuve** avant décision.

## Périmètre machine

Aucun changement de puissance, vitesse, passes, EasySet ni déclenchement du laser.
