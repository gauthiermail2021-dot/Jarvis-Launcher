package ca.jarvis.bridge;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

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
public class JarvisTestShortcutActivity extends ComponentActivity {
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
    private boolean silentBubbleSend;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        silentBubbleSend = this instanceof JarvisSilentSvgSendActivity;
        if (silentBubbleSend) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setWindowAnimations(0);
        }
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
        // The ordinary launcher icon still offers the full diagnostic UI.
        // The floating SVG bubble instead uses a distinct transparent
        // Activity, without attaching any view at all.
        if (!silentBubbleSend) setContentView(root);

        if (state == null) beginSend();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Tapping the same home-screen icon a second time reuses this
        // singleTop Activity. Every deliberate new tap starts the next test.
        beginSend();
    }

    private void beginSend() {
        // A second tap must not finish the Activity that owns an ongoing
        // Gmail authorization/send or the subsequent read-only checks.
        if (locallyStarted) {
            if (status != null) status.setText("Envoi SVG en cours.");
            return;
        }
        if (!IN_FLIGHT.compareAndSet(false, true)) {
            String notice = "Un envoi Jarvis est déjà en cours.";
            if (status != null) status.setText(notice);
            if (silentBubbleSend) {
                Toast.makeText(this, notice, Toast.LENGTH_SHORT).show();
                // This new Activity instance does not own the active send.
                finish();
            }
            return;
        }
        locallyStarted = true;
        long previousSendAt = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong("last_sent_at", 0L);
        long now = System.currentTimeMillis();
        if (previousSendAt > 0L && now - previousSendAt >= 0L
                && now - previousSendAt < 5000L) {
            finishSend("Un test vient d'être envoyé. Réessaie après quelques secondes.");
            return;
        }
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

                // An explicit user press on TEST SVG JARVIS is a request
                // to hand off a NEW drawing, even when a previous SVG import
                // left Jarvis' protected xTool-session flag active. That flag
                // normally suppresses Gmail checks, and was the reason tests
                // #001 through #003 arrived but never opened xTool.
                //
                // End the *Jarvis session flag only*, immediately BEFORE the
                // send: finish() deliberately skips all mail queued up to
                // now. Calling it AFTER sending would wrongly skip this SVG.
                // This does NOT close, overwrite, save, discard, or change
                // the existing xTool document. xTool remains responsible for
                // asking the user what to do with unsaved work.
                XtoolSessionManager.finish(this);

                JSONObject sent = post(API + "/messages/send", token,
                        new JSONObject().put("raw", raw));
                String id = sent.optString("id", "");
                if (id.isEmpty())
                    throw new IllegalStateException("Gmail n'a pas confirmé l'envoi");
                acceptedByGmail = true;
                // Protect against Gmail's own notification re-opening the
                // same test message while its directly generated SVG is handed
                // to xTool. This protection is released on any handoff error.
                XtoolSessionManager.begin(this);

                // Persist immediately after confirmed send: a later label error
                // must not reuse the same name and falsely promise "same click".
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putLong(LAST_SENT, number)
                        .putLong("last_sent_at", System.currentTimeMillis())
                        .commit();

                post(API + "/messages/" + id + "/modify", token,
                        new JSONObject().put("addLabelIds",
                                new JSONArray().put(labelId)));

                runOnUiThread(() -> {
                    try {
                        // The exact SVG has ALREADY been validated and sent.
                        // Do not start a Gmail reader Activity just to obtain
                        // a second copy of these same bytes.
                        JarvisTestSvgLocalHandoff.saveAndOpen(this, filename, svg);
                        JarvisBubbleService.onSvgOpenRequested(true);
                        // The bubble's local fast path must arm the SAME
                        // Conception -> Dégrouper one-shot as Gmail import.
                        // Without this, SVG opens but never ungroups.
                        ChatGptBubbleAccessibilityService
                                .requestAutomaticUngroupAfterSvg();
                        finishSend("SVG envoyé et ouverture xTool demandée !\n"
                                + filename);
                        // The transparent sender should never remain as an
                        // empty black window behind xTool.
                        if (silentBubbleSend) finish();
                    } catch (Exception openError) {
                        XtoolSessionManager.finish(this);
                        JarvisBubbleService.onOperationFailed();
                        finishSend("SVG envoyé dans Gmail, mais xTool non ouvert : "
                                + openError.getClass().getSimpleName() + " : "
                                + String.valueOf(openError.getMessage()));
                    }
                });
            } catch (Exception error) {
                final boolean sent = acceptedByGmail;
                if (sent) XtoolSessionManager.finish(this);
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
        // Successful sends stay transparent until the explicit Gmail
        // handoff above. Errors close the invisible Activity promptly.
        if (silentBubbleSend && !text.startsWith("Envoyé et étiqueté")) {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
