# Jarvis Bridge 1.0 — Prototype visuel

Cette branche expérimentale conserve les fonctions validées en 0.9, sans modifier les versions existantes.

- Bulle 54 dp, agrandie à 110 dp au premier toucher; le deuxième toucher ouvre la conversation ChatGPT déjà configurée.
- Après **5 secondes sans interaction**, la bulle agrandie reprend automatiquement 54 dp.
- Déplacer la bulle n'ouvre pas ChatGPT et repousse le délai de réduction.
- Appui de 2 secondes : menu conservé; le menu empêche la réduction tant qu'il est ouvert.
- Au centre de la bulle agrandie : heure locale du téléphone et pourcentage réel de batterie.
- Signal bleu/turquoise en veille; animation lumineuse discrète, désactivable par le menu.
- Une **nouvelle notification Gmail** signale temporairement l'événement en ambre et agrandit la bulle, puis elle se réduit après 5 secondes sans autre activité.
- Le signal Gmail est général; il ne signifie **pas** que le contenu du courriel est un SVG ou que ChatGPT est à l'écoute.
- Le lien privé de conversation ChatGPT reste stocké uniquement dans les préférences de cette application, jamais dans le dépôt.

**Installation :** `ca.jarvis.bridge.preview10` est une application de test distincte de la 0.7, la 0.8.1 et la 0.9. Il faut donc reconfigurer le lien de conversation pour cette version une seule fois.

**Validation requise :** compiler puis vérifier sur le Samsung les animations, la réduction automatique, les notifications Gmail, le menu et l'ouverture de ChatGPT. Le prototype n'est pas une intégration officielle à ChatGPT.
