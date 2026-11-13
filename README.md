# JARVIS — dépôt Android officiel

**Dépôt de développement unique :** [Jarvis-Launcher](https://github.com/gauthiermail2021-dot/Jarvis-Launcher). L'application installée se nomme **Jarvis Bridge** et conserve son identifiant Android **`ca.jarvis.bridge`** ; le nom de l'application et celui du dépôt ne désignent pas deux applications différentes.

## Référence de travail — 13 novembre 2026

| Élément | Emplacement | Niveau de validation |
| --- | --- | --- |
| Code Android de référence | **`main/android/` — Jarvis 1.2.69** (`versionCode 88`) | **Testé sur le Samsung** : ouverture SVG, Conception et Dégrouper opérationnels |
| Fonction à terminer | Calques après Dégrouper | **Non fonctionnelle automatiquement**, ne pas annoncer le contraire |
| Maintenance séparée | [`fix/audit-stability-1.2.70`](../../tree/fix/audit-stability-1.2.70) | **1.2.70**, `versionCode 89`, tests/compilation/signature GitHub réussis ; validation sur téléphone à effectuer |
| Dépôt ancien | [Jarvis-Bridge-Android](https://github.com/gauthiermail2021-dot/Jarvis-Bridge-Android) | **Archivé, lecture seule. Ne pas compiler son prototype.** |

**Règle :** la version validée sur téléphone constitue la référence fonctionnelle. Une CI réussie valide la compilation et les tests automatisés, **pas** le comportement xTool sur l'appareil.

## Procédure de développement

1. Lire `AGENTS.md`, `docs/SOURCE_OF_TRUTH.md`, `android/app/build.gradle` et vérifier la branche/SHA courants.
2. Partir de `main` pour une évolution fonctionnelle après avoir vérifié qu'il pointe toujours vers la dernière **base approuvée sur appareil** ; ne pas réintroduire un ancien prototype.
3. Créer une branche de correction isolée. Faire une PR avec un diff ciblé, des tests automatisés et un build signé avant la validation manuelle Android.
4. Préserver l'identifiant `ca.jarvis.bridge`, la signature Android, la configuration Gmail, les accès Android, la bulle, les animations/sons et les garde-fous SVG.
5. Ne pas automatiser le déclenchement du laser ni modifier les paramètres du traitement ; OCR EasySet en lecture seule.

## Structure du dépôt

- `android/` : **application Jarvis Bridge** Android.
- `android/app/src/test/` : tests JVM.
- `android/app/build.gradle` : identité et version Android.
- `.github/workflows/build-jarvis-android.yml` : compilation de contrôle de `main`, tests et signature, **sans publication automatique comme version stable**.
- `index.html` : lanceur Web préservé, distinct de l'APK.
- `docs/SOURCE_OF_TRUTH.md` : suivi des états, procédures et sauvegardes.

L'ancien prototype, les versions antérieures et leurs workflows restent consultables dans l'historique Git et dans les branches spécifiques, mais ne sont plus proposés comme chemin de développement actif.
