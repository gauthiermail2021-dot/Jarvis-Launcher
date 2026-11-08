# Jarvis Bridge 1.0.4 — version test corrigée

**Compilation demandée explicitement le 8 novembre 2026.**

Version de test **1.0.4** (application Android distincte `ca.jarvis.bridge.preview104`) pour éviter un conflit de signature debug avec les anciennes versions et ne pas perturber la 0.7 validée. Les anciennes versions ne sont pas désinstallées automatiquement.

- Menu par appui long **1 seconde**.
- Bulle de 54 dp : heure seulement; bulle agrandie de 110 dp : statut sans heure; retour automatique après 5 secondes.
- Notification Gmail → « Toucher pour ouvrir » → récupération du dernier SVG non lu portant le libellé `Jarvis SVG` → demande d'ouverture dans xTool.
- Priorité à xTool si le système reconnaît son activité SVG; sinon sélection d'une application extérieure.
- Ignore **tous** les packages `ca.jarvis.bridge`, `ca.jarvis.bridge.*` et les activités nommées Jarvis Bridge, empêchant le renvoi accidentel d'un SVG à une autre version du prototype.
- Les statuts de la bulle correspondent à des opérations réellement lancées; « Ouverture xTool » signifie demande d'ouverture Android, pas confirmation d'import réussi.

**Attention** : l'ouverture xTool sans toucher à la notification n'est pas encore implémentée. Cette version doit être testée directement sur le téléphone et dans xTool avant de supprimer l'ancienne 0.7 fonctionnelle. Sur Samsung, n'autoriser qu'**une seule** version de Jarvis à lire les notifications Gmail pour éviter les notifications en double.

**Après validation sur 1.0.4**, supprimer les anciennes versions et ne conserver qu'une installation Jarvis Bridge. La signature debug est éphémère et le package de test change à chaque version : une future version stable devra recevoir une signature persistante.

**Commandes futures** : nouvelle compilation uniquement après autorisation explicite, via `workflow_dispatch`.

---

# Jarvis Bridge 1.0.3 — Correctif de boucle Android SVG

**Statut : compilation GitHub Actions réussie (run 42517609412). APK publié sous la version 1.0.3.** Compilation uniquement sur commande explicite via GitHub Actions (`workflow_dispatch`).

Bogue reproduit conceptuellement : Android choisit Jarvis Bridge « Toujours » pour ouvrir un SVG; Jarvis importe le SVG puis relance un ACTION_VIEW implicite, que le système réachemine à Jarvis → boucle infinie.

Correctifs :
- Supprimer le filtre d'ouverture `ACTION_VIEW image/svg+xml` dans le manifeste, tout en gardant le partage `ACTION_SEND` et la réception Gmail.
- Lors de l'ouverture du SVG, consulter la liste des applications externes capables de le traiter, **exclure le propre package Jarvis Bridge**, et utiliser des intents explicites.
- Si xTool apparaît comme gestionnaire compatible, privilégier son application; sinon proposer un choix parmi les applications **externes seulement**.
- Si aucun gestionnaire externe n'est disponible, afficher une erreur au lieu de boucler.
- Conserver le délai d'appui long à 1 s, l'horloge au repos, les animations d'activité et le retour à 54 dp après 5 secondes.

Pour sortir de la boucle déjà installée en 1.0.2 : paramètres Android → Applications → Jarvis Bridge 1.0.2 Test → Forcer l'arrêt, puis effacer le choix « Toujours » pour les SVG ou désinstaller **uniquement** cette version de test.

`ca.jarvis.bridge.preview103` reste séparé des anciennes applications d'essai. Lien de conversation à reconfigurer pour ce package après compilation.

Remarque : xTool doit réellement exposer une activité Android capable de gérer ACTION_VIEW sur un URI `content://` SVG. L'ouverture directe reste à valider sur le téléphone.

