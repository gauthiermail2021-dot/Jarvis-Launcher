# Jarvis 1.2.40 — animations Gmail et SVG visibles pendant 500 ms

## Retour utilisateur du 10 novembre 2026

L'utilisateur confirme que la réception du courriel SVG et la demande d'ouverture xTool produisent des animations dans la bulle Jarvis, mais elles passent trop rapidement pour être lues. Demande : **un délai de 500 ms pour les voir**.

## Modification

- Les états **réellement observés** par Jarvis (notification Gmail, recherche Gmail, réception du SVG, demande d'ouverture xTool) sont mis dans une **file FIFO uniquement visuelle** dans `JarvisBubbleService`.
- Chaque état affiché reste **lisible au moins 500 ms** avant de passer au suivant. Le premier bénéficie de 500 ms supplémentaires pour laisser se terminer l'animation d'agrandissement (54 à 110 dp). L'agrandissement animé dure dorénavant 500 ms au lieu de 390 ms.
- Le **travail sous-jacent** (Gmail, vérification SHA-256, ouverture Android de xTool et OCR EasySet) continue sans attendre cette file : aucun délai réseau et aucun bouton de lancement machine.
- Les événements sont dédupliqués s'ils concernent le même état et la file est bornée à sept étapes, pour éviter des animations sans fin en cas de notifications répétées.
- Un événement reçu **avant** l'activation de la session xTool garde ses 500 ms de visibilité même si xTool est maintenant actif. Une notification Gmail arrivant **après** le début d'une session protégée n'interrompt pas le statut xTool « EN COURS ».
- La bulle reste agrandie pour 5 secondes après la dernière étape, comme auparavant. La petite bulle SVG flottante et l'envoi Gmail silencieux 1.2.39 sont conservés.
- Dans le menu Jarvis, **Animations : désactivées** garde un affichage immédiat sans aucun délai de mise en scène. Les événements ne sont jamais inventés pour embellir l'interface.
- Aucun changement de réglages xTool, pas de modification de dessin ni de commande laser.

## Tests

- `BubbleVisualStageTimingTest` vérifie 500 ms après agrandissement, 1000 ms pour la toute première étape (dont 500 ms d'ouverture), et 0 ms d'attente si les animations sont désactivées.
- Compilation signée, tests Java et vérification APK en GitHub Actions.
- Validation visuelle sur Samsung à faire : un appui sur la bulle SVG puis voir une progression Gmail → Réception SVG → xTool ; les séquences peuvent varier selon les événements effectivement détectés.

Version Android : **1.2.40**, `versionCode 59`, base exacte : `snapshot/jarvis-1.2.39-signed-silent-panel-2026-11-10`. Branche de test : `feature/visible-animation-hold-500ms-1.2.40`.
