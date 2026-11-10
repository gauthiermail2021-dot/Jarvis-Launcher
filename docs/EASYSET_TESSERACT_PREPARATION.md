# Tesseract 5 — travail préparatoire vers Jarvis 1.2.32

**BRANCHE EXPÉRIMENTALE NON INSTALLABLE EN VERSION OFFICIELLE.**

- Basée sur `main` du dépôt **Jarvis-Launcher** après la migration canonique du 10 novembre 2026.
- Base Android **1.2.31** inchangée : `versionCode 50`, package `ca.jarvis.bridge`. La prochaine vraie version 1.2.32 devra avoir un `versionCode` supérieur à 50.
- `EasySetPassesTesseract.java` a été récupéré de `Jarvis-Bridge-Android/feature/easyset-tesseract-staging` **sans reprendre le reste de la base obsolète 0.7**.
- Le module Tesseract est ajouté à la compilation mais **n'est volontairement pas encore raccordé au diagnostic EasySet**. Un APK compilé ne prouve donc pas que la lecture du chiffre est corrigée.
- Le build spécial `build-tesseract-staging.yml` compile uniquement la variante **debug** `ca.jarvis.bridge.dev`. Il ne publie aucun APK, n'utilise pas de clé privée release et ne modifie pas l'app installée.

## Étapes restantes pour rendre une future 1.2.32 réelle

1. Cadrer avec précision la case **numérique** à droite du libellé `Passer` avec la géométrie de la capture originale. L'ancienne classe `EasySetPassesRegion` renvoie actuellement **toute la ligne**, ce qui est trop large pour le module de lecture chiffre seul.
2. Brancher `EasySetPassesTesseract.read` en repli sur l'algorithme réel `EasySetVisionAnalyzer` uniquement lorsque « Passages : non lus » ; la lecture doit s'exécuter hors du fil d'interface, tout en conservant le bitmap le temps nécessaire et en recyclant le résultat.
3. Ne jamais accepter un nombre absent, hors zone ou contradictoire avec l'observation ML Kit, et ne jamais forcer `1`.
4. Ajouter tests sur des captures d'écran xTool représentatives, en conservant le caractère éphémère des informations OCR.
5. Tests de non-régression : Gmail → SVG → xTool, bulle, sons, sessions de capture et signature release. Aucun démarrage laser.
6. Quand tout est validé : `versionCode 51`, `versionName '1.2.32'`, compilation release signée, conservation durable de l'APK et des notes de test.

## Source de vérité
`main` du dépôt **Jarvis-Launcher**. Aucune modification fonctionnelle n'a été faite sur l'ancienne base 0.7 durant cette migration du module.


## Test réel du 10 novembre 2026 — capture fournie par l'utilisateur

- Capture xTool EasySet de **869 × 1536 pixels**, avec les champs visibles **Puissance 69 %**, **Vitesse 152 mm/s** et **Passer 1**.
- Le panneau diagnostic **Jarvis 1.2.31** indique : `Libellé : repéré`, `Écran : aucun petit caractère détecté à droite`, `Colonne droite : aucun petit caractère détecté à droite`, `Zone agrandie : aucun petit caractère détecté à droite`, puis `Passages : non lus`.
- Cela confirme que l'échec actuel est **en amont de l'association** : aucun chiffre n'est renvoyé par les trois étapes ML Kit, malgré la présence visible du `1`.
- Analyse locale séparée avec **Tesseract CLI 5.5** : le chiffre `1` est reconnu lorsque la zone numérique droite est recadrée jusqu'aux traits noirs (environ 7 × 28 pixels dans cette capture), puis agrandie ×6 avec une bordure blanche d'environ 12 pixels avant agrandissement, avec segmentation `PSM_SINGLE_WORD`. Une tentative locale avait une confiance d'environ **67 %**. Les cadrages plus larges ou `PSM_SINGLE_CHAR` échouent fréquemment.
- **Ce test local ne valide pas Android** : le modèle de langue, la version Tesseract et les caractéristiques de capture peuvent différer.
- Correctif préparatoire : `EasySetPassesRegion.numberField()` cible le cadran numérique près du bord droit, et `EasySetPassesTesseract` cherche la boîte des pixels foncés, excluant la flèche grise, avant zoom ×6 et OCR PSM_SINGLE_WORD.
- Un test JVM de géométrie utilise une disposition xTool de 869 × 1536. **Aucune capture personnelle n'a été publiée dans le dépôt GitHub public.**
- **Reste indispensable** : raccorder la lecture Tesseract à `EasySetVisionAnalyzer` sur le fil de travail ; tester les cas 1, 2, 10, valeurs contradictoires, différents thèmes et orientations ; vérifier que l'app ne modifie jamais les réglages xTool.

