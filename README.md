# Jarvis Bridge 1.0.2 — Horloge et activité visuelle

Version d'essai pour Samsung Android, dérivée de 1.0.1 sans remplacer les versions déjà installées.

- **Réduite (54 dp)** : n'affiche que l'heure locale `HH:mm`, sans pourcentage de batterie, sans animation continue. Rafraîchissement au changement de minute.
- **Agrandie (110 dp)** : affiche `JARVIS`, `PRÊT` et l'heure en l'absence d'activité. Retour automatique à 54 dp après **5 secondes d'inactivité**.
- **Activité réelle** : une notification Gmail (ambre), une tentative de traitement de SVG (cyan), une demande d'ouverture SVG envoyée à Android (vert) ou une erreur (rouge) provoque un agrandissement et une lumière qui circule autour du cadre pendant environ 5 secondes.
- L'indicateur « ENVOYÉ » signifie seulement que la demande d'ouverture du SVG a été transmise à Android, pas que xTool a terminé la tâche.
- **Appui long de 1,5 seconde** : affiche le menu. Un premier toucher agrandit, un deuxième ouvre la conversation ChatGPT configurée et réduit la bulle. La bulle est déplaçable.
- Dans le menu, une option désactive la lumière animée tout en conservant les changements de couleur.

Cette version ne peut **pas** détecter si ChatGPT réfléchit, écoute, ou a fini de répondre : aucun état de ce type n'est affiché.
Le signal Gmail est déclenché par une notification Gmail, pas forcément un mail SVG.

APK de test avec identifiant Android `ca.jarvis.bridge.preview102`, distinct de 0.7 / 0.8.1 / 0.9 / 1.0 / 1.0.1.
Il faut reconfigurer le lien de conversation **une seule fois** pour cette application de test.
Une stratégie de signature Android stable est nécessaire pour de véritables mises à jour qui préservent automatiquement les données.
