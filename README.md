# JARVIS — DÉPÔT OFFICIEL (Android + lanceur)

> **SOURCE UNIQUE DE VÉRITÉ : ce dépôt, branche `main`.**
> Le dépôt **Jarvis-Bridge-Android** est un prototype historique **0.7**, et ne doit jamais servir à construire une nouvelle version Jarvis.
> Aucune nouvelle application n'est compilée depuis l'ancien dépôt.

## État de référence au 10 novembre 2026

| Élément | Référence | Statut |
| --- | --- | --- |
| Code Android actuel | `android/` sur `main` | **1.2.31** (versionCode **50**), base pour la suite |
| Snapshot exact du build vérifié | `snapshot/jarvis-1.2.31-build-verified` | Commit `6c7a3fd3767e134cefb2d2598895d963edc78810` |
| Dernier APK 1.2.31 compilé et signé | [GitHub Actions #42800834402](https://github.com/gauthiermail2021-dot/Jarvis-Launcher/actions/runs/42800834402) | Compilation et tests JVM **réussis** ; artefact **temporaire** |
| OCR EasySet « Passer » | ML Kit | Diagnostic incomplet sur téléphone ; **ne pas présumer qu'il reconnaît « 1 »** |
| Version antérieure éprouvée dans les essais | `feature/jarvis-rollback-stable-1.2.7` | Préservée ; les versions plus récentes ne sont pas présumées stables |
| Ancien état du site `main` | `archive/pre-canonical-main-2026-11-10` | Préservé, aucun historique supprimé |
| Projet historique 0.7 | [Jarvis-Bridge-Android](https://github.com/gauthiermail2021-dot/Jarvis-Bridge-Android) | **ARCHIVE / NON COMPILABLE EN PRODUCTION** |

## Règles non négociables

1. **Toujours inspecter les deux dépôts et les branches récentes** avant de modifier un projet Jarvis ; seule cette branche `main` fait foi pour le code Android actuel.
2. Toute nouveauté se fait dans une branche `feature/` ou `experiment/` **issue de `main`** ; la branche de travail ne devient pas automatiquement une version officielle.
3. Ne jamais repartir de 0.7 ni d'un vieux ZIP/APK pour reconstruire une version plus récente.
4. Conserver l'identité Android `ca.jarvis.bridge`, la signature de release permanente et les réglages OAuth Gmail ; ne jamais mettre de secret, clé ou mot de passe dans GitHub.
5. Produire un **APK signé** seulement après compilation, tests automatiques, contrôle de signature et vérification des fonctions existantes ; ne pas appeler « stable » une compilation réussie seule.
6. Avant mise à jour : vérifier l'ouverture Gmail → SVG → xTool, la bulle, les sons, l'accès Android, les sessions de capture, l'interprétation EasySet et l'absence de toute commande laser.
7. Chaque version distribuée doit avoir un commit, un `versionCode` croissant, un APK en sauvegarde durable, un état de test et une procédure de retour arrière.
8. Les essais Tesseract démarrés à tort dans le dépôt historique restent en quarantaine. Les nouveaux essais doivent partir de la vraie 1.2.31 ici.

## Répertoires

- `android/app/src/main/java/ca/jarvis/bridge/` : code de l'application réelle.
- `android/app/src/test/` : tests JVM.
- `android/app/build.gradle` : version, identifiant Android et dépendances.
- `.github/workflows/build-jarvis-android.yml` : compilation/signature Android, **sur branches autorisées seulement**.
- `index.html` : page du lanceur existant (GitHub Pages) ; distincte du logiciel Android.
- `docs/SOURCE_OF_TRUTH.md` : protocole de travail et points à protéger.

---

## Historique fonctionnel de la 1.2.31 (conservé tel quel)

# Jarvis Bridge 1.2.31 — Diagnostic ciblé OCR « Passer »

Branche expérimentale distincte de 1.2.30. La branche stable 1.2.21 est inchangée, ainsi que les fonctions Gmail → SVG → xTool.

## Constat

La version 1.2.30 lit correctement les deux réglages **Puissance** et **Vitesse**, mais indique toujours **« Passages : non lus »** lorsque xTool montre **Passer : 1**. La reconnaissance Samsung Gallery lit le chiffre, mais cela ne prouve pas que Google ML Kit le reconnaisse.

## Diagnostic affiché (sans supposition)

Une analyse manuelle depuis la bulle → **« Analyser EasySet maintenant »** produit le résultat habituel suivi de :

- **Libellé :** si « Passer » est réellement reconnu.
- **Écran :** courts caractères lus à droite du libellé pendant l'OCR de l'écran complet, et si le nombre est associé.
- **Colonne droite :** résultats de l'OCR agrandi à droite, ou pourquoi cette analyse n'a pas été exécutée.
- **Zone agrandie :** caractères reconnus dans le recadrage « Passer » contrasté, ou pourquoi ce recours n'a pas été effectué.
- **Association :** résultat accepté ou rejeté, avec une cause classée : aucun chiffre, lettre/flèche reconnue plutôt qu'un entier, ou chiffre visible non validé à cause du format ou de l'association géométrique.

Seuls de courts tokens composés de chiffres, signes de navigation et lettres pouvant être confondues avec « 1 » sont affichés. **Aucune copie du texte intégral de l'écran** n'est fournie par le diagnostic. Capture et textes restent en mémoire, le résultat détaillé n'est pas sauvegardé. Le panneau reste affiché au maximum 40 secondes pour laisser le temps de prendre une capture d'écran.

Cette version **ne change pas** la reconnaissance des nombres dans EasySet : elle vise à diagnostiquer pourquoi « Passer » échoue, pas à forcer 1.

## Essai Android

1. Installer Jarvis 1.2.31 signé sur le Samsung.
2. Dans xTool, afficher EasySet avec les trois valeurs visibles.
3. Appui long sur la bulle Jarvis → « Analyser EasySet maintenant », dans la session de partage d'écran déjà autorisée.
4. Faire une capture d'écran du résultat, incluant le nouveau **DIAGNOSTIC PASSER (temporaire)**, et transmettre cette capture pour identifier l'étape précise du blocage.

## Limites

- Ne modifie aucun paramètre xTool et ne commande pas la machine ou le laser.
- Aucun nouveau consentement Android tant que la même session de capture reste active.
- Les tests automatiques couvrent les caractères « 1 », « l », « › », la géométrie et le filtrage des contenus hors zone. La lecture réelle demeure à valider sur téléphone.
