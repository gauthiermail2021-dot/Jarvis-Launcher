# Jarvis Bridge Android — prototype 0.8

Ce dépôt préserve la page web `index.html` originale.

Les sources `android/baseline-v0.7/` proviennent d'une archive documentaire reconstituée de la version 0.7 (Gmail → SVG → xTool). Il ne s'agit pas d'une copie certifiée du dépôt Android original.

Le projet `android/` ajoute un service de bulle flottante au premier plan : activation explicite, autorisation système « afficher par-dessus les applications », déplacement tactile, un toucher pour agrandir (54→110 dp), deuxième toucher pour ouvrir ChatGPT, puis réduction. Une action dans la notification ou un bouton de l'application permet de l'arrêter.

**Attention** : le prototype 0.8 n'a pas encore été compilé ni essayé sur téléphone. Ne pas désinstaller ni remplacer l'APK 0.7 stable. Les APK construits ailleurs peuvent être signés avec un autre certificat, ce qui affecterait les mises à jour et Google OAuth.

Pour compiler : ouvrir `android/` dans Android Studio (Java 17, Android SDK 35, Gradle 8.9), ou suivre GitHub Actions sur la branche `feature/android-jarvis-bubble-v0.8`.

Aucun secret OAuth ni clé de signature ne doit être commité au dépôt.
