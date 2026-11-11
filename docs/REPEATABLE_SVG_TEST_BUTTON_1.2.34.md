# Jarvis 1.2.34 — icône externe « TEST SVG JARVIS »

## Demande

Un bouton Android indépendant de ChatGPT (deuxième icône dans le lanceur Samsung) qui envoie **le même SVG** de test à l'adresse du compte Gmail autorisé. À chaque clic **réussi**, le nom augmente de 1 : `Jarvis_Test_001.svg`, `Jarvis_Test_002.svg`, `Jarvis_Test_003.svg`, etc. Le compteur persiste dans les données de l'application Android.

## Dessin fixe

Géométrie **identique** au SVG de test envoyé en novembre 2026 :
- trapèze extérieur 44 × 30 mm ;
- deux trous ronds de rayon 2 mm ;
- losange central ;
- contours noirs, épaisseur 0,20 mm.

Un **commentaire XML invisible** contenant le numéro change à chaque envoi ; le hash SHA-256 change donc aussi. Cette mesure est importante : le traitement Gmail existant dans `MainActivity` refuse les SVG identiques aux précédents, même lorsque le nom change. Aucun élément visible ni réglage machine ne change.

## Architecture et sécurité

1. Nouvelle activité séparée `JarvisTestShortcutActivity` déclarée avec une icône `TEST SVG JARVIS` dans `AndroidManifest.xml`.
2. Premier clic : **autorisation Google explicite** d'accéder au scope `gmail.modify`. Ce scope permet l'envoi et l'application du libellé `Jarvis SVG`. Il est **plus large** que le scope Gmail `gmail.readonly` déjà utilisé par Jarvis ; l'utilisateur doit le consentir. Ne jamais obtenir de jeton Google en contournant le consentement.
3. Chaque clic : produire un nouveau SVG `JarvisRepeatableTestSvg.bytes(n)`, récupérer le compte Gmail connecté, valider le SVG avec `JarvisFileRouter`, envoyer un courriel à soi-même avec objet `JARVIS-SVG-AUTO - Bouton TEST #NNN`, appliquer le libellé `Jarvis SVG` au message, lancer l'ancien vérificateur Gmail.
4. Un compteur est incrémenté **après envoi confirmé** par Gmail, même en cas d'échec ultérieur de l'application du libellé.
5. Seuls le compteur et l'instant du dernier envoi sont persistés. Aucun jeton OAuth n'est enregistré. Les messages ne sont envoyés que sur déclenchement de l'icône Android et un maximum d'un envoi toutes les 5 secondes.
6. **Aucune commande xTool ni laser** dans ce bouton ; il se limite à l'envoi d'un fichier SVG de test.

## Prérequis et limites à vérifier

- Gmail `gmail.modify` peut déclencher une nouvelle demande d'autorisation ou une restriction Google pour une application non vérifiée. La réussite de la compilation Java **ne garantit pas** le succès OAuth.
- Gmail peut ne pas notifier automatiquement un message auto-envoyé à soi-même ; l'activité demande également à Jarvis de vérifier Gmail via l'action existante.
- Si la session xTool précédente est encore active, `XtoolSessionManager` peut protéger la session et empêcher l'importation d'un nouveau SVG. Le bouton envoie tout de même le courriel. Ne jamais fermer ni jeter automatiquement le dessin existant.
- Après la première autorisation réussie, les clics ultérieurs devraient être automatiques, tant que le jeton Google est accessible.
- Le numéro recommence à 001 si les **données de l'application** sont effacées ou si elle est entièrement désinstallée.
- Un même clic ne doit pas envoyer plusieurs courriels.
- La version **1.2.33** reste inchangée ; le développement 1.2.34 reste en branche `feature/repeatable-home-test-svg-1.2.34` jusqu'au test Samsung.

## Tests

- Vérifier le format et l'incrément du nom pour 1, 2, 10 et 1000.
- Vérifier que les octets SVG diffèrent mais que leur géométrie reste inchangée.
- Tester le validateur SVG existant et la compatibilité du préfixe de sujet avec `JarvisMailGate`.
- Compiler l'application et vérifier la signature persistante sur GitHub.
- Test réel sur Samsung : envoi #001, puis #002, puis #003 avec libellé Gmail ; confirmer que Jarvis reçoit le dernier SVG et ouvre xTool sans démarrer le laser.

