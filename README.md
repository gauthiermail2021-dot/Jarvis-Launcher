# Jarvis Bridge 1.0.3 — Correctif de boucle Android SVG

**Statut : source préparée, NON COMPILÉE.** Compilation uniquement sur commande explicite via GitHub Actions (`workflow_dispatch`).

Bogue reproduit conceptuellement : Android choisit Jarvis Bridge « Toujours » pour ouvrir un SVG; Jarvis importe le SVG puis relance un ACTION_VIEW implicite, que le système réachemine à Jarvis → boucle infinie.

Correctifs :
- Supprimer le filtre d'ouverture `ACTION_VIEW image/svg+xml` dans le manifeste, tout en gardant le partage `ACTION_SEND` et la réception Gmail.
- Lors de l'ouverture du SVG, consulter la liste des applications externes capables de le traiter, **exclure le propre package Jarvis Bridge**, et utiliser des intents explicites.
- Si xTool apparaît comme gestionnaire compatible, privilégier son application; sinon proposer un choix parmi les applications **externes seulement**.
- Si aucun gestionnaire externe n'est disponible, afficher une erreur au lieu de boucler.
- Conserver le délai d'appui long à 1,5 s, l'horloge au repos, les animations d'activité et le retour à 54 dp après 5 secondes.

Pour sortir de la boucle déjà installée en 1.0.2 : paramètres Android → Applications → Jarvis Bridge 1.0.2 Test → Forcer l'arrêt, puis effacer le choix « Toujours » pour les SVG ou désinstaller **uniquement** cette version de test.

`ca.jarvis.bridge.preview103` reste séparé des anciennes applications d'essai. Lien de conversation à reconfigurer pour ce package après compilation.

Remarque : xTool doit réellement exposer une activité Android capable de gérer ACTION_VIEW sur un URI `content://` SVG. L'ouverture directe reste à valider sur le téléphone.
