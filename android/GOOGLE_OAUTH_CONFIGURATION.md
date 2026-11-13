# Jarvis Bridge — configuration OAuth Gmail et signature Android

> **Identité active :** dépôt `Jarvis-Launcher`, application Android `ca.jarvis.bridge`. Cette fiche conserve des détails d'authentification établis dans les premières versions ; elle **ne doit pas être lue comme une description de la dernière version de Jarvis**. Pour la bonne branche de compilation, consulter [SOURCE_OF_TRUTH.md](../docs/SOURCE_OF_TRUTH.md).

## Identité Android stable

- ApplicationId officiel : `ca.jarvis.bridge`, sans suffixes `previewNNN`.
- Lire la version courante dans `android/app/build.gradle` **sur la branche active** ; les anciens numéros de cette fiche ne sont pas une cible de compilation.
- Seule la variante `release` destinée au téléphone porte le certificat permanent.
- Les build `debug` restent réservés au développement sous `ca.jarvis.bridge.dev` et ne sont pas utilisés pour tester OAuth.
- Le certificat doit être réutilisé **sans changement** pour toutes les prochaines mises à jour officielles.

SHA-1 **public du certificat** (à saisir dans Google Cloud) :

`E4:C2:4E:54:2B:9B:F1:B3:34:30:81:24:0C:EE:E5:97:C5:28:51:2F`

SHA-256 :

`68:98:9E:DC:69:06:1C:CC:06:D0:D6:BC:0C:B0:E3:FE:5B:DE:15:7A:B7:AD:E5:11:1F:06:E8:67:A7:44:13:B7`

Le fichier `.jks`, sa représentation Base64 et les mots de passe ne doivent **jamais** être ajoutés au dépôt public. Ils sont distribués séparément au propriétaire dans l'archive privée.

## Une seule configuration Google OAuth

1. Ouvrir le projet Google Cloud qui gère l'accès Gmail du prototype ou créer un projet dédié.
2. Activer **Gmail API** dans Google Cloud (APIs & Services / Library).
3. Dans Google Auth Platform → **Clients**, créer un client OAuth 2.0 de type **Android** pour :
   - Package : `ca.jarvis.bridge`
   - Certificat SHA-1 : celui indiqué ci-dessus.
4. Dans l'écran de consentement, configurer le scope `https://www.googleapis.com/auth/gmail.readonly`. Ce scope a des exigences de vérification supplémentaires pour un déploiement public; pour un prototype personnel, utiliser le mode Test et ajouter le compte Gmail concerné comme utilisateur de test lorsque Google le demande.
5. Vérifier que Google Auth Platform / Branding / Audience indique le bon projet et la bonne application.
6. Ne **pas** mettre un client secret Google ni un jeton OAuth dans le code Android, un courriel, le dépôt ou une capture d'écran.

**Vérification indispensable avant publication :** le package et SHA-1 dans le client OAuth Android doivent correspondre exactement à ceux du certificat de l'APK `release` publié.

## GitHub Actions : quatre secrets, une seule fois

Dans **Settings → Secrets and variables → Actions** du dépôt `gauthiermail2021-dot/Jarvis-Launcher`, enregistrer ces quatre secrets au niveau du repository :

- `JARVIS_KEYSTORE_BASE64` : le contenu intégral de `keystore_base64.txt` dans l'archive privée.
- `JARVIS_KEYSTORE_PASSWORD` : le mot de passe JKS dans `CONFIGURATION_PRIVEE.txt`.
- `JARVIS_KEY_ALIAS` : `jarvisbridge`.
- `JARVIS_KEY_PASSWORD` : le mot de passe de la clé dans `CONFIGURATION_PRIVEE.txt`.

Chaque workflow de build signé doit refuser de compiler si un secret manque. Ne lancer que le workflow correspondant à la branche et à la version en cours. Après compilation et vérification de signature, le `.jks` temporaire est effacé avant le dépôt d'artefacts. Aucune publication GitHub Releases n'est tentée.

**Sauvegarde :** conserver l'archive privée hors du dépôt GitHub, dans un emplacement fiable. Si la clé privée est perdue, on ne peut pas signer les mises à jour de la même application avec un nouveau certificat.

## Changement technique d'authentification

- Migration du lancement Google vers `ActivityResultContracts.StartIntentSenderForResult` via `androidx.activity.ComponentActivity`, conformément à la documentation Android.
- Analyse du résultat et des erreurs `ApiException`; arrêt du flux Gmail si aucune autorisation valide n'est revenue.
- Conservation de l'état de diagnostic à l'écran; aucun jeton Gmail n'est enregistré sur disque.
- Conserve l'ouverture Gmail « Toucher pour ouvrir » et l'exclusion de toutes les versions Jarvis Bridge comme gestionnaires SVG externes.
- Bulle inchangée : petite heure, grande sans heure, appui long de 1 s, réduction après 5 s.
- L'ouverture sans toucher a été validée pour 1.2.8 sur un Samsung Android 13 avec la bulle visible. Elle reste dépendante des autorisations et notifications Gmail/Android.

**Ce que cela ne garantit pas :** la connexion Gmail peut échouer après révocation du consentement OAuth ou en absence de notification Android. La toute nouvelle version doit être validée sur le téléphone avec un nouveau SVG après sa compilation.

## Vérifications techniques avant chaque future compilation

- Un seul package officiel `ca.jarvis.bridge`.
- Réutiliser la même clé de signature.
- `assembleRelease` et non `assembleDebug`.
- Vérifier l'empreinte du certificat de l'APK contre celle ci-dessus.
- Ne jamais publier le keystore, ses mots de passe, ni un jeton OAuth.
- Ne pas compiler d'ancienne branche par défaut : confirmer la branche et le numéro de version avant toute publication.
