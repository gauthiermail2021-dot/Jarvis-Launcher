## 1.2.19 — Trois sons discrets et distincts

- **Miniature :** petit tic électronique original de 60 ms au toucher.
- **Ouverture :** whoosh montant discret de 250 ms, joué 85 ms après l'ouverture ; distinct du tic.
- **Menu :** bip de confirmation distinct de 90 ms après sélection d'un bouton, sans bruit pendant le déplacement.
- Les trois nouveaux échantillons WAV sont synthétisés pendant la compilation, sans échantillon externe.
- Tous les nouveaux sons respectent le réglage existant **« Son »**. L'animation, la réception des SVG, xTool et les corrections Gmail restent inchangés.

## 1.2.18 — Ouverture plus énergique

- Animation d'ouverture plus dynamique : expansion rapide de **54 à 110 dp**, petit rebond interne **110 → 103 → 110 dp** en 390 ms (sans agrandir la fenêtre au-delà de son format final).
- **Deux tours lumineux rapides** autour de la bulle en 510 ms, avec un éclaircissement bref du contour en fin d'ouverture.
- L'effet respecte « Animations : désactivées ». La bulle fermée et son apparence finale sont inchangées.
- Aucune modification de la réception des SVG, des sons, de la gestion des sessions xTool, ni du filtrage des notifications Gmail établi en 1.2.17.

## 1.2.17 — Ouverture animée et notifications ciblées

- Agrandissement fluide de 54 à 110 dp en **280 ms**, avec décélération douce, au toucher ou lors d'un événement SVG.
- Un **point lumineux effectue une rotation complète du cadre (560 ms)** à chaque ouverture, indépendamment de l'animation déjà utilisée à la réception des SVG.
- Si d'autres événements arrivent pendant l'ouverture, l'animation continue sans être relancée. L'option **« Animations : désactivées »** supprime les deux effets d'ouverture.
- Les notifications groupées de Gmail ne sont plus prises pour des messages nouveaux.
- **Plus d'alerte Jarvis manuelle pour chaque courriel Gmail ordinaire** : le repli manuel ne s'affiche que si la notification évoque clairement Jarvis ou un SVG. Les vérifications automatiques via le libellé Gmail continuent, sans filtre risquant de perdre un SVG.
- La notification Android permanente « Bulle flottante active » reste nécessaire au service en arrière-plan, mais n'est plus republiée à chaque redémarrage de l'activité.
- Bulle au repos inchangée : couleurs, cadre, dimensions finales, textes, sons et sessions xTool conservés.

## 1.2.16 — Fenêtre noire derrière la bulle

- **Correctif ciblé Android :** les activités temporaires utilisées pour vérifier Gmail ou démarrer uniquement la bulle n'affichent plus de vue couvrant l'écran.
- Leur fond est explicitement transparent et aucun assombrissement de la fenêtre n'est demandé.
- Le lancement compact ferme son activité après avoir démarré le service flottant, au lieu de conserver une fenêtre vide dans la pile Android via `moveTaskToBack(true)`.
- Les vérifications Gmail réutilisant le lancement compact se terminent elles aussi, après le traitement.
- Les nouvelles notifications ne relancent pas une vérification Gmail quand une session xTool est déjà active.
- **Aucun changement de la bulle elle-même**, des couleurs, de la taille, des animations, des messages, des sons, de l'importation SVG ni du verrouillage de session.
- À vérifier sur Samsung : après ouverture d'un SVG, la bulle reste visible et il n'y a plus de fond noir plein écran.

## 1.2.15 — Son unique au refus d'un fichier

- **Un seul son électronique bref et grave** (220 Hz, durée 250 ms, saturation douce), généré localement pendant la compilation, sans téléchargement.
- Déclenché uniquement à la réception d'un fichier refusé (« REFUSÉ »), une fois par événement; le registre Gmail de la 1.2.11 empêche les répétitions liées au même message.
- L'option « Son : désactivé » supprime aussi le son de refus.
- **Aucun changement visuel** : la bulle, son menu, ses couleurs, sa taille, ses animations et ses messages sont inchangés.
- Les quatre sons habituels du xylophone ne changent pas. Aucun bouton de test supplémentaire.

## 1.2.14 — Menu de la bulle simplifié

- Renommage de « Xylophone : activé/désactivé » en **« Son : activé/désactivé »**.
- Suppression complète du bouton **« Tester les 4 notes »**.
- Les sons de notification, les réglages audio existants et le mode session xTool sont inchangés.

