# Jarvis Bridge 1.0.5 — diagnostic Gmail

**Compilation explicitement demandée le 8 novembre 2026.**

Version Android de test séparée `ca.jarvis.bridge.preview105`, pour préserver les APK installés.

- Correction inter-versions : ne jamais renvoyer un SVG à un ancien Jarvis Bridge.
- Menus et bulle inchangés : appui long 1 s; heure uniquement en petite bulle; statut sans heure en grande bulle; retour après 5 s.
- Détection Gmail : notification « Toucher pour ouvrir » → compte Google → recherche du dernier courriel non lu marqué `Jarvis SVG` → ouverture SVG xTool si le système prend en charge le fichier.
- Diagnostic renforcé : affichage durable du statut après le retour du sélecteur Google, erreurs explicites lorsque l'accès Gmail ou le lancement du lecteur SVG échoue.
- **Le choix du compte pourrait encore être nécessaire** : la cause du problème initial n'est pas confirmée et le diagnostic vise à l'identifier, pas à garantir sa correction.
- Si le traitement ne parvient pas jusqu'à xTool, vérifier précisément le message d'état sous le bouton « Arrêter la bulle ».

Ne pas désinstaller la version 0.7 fonctionnelle avant d'avoir confirmé le fonctionnement de la 1.0.5. N'autoriser qu'une version à lire les notifications Gmail pendant le test.

La récupération entièrement automatique sans toucher la notification n'est pas encore disponible.

