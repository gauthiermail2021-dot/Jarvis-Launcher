# Jarvis 1.2.36 — Bulle flottante SVG (test numéroté)

Date : 2026-11-10.

## Demande

L'utilisateur préfère un **bouton flottant comme la bulle Jarvis**, à l'écran au-dessus de ChatGPT et xTool, plutôt que d'aller chercher l'icône Android `TEST SVG JARVIS` à chaque essai.

## Implémentation

- `JarvisTestBubbleOverlay.java` ajoute un bouton rond de **48 dp**, fond bleu nuit, contour cyan, libellé **SVG**.
- Cette bulle est **indépendante de la bulle Jarvis principale**, affichée automatiquement à côté d'elle lorsque le service flottant est lancé.
- **Une pression courte** lance `JarvisTestShortcutActivity` et son envoi automatique existant. Aucun deuxième appui n'est requis dans les conditions où l'accès Gmail est déjà autorisé.
- **Glisser-déposer** déplace uniquement la bulle ; aucun envoi pendant un glissement. Position sauvegardée dans les préférences Android `jarvis_bubble/test_svg_x` et `test_svg_y`.
- Appuis rapprochés bloqués brièvement pour éviter les doublons involontaires ; `JarvisTestShortcutActivity` conserve son propre contrôle d'une émission toutes les cinq secondes.
- Les noms des pièces jointes continuent à incrémenter `Jarvis_Test_001.svg`, `Jarvis_Test_002.svg`, etc. Le dessin visible reste identique, avec un commentaire XML invisible changeant pour franchir la protection anti-doublons.
- Depuis un appui long sur Jarvis : `Bulle TEST SVG : masquer / afficher` permet de la désactiver ou réactiver, et `Envoyer SVG test (+1)` sert de repli manuel.
- Pendant une capture EasySet consentie par Android, **les deux bulles sont temporairement cachées**, puis restaurées, pour ne pas contaminer l'OCR.
- Il s'agit d'une superposition Android existante `SYSTEM_ALERT_WINDOW` ; **aucune autorisation additionnelle de superposition**. L'autorisation Google `gmail.modify` peut être demandée séparément au premier envoi.
- L'application ne modifie aucun réglage xTool et ne démarre aucun laser. Elle peut demander à Android d'ouvrir un nouveau SVG ; xTool reste seul à gérer la sauvegarde ou conservation d'un dessin déjà ouvert.

## Compatibilité et limites

- L'icône `TEST SVG JARVIS` demeure pour compatibilité.
- La nouvelle bulle apparaît **seulement quand la bulle principale Jarvis est active**, avec les autorisations de superposition Android déjà accordées. Elle ne démarre pas seule au démarrage du téléphone.
- Le bouton déclenche toujours le flux Gmail → vérification Jarvis → xTool : il n'envoie pas directement un dessin à la machine.
- Pendant l'envoi, la fenêtre temporaire de test Jarvis peut devenir visible brièvement. Les interactions d'autorisation Gmail dépendent du consentement Google.
- La version est un **candidat de test** ; compilation réussie seule ne garantit pas l'affichage réel de la bulle sur Samsung ni le comportement de la réouverture xTool.

## Vérification Samsung sans lancer de laser

1. Installer `Jarvis 1.2.36` par-dessus la version précédente, sans désinstaller, pour conserver le compteur.
2. Démarrer la bulle Jarvis, et trouver la petite bulle ronde `SVG` à proximité ; la déplacer à un endroit libre.
3. Cliquer une fois. Vérifier l'envoi du prochain `Jarvis_Test_00N.svg` dans Gmail avec le libellé `Jarvis SVG`, puis la demande d'ouverture xTool.
4. Répéter (après plus de cinq secondes) et vérifier l'incrément sans réimporter d'ancien dessin.
5. Contrôler que la capture EasySet masque et restaure les deux bulles.
6. Tester le menu d'appui long de Jarvis pour masquer/réafficher `SVG`.

Code source initial : branche `feature/floating-svg-test-bubble-1.2.36` dérivée du snapshot signé 1.2.35.
