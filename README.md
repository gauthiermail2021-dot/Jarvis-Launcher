# Jarvis Bridge 1.2.27 — EasySet, valeurs numériques en lecture seule

**Branche expérimentale**, basée sur 1.2.26. La branche stable 1.2.21 et le flux Gmail → SVG → xTool restent inchangés.

## Parcours

1. Ouvrir ChatGPT avec la bulle Jarvis ; accepter une fois la session Android de capture, si le dialogue est proposé.
2. Envoyer un SVG sous le libellé « Jarvis SVG ». Jarvis ouvre le dessin dans xTool comme auparavant.
3. **Ouvrir manuellement EasySet** jusqu'à voir Puissance, Vitesse et Passer, avec leurs valeurs.
4. Maintenir la bulle → **Analyser EasySet maintenant** : la capture utilise la session Android déjà autorisée, sans nouvelle demande.
5. Le panneau flottant propose les trois valeurs numériques repérées, avec les unités de puissance (%) et de vitesse (mm/s). Tout paramètre non associé de façon fiable est marqué **« non lue »** plutôt qu'inventé. Les valeurs sont **indicatives**, à confirmer visuellement dans xTool.
6. La session Android est interrompue avec la bulle ou le bouton « Arrêter le partage d'écran ».

## Fonctionnement de la lecture

- Google ML Kit détecte uniquement les lignes OCR et leur position à l'écran. Le parseur prend soit une valeur figurant sur la **même ligne de texte** après le libellé, soit un nombre sur une ligne **située à droite, à la même hauteur** que le libellé.
- Unités incohérentes, nombres hors plage ou texte provenant de la fenêtre Jarvis sont refusés. Les dimensions du SVG ne doivent pas être prises pour des paramètres EasySet.
- L'image et le texte OCR brut ne sont pas persistés. **Le dernier résultat stocké localement ne contient pas les nombres** ; les valeurs sont visibles seulement pendant le panneau flottant temporaire.
- Les chiffres ne constituent **ni une mesure réelle de la machine, ni une preuve que le logiciel a appliqué ces paramètres**.
- **Aucune interaction avec les commandes xTool et aucune modification ni activation du laser**.

## Validation

Les nouveaux tests JVM couvrent plusieurs présentations de libellés, les unités, les valeurs en colonnes, les dimensions SVG, les valeurs incohérentes et une capture accidentelle de l'écran Jarvis. La compilation GitHub Actions produit un APK signé. Le résultat réel reste à valider dans xTool sur le Samsung ; fournir une capture du panneau EasySet complet si les chiffres restent « non lus ».
