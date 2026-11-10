package ca.jarvis.bridge;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated *external Android launcher icon* "TEST SVG JARVIS".
 *
 * One tap = send the same 44 x 30 mm SVG to the signed-in Gmail account.
 * The attachment filename and a non-visible comment increment after each
 * confirmed send. This OPTIONAL activity asks for gmail.modify permission
 * only on first use, as sending AND applying the "Jarvis SVG" label requires
 * explicit Google authorization; the main Jarvis Gmail reader stays readonly.
 *
 * Does not control xTool, machinery, laser, passes, speed, or power.
 */
public final class JarvisTestShortcutActivity extends ComponentActivity {
    private static final String PREFS = "jarvis_test_button";
    private static final String LAST_SENT = "last_sent_number";
    private static final String API = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final String SEND_AND_LABEL_SCOPE =
            "https://www.googleapis.com/auth/gmail.modify";
    private static final String REQUIRED_LABEL = "Jarvis SVG";
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);

    private AuthorizationClient google;
    private ActivityResultLauncher<IntentSenderRequest> authLauncher;
    private TextView status;
    private boolean locallyStarted;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        google = Identity.getAuthorizationClient(this);
        authLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), response -> {
                    if (response.getResultCode() != Activity.RESULT_OK
                            || response.getData() == null) {
                        fail("Autorisation Gmail annulée. Aucun SVG envoyé.");
                        return;
                    }
                    try {
                        AuthorizationResult grant = google
                                .getAuthorizationResultFromIntent(response.getData());
                        sendWithToken(grant.getAccessToken());
                    } catch (Exception error) {
                        fail("Autorisation Google : " + error.getClass().getSimpleName());
                    }
                });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 44, 32, 30);
        TextView title = new TextView(this);
        title.setText("TEST SVG JARVIS");
        title.setTextSize(22);
        root.addView(title);
        status = new TextView(this);
        status.setTextSize(17);
        root.addView(status);
        Button send = new Button(this);
        send.setText("Envoyer un nouveau SVG test");
        root.addView(send);
        send.setOnClickListener(v -> beginSend());
        setContentView(root);

        if (state == null) beginSend();
    }

    private void beginSend() {
        if (locallyStarted || !IN_FLIGHT.compareAndSet(false, true)) {
            status.setText("Un envoi Jarvis est déjà en cours.");
            return;
        }
        locallyStarted = true;
        long next = nextNumber();
        status.setText("Préparation de " + JarvisRepeatableTestSvg.filename(next)
                + "\nConnexion Gmail…");
        AuthorizationRequest authorization = AuthorizationRequest.builder()
                .setRequestedScopes(Arrays.asList(new Scope(SEND_AND_LABEL_SCOPE)))
                .build();
        google.authorize(authorization)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        try {
                            PendingIntent consent = result.getPendingIntent();
                            if (consent == null)
                                throw new IllegalStateException("Google consent unavailable");
                            authLauncher.launch(new IntentSenderRequest.Builder(
                                    consent.getIntentSender()).build());
                        } catch (Exception e) {
                            fail("Autorisation Gmail indisponible : "
                                    + e.getClass().getSimpleName());
                        }
                    } else {
                        sendWithToken(result.getAccessToken());
                    }
                })
                .addOnFailureListener(error ->
                        fail("Gmail : autorisation requise ou indisponible ("
                                + error.getClass().getSimpleName() + ")"));
    }

    private long nextNumber() {
        long last = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(LAST_SENT, 0L);
        if (last >= 999_999_999L) return 1L;
        return last + 1L;
    }

    private void sendWithToken(String token) {
        if (token == null || token.isEmpty()) {
            fail("Aucun jeton Gmail : SVG non envoyé.");
            return;
        }
        new Thread(() -> {
            final long number = nextNumber();
            final String filename = JarvisRepeatableTestSvg.filename(number);
            boolean acceptedByGmail = false;
            try {
                JSONObject profile = get(API + "/profile", token);
                String address = profile.optString("emailAddress", "");
                if (address.isEmpty() || !address.contains("@"))
                    throw new IllegalStateException("Compte Gmail non identifié");

                String labelId = findOrCreateLabel(token);
                byte[] svg = JarvisRepeatableTestSvg.bytes(number);
                JarvisFileRouter.requireSvgForXtool(filename, "image/svg+xml",
                        svg, 5_000_000);
                String mime = makeMime(address, filename,
                        JarvisRepeatableTestSvg.subject(number), svg);
                String raw = Base64.encodeToString(
                        mime.getBytes(StandardCharsets.UTF_8),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);

                JSONObject sent = post(API + "/messages/send", token,
                        new JSONObject().put("raw", raw));
                String id = sent.optString("id", "");
                if (id.isEmpty())
                    throw new IllegalStateException("Gmail n'a pas confirmé l'envoi");
                acceptedByGmail = true;

                // Persist immediately after confirmed send: a later label error
                // must not reuse the same name and falsely promise "same click".
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putLong(LAST_SENT, number).commit();

                post(API + "/messages/" + id + "/modify", token,
                        new JSONObject().put("addLabelIds",
                                new JSONArray().put(labelId)));

                runOnUiThread(() -> {
                    finishSend("Envoyé et étiqueté Jarvis SVG !\n" + filename);
                    // Trigger the existing Gmail reader; it still verifies
                    // the self-sent subject + SENT label before opening xTool.
                    if (!XtoolSessionManager.isActive(this)) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent refresh = new Intent(this, MainActivity.class);
                            refresh.setAction(MainActivity.ACTION_CHECK_GMAIL);
                            refresh.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(refresh);
                        }, 600L);
                    }
                });
            } catch (Exception error) {
                final boolean sent = acceptedByGmail;
                runOnUiThread(() -> finishSend(sent
                        ? "SVG envoyé (" + filename
                            + "), mais étiquetage Jarvis SVG à vérifier : "
                            + error.getClass().getSimpleName()
                        : "Aucun envoi confirmé. "
                            + error.getClass().getSimpleName() + " : "
                            + String.valueOf(error.getMessage())));
            }
        }, "jarvis-test-svg-mail").start();
    }

    private String findOrCreateLabel(String token) throws Exception {
        JSONArray labels = get(API + "/labels", token).optJSONArray("labels");
        if (labels != null) {
            for (int i = 0; i < labels.length(); i++) {
                JSONObject label = labels.optJSONObject(i);
                if (label != null && REQUIRED_LABEL.equals(label.optString("name")))
                    return label.getString("id");
            }
        }
        JSONObject created = post(API + "/labels", token,
                new JSONObject().put("name", REQUIRED_LABEL)
                        .put("labelListVisibility", "labelShow")
                        .put("messageListVisibility", "show"));
        String id = created.optString("id", "");
        if (id.isEmpty()) throw new IllegalStateException("Libellé Gmail introuvable");
        return id;
    }

    private static String makeMime(String email, String filename,
                                   String subject, byte[] svg) {
        String boundary = "jarvis-test-" + System.nanoTime();
        String attachment = Base64.encodeToString(svg, Base64.NO_WRAP);
        return "From: " + email + "\r\n"
                + "To: " + email + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n"
                + "\r\n--" + boundary + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n\r\n"
                + "Test Jarvis automatique. SVG non executable : aucun laser demarre.\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: image/svg+xml; name=\"" + filename + "\"\r\n"
                + "Content-Disposition: attachment; filename=\"" + filename + "\"\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + attachment + "\r\n"
                + "--" + boundary + "--\r\n";
    }

    private static JSONObject get(String url, String token) throws Exception {
        return call("GET", url, token, null);
    }

    private static JSONObject post(String url, String token, JSONObject json)
            throws Exception {
        return call("POST", url, token, json);
    }

    private static JSONObject call(String method, String address, String token,
                                   JSONObject json) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            if (json != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300)
                throw new IllegalStateException("Gmail HTTP " + response);
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) != -1) {
                    if (buffer.size() + n > 1_000_000)
                        throw new IllegalStateException("Réponse Gmail trop volumineuse");
                    buffer.write(b, 0, n);
                }
                return new JSONObject(new String(
                        buffer.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    private void fail(String message) {
        finishSend(message);
    }

    private void finishSend(String text) {
        IN_FLIGHT.set(false);
        locallyStarted = false;
        if (status != null) status.setText(text);
    }
}
