# Jarvis — source officielle et protocole de validation

## Correction 1.2.83 — session SVG terminée mais xTool reste ouvert

- **Retour Samsung sur 1.2.82 :** « Session xTool terminée. Aucun geste. » en appuyant manuellement sur « Défiler et ouvrir Calque ». Le verrou provenait de `XtoolSessionManager.isActive()`, état de transfert SVG qui ne prouve pas si xTool est ouvert à l'écran.
- **Changement :** la navigation manuelle explicite peut fonctionner quand le SVG session flag est faux **à condition que la fenêtre active soit réellement `com.makeblock.xcs`** et que le contrôle Calque ou un outil de la barre soit identifié. Le parcours automatique garde l'obligation de session.
- Le dernier toucher nommé Calque suit la même règle; il n'est jamais autorisé sur une autre application. Aucune réactivation de session persistante ni modification du suivi Gmail.
- **Autre correction :** la présence du seul libellé « Conception » en bas ne prouve pas la présence d'une feuille contextuelle ; seul un menu Dégrouper accessible et visible bloque désormais le glissement. Aucun clic de désélection hasardeux.
- Test unitaire ajouté pour distinguer manuel/automatique/fenêtre étrangère. Build et test Samsung nécessaires avant de considérer le comportement résolu.


## Candidate 1.2.82 — barre inférieure xTool (21 novembre 2026)

- Base : branche `feature/1.2.81-calques-named-tap` (version candidate 1.2.81), non promue à `main`.
- Vidéo Samsung 9,5 s : après désélection manuelle, le panneau « Conception » se ferme; la barre d'outils inférieure défile horizontalement vers la gauche, dévoilant « Calque », qui ouvre ensuite la liste bleu/vert/rouge.
- Nouvelle action ponctuelle dans la bulle : « Défiler et ouvrir Calque (1 fois) ». La séquence post-SVG tente également la navigation automatiquement pendant une fenêtre finie, **uniquement quand le volet Conception a disparu**.
- Un glissement part d'un libellé visible et identifié de la barre inférieure (« Forme », « Graffiti », « Toile », « Image » ou « Texte ») ou d'un « Calque » partiellement visible à droite. Deux glissements maximum, puis toucher du seul libellé Calque entièrement visible. Arrêt sur anomalie.
- **Limite actuelle :** la désélection automatique des formes n'est PAS implémentée; ACTION_CLEAR_SELECTION sur la racine a été refusée sur Samsung et les touches aveugles ont déjà causé une régression. Si le dessin reste sélectionné, Jarvis attend que l'utilisateur le désélectionne lui-même.
- Aucune écriture EasySet, aucun déclenchement du laser. Envoi d'un geste Android ≠ confirmation du panneau ouvert. Test réel avec un nouvel APK signé requis. `main` inchangée.


État établi : **13 novembre 2026**.

## Dépôt unique et application stable

- **Dépôt officiel :** `gauthiermail2021-dot/Jarvis-Launcher`.
- **Nom de l'application :** Jarvis Bridge ; `applicationId` **`ca.jarvis.bridge`**. Ne pas changer l'identité pour renommer le projet.
- **Lanceur Web :** `index.html`, conservé séparément de l'application Android.
- **Ancien dépôt :** `gauthiermail2021-dot/Jarvis-Bridge-Android`, désormais **archivé** ; aucun code, workflow ou APK n'y fait foi pour une version actuelle.

## Branche de référence et versions

| Référence | Version | Statut |
| --- | --- | --- |
| **`main`** | **1.2.69**, `versionCode 88` | **Référence fonctionnelle testée sur Samsung** ; Gmail/SVG, Conception et Dégrouper fonctionnent ; Calques reste à corriger |
| [`fix/layers-dismiss-after-ungroup-1.2.69`](../../tree/fix/layers-dismiss-after-ungroup-1.2.69) | 1.2.69 | Branche historique précise du code promu vers `main`, à conserver comme sauvegarde |
| [`fix/audit-stability-1.2.70`](../../tree/fix/audit-stability-1.2.70) | 1.2.70, `versionCode 89` | Ressources d'accessibilité nettoyées, modèle OCR épinglé. Build + tests JVM + signature validés. **Test téléphone non confirmé** |
| `snapshot/jarvis-1.2.31-build-verified` | 1.2.31 | Archive historique de restauration, non utilisable comme nouvelle base |

La branche `main` conserve l'histoire du lanceur et des versions précédentes. La source Android actuelle a été remplacée intégralement par l'arbre exact de 1.2.69, sans copier d'anciennes classes isolées.

## Contrôles indispensables

1. Vérifier branche, SHA, `versionName`, `versionCode` et tâches CI avant la compilation.
2. Brancher les corrections futures depuis la dernière base **réellement approuvée sur Samsung** ; comparer la branche de maintenance 1.2.70 et sélectionner uniquement ses changements testés.
3. Maintenir les tests pour la validation SVG, les recherches Gmail, la protection anti-doublons, la session xTool, la bulle, les permissions et l'OCR.
4. Exécuter `testReleaseUnitTest assembleRelease`, confirmer les métadonnées d'APK, le certificat de signature et la présence des assets OCR hors ligne.
5. Avant d'appeler un APK « stable », le tester sur appareil réel : SVG Gmail, SVG depuis la bulle, Conception, Dégrouper, Calques, notification unique, bulles, sons et EasySet.
6. Les commandes du laser et ses paramètres sont exclusivement sous contrôle humain.

## Publication et retour arrière

- Toute nouveauté arrive sur une branche, en PR, avec un diff vérifié. Ne pas fusionner des fonctionnalités non vérifiées vers `main`.
- `.github/workflows/build-jarvis-android.yml` vérifie l'APK sur `main` sans le publier comme mise à jour officielle.
- Les artefacts GitHub Actions sont **expirables**. Pour une vraie release : conserver le SHA du commit, un APK signé durable, le résultat des tests, le hash du certificat public et une procédure de restauration.
- Ne jamais enregistrer de keystore, mot de passe ou jeton OAuth dans GitHub.
- Les branches historiques et les anciennes versions restent dans l'historique Git ; aucun effacement de données locales Android ne doit être nécessaire pour consolider la branche.