## Affichage de la bulle (modification non compilée)
- Mode réduit (54 dp) : heure locale seulement.
- Mode agrandi (110 dp) : titre et état de Jarvis, **sans horloge**; l'espace central sert à afficher les activités.
- Appui long de 1 seconde et retour automatique après 5 secondes conservés.

## Prochain objectif distinct (non encore implémenté)
- À la réception d'un courriel avec SVG, tenter la récupération et l'ouverture xTool sans le toucher sur la notification.
- Avant de promettre une ouverture entièrement silencieuse, vérifier les restrictions d'Android concernant le lancement d'une activité depuis l'arrière-plan et les autorisations OAuth Gmail. Prévoir une notification actionnable de secours.

## Améliorations ajoutées dans la version compilable
- Appui immobile de **1 seconde** pour afficher le menu de la bulle.
- Lors de la réception ou de la préparation d'un SVG : **RÉCEPTION / .SVG** dans la bulle agrandie.
- Quand Jarvis demande à Android de lancer xTool : **OUVERTURE / xTool** dans la bulle agrandie. Ce message n'est **pas** une confirmation que xTool a chargé le dessin.
- En cas d'autre application SVG externe : **SVG / ENVOYÉ**.
- L'heure ne s'affiche **qu'en mode réduit**.
- L'ouverture en boucle a été bloquée en excluant Jarvis Bridge lui-même des gestionnaires SVG externes.

**Limite de l'essai :** avec Jarvis Bridge en arrière-plan, l'utilisateur doit encore toucher la notification « Toucher pour ouvrir » avant la récupération Gmail. Une ouverture vraiment sans toucher nécessitera un mécanisme d'autorisation Gmail en arrière-plan et le traitement des restrictions Android de lancement d'activité. Ce comportement n'est pas annoncé comme déjà réalisé.

Téléchargement APK : https://github.com/gauthiermail2021-dot/Jarvis-Launcher/releases/download/jarvis-1.0.3-preview/Jarvis-Bridge-1.0.3-Test.apk

## Correctif supplémentaire après test simultané 0.7 / 1.0.3 (non compilé)
- Sur Android, chaque ancienne version de Jarvis Bridge peut apparaître comme une application SVG externe à la version 1.0.3.
- Le code filtre maintenant **tous** les packages de la famille `ca.jarvis.bridge` et les activités nommées « Jarvis Bridge », au lieu de seulement exclure le package courant.
- xTool demeure prioritaire s'il expose un gestionnaire Android SVG. En son absence, Jarvis demande un choix au lieu d'ouvrir arbitrairement une application.
- Le correctif est inclus dans le code destiné à la **1.0.4**; ne pas confondre avec l'ancien APK 1.0.3.

## Passage à une installation unique (procédure de test)

**IMPORTANT : la version 1.0.3 déjà publiée n'inclut pas le filtre des *autres* versions de Jarvis Bridge.** Ce filtre est présent dans les sources, en attente de la prochaine compilation demandée par l'utilisateur.

1. Conserver une copie installable de l'ancienne application 0.7 si possible : c'est la version Gmail → SVG → xTool déjà vérifiée.
2. Avant d'installer la prochaine APK corrigée, arrêter les anciennes versions et leur retirer « Accès aux notifications ».
3. Vérifier l'activation de l'accès aux notifications Gmail pour la seule version récente, son autorisation de superposition, sa configuration ChatGPT et l'autorisation Gmail OAuth.
4. **Sur la version corrigée**, tester un nouveau courriel non lu avec libellé `Jarvis SVG`; utiliser la notification « Toucher pour ouvrir ». Vérifier que xTool reçoit le SVG sans lancement d'ancienne version.
5. **Après ce test réussi uniquement**, désinstaller 0.7, 1.0.2 et les prototypes inutilisés et retirer les anciennes associations « Toujours ».
6. Ne pas lancer de nouvelle compilation avant la demande explicite « Compile ».

Ne pas considérer le simple fait de forcer l'arrêt de 0.7 comme une protection suffisante : si le code d'un autre APK la choisit comme gestionnaire SVG, Android peut la relancer.
