package ca.jarvis.bridge;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.content.ClipData;
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

public class MainActivity extends Activity {
    private static final int PICK_SVG = 1001;
    private static final int AUTH_REQUEST = 1002;
    private static final int NOTIFICATION_PERMISSION = 1003;
    public static final String ACTION_CHECK_GMAIL = "ca.jarvis.bridge.ACTION_CHECK_GMAIL";
    private static volatile MainActivity foregroundActivity;
    private static final int MAX_SVG_BYTES = 5000000;
    private static final String PREFS = "jarvis_bridge";
    private static final String LAST_SVG = "last_svg";
    private static final String LAST_GMAIL_ID = "last_gmail_id";
    private static final String HANDLED_GMAIL_IDS = "handled_gmail_ids";
    private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final String LABEL_NAME = "Jarvis SVG";
    private static final String API = "https://gmail.googleapis.com/gmail/v1/users/me";
    private EditText urlInput;
    private TextView status;
    private AuthorizationClient authorizationClient;
    private boolean isBusy = false;
    private boolean startBubbleAfterPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authorizationClient = Identity.getAuthorizationClient(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 45, 30, 30);

        TextView title = new TextView(this);
        title.setText("Jarvis Bridge 0.8.1 — prototype");
        title.setTextSize(25);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText("Gmail → notification → SVG xTool (sans vérification périodique)");
        layout.addView(description);

        Button gmail = new Button(this);
        gmail.setText("Ouvrir le dernier SVG Gmail");
        layout.addView(gmail);
        gmail.setOnClickListener(v -> requestGmailAndOpen());

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
        status.setText("Prêt. Activer l’accès aux notifications Android.");
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
        setStatus("Bulle activée : 1 toucher agrandit, 2e toucher ouvre ChatGPT.");
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
        setStatus("Connexion Google...");
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Arrays.asList(new Scope(GMAIL_SCOPE)))
                .build();

        authorizationClient.authorize(request)
            .addOnSuccessListener(result -> {
                if (result.hasResolution()) {
                    try {
                        PendingIntent pendingIntent = result.getPendingIntent();
                        startIntentSenderForResult(
                            pendingIntent.getIntentSender(), AUTH_REQUEST,
                            null, 0, 0, 0);
                    } catch (Exception e) {
                        isBusy = false;
                        setStatus("Autorisation impossible : " + e.getMessage());
                    }
                } else {
                    acceptAuthorizationResult(result);
                }
            })
            .addOnFailureListener(e -> {
                isBusy = false;
                setStatus("Google : " + e.getMessage());
            });
    }

    private void acceptAuthorizationResult(AuthorizationResult result) {
        String token = result.getAccessToken();
        if (token == null || token.isEmpty()) {
            isBusy = false;
            setStatus("Aucun jeton Gmail. Autorisation nécessaire.");
            return;
        }
        setStatus("Recherche du dernier SVG dans Gmail...");
        new Thread(() -> {
            try {
                fetchNewestGmailSvg(token);
            } catch (Exception e) {
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
                } else {
                    setStatus("SVG enregistré, ouverture xTool impossible");
                }
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
                && "jarvisbridge".equalsIgnoreCase(data.getScheme())
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
        if (requestCode == AUTH_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    acceptAuthorizationResult(authorizationClient.getAuthorizationResultFromIntent(data));
                } catch (Exception e) {
                    isBusy = false;
                    setStatus("Autorisation refusée ou impossible : " + e.getMessage());
                }
            } else {
                isBusy = false;
                setStatus("Autorisation Gmail annulée.");
            }
            return;
        }
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

    private boolean openSvg(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/svg+xml");
        intent.setClipData(ClipData.newUri(getContentResolver(), "Jarvis SVG", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            showMessage("Aucune application compatible ou fichier inaccessible");
            return false;
        }
    }

    private void setStatus(String value) {
        if (status != null) status.setText(value);
    }
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private void showError(String prefix, Exception error) {
        runOnUiThread(() -> showMessage(prefix + " : " + error.getMessage()));
    }
}
