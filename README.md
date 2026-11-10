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
