# Jarvis Bridge 1.2.28 — lecture numérique EasySet par zones

Branche expérimentale indépendante créée depuis 1.2.27. La version stable 1.2.21 et l'ouverture Gmail → SVG → xTool ne sont pas modifiées.

## Correction 1.2.28

Dans xTool Android, le libellé « Vitesse (mm/s) » est placé plus haut que sa valeur, tandis que « Passer » et son nombre peuvent être alignés. La version 1.2.27 exigeait souvent une valeur sur la même ligne OCR, d'où « non lue » malgré les chiffres visibles.

Jarvis associe maintenant les nombres **situés à droite et à l'intérieur de la zone verticale du paramètre**. Une zone se termine au libellé de paramètre suivant ; elle est aussi limitée par la hauteur du libellé pour ne pas englober tout l'écran. Les petits chevrons de navigation à droite des chiffres sont tolérés.

Chaque valeur doit être cohérente avec le champ : puissance 0–100 %, vitesse positive et passages entiers. En cas de chiffres contradictoires, la valeur reste **« non lue »**. La lecture des valeurs reste indicative, sans enregistrement durable des nombres.

## Essai sur Samsung

1. Installer l'APK 1.2.28 signé dans la continuité de Jarvis 1.2.27.
2. Ouvrir xTool et afficher le volet EasySet, **avec les trois valeurs numériques visibles**.
3. Faire un appui long sur la bulle Jarvis → « Analyser EasySet maintenant » ; réutilisation du consentement de capture Android si la session demeure active.
4. Comparer la fenêtre de résultat à ce qui est réellement affiché dans xTool. Une valeur non lue ne doit pas être inventée.
5. Aucun changement de réglage n'est effectué et aucun démarrage laser n'est possible depuis ce diagnostic.

## Vérifications automatiques

La suite de tests JVM contient des cas reproduisant la disposition observée dans xTool (curseurs et valeurs plus bas que les libellés), les chevrons, le masquage des valeurs hors-zone et les conflits. La compilation GitHub Actions exécute les tests, signe l'APK et vérifie la signature. Les tests ne remplacent pas la vérification sur le Samsung.
