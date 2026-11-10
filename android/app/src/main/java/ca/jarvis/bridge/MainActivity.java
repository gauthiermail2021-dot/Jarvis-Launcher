package ca.jarvis.bridge;

import android.app.Activity;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.IntentSenderRequest;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Parcelable;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.ApiException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.MessageDigest;

public class MainActivity extends ComponentActivity {
    private static final int PICK_SVG = 1001;
    private static final int NOTIFICATION_PERMISSION = 1003;
    public static final String ACTION_CHECK_GMAIL = "ca.jarvis.bridge.ACTION_CHECK_GMAIL";
    private static volatile MainActivity foregroundActivity;
    private static final int MAX_SVG_BYTES = 5000000;
    private static final String PREFS = "jarvis_bridge";
    private static final String LAST_STATUS = "last_status";
    private static final String AUTH_PENDING = "gmail_auth_pending";
    static final String AUTO_OPEN_ENABLED = "auto_open_enabled";
    static final String AUTO_BUBBLE_ON_LAUNCH = "auto_bubble_on_launch";
    static final String COMPACT_LAUNCH = "compact_launch";
    private static final String LAST_SVG = "last_svg";
    private static final String LAST_GMAIL_ID = "last_gmail_id";
    private static final String LAST_GMAIL_TIMESTAMP = "last_gmail_timestamp";
    private static final String LAST_SVG_SHA256 = "last_svg_sha256";
    // Garde-fou partagé par toutes les activités Jarvis ; les notifications
    // successives ne doivent pas lancer plusieurs chargements simultanés.
    private static final AtomicBoolean GMAIL_CHECK_IN_PROGRESS = new AtomicBoolean(false);
    // Toute notification arrivant pendant la lecture Gmail déclenche une
    // nouvelle vérification à la fin, au lieu d'être silencieusement perdue.
    private static final AtomicBoolean GMAIL_RECHECK_REQUESTED = new AtomicBoolean(false);
    private static final String HANDLED_GMAIL_IDS = "handled_gmail_ids";
    private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final String LABEL_NAME = "Jarvis SVG";
    private static final String API = "https://gmail.googleapis.com/gmail/v1/users/me";
    private EditText urlInput;
    private TextView status;
    private AuthorizationClient authorizationClient;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private boolean isBusy = false;
    private boolean startBubbleAfterPermission = false;
    // Seulement les ouvertures automatiques Gmail : aucune interface géante
    // à afficher pendant le transfert SVG. Les réglages restent accessibles.
    private boolean silentAutoCheck = false;
    private boolean compactLauncher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent launchIntent = getIntent();
        silentAutoCheck = launchIntent != null
                && ACTION_CHECK_GMAIL.equals(launchIntent.getAction())
                && getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getBoolean(AUTO_OPEN_ENABLED, false);
        compactLauncher = launchIntent != null
                && Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getBoolean(COMPACT_LAUNCH, true)
                && Settings.canDrawOverlays(this);
        if (silentAutoCheck || compactLauncher) {
            // Éviter le clignotement des réglages. L'icône lance seulement
            // la bulle; les réglages complets restent au menu de la bulle.
            setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        }
        super.onCreate(savedInstanceState);
        if (silentAutoCheck || compactLauncher) {
            // Un fond de fenêtre opaque ou atténué peut laisser un écran
            // noir derrière la bulle, surtout après un lancement Gmail.
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        authorizationClient = Identity.getAuthorizationClient(this);
        // Cycle de vie géré par AndroidX : le résultat Google est livré même
        // après la recréation de l'Activity, avec un diagnostic explicite.
        authorizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                activityResult -> handleGoogleAuthorizationReturn(
                        activityResult.getResultCode(), activityResult.getData()));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 45, 30, 30);

        TextView title = new TextView(this);
        String appVersion = "inconnue";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) { }
        title.setText("Jarvis Bridge " + appVersion + " — connexion Gmail");
        title.setTextSize(25);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText("Gmail → notification → SVG xTool (sans vérification périodique)");
        layout.addView(description);

        TextView conversationTitle = new TextView(this);
        conversationTitle.setText("Conversation Jarvis");
        conversationTitle.setTextSize(19);
        layout.addView(conversationTitle);

        TextView conversationHelp = new TextView(this);
        conversationHelp.setText("Colle l’adresse privée de cette conversation (https://chatgpt.com/c/...). "
                + "N’utilise pas un lien de partage : il s’agit d’une copie de la conversation.");
        layout.addView(conversationHelp);

        EditText conversationInput = new EditText(this);
        conversationInput.setSingleLine(true);
        conversationInput.setHint("https://chatgpt.com/c/...");
        conversationInput.setText(getSharedPreferences(JarvisConversationLink.PREFS, MODE_PRIVATE)
                .getString(JarvisConversationLink.KEY_URL, ""));
        layout.addView(conversationInput);

        Button pasteConversation = new Button(this);
        pasteConversation.setText("Coller le lien du presse-papiers");
        layout.addView(pasteConversation);
        pasteConversation.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                showMessage("Presse-papiers vide.");
                return;
            }
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            conversationInput.setText(text == null ? "" : text.toString().trim());
        });

        Button saveConversation = new Button(this);
        saveConversation.setText("Enregistrer et tester le lien");
        layout.addView(saveConversation);
        saveConversation.setOnClickListener(v -> {
            String url = JarvisConversationLink.normalize(conversationInput.getText().toString());
            if (url == null) {
                setStatus("Lien non valide : utilise l’adresse privée https://chatgpt.com/c/...");
                return;
            }
            getSharedPreferences(JarvisConversationLink.PREFS, MODE_PRIVATE)
                    .edit().putString(JarvisConversationLink.KEY_URL, url).apply();
            conversationInput.setText(url);
            setStatus("Lien enregistré localement. Ouverture de la conversation...");
            if (!JarvisConversationLink.open(this)) {
                setStatus("Lien enregistré, mais impossible d’ouvrir ChatGPT.");
            }
        });

        Button clearConversation = new Button(this);
        clearConversation.setText("Oublier le lien enregistré");
        layout.addView(clearConversation);
        clearConversation.setOnClickListener(v -> {
            getSharedPreferences(JarvisConversationLink.PREFS, MODE_PRIVATE)
                    .edit().remove(JarvisConversationLink.KEY_URL).apply();
            conversationInput.setText("");
            setStatus("Lien effacé. La bulle ouvrira ChatGPT normalement.");
        });

        Button gmail = new Button(this);
        gmail.setText("Ouvrir le dernier SVG Gmail");
        layout.addView(gmail);
        gmail.setOnClickListener(v -> requestGmailAndOpen());

        // L'utilisateur choisit explicitement d'autoriser les ouvertures
        // automatiques lors d'une notification Gmail Jarvis SVG.
        // Désactivé par défaut : aucune fenêtre surprise pour d'autres courriels.
        Button autoOpen = new Button(this);
        boolean autoEnabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(AUTO_OPEN_ENABLED, false);
        autoOpen.setText(autoEnabled
                ? "Ouverture automatique xTool : ACTIVÉE"
                : "Ouverture automatique xTool : DÉSACTIVÉE");
        layout.addView(autoOpen);
        autoOpen.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            boolean enabled = !prefs.getBoolean(AUTO_OPEN_ENABLED, false);
            prefs.edit().putBoolean(AUTO_OPEN_ENABLED, enabled).apply();
            autoOpen.setText(enabled
                    ? "Ouverture automatique xTool : ACTIVÉE"
                    : "Ouverture automatique xTool : DÉSACTIVÉE");
            setStatus(enabled
                    ? "Automatique activé : conserver la bulle affichée ; Android peut limiter les ouvertures en arrière-plan."
                    : "Automatique désactivé : notification Jarvis avec ouverture par toucher.");
        });

        Button access = new Button(this);
        access.setText("Autoriser la détection des notifications Gmail");
        layout.addView(access);
        access.setOnClickListener(v -> {
            Intent settings = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(settings);
        });

        Button bubbleOnLaunch = new Button(this);
        boolean bubbleOn = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(AUTO_BUBBLE_ON_LAUNCH, true);
        bubbleOnLaunch.setText(bubbleOn
                ? "Bulle au lancement de Jarvis : ACTIVÉE"
                : "Bulle au lancement de Jarvis : DÉSACTIVÉE");
        layout.addView(bubbleOnLaunch);
        bubbleOnLaunch.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            boolean enabled = !prefs.getBoolean(AUTO_BUBBLE_ON_LAUNCH, true);
            prefs.edit().putBoolean(AUTO_BUBBLE_ON_LAUNCH, enabled).apply();
            bubbleOnLaunch.setText(enabled
                    ? "Bulle au lancement de Jarvis : ACTIVÉE"
                    : "Bulle au lancement de Jarvis : DÉSACTIVÉE");
            if (enabled) requestBubble();
            else setStatus("Ouverture automatique de la bulle désactivée. Bulle manuelle toujours disponible.");
        });

        TextView chatgptAutoHelp = new TextView(this);
        chatgptAutoHelp.setText("Option ChatGPT : activer le service d'accessibilité Jarvis "
                + "pour détecter uniquement l'ouverture de ChatGPT. "
                + "Autorisation facultative, désactivée par défaut. "
                + "Jarvis ne lit pas le contenu de vos conversations.");
        layout.addView(chatgptAutoHelp);
        Button chatgptAuto = new Button(this);
        chatgptAuto.setText("Configurer la bulle à l'ouverture de ChatGPT");
        layout.addView(chatgptAuto);
        chatgptAuto.setOnClickListener(v -> {
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(settings);
            setStatus("Dans Accessibilité > Applications installées, autorise Jarvis Bridge — ChatGPT. "
                    + "Autorise aussi l'affichage par-dessus les applications.");
        });

        TextView easysetVisionHelp = new TextView(this);
        easysetVisionHelp.setText("EXPÉRIMENTAL 1.2.23 : reconnaissance visuelle "
                + "des libellés EasySet, sans autorisation d'accessibilité. "
                + "Capture ponctuelle avec ton accord, ou importation d'une image. "
                + "Aucune commande xTool.");
        layout.addView(easysetVisionHelp);
        Button easysetVision = new Button(this);
        easysetVision.setText("Analyser EasySet visuellement (sans accessibilité)");
        layout.addView(easysetVision);
        easysetVision.setOnClickListener(v ->
                startActivity(new Intent(this, EasySetVisionActivity.class)));

        Button compactMode = new Button(this);
        boolean compact = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(COMPACT_LAUNCH, true);
        compactMode.setText(compact
                ? "Icône Jarvis : bulle uniquement (ACTIVÉ)"
                : "Icône Jarvis : afficher les réglages (ACTIVÉ)");
        layout.addView(compactMode);
        compactMode.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            boolean next = !prefs.getBoolean(COMPACT_LAUNCH, true);
            prefs.edit().putBoolean(COMPACT_LAUNCH, next).apply();
            compactMode.setText(next
                    ? "Icône Jarvis : bulle uniquement (ACTIVÉ)"
                    : "Icône Jarvis : afficher les réglages (ACTIVÉ)");
            setStatus("Réglages accessibles via appui long (1 s) sur la bulle → Ouvrir Jarvis Bridge.");
        });

        Button startBubble = new Button(this);
        startBubble.setText("Activer la bulle flottante");
        layout.addView(startBubble);
        startBubble.setOnClickListener(v -> requestBubble());

        Button stopBubble = new Button(this);
        stopBubble.setText("Arrêter la bulle");
        layout.addView(stopBubble);
        stopBubble.setOnClickListener(v -> {
            startBubbleAfterPermission = false;
            stopService(new Intent(this, JarvisBubbleService.class));
            setStatus("Bulle arrêtée.");
        });

        status = new TextView(this);
        // Ne pas effacer le diagnostic lorsque Google ramène l'application
        // au premier plan ou qu'Android recrée l'activité.
        status.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(
                LAST_STATUS, "Prêt. En attente d'un SVG Gmail."));
        layout.addView(status);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://exemple.com/piece.svg");
        layout.addView(urlInput);

        Button download = new Button(this);
        download.setText("Ouvrir un SVG depuis un lien");
        layout.addView(download);
        download.setOnClickListener(v -> downloadSvg(urlInput.getText().toString().trim()));

        Button select = new Button(this);
        select.setText("Choisir un fichier SVG");
        layout.addView(select);
        select.setOnClickListener(v -> selectSvg());

        Button open = new Button(this);
        open.setText("Ouvrir le dernier SVG local");
        layout.addView(open);
        open.setOnClickListener(v -> openLastSvg());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        // Les vérifications Gmail et le lancement compact n'ont besoin
        // d'aucune interface : ne PAS ajouter de vue couvrant tout l'écran.
        if (!silentAutoCheck && !compactLauncher) setContentView(scroll);
        // La bulle accompagne automatiquement Jarvis après la première
        // autorisation de superposition. Aucun dialogue intrusif au lancement.
        if (getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(AUTO_BUBBLE_ON_LAUNCH, true)
                && Settings.canDrawOverlays(this)) {
            startBubbleService();
        }
        JarvisNotificationListener.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
        if (savedInstanceState == null) handleIncomingIntent(getIntent());
        if (compactLauncher) {
            // Le service flottant continue indépendamment de cette Activity.
            // moveTaskToBack(true) gardait une fenêtre vide dans la pile
            // Android, parfois affichée entièrement noire au retour.
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> {
                        if (!isFinishing() && !isBusy) finish();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        foregroundActivity = this;
        if (startBubbleAfterPermission && Settings.canDrawOverlays(this)) {
            startBubbleAfterPermission = false;
            startBubbleService();
        }
    }

    @Override
    protected void onPause() {
        if (foregroundActivity == this) foregroundActivity = null;
        super.onPause();
    }

    private void requestBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startBubbleAfterPermission = true;
            Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            setStatus("Autorise Jarvis Bridge à s'afficher au-dessus des autres applis.");
            return;
        }
        startBubbleService();
        setStatus("Bulle active : agrandissement puis réduction après 5 s d’inactivité.");
    }

    private void startBubbleService() {
        Intent intent = new Intent(this, JarvisBubbleService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        // Ne pas écraser le dernier diagnostic Gmail en rouvrant les réglages.
    }

    public static boolean handleGmailNoticeIfVisible() {
        MainActivity active = foregroundActivity;
        if (active == null) return false;
        // Même si une fenêtre Jarvis est ouverte, une session xTool prime.
        if (XtoolSessionManager.isActive(active)) return true;
        active.runOnUiThread(() -> active.requestGmailAndOpen());
        return true;
    }

    private void completeGmailCheck() {
        isBusy = false;
        GMAIL_CHECK_IN_PROGRESS.set(false);
        // Le SVG a ouvert xTool : aucune vérification supplémentaire ne
        // doit remettre la fenêtre Jarvis au premier plan.
        if (XtoolSessionManager.isActive(this)) {
            GMAIL_RECHECK_REQUESTED.set(false);
        }
        if (GMAIL_RECHECK_REQUESTED.getAndSet(false)) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(this::requestGmailAndOpen, 800L);
        } else if (silentAutoCheck || compactLauncher) {
            // Ne jamais laisser la fenêtre de service en arrière-plan.
            // La bulle est un service Android distinct et reste active.
            finish();
        }
    }

    private void requestGmailAndOpen() {
        if (XtoolSessionManager.isActive(this)) {
            // Pas d'accès à Gmail, pas de statut et pas de nouvelle fenêtre :
            // l'utilisateur travaille déjà avec son dessin dans xTool.
            GMAIL_RECHECK_REQUESTED.set(false);
            if (!silentAutoCheck) setStatus("xTool en cours : terminer la session depuis la bulle.");
            else finish();
            return;
        }
        if (isBusy || !GMAIL_CHECK_IN_PROGRESS.compareAndSet(false, true)) {
            GMAIL_RECHECK_REQUESTED.set(true);
            return;
        }
        isBusy = true;
        // La bulle ne doit se réveiller qu'une fois un fichier réellement
        // sélectionné. Une notification Gmail ne prouve pas un nouveau SVG.
        if (!silentAutoCheck) setStatus("Connexion Google...");
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Arrays.asList(new Scope(GMAIL_SCOPE)))
                .build();

        authorizationClient.authorize(request)
            .addOnSuccessListener(result -> {
                if (result.hasResolution()) {
                    try {
                        PendingIntent pendingIntent = result.getPendingIntent();
                        if (pendingIntent == null) {
                            throw new IllegalStateException("Choix du compte Google indisponible");
                        }
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putBoolean(AUTH_PENDING, true).apply();
                        setStatus("Google : en attente du choix de compte...");
                        authorizationLauncher.launch(
                                new IntentSenderRequest.Builder(
                                        pendingIntent.getIntentSender()).build());
                    } catch (Exception e) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putBoolean(AUTH_PENDING, false).apply();
                        completeGmailCheck();
                        JarvisBubbleService.onOperationFailed();
                        setStatus("Autorisation impossible : " + e.getMessage());
                    }
                } else {
                    acceptAuthorizationResult(result);
                }
            })
            .addOnFailureListener(e -> {
                completeGmailCheck();
                JarvisBubbleService.onOperationFailed();
                setStatus("Google : " + e.getMessage());
            });
    }


    /**
     * Reçoit l'autorisation depuis le contrat AndroidX. Ne jamais conclure
     * à une annulation sans examiner d'abord les données retournées par Google.
     */
    private void handleGoogleAuthorizationReturn(int resultCode, Intent data) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(AUTH_PENDING, false).apply();
        if (data != null) {
            try {
                AuthorizationResult authorized =
                        authorizationClient.getAuthorizationResultFromIntent(data);
                if (resultCode == Activity.RESULT_OK) {
                    setStatus("Google : autorisation reçue, lecture Gmail...");
                    acceptAuthorizationResult(authorized);
                    return;
                }
                // Résultat contradictoire : jamais supposer la réussite.
                completeGmailCheck();
                JarvisBubbleService.onOperationFailed();
                setStatus("Google : résultat non confirmé (" + resultCode
                        + "), aucun accès Gmail lancé.");
                return;
            } catch (ApiException error) {
                completeGmailCheck();
                JarvisBubbleService.onOperationFailed();
                setStatus("Google OAuth : code " + error.getStatusCode()
                        + " — vérifier le client OAuth Android et le certificat SHA-1.");
                return;
            } catch (Exception error) {
                completeGmailCheck();
                JarvisBubbleService.onOperationFailed();
                setStatus("Google : résultat illisible — " + error.getClass().getSimpleName());
                return;
            }
        }
        completeGmailCheck();
        JarvisBubbleService.onOperationFailed();
        setStatus("Google : aucune donnée de retour (résultat " + resultCode
                + "). Vérifier package Android, SHA-1 et consentement OAuth.");
    }

    private void acceptAuthorizationResult(AuthorizationResult result) {
        String token = result.getAccessToken();
        if (token == null || token.isEmpty()) {
            completeGmailCheck();
            JarvisBubbleService.onOperationFailed();
            setStatus("Aucun jeton Gmail. Autorisation nécessaire.");
            return;
        }
        if (!silentAutoCheck) setStatus("Recherche du dernier SVG dans Gmail...");
        new Thread(() -> {
            try {
                fetchNewestGmailSvg(token);
            } catch (Exception e) {
                JarvisBubbleService.onOperationFailed();
                runOnUiThread(() -> {
                    setStatus("Gmail : " + e.getMessage());
                    // La fermeture automatique est centralisée dans completeGmailCheck().
                });
            } finally {
                runOnUiThread(this::completeGmailCheck);
            }
        }).start();
    }

    private void fetchNewestGmailSvg(String token) throws Exception {
        if (XtoolSessionManager.isActive(this)) return;
        JSONObject labels = getJson(API + "/labels", token);
        JSONArray all = labels.optJSONArray("labels");
        String labelId = null;
        if (all != null) for (int i = 0; i < all.length(); i++) {
            JSONObject item = all.getJSONObject(i);
            if (LABEL_NAME.equals(item.optString("name"))) {
                labelId = item.getString("id");
                break;
            }
        }
        if (labelId == null) throw new Exception("Libellé Jarvis SVG introuvable");

        // Gmail renvoie les messages du plus récent au plus ancien.
        // Le serveur filtre déjà les sujets Jarvis envoyés depuis ce compte.
        String query = API + "/messages?labelIds="
                + URLEncoder.encode(labelId, "UTF-8")
                + "&q=" + URLEncoder.encode("subject:JARVIS-SVG-AUTO in:sent", "UTF-8")
                + "&maxResults=50";
        JSONArray messages = getJson(query, token).optJSONArray("messages");
        if (messages == null || messages.length() == 0) {
            if (!silentAutoCheck) runOnUiThread(() ->
                    setStatus("Aucun courriel sous Jarvis SVG"));
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> processed = new HashSet<>(prefs.getStringSet(
                HANDLED_GMAIL_IDS, Collections.emptySet()));
        String previous = prefs.getString(LAST_GMAIL_ID, "");
        long lastProcessedTime = prefs.getLong(LAST_GMAIL_TIMESTAMP, 0L);
        String lastHash = prefs.getString(LAST_SVG_SHA256, "");
        // Migration depuis Jarvis 1.2.1 : utiliser la date du dernier
        // courriel réellement traité, pour ne pas rouvrir les anciens
        // SVG non lus après la mise à jour.
        if (lastProcessedTime == 0L && !previous.isEmpty()) {
            int separator = previous.indexOf(':');
            String previousMessageId = separator > 0
                    ? previous.substring(0, separator) : previous;
            try {
                JSONObject lastMessage = getJson(API + "/messages/"
                        + URLEncoder.encode(previousMessageId, "UTF-8")
                        + "?format=minimal", token);
                lastProcessedTime = Long.parseLong(
                        lastMessage.optString("internalDate", "0"));
                if (lastProcessedTime > 0L) {
                    prefs.edit().putLong(LAST_GMAIL_TIMESTAMP, lastProcessedTime).apply();
                }
            } catch (Exception ignored) {
                // Si l'ancien courriel a été effacé, la liste des IDs
                // traités demeure notre deuxième protection.
            }
        }

        // Gmail peut afficher deux notifications pour un message. De plus,
        // une correction vocale peut générer deux courriels différents.
        // On choisit seulement le SVG valide le plus récent, jamais un ancien
        // dessin en attente qui pourrait rouvrir xTool au prochain signal.
        JSONObject newestMessage = null;
        JSONObject newestPart = null;
        String newestId = null;
        String newestFilename = null;
        String newestIdentity = null;
        long newestTime = lastProcessedTime;
        for (int i = 0; i < messages.length(); i++) {
            String id = messages.getJSONObject(i).getString("id");
            // Ne pas recharger une pièce jointe d'un message déjà réglé :
            // Gmail peut notifier plusieurs fois le même courriel.
            if (JarvisMailLedger.wasLastMessage(id, previous)) break;
            JSONObject message = getJson(API + "/messages/"
                    + URLEncoder.encode(id, "UTF-8") + "?format=full", token);
            long timestamp;
            try { timestamp = Long.parseLong(message.optString("internalDate", "0")); }
            catch (NumberFormatException ignored) { continue; }
            if (timestamp <= lastProcessedTime) break;
            JSONObject payload = message.optJSONObject("payload");
            JSONArray labelIds = message.optJSONArray("labelIds");
            boolean selfSent = false;
            if (labelIds != null) for (int k = 0; k < labelIds.length(); k++) {
                if ("SENT".equals(labelIds.optString(k))) selfSent = true;
            }

            String subject = null;
            JSONArray headers = payload == null ? null : payload.optJSONArray("headers");
            if (headers != null) for (int k = 0; k < headers.length(); k++) {
                JSONObject header = headers.optJSONObject(k);
                if (header != null && "subject".equalsIgnoreCase(header.optString("name"))) {
                    subject = header.optString("value");
                    break;
                }
            }
            // Le libellé Gmail seul n'est pas une preuve de provenance.
            // Ce flux est réservé aux fichiers envoyés par le compte lui-même.
            if (!JarvisMailGate.allow(subject, selfSent)) continue;
            JSONObject part = findSvgPart(payload);
            if (part == null) continue;
            String filename = part.optString("filename", "piece.svg");
            String identity = JarvisMailLedger.identity(id, filename);
            if (timestamp <= newestTime
                    || JarvisMailLedger.isAlreadyHandled(identity, previous, processed))
                continue;
            newestTime = timestamp;
            newestMessage = message;
            newestPart = part;
            newestId = id;
            newestFilename = filename;
            newestIdentity = identity;
            // Ne rouvrir que le dessin le plus récent en cas d'envois groupés.
            break;
        }

        if (newestMessage == null) {
            if (!silentAutoCheck) runOnUiThread(() ->
                    setStatus("Aucun nouveau dessin SVG à ouvrir"));
            return;
        }

        JSONObject body = newestPart.optJSONObject("body");
        if (body == null) throw new Exception("Pièce SVG sans contenu");
        String encoded = body.optString("data", "");
        if (encoded.isEmpty()) {
            String attachmentId = body.optString("attachmentId", "");
            if (attachmentId.isEmpty()) throw new Exception("Pièce SVG sans identifiant");
            JSONObject attachment = getJson(API + "/messages/"
                    + URLEncoder.encode(newestId, "UTF-8")
                    + "/attachments/" + URLEncoder.encode(attachmentId, "UTF-8"), token);
            encoded = attachment.optString("data", "");
        }
        if (encoded.isEmpty()) throw new Exception("Pièce SVG vide");
        byte[] bytes = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP);
        // Une autre ouverture peut avoir lancé xTool pendant ce téléchargement.
        if (XtoolSessionManager.isActive(this)) return;
        try {
            JarvisFileRouter.requireSvgForXtool(newestFilename,
                    newestPart.optString("mimeType", ""), bytes, MAX_SVG_BYTES);
        } catch (JarvisFileRouter.RejectedFileException rejected) {
            // Un refus définitif n'est PAS une panne réseau. Mémoriser la
            // décision avant de la signaler à l'utilisateur, pour que les
            // notifications suivantes ne provoquent pas une deuxième alerte.
            rememberHandledGmail(prefs, processed, newestIdentity, newestTime);
            final String rejectedFilename = newestFilename;
            runOnUiThread(() -> {
                setStatus("Gmail : fichier refusé (" + rejectedFilename + ") : "
                        + rejected.getMessage());
                JarvisBubbleService.onFileRejected(rejected.kind);
            });
            return;
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hashBuilder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hashBuilder.append(Character.forDigit((value >>> 4) & 15, 16));
            hashBuilder.append(Character.forDigit(value & 15, 16));
        }
        String svgHash = hashBuilder.toString();
        final String chosenIdentity = newestIdentity;
        final String chosenFilename = newestFilename;
        final long chosenTime = newestTime;

        if (svgHash.equals(lastHash)) {
            // Contenu identique, même envoyé dans un courriel différent :
            // ne pas rouvrir xTool. Avancer quand même le curseur temporel.
            rememberHandledGmail(prefs, processed, chosenIdentity, chosenTime);
            runOnUiThread(() -> {
                setStatus("SVG identique déjà traité : ouverture évitée");
                // La fermeture automatique est centralisée dans completeGmailCheck().
            });
            return;
        }

        if (XtoolSessionManager.isActive(this)) return;
        JarvisBubbleService.onSvgWorkStarted();
        Uri saved = saveSvg(bytes);
        runOnUiThread(() -> {
            if (XtoolSessionManager.isActive(this)) return;
            saveLastSvg(saved);
            if (openSvg(saved)) {
                rememberHandledGmail(prefs, processed, chosenIdentity, chosenTime);
                prefs.edit().putString(LAST_SVG_SHA256, svgHash).apply();
                setStatus(XtoolSessionManager.isActive(this)
                        ? "xTool en cours : " + chosenFilename
                        : "SVG le plus récent envoyé à Android : " + chosenFilename);
            }
            // La fermeture automatique est centralisée dans completeGmailCheck().
        });
    }

    private void rememberHandledGmail(SharedPreferences prefs, Set<String> processed,
            String identity, long timestamp) {
        Set<String> remembered = JarvisMailLedger.remember(processed, identity, 100);
        processed.clear();
        processed.addAll(remembered);
        // Écrire le résultat complet d'un seul coup : la prochaine notification
        // doit voir le même message comme déjà traité, même après redémarrage.
        prefs.edit()
                .putStringSet(HANDLED_GMAIL_IDS, new HashSet<>(processed))
                .putString(LAST_GMAIL_ID, identity)
                .putLong(LAST_GMAIL_TIMESTAMP, timestamp)
                .apply();
    }

    private JSONObject findSvgPart(JSONObject part) {
        if (part == null) return null;
        String file = part.optString("filename", "").toLowerCase(Locale.ROOT);
        if (file.endsWith(".svg")) return part;
        JSONArray children = part.optJSONArray("parts");
        JSONObject otherAttachment = null;
        if (children != null) for (int i = 0; i < children.length(); i++) {
            JSONObject found = findSvgPart(children.optJSONObject(i));
            if (found == null) continue;
            String foundName = found.optString("filename", "").toLowerCase(Locale.ROOT);
            if (foundName.endsWith(".svg")) return found;
            if (otherAttachment == null) otherAttachment = found;
        }
        if (otherAttachment != null) return otherAttachment;
        // Les STEP/STL/DXF joints ne doivent pas être ignorés : on les
        // transmet au routeur qui les refuse et mémorise ce refus.
        return file.isEmpty() ? null : part;
    }

    private JSONObject getJson(String address, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != 200) throw new Exception("HTTP " + status
                    + (status == 401 ? " : reconnecte Google." : ""));
            try (InputStream in = connection.getInputStream()) {
                return new JSONObject(new String(readStream(in), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Si Android réutilise l'Activity compacte pour une notification,
        // sa vérification Gmail reste temporaire et doit se fermer à la fin.
        if (intent != null && ACTION_CHECK_GMAIL.equals(intent.getAction())
                && compactLauncher && getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getBoolean(AUTO_OPEN_ENABLED, false)) {
            silentAutoCheck = true;
        }
        handleIncomingIntent(intent);
    }

    private boolean handleIncomingIntent(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        if (ACTION_CHECK_GMAIL.equals(action)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(JarvisNotificationListener.NOTIFICATION_ID);
            requestGmailAndOpen();
            return true;
        }
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null
                && ("jarvisbridge".equalsIgnoreCase(data.getScheme())
                     || "jarvisbridge".equalsIgnoreCase(data.getScheme()))
                && "open".equalsIgnoreCase(data.getHost())) {
            String address = data.getQueryParameter("url");
            if (address == null || address.trim().isEmpty())
                showMessage("Lien Jarvis sans adresse SVG");
            else {
                urlInput.setText(address);
                downloadSvg(address.trim());
            }
            return true;
        }
        return handleSharedSvg(intent);
    }

    private boolean handleSharedSvg(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action) && !Intent.ACTION_VIEW.equals(action))
            return false;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(action)) uri = intent.getData();
        else {
            Parcelable shared = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (shared instanceof Uri) uri = (Uri) shared;
            if (uri == null) {
                ClipData clip = intent.getClipData();
                if (clip != null && clip.getItemCount() > 0) uri = clip.getItemAt(0).getUri();
            }
        }
        if (uri == null) {
            showMessage("Aucun fichier SVG reçu");
            return true;
        }
        importSharedSvg(uri);
        return true;
    }

    private void importSharedSvg(Uri uri) {
        if (blockWhileXtoolActive()) return;
        JarvisBubbleService.onSvgWorkStarted();
        showMessage("Vérification du fichier...");
        new Thread(() -> {
            try {
                String filename = displayNameForUri(uri);
                String mimeType = getContentResolver().getType(uri);
                byte[] bytes = readBytes(uri);
                JarvisFileRouter.requireSvgForXtool(
                        filename, mimeType, bytes, MAX_SVG_BYTES);
                Uri saved = saveSvg(bytes);
                runOnUiThread(() -> {
                    if (XtoolSessionManager.isActive(this)) return;
                    saveLastSvg(saved);
                    showMessage("SVG reçu et enregistré");
                    openSvg(saved);
                });
            } catch (Exception e) { showError("Importation impossible", e); }
        }).start();
    }

    private void downloadSvg(String address) {
        if (blockWhileXtoolActive()) return;
        Uri parsed;
        try {
            parsed = Uri.parse(address);
            if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getHost() == null) {
                showMessage("Utilise un lien HTTPS direct vers un SVG");
                return;
            }
            JarvisFileRouter.Kind known = JarvisFileRouter.classify(
                    parsed.getPath(), null, null);
            if (known == JarvisFileRouter.Kind.CAD_3D
                    || known == JarvisFileRouter.Kind.DXF_2D) {
                setStatus("Fichier CAD/DXF détecté : aucun transfert automatique vers xTool.");
                showMessage("Pour la P2S, exporter le dessin 2D en SVG.");
                return;
            }
        } catch (Exception e) { showMessage("Adresse HTTPS invalide"); return; }
        JarvisBubbleService.onSvgWorkStarted();
        showMessage("Téléchargement en cours...");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                if (connection.getResponseCode() != 200)
                    throw new Exception("Erreur HTTP " + connection.getResponseCode());
                if (!"https".equalsIgnoreCase(connection.getURL().getProtocol()))
                    throw new Exception("Redirection non sécurisée");
                byte[] bytes;
                try (InputStream in = connection.getInputStream()) { bytes = readStream(in); }
                JarvisFileRouter.requireSvgForXtool(
                        connection.getURL().getPath(), connection.getContentType(),
                        bytes, MAX_SVG_BYTES);
                Uri saved = saveSvg(bytes);
                runOnUiThread(() -> {
                    if (XtoolSessionManager.isActive(this)) return;
                    saveLastSvg(saved);
                    showMessage("SVG téléchargé");
                    openSvg(saved);
                });
            } catch (Exception e) { showError("Téléchargement impossible", e); }
            finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    private String displayNameForUri(Uri uri) {
        String filename = uri.getLastPathSegment();
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                    filename = cursor.getString(0);
                }
            } catch (RuntimeException ignored) {
                // Certains fournisseurs Android refusent la requête : analyser
                // alors le contenu du fichier et son type MIME.
            }
        }
        return filename == null ? "" : filename;
    }

    private byte[] readBytes(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("Impossible de lire le fichier partagé");
            return readStream(in);
        }
    }

    private byte[] readStream(InputStream in) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > MAX_SVG_BYTES)
                    throw new Exception("Fichier trop volumineux (maximum 5 Mo)");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

     private Uri saveSvg(byte[] bytes) throws Exception {
        if (Build.VERSION.SDK_INT < 29) throw new Exception("Android 10 ou plus récent requis");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME,
                "jarvis_" + System.currentTimeMillis() + ".svg");
        values.put(MediaStore.Downloads.MIME_TYPE, "image/svg+xml");
        values.put(MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/JarvisBridge");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri destination = getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (destination == null) throw new Exception("Impossible de créer le SVG");
        try {
            try (OutputStream out = getContentResolver().openOutputStream(destination)) {
                if (out == null) throw new Exception("Impossible d'écrire le SVG");
                out.write(bytes);
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(destination, ready, null, null);
            return destination;
        } catch (Exception e) {
            getContentResolver().delete(destination, null, null);
            throw e;
        }
    }

    private void selectSvg() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_SVG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SVG && resultCode == RESULT_OK
                && data != null && data.getData() != null) importSharedSvg(data.getData());
    }

    private void saveLastSvg(Uri uri) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(LAST_SVG, uri.toString()).apply();
    }

    private void openLastSvg() {
        if (blockWhileXtoolActive()) return;
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(LAST_SVG, null);
        if (saved == null) { showMessage("Choisis d'abord un SVG"); return; }
        openSvg(Uri.parse(saved));
    }

    /**
     * Ouvre les SVG avec xTool s'il est declare compatible, en essayant les
     * types utilises par les gestionnaires de fichiers Samsung. La v0.7
     * laissait Android resoudre ACTION_VIEW directement ; la v1.1.0
     * imposait un chooser qui pouvait masquer l'association par defaut.
     *
     * Ne jamais cibler explicitement une autre version de Jarvis Bridge :
     * cela creerait une boucle de reouverture des fichiers.
     */
    private boolean openSvg(Uri uri) {
        if (blockWhileXtoolActive()) return false;
        PackageManager pm = getPackageManager();
        String[] mimeTypes = {
                "image/svg+xml", "application/octet-stream",
                "application/xml", "text/xml", "*/*"
        };
        Intent xToolIntent = null;
        ArrayList<Intent> safeViewers = new ArrayList<>();

        for (String mime : mimeTypes) {
            Intent probe = new Intent(Intent.ACTION_VIEW);
            probe.setDataAndType(uri, mime);
            probe.setClipData(ClipData.newUri(getContentResolver(), "Jarvis SVG", uri));
            probe.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> handlers = pm.queryIntentActivities(
                    probe, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo handler : handlers) {
                if (handler.activityInfo == null) continue;
                String packageName = handler.activityInfo.packageName;
                String activityName = handler.activityInfo.name;
                if (packageName == null || activityName == null) continue;
                String normalized = packageName.toLowerCase(Locale.ROOT);
                String label = String.valueOf(handler.loadLabel(pm))
                        .toLowerCase(Locale.ROOT);
                if (normalized.equals("ca.jarvis.bridge")
                        || normalized.startsWith("ca.jarvis.bridge.")
                        || label.startsWith("jarvis bridge")) continue;

                Intent explicit = new Intent(probe);
                explicit.setClassName(packageName, activityName);
                if (normalized.contains("xtool") || label.contains("xtool")) {
                    xToolIntent = explicit;
                    break;
                }
                if ("image/svg+xml".equals(mime)) safeViewers.add(explicit);
            }
            if (xToolIntent != null) break;
        }

        try {
            if (xToolIntent != null) {
                startActivity(xToolIntent);
                onSvgHandoffSucceeded("ouverture directe xTool");
                return true;
            }

            // Retour au comportement de la v0.7 : Android conserve sa
            // propre association par defaut (comme depuis Mes fichiers).
            Intent standard = new Intent(Intent.ACTION_VIEW);
            standard.setDataAndType(uri, "image/svg+xml");
            standard.setClipData(ClipData.newUri(
                    getContentResolver(), "Jarvis SVG", uri));
            standard.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            ResolveInfo defaultHandler = pm.resolveActivity(
                    standard, PackageManager.MATCH_DEFAULT_ONLY);
            if (defaultHandler != null && defaultHandler.activityInfo != null) {
                String pkg = defaultHandler.activityInfo.packageName;
                String label = String.valueOf(defaultHandler.loadLabel(pm))
                        .toLowerCase(Locale.ROOT);
                if (pkg != null
                        && (pkg.equals("ca.jarvis.bridge")
                            || pkg.startsWith("ca.jarvis.bridge.")
                            || label.startsWith("jarvis bridge"))) {
                    JarvisBubbleService.onOperationFailed();
                    setStatus("Association SVG vers Jarvis détectée ; ouverture bloquée pour éviter une boucle.");
                    return false;
                }
            }

            if (defaultHandler == null && safeViewers.isEmpty()) {
                JarvisBubbleService.onOperationFailed();
                setStatus("SVG sauvegardé ; aucune application externe détectée.");
                return false;
            }
            // Android peut remettre le fichier à xTool même si
            // resolveActivity() renvoie un intermédiaire Android (résolveur,
            // relais, association par défaut). Ce chemin ouvre déjà xTool sur
            // le Samsung; il doit donc activer la MÊME session protégée.
            startActivity(standard);
            onSvgHandoffSucceeded("association Android");
            return true;
        } catch (Exception e) {
            JarvisBubbleService.onOperationFailed();
            setStatus("Ouverture du SVG impossible : " + e.getMessage());
            showMessage("Ouverture du SVG impossible : " + e.getMessage());
            return false;
        }
    }

    /**
     * Les deux chemins d'ouverture d'un SVG démarrent la même session.
     * Android ne fournit pas la preuve que le dessin est chargé dans xTool;
     * nous sécurisons la session dès que la demande d'ouverture a réussi.
     */
    private void onSvgHandoffSucceeded(String route) {
        XtoolSessionManager.begin(this);
        GMAIL_RECHECK_REQUESTED.set(false);
        JarvisBubbleService.onSvgOpenRequested(true);
        setStatus("xTool en cours : session protégée (" + route
                + "). Appui long sur la bulle pour terminer.");
    }

    private boolean blockWhileXtoolActive() {
        if (!XtoolSessionManager.isActive(this)) return false;
        setStatus("xTool en cours : utiliser le menu de la bulle pour terminer la session.");
        return true;
    }

    private void setStatus(String value) {
        // Persistance locale du statut, sans conserver de jeton ou de données Gmail.
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(LAST_STATUS, value).apply();
        if (status != null) status.setText(value);
    }
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private void showError(String prefix, Exception error) {
        JarvisBubbleService.onOperationFailed();
        String diagnostic = prefix + " : " + error.getMessage();
        runOnUiThread(() -> {
            setStatus(diagnostic);
            showMessage(diagnostic);
        });
    }
}
