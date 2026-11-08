# Jarvis Bridge 1.1.0 — Identité stable & Gmail OAuth

**Branche : `feature/android-jarvis-stable-oauth`. Aucun APK 1.1.0 compilé à ce stade.**

Le prototype 0.7 a permis un test Gmail → SVG → xTool réussi. Les versions 1.0.x ont introduit une identité de test Android différente à chaque compilation; la 1.0.5 affiche « sélection de compte annulée (résultat 0) » sur le Samsung. Ce symptôme ne prouve pas à lui seul une erreur de signature, mais rend nécessaire la vérification du client OAuth Android.

La branche 1.1.0 prépare :
- un package **fixe** `ca.jarvis.bridge`;
- une signature de version `release` **réutilisable** au lieu d'une nouvelle clé debug à chaque compilation;
- un flux d'autorisation Google basé sur AndroidX ActivityResult;
- des messages d'erreur Gmail persistants et une vérification stricte de la réussite OAuth;
- la protection empêchant de renvoyer un SVG à une autre version Jarvis;
- la bulle 54 dp / 110 dp, heure seulement en petit, menu 1 seconde, repli 5 secondes.

Le certificat privé est fourni séparément à l'utilisateur, **pas** dans le dépôt GitHub. Pour fonctionner, Google Cloud doit autoriser le package et le SHA-1 de ce certificat et les 4 secrets GitHub Actions doivent être configurés.

Consulter **`android/GOOGLE_OAUTH_CONFIGURATION.md`** pour les étapes précises.

**Compilation exclusivement sur demande explicite.** Le fonctionnement Gmail → xTool doit être retesté sur le Samsung après l'autorisation Google; il n'est pas encore confirmé.