## 1.2.13 — Correction : xTool lancé via l'association Android

- **Défaut confirmé en 1.2.12 :** sur certains téléphones Samsung, xTool reçoit bien le SVG par l'action Android standard, mais le résolveur ne porte pas le nom « xTool ». La session ne s'activait pas, et la bulle n'affichait pas « xTool EN COURS ».
- **Correctif :** après tout envoi SVG réussi depuis Jarvis, **que l'activité xTool ait été trouvée directement ou qu'Android utilise son association par défaut**, Jarvis démarre la session protégée et affiche « xTool / EN COURS » dans la bulle.
- **Menu :** l'appui long propose « Terminer la session xTool »; déplacer la bulle n'achève jamais la session.
- **Diagnostic :** le statut Jarvis précise si le SVG a été envoyé directement à xTool ou via l'association Android.
- **Limite :** Android ne permet pas de certifier que l'importation a réussi dans xTool. Si l'association SVG du téléphone est changée vers une autre application, Jarvis protègera quand même la session après une ouverture réussie; il faudra alors terminer la session par le menu.

## 1.2.12 — Mode session xTool protégée

- **Un seul SVG ouvre la session :** après l'envoi réussi vers xTool, Jarvis mémorise une session active sur le téléphone. Cette session reste active même si Android redémarre l'activité ou la bulle.
- **Affichage de la bulle :** petite ou agrandie, elle indique **xTool / EN COURS**. Ce statut signifie que Jarvis protège la session du dessin, et non que la machine laser travaille réellement.
- **Notifications pendant la session :** le filtre Gmail n'analyse aucun nouveau fichier. Aucune nouvelle ouverture xTool ne peut interrompre le dessin actif, même en cas de notification répétée.
- **Gestes :** glisser la bulle ne termine rien; un appui simple agrandit la bulle; un appui long ouvre le menu.
- **Terminer :** dans le menu, toucher **Terminer la session xTool**. Cela réarme Jarvis pour les futurs courriels sans fermer xTool. Les courriels reçus pendant la session restent ignorés après le réarmement (curseur temporel Gmail avancé à la fin de la session).
- **Limite :** la fermeture réelle de xTool n'est pas détectée automatiquement; Android ne garantit pas la différence entre fermeture et arrière-plan. Le menu manuel reste la confirmation fiable.
- **Appels externes :** l'envoi d'un SVG depuis une autre application ou un lien HTTPS est également bloqué pendant la session.

## 1.2.11 — Traitement unique par message Gmail
- Gmail peut générer plusieurs notifications pour un seul courriel. Les notifications déclenchent une vérification discrète; **elles n'affichent plus la bulle tant qu'un nouveau fichier n'a pas été confirmé**.
- Les fichiers CAD/3D, DXF, inconnus et SVG invalides sont **refusés une seule fois**, puis leur identifiant Gmail et leur horodatage sont enregistrés comme déjà traités. Une notification suivante ne réaffiche pas le refus.
- Les refus de format sont distingués des véritables erreurs transitoires (réseau, authentification) afin de ne pas empêcher la reprise d'une connexion interrompue.
- La bulle affiche **CAD / REFUSÉ**, **DXF / REFUSÉ** ou **FICHIER / REFUSÉ** selon le cas.
- Seuls les messages admissibles avec une nouvelle pièce jointe déclenchent l'animation; l'ouverture SVG de la 1.2.10 est conservée.
- Les pièces jointes STEP, STL et autres formats non SVG sont maintenant identifiées et refusées explicitement au lieu d'être ignorées.

# Jarvis Bridge Android 1.2.16 — Contrôle des formats

Application Android pour récupérer des dessins SVG depuis Gmail et demander leur ouverture dans xTool. L'APK 1.2.8 a été testé avec succès sur Samsung Android 13, avec la fenêtre fermée et la bulle visible. La 1.2.9 renforce le code; elle doit être testée sur l'appareil avant de remplacer la version de référence.

