# Jarvis Bridge 1.2.29 — Lecture des éléments OCR EasySet

Cette version expérimentale dérive de 1.2.28. La version stable Jarvis 1.2.21 n'est pas modifiée, ni le transfert Gmail → SVG → xTool.

## Ce qui a changé depuis 1.2.28

La reconnaissance numérique précédente a lu **Puissance 59 %**, mais pas **Vitesse 114 mm/s** ni **Passer 1** sur la capture xTool du Samsung.

1. Le programme transmet au lecteur non seulement les lignes de texte ML Kit, mais également chaque petit **Element** reconnu individuellement, avec sa position à l'écran. Une ligne polluée par un petit chevron peut ainsi contenir un élément numérique utilisable.
2. Si un ou plusieurs paramètres restent « non lus », une **seconde analyse OCR du tiers droit élargi de la même image** agrandit les petits chiffres jusqu'à 2×. Aucun nouveau partage d'écran n'est demandé.
3. Les éléments de la seconde lecture sont repositionnés aux coordonnées de l'image originale et sont acceptés **seulement s'ils restent dans la bonne zone EasySet**.
4. Les valeurs reconnues au premier passage conservent la priorité. Les lectures contradictoires ou non associables restent **« non lue »**, pas des valeurs inventées.
5. Les pixels, les textes bruts et les valeurs numériques ne sont pas persistés. Ils ne servent qu'au diagnostic temporaire à l'écran.

## Tester sur Samsung

1. Installer l'APK signé 1.2.29 par-dessus Jarvis.
2. Afficher dans xTool le volet EasySet avec **Puissance, Vitesse et Passer** ouverts et les trois valeurs bien visibles.
3. Dans la bulle Jarvis, maintenir une seconde et choisir **« Analyser EasySet maintenant »**. La session de capture Android existante est réutilisée lorsqu'elle est active.
4. Comparer le résultat aux valeurs effectivement visibles dans xTool. En cas de doute, conserver une capture du résultat.

## Sécurité

Aucune modification de puissance, vitesse, passages ni commande de démarrage laser. La reconnaissance des nombres est expérimentale et doit être confirmée visuellement avant toute fabrication. La session MediaProjection reste révocable depuis la bulle et la notification Android.
