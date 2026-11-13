# Jarvis Bridge — contrôles de qualité avant une nouvelle version

Cette liste s'applique à chaque PR qui change l'application `ca.jarvis.bridge`.

## Traçabilité

- [ ] Dépôt : `gauthiermail2021-dot/Jarvis-Launcher`, branche exacte et SHA relevés.
- [ ] Base de départ : dernier commit **validé sur le Samsung**, jamais une ancienne archive.
- [ ] Comparaison des fichiers modifiés à la base validée ; aucun remplacement partiel de classes ni perte de fonctions antérieures.
- [ ] `versionCode` supérieur à la version APK à mettre à jour ; `versionName` et certificat de release cohérents.
- [ ] Changelog : fonctionnalités modifiées, différences d'UI, limites, état de test.

## Tests automatiques / chaîne de compilation

- [ ] `testReleaseUnitTest assembleRelease` réussi sur le commit **exact** à livrer.
- [ ] Tests de validation SVG, anti-doublons Gmail, ordre des sessions et ciblage précis des contrôles réussis.
- [ ] `apksigner verify` et identité `ca.jarvis.bridge` vérifiés.
- [ ] Les actifs OCR utilisés hors ligne sont épinglés à un commit connu.
- [ ] Secrets GitHub injectés dans le workflow seulement, jamais dans le dépôt ni l'artifact.
- [ ] Aucun ancien workflow de publication n'a été réactivé par inadvertance.

## Essais Samsung (validation humaine)

- [ ] Mise à jour sans désinstallation ni perte de données locales.
- [ ] SVG Gmail portant le libellé `Jarvis SVG` : une seule ouverture dans xTool.
- [ ] SVG depuis la petite bulle TEST : même comportement, sans doublon.
- [ ] Bulle, menu, sons, animations et passage entre applications inchangés.
- [ ] Onglet Conception réellement sélectionné dans xTool.
- [ ] Dégrouper appliqué au dessin, pas seulement toucher Android transmis.
- [ ] Calques visible et ouvert **si cette fonction est annoncée** ; sinon état explicitement « non confirmé ».
- [ ] Diagnostic EasySet en lecture seule et consentement Android conformes.
- [ ] Fin de session xTool : aucun ancien Gmail ne se réouvre ; absence de surveillance répétée.
- [ ] Aucun lancement de laser, réglage ou action machine autonome.

## Décision de publication

**Ne pas fusionner vers `main` une fonction nouvelle non validée sur appareil.** Une compilation signée réussie est une **candidate de test**, pas une preuve de stabilité. Conserver un APK de retour arrière et un commit identifié. Les artefacts Actions expirent ; archiver durablement les releases approuvées sans stocker de clé privée.
