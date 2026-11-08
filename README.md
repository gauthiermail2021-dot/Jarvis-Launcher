# Jarvis Bridge Android — prototype 0.9

La branche `feature/android-jarvis-specific-chat-v0.9` ajoute la mémorisation locale d'un lien de conversation ChatGPT (format `https://chatgpt.com/c/...`).

- Un toucher : agrandir la bulle
- Deuxième toucher : tenter d'ouvrir la conversation enregistrée dans l'application ChatGPT
- Un glissement : déplacer la bulle
- Appui immobile de 2 secondes : ouvrir le menu avec Jarvis Bridge / Fermer la bulle / Annuler
- Sans lien enregistré : ouvrir ChatGPT normalement
- En cas d'impossibilité de router le lien vers l'application ChatGPT : tenter l'ouverture par navigateur
- Depuis Jarvis Bridge 0.9 : coller, enregistrer/tester, ou effacer un lien

**Important** : ne pas utiliser les URL `/share/`, qui représentent des liens partagés et non l'accès privé direct à une conversation existante. Le lien est conservé sur le téléphone dans les préférences de l'application ; il n'est ni publié sur GitHub ni envoyé par Jarvis. Obtenir ce lien depuis l'URL de la conversation dans le navigateur connecté à ChatGPT.

La nouvelle application `ca.jarvis.bridge.preview09` est installable séparément de la 0.7 stable et de la 0.8.1 de test.

Ce code est expérimental : une compilation réussie ne confirme pas que la version installée de ChatGPT sait router une URL `/c/...`. Il faudra le tester sur Android.

Les sources `android/baseline-v0.7/` proviennent d'une archive documentaire reconstituée de la version Gmail → SVG → xTool; ne pas les traiter comme une copie certifiée du dépôt d'origine. La version `0.8.1` testée avec succès reste préservée sur la branche `feature/android-jarvis-bubble-v0.8`.

Pour compiler : GitHub Actions publie l'APK sous une préversion GitHub `jarvis-0.9-preview`. Pas de secret ni clé de signature à commiter.
