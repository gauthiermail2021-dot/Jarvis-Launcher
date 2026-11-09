# Jarvis Bridge Android 1.2.9 — Stabilisation

Application Android pour récupérer des dessins SVG depuis Gmail et demander leur ouverture dans xTool. L'APK 1.2.8 a été testé avec succès sur Samsung Android 13, avec la fenêtre fermée et la bulle visible. La 1.2.9 renforce le code; elle doit être testée sur l'appareil avant de remplacer la version de référence.

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