## Routage de fichiers ajouté en 1.2.10
- **SVG 2D valide** : chemin Gmail / fichier Android / HTTPS vers xTool P2S, inchangé.
- **STL, STEP, STP, SLDPRT, OBJ, 3MF, IGES et autres fichiers CAD 3D** : refus explicite, aucun lancement de xTool; exporter une projection 2D en SVG depuis SolidWorks pour une découpe laser.
- **DXF 2D** : refus du transfert automatique actuel (qui est réservé au SVG). Import DXF manuel dans xTool ou conversion préalable en SVG.
- **Fichier inconnu ou SVG invalide** : refus. Les contenus trompeurs (par exemple STEP nommé .svg) sont bloqués.
- La fenêtre des réglages affiche la **vraie version Android** et le démarrage de la bulle n'efface plus le dernier état de diagnostic.
- **Limite :** ce contrôle ne peut pas modifier la gestion interne des fichiers ni le choix de machine dans xTool. Jarvis sait demander l'ouverture mais ne peut pas confirmer que le dessin a bien été importé. Le routage automatique de CAD 3D vers SolidWorks ou Raise3D n'est pas encore développé.

## Flux de travail
1. Un dessin SVG est envoyé depuis le compte Gmail connecté avec un sujet commençant par `JARVIS-SVG-AUTO`.
2. Le filtre Gmail applique l'étiquette `Jarvis SVG`. La pièce doit être un fichier `.svg`.
3. Une notification Gmail déclenche la recherche, sans vérifier périodiquement les courriels.
4. Jarvis recherche les messages sous cette étiquette, y compris ceux déjà marqués « lus », et retient le dessin autorisé le plus récent.
5. Jarvis vérifie que le message porte l'étiquette système Gmail `SENT`, contrôle le sujet et valide le contenu SVG avant de demander son ouverture dans xTool.

**Important :** l'ouverture de xTool n'est pas une commande de découpe, de gravure ou de démarrage du laser. Vérifier le dessin, le placement, le matériau, la ventilation et les paramètres avant toute utilisation physique.

## Garanties et limites
- Le mécanisme de déduplication conserve les identifiants Gmail, l'heure du dernier message traité et l'empreinte SHA-256 du dernier SVG.
- Les notifications arrivées pendant une recherche provoquent une nouvelle vérification après le traitement en cours.
- En cas de courriels envoyés très rapidement, **seul le dessin valide le plus récent** est ouvert automatiquement. Les fichiers antérieurs restent dans Gmail.
- Aucune lecture Gmail n'est exécutée de manière périodique. Android ou Gmail peuvent supprimer des notifications : si aucun nouvel événement n'arrive, un message peut rester en attente jusqu'à la prochaine notification ou jusqu'à l'utilisation du bouton « Ouvrir le dernier SVG Gmail ».
- L'ouverture totalement automatique dépend des autorisations de notifications et de superposition, de la présence de la bulle et des restrictions d'Android. Les mises à jour Android peuvent changer le comportement.
- Pour l'importation automatique, le SVG doit respecter le sous-ensemble vectoriel sûr : éléments graphiques usuels, sans script, image liée, DTD, entité XML externe ni référence de réseau. Il doit comporter des dimensions explicites bornées. Les SVG complexes peuvent être refusés et nécessiter une vérification manuelle.
- Le niveau Android cible est toujours 28 pour préserver la compatibilité de ce prototype; une migration vers les API modernes et de nouveaux essais sur appareils sont nécessaires avant publication grand public.

## Structure
- `MainActivity.java` : écran de paramètres, autorisation Gmail, récupération et transmission des SVG.
- `JarvisNotificationListener.java` : réception et regroupement des notifications Gmail.
- `JarvisBubbleService.java` : bulle flottante, statuts, interactions.
- `JarvisMailGate.java` : vérification des sujets et provenance.
- `SafeSvgValidator.java` : validation du profil SVG avant ouverture dans xTool.
- `android/app/src/test/` : tests JVM de sécurité et de vérification des courriels.

## Compilation
La branche de stabilisation est `feature/jarvis-bridge-stability-1.2.9`. Le workflow `Build and test Jarvis Bridge 1.2.9 - signed APK` exécute les tests, assemble la version `release`, vérifie sa signature et dépose l'APK signé dans les **Artifacts GitHub Actions**. Il n'essaye plus de publier un GitHub Release (l'API Releases retournait HTTP 403 dans les essais précédents).

Le certificat de signature permanent reste dans les secrets GitHub, jamais dans le dépôt. Une installation de mise à jour doit utiliser le même package `ca.jarvis.bridge`, le même certificat et un `versionCode` supérieur. Ne pas désinstaller 1.2.8 avant d'avoir validé 1.2.9.

Voir `android/GOOGLE_OAUTH_CONFIGURATION.md` pour les détails d'identité et de signature.
