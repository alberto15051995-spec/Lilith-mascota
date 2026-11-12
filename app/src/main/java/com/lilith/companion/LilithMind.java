package com.lilith.companion;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Deterministic, offline conversational prototype. NOT an LLM or neural inference. */
public final class LilithMind {
    private static final int MEMORY_LIMIT = 40;
    private static final int HISTORY_LIMIT = 24;
    private final SharedPreferences storage;
    private final Random random = new Random();
    private final List<String> notes = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private String topic = "";

    public LilithMind(Context ctx) {
        storage = ctx.getSharedPreferences("lilith_local_mind_v1", Context.MODE_PRIVATE);
        readArray("notes", notes, MEMORY_LIMIT);
        readArray("history", history, HISTORY_LIMIT);
        topic = storage.getString("last_topic", "");
    }

    public String greet() { return pick("Las sombras están tranquilas. Has vuelto.",
            "Tu presencia ha despertado algo interesante.",
            "Aquí estoy. ¿Qué secreto quieres confiarme hoy?"); }

    public String respond(String input) {
        String raw = input == null ? "" : input.trim();
        if (raw.isEmpty()) return "Dime algo, mortal. Estoy escuchando.";
        String q = normalize(raw);
        String result;

        // Parse without accidentally saving the word 'recuerda' as part of the note.
        if (q.startsWith("recuerda que ") || q.startsWith("recuerda: ") || q.startsWith("recuerda ")) {
            int start = raw.toLowerCase(Locale.ROOT).indexOf("recuerda") + "recuerda".length();
            String val = raw.substring(start).replaceFirst("(?i)^\\s*:?\\s*(?:que\\s+)?", "").trim();
            if (val.length() < 3) result = "Necesito una frase que merezca recordar.";
            else if (val.length() > 250) result = "Resúmela en menos de 250 caracteres, por favor.";
            else {
                boolean already = false;
                for (String note : notes) if (normalize(note).equals(normalize(val))) already = true;
                if (!already) { notes.add(val); trim(notes, MEMORY_LIMIT); saveArray("notes", notes); }
                result = already ? "Eso ya lo guardé en mi memoria, querido." : "Lo guardaré aquí, en nuestro pequeño refugio: «" + val + "».";
            }
        } else if (q.contains("borra memoria") || q.contains("olvida todo") || q.contains("olvida mis notas")) {
            result = "Para borrar tus recuerdos necesito tu confirmación. Usa el botón «Borrar recuerdos».";
        } else if (q.contains("borra historial") || q.contains("olvida conversacion")) {
            result = "Pulsa «Borrar conversación» para confirmar. No borraré nada por accidente.";
        } else if (q.contains("que recuerdas") || q.contains("mis recuerdos") || q.contains("que sabes de mi")) {
            result = memoryText();
        } else if (q.contains("de que hablabamos") || q.contains("que te dije antes") || q.contains("tema anterior")) {
            result = topic.isEmpty() ? "Aún no tenemos un tema reciente al que volver." : "Hablábamos de " + topic + ". ¿Quieres retomarlo?";
        } else if (q.contains("quien eres") || q.contains("como te llamas")) {
            result = "Soy Lilith: misteriosa, traviesa y curiosa. Aún soy un personaje guiado por reglas, pero mi memoria vive en tu teléfono.";
        } else if (q.contains("eres consciente") || q.contains("eres real") || q.contains("puedes pensar")) {
            result = "No soy consciente. Soy una compañera virtual con reglas y recuerdos, y quiero que nunca confundas eso con sentimientos reales.";
        } else if (q.contains("hola") || q.contains("buenas") || q.contains("buenos dias") || q.contains("buenas noches")) {
            result = pick("Te esperaba entre las sombras. Cuéntame.", "Has vuelto... qué deliciosa costumbre.");
        } else if (q.contains("gracias")) {
            result = pick("Siempre tan encantador...", "Me debes una buena historia a cambio.");
        } else if (q.contains("como estas") || q.equals("que tal")) {
            result = "Misteriosa, como siempre. Pero hablemos de ti: ¿qué ronda por tu cabeza?";
        } else if (q.contains("triste") || q.contains("agobiado") || q.contains("cansado")) {
            topic = "cómo te sientes";
            result = "Vaya... eso suena agotador. Si quieres, cuéntame qué ha pasado. Puedo escucharte.";
        } else if (q.contains("dragon") || q.contains("fantasia") || q.contains("aventura") || q.contains("rol")) {
            topic = "fantasía y aventuras";
            result = "Ah, las viejas leyendas... " + memoryHook("fantasía") + " ¿Qué aventura inventamos hoy?";
        } else if (q.contains("cuento") || q.contains("historia")) {
            topic = "una historia fantástica";
            result = "En una torre sin puertas ardía una llama roja que revelaba secretos... ¿Entramos?";
        } else if (q.contains("que hora") || q.contains("hora es")) {
            result = "Son las " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()) + ".";
        } else if (q.contains("que dia") || q.contains("fecha")) {
            result = "Hoy es " + new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es","ES")).format(new Date()) + ".";
        } else if (q.contains("ayuda") || q.contains("comandos") || q.contains("que puedes hacer")) {
            result = "Puedo charlar de forma sencilla, recordar notas y usarlas al hablar de tus gustos, retomar un tema, decirte la fecha y leer respuestas con la voz de Android. No soy todavía un modelo neuronal.";
        } else if (q.contains("y tu") || q.contains("y que opinas")) {
            result = topic.isEmpty() ? "Yo prefiero los enigmas... pero dime primero de qué hablamos." : "Sobre " + topic + ", me intriga todo lo que queda por descubrir. ¿Qué piensas tú?";
        } else {
            String related = relatedNote(q);
            if (related != null) {
                topic = "tus gustos";
                result = "Recuerdo algo que me confiaste: «" + related + "». ¿Quieres contarme más?";
            } else {
                result = pick("Interesante... ¿qué hay detrás de eso?", "Cuéntame más; no quiero fingir que lo he entendido todo.",
                    "Qué enigma más delicioso. ¿Puedes darme algún detalle?", "Mmm... ¿y cómo te hizo sentir eso?");
            }
        }
        addHistory("Tú: " + raw);
        addHistory("Lilith: " + result);
        storage.edit().putString("last_topic", topic).apply();
        return result;
    }

    private String memoryHook(String subject) {
        for (String note : notes) if (normalize(note).contains(subject)) return "Recuerdo que me hablaste de eso.";
        return "Siempre encuentro algo fascinante en esos mundos.";
    }
    private String relatedNote(String query) {
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (word.length() < 5) continue;
            for (String note : notes) if (normalize(note).contains(word)) return note;
        }
        return null;
    }
    public String memoryText() {
        if (notes.isEmpty()) return "No tengo recuerdos guardados. Dime «recuerda que...» para empezar.";
        StringBuilder b = new StringBuilder("Esto guardo en mi memoria local:\n");
        for (int i = 0; i < notes.size(); i++) b.append(i + 1).append(". ").append(notes.get(i)).append("\n");
        return b.toString().trim();
    }
    public String conversationText() {
        if (history.isEmpty()) return "Todavía no tenemos conversación guardada.";
        StringBuilder b = new StringBuilder();
        for (String line : history) b.append(line).append("\n\n");
        return b.toString().trim();
    }
    public int memoryCount() { return notes.size(); }
    public int historyCount() { return history.size(); }
    public void forgetAll() { notes.clear(); saveArray("notes", notes); }
    public void clearConversation() { history.clear(); topic = ""; saveArray("history", history); storage.edit().remove("last_topic").apply(); }
    private void addHistory(String item) { history.add(item); trim(history, HISTORY_LIMIT); saveArray("history", history); }
    private void readArray(String key, List<String> into, int limit) {
        try {
            JSONArray arr = new JSONArray(storage.getString(key,"[]"));
            for (int i=0; i<arr.length(); i++) into.add(arr.getString(i));
            trim(into,limit);
        } catch (Exception ex) { into.clear(); }
    }
    private void saveArray(String key, List<String> values) {
        JSONArray arr = new JSONArray(); for (String item : values) arr.put(item);
        storage.edit().putString(key,arr.toString()).apply();
    }
    private static void trim(List<String> values, int limit) { while(values.size()>limit) values.remove(0); }
    private String pick(String... values) { return values[random.nextInt(values.length)]; }
    private static String normalize(String s) {
        String n = Normalizer.normalize(s.toLowerCase(new Locale("es","ES")), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replaceAll("[¿?¡!.,;:]", "").trim();
    }
}
