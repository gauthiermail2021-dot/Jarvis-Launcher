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
    private static final String LAST_SVG = "last_svg";
    private static final String LAST_GMAIL_ID = "last_gmail_id";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        title.setText("Jarvis Bridge 1.1 — connexion Gmail");
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
        setContentView(scroll);
        JarvisNotificationListener.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
        if (savedInstanceState == null) handleIncomingIntent(getIntent());
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
    }

    private void startBubbleService() {
        Intent intent = new Intent(this, JarvisBubbleService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        setStatus("Bulle active : agrandissement puis réduction après 5 s d’inactivité.");
    }

    public static boolean handleGmailNoticeIfVisible() {
        MainActivity active = foregroundActivity;
        if (active == null) return false;
        active.runOnUiThread(() -> active.requestGmailAndOpen());
        return true;
    }

    private void requestGmailAndOpen() {
        if (isBusy) return;
        isBusy = true;
        JarvisBubbleService.onGmailWorkStarted();
        setStatus("Connexion Google...");
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
                        isBusy = false;
                        JarvisBubbleService.onOperationFailed();
                        setStatus("Autorisation impossible : " + e.getMessage());
                    }
                } else {
                    acceptAuthorizationResult(result);
                }
            })
            .addOnFailureListener(e -> {
                isBusy = false;
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
                isBusy = false;
                JarvisBubbleService.onOperationFailed();
                setStatus("Google : résultat non confirmé (" + resultCode
                        + "), aucun accès Gmail lancé.");
                return;
            } catch (ApiException error) {
                isBusy = false;
                JarvisBubbleService.onOperationFailed();
                setStatus("Google OAuth : code " + error.getStatusCode()
                        + " — vérifier le client OAuth Android et le certificat SHA-1.");
                return;
            } catch (Exception error) {
                isBusy = false;
                JarvisBubbleService.onOperationFailed();
                setStatus("Google : résultat illisible — " + error.getClass().getSimpleName());
                return;
            }
        }
        isBusy = false;
        JarvisBubbleService.onOperationFailed();
        setStatus("Google : aucune donnée de retour (résultat " + resultCode
                + "). Vérifier package Android, SHA-1 et consentement OAuth.");
    }

    private void acceptAuthorizationResult(AuthorizationResult result) {
        String token = result.getAccessToken();
        if (token == null || token.isEmpty()) {
            isBusy = false;
            JarvisBubbleService.onOperationFailed();
            setStatus("Aucun jeton Gmail. Autorisation nécessaire.");
            return;
        }
        setStatus("Recherche du dernier SVG dans Gmail...");
        new Thread(() -> {
            try {
                fetchNewestGmailSvg(token);
            } catch (Exception e) {
                JarvisBubbleService.onOperationFailed();
                runOnUiThread(() -> setStatus("Gmail : " + e.getMessage()));
            } finally {
                runOnUiThread(() -> isBusy = false);
            }
        }).start();
    }

    private void fetchNewestGmailSvg(String token) throws Exception {
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

        String query = API + "/messages?labelIds="
                + URLEncoder.encode(labelId, "UTF-8")
                + "&labelIds=UNREAD&maxResults=50";
        JSONArray messages = getJson(query, token).optJSONArray("messages");
        if (messages == null || messages.length() == 0) {
            runOnUiThread(() -> setStatus("Aucun courriel non lu sous Jarvis SVG"));
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> processed = new HashSet<>(prefs.getStringSet(
                HANDLED_GMAIL_IDS, Collections.emptySet()));
        String previous = prefs.getString(LAST_GMAIL_ID, "");
        // Traite un SVG par déclenchement. Les doublons sont ignorés.
        for (int i = 0; i < messages.length(); i++) {
            String id = messages.getJSONObject(i).getString("id");
            JSONObject message = getJson(API + "/messages/"
                    + URLEncoder.encode(id, "UTF-8") + "?format=full", token);
            JSONObject part = findSvgPart(message.optJSONObject("payload"));
            if (part == null) continue;
            String filename = part.optString("filename", "piece.svg");
            String identity = id + ":" + filename;
            if (processed.contains(identity) || previous.equals(identity)) continue;

            JSONObject body = part.optJSONObject("body");
            if (body == null) continue;
            String encoded = body.optString("data", "");
            if (encoded.isEmpty()) {
                String attachmentId = body.optString("attachmentId", "");
                if (attachmentId.isEmpty()) continue;
                JSONObject attachment = getJson(API + "/messages/"
                        + URLEncoder.encode(id, "UTF-8")
                        + "/attachments/" + URLEncoder.encode(attachmentId, "UTF-8"), token);
                encoded = attachment.optString("data", "");
            }
            if (encoded.isEmpty()) continue;
            byte[] bytes = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP);
            validateSvg(bytes);
            JarvisBubbleService.onSvgWorkStarted();
            Uri saved = saveSvg(bytes);

            runOnUiThread(() -> {
                saveLastSvg(saved);
                if (openSvg(saved)) {
                    processed.add(identity);
                    // Conserve au plus 100 identifiants pour éviter l'accumulation.
                    if (processed.size() > 100) processed.clear();
                    processed.add(identity);
                    prefs.edit()
                            .putStringSet(HANDLED_GMAIL_IDS, new HashSet<>(processed))
                            .putString(LAST_GMAIL_ID, identity)
                            .apply();
                    setStatus("Demande d'ouverture envoyée à xTool : " + filename);
                } // En cas d'échec, conserver le diagnostic précis de openSvg().
            });
            return;
        }
        runOnUiThread(() -> setStatus("Aucun nouveau SVG non traité"));
    }

    private JSONObject findSvgPart(JSONObject part) {
        if (part == null) return null;
        String file = part.optString("filename", "").toLowerCase(Locale.ROOT);
        if (file.endsWith(".svg")) return part;
        JSONArray children = part.optJSONArray("parts");
        if (children != null) for (int i = 0; i < children.length(); i++) {
            JSONObject found = findSvgPart(children.optJSONObject(i));
            if (found != null) return found;
        }
        return null;
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
        JarvisBubbleService.onSvgWorkStarted();
        showMessage("Réception du SVG...");
        new Thread(() -> {
            try {
                byte[] bytes = readBytes(uri);
                validateSvg(bytes);
                Uri saved = saveSvg(bytes);
                runOnUiThread(() -> {
                    saveLastSvg(saved);
                    showMessage("SVG reçu et enregistré");
                    openSvg(saved);
                });
            } catch (Exception e) { showError("Importation impossible", e); }
        }).start();
    }

    private void downloadSvg(String address) {
        Uri parsed;
        try {
            parsed = Uri.parse(address);
            if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getHost() == null) {
                showMessage("Utilise un lien HTTPS direct vers un SVG");
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
                validateSvg(bytes);
                Uri saved = saveSvg(bytes);
                runOnUiThread(() -> {
                    saveLastSvg(saved);
                    showMessage("SVG téléchargé");
                    openSvg(saved);
                });
            } catch (Exception e) { showError("Téléchargement impossible", e); }
            finally { if (connection != null) connection.disconnect(); }
        }).start();
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

    private void validateSvg(byte[] bytes) throws Exception {
        String content = new String(bytes, StandardCharsets.UTF_8);
        String normalized = content.toLowerCase(Locale.ROOT);
        if (!normalized.contains("<svg")
                || normalized.contains("<!doctype")
                || normalized.contains("<!entity")
                || normalized.contains("<script")
                || normalized.contains("javascript:"))
            throw new Exception("SVG absent ou contenu non autorisé");
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
                JarvisBubbleService.onSvgOpenRequested(true);
                setStatus("SVG transmis directement à xTool.");
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
            startActivity(standard);
            JarvisBubbleService.onSvgOpenRequested(false);
            setStatus("SVG envoyé à Android pour ouverture (association par défaut).");
            return true;
        } catch (Exception e) {
            JarvisBubbleService.onOperationFailed();
            setStatus("Ouverture du SVG impossible : " + e.getMessage());
            showMessage("Ouverture du SVG impossible : " + e.getMessage());
            return false;
        }
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
        runOnUiThread(() -> showMessage(prefix + " : " + error.getMessage()));
    }
}
