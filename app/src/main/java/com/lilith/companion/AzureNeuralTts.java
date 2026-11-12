package com.lilith.companion;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Asynchronous Azure Neural TTS adapter. No cloud call occurs without a configured user key. */
public final class AzureNeuralTts {
    public interface Callback { void status(String message); }
    private final android.content.Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> active;
    private MediaPlayer player;
    private File tempFile;
    private volatile long generation = 0;
    public AzureNeuralTts(android.content.Context context) { this.context = context.getApplicationContext(); }
    private static String escapeXml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
    static String ssml(String content, String voice, float pitch, float speed) {
        int pitchPercent = Math.round((pitch - 1f) * 100);
        int ratePercent = Math.round((speed - 1f) * 100);
        return "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"es-ES\">"
            + "<voice name=\"" + escapeXml(voice) + "\"><prosody pitch=\""
            + String.format(Locale.US, "%+d%%", pitchPercent) + "\" rate=\""
            + String.format(Locale.US, "%+d%%", ratePercent) + "\">"
            + escapeXml(content) + "</prosody></voice></speak>";
    }
    public synchronized void stop() {
        generation++;
        if (active != null) active.cancel(true);
        main.post(() -> {
            if (player != null) { player.release(); player = null; }
            if (tempFile != null) { tempFile.delete(); tempFile = null; }
        });
    }
    public void shutdown() { stop(); worker.shutdownNow(); }
    public synchronized void speak(String text, String voice, String region, String key,
                                   float pitch, float speed, Callback callback) {
        stop();
        if (key == null || key.isEmpty()) { callback.status("Configura una clave de Azure Speech antes de usar la voz neuronal."); return; }
        if (region == null || !region.matches("[a-z0-9]{2,24}")) { callback.status("La región de Azure no es válida."); return; }
        final long requestId = generation;
        active = worker.submit(() -> {
            HttpURLConnection conn = null;
            try {
                main.post(() -> callback.status("Generando voz neuronal…"));
                URL url = new URL("https://" + region + ".tts.speech.microsoft.com/cognitiveservices/v1");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST"); conn.setConnectTimeout(12000); conn.setReadTimeout(25000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Ocp-Apim-Subscription-Key", key);
                conn.setRequestProperty("Content-Type", "application/ssml+xml; charset=utf-8");
                conn.setRequestProperty("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3");
                conn.setRequestProperty("User-Agent", "LilithCompanion/0.2");
                byte[] body = ssml(text, voice, pitch, speed).getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = conn.getOutputStream()) { out.write(body); }
                int code = conn.getResponseCode();
                if (code != 200) throw new IllegalStateException(code == 401 || code == 403 ?
                    "Azure ha rechazado la clave o los permisos (HTTP " + code + ")." :
                    "Azure no pudo generar la voz (HTTP " + code + "). Revisa región, voz y cuota.");
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (InputStream in = conn.getInputStream()) {
                    byte[] part = new byte[8192]; int n;
                    while ((n = in.read(part)) >= 0) {
                        if (Thread.currentThread().isInterrupted()) return;
                        if (buf.size() + n > 4_000_000) throw new IllegalStateException("El audio supera el límite de seguridad.");
                        buf.write(part, 0, n);
                    }
                }
                File audio = File.createTempFile("lilith_neural_", ".mp3", context.getCacheDir());
                try (FileOutputStream out = new FileOutputStream(audio)) { out.write(buf.toByteArray()); }
                main.post(() -> {
                    if (requestId != generation) { audio.delete(); return; }
                    try {
                        tempFile = audio;
                        player = new MediaPlayer();
                        player.setDataSource(audio.getAbsolutePath());
                        player.setOnCompletionListener(mp -> { mp.release(); if (player == mp) player = null; audio.delete(); if (tempFile == audio) tempFile = null; callback.status("Prueba completada."); });
                        player.prepare(); player.start(); callback.status("Reproduciendo voz neuronal: " + voice);
                    } catch (Exception e) { audio.delete(); callback.status("No se pudo reproducir el audio generado."); }
                });
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    String message = e instanceof IllegalStateException ? e.getMessage() : "Error de conexión con Azure Speech.";
                    main.post(() -> { if (requestId == generation) callback.status(message); });
                }
            } finally { if (conn != null) conn.disconnect(); }
        });
    }
}
