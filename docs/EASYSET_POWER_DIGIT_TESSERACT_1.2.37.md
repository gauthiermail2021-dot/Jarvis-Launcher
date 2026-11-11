# Jarvis 1.2.37 — OCR ciblé « Puissance : 1 % »

## Retour Samsung du 10 novembre 2026

L'utilisateur a fourni une capture de la version 1.2.36 montrant **la nouvelle bulle flottante SVG correctement visible** au-dessus de xTool Creative Space. Le volet EasySet indique **Puissance = 1 %**, **Vitesse = 20 mm/s**, **Passer = 1**. Le panneau de diagnostic Jarvis confirme **Puissance : non lue**, **Vitesse : 20 mm/s**, **Passages : 1** après OCR Tesseract « Passer » avec confiance affichée de 95 %.

Ce screenshot prouve l'**affichage de la seconde bulle SVG**, pas nécessairement que son clic a déclenché un nouvel envoi et une nouvelle ouverture xTool. L'envoi et le chemin Gmail → xTool restent à confirmer séparément.

## Cause

Le fallback Tesseract4Android de la version 1.2.36 est spécifique à « Passer ». Les chiffres isolés à une seule barre du champ « Puissance (%) », malgré leur lisibilité par un humain, peuvent ne pas apparaître dans ML Kit et restaient « non lue ».

## Correctif expérimental 1.2.37

- Le nouveau `EasySetPowerRegion` trouve l'étiquette réelle « Puissance » dans la capture d'écran consentie et recadre uniquement le **champ numérique à droite, sur la ligne du curseur sous le libellé**, sans inclure la flèche de navigation ni le curseur vert.
- Cette zone est petite (sur Galaxy Note20 Ultra, typiquement moins de 100 × 80 pixels), nettement séparée de la ligne « Vitesse ». Elle n'existe pas sans libellé Puissance reconnu ni si la ligne suivante serait chevauchée.
- Le recadrage se fait **avant** que le Bitmap original soit recyclé à la fin de la reconnaissance plein écran.
- La méthode `EasySetPassesTesseract.readPower` réutilise les mêmes étapes de seuillage, zoom ×6, fond blanc, liste de chiffres autorisés et comparaison des lectures Tesseract, avec validation numérique **0 à 100** uniquement pour Puissance, et **1 à 99** inchangé pour Passer.
- Le fallback est exécuté sur un worker hors thread UI **uniquement si Puissance reste « non lue » après ML Kit**, que Passer soit déjà lu ou non. Une valeur déjà reconnue par ML Kit n'est jamais écrasée.
- Une lecture absente, hors zone ou contradictoire laisse **« Puissance : non lue »**. Résultat toujours indicatif. Aucune interaction, modification de réglage ni commande laser n'est effectuée.
- Le diagnostic final affiche aussi la confiance du Tesseract Puissance lorsqu'il a été utilisé; le diagnostic Passer est conservé.

## Tests et statut

- Tests JVM ajoutés dans `EasySetPowerRegionTest` : capture 1 %, sous-la-ligne, crop de taille bornée, absence de libellé, exclusion de la ligne Vitesse et conservation du parcours OCR existant.
- Version Android `1.2.37`, `versionCode 56`, paquet `ca.jarvis.bridge`.
- Branche de travail `fix/easyset-power-small-digit-1.2.37` isolée de la version 1.2.36 déjà fonctionnelle pour la bulle SVG.
- **À confirmer sur le Samsung** : lecture réelle de « Puissance : 1 % » avec ML Kit ou Tesseract et absence de régression sur « Passer : 1 », « Vitesse : 20 ».
