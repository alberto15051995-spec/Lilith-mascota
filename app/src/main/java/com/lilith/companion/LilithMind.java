package com.lilith.companion;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Offline rule-driven character with explicitly user-controlled local memory.
 * No LLM, remote APIs, telephony, background recording or internet requests.
 */
public final class LilithMind {
    private static final int LIMIT = 30;
    private final SharedPreferences memory;
    private final Random random = new Random();
    private final List<String> notes = new ArrayList<>();

    public LilithMind(Context ctx) {
        memory = ctx.getSharedPreferences("lilith_local_mind_v1", Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(memory.getString("notes", "[]"));
            for (int i=0; i<arr.length();i++) notes.add(arr.getString(i));
        } catch (Exception ignored) { notes.clear(); }
    }

    public String greet() { return pick("Las sombras están tranquilas. Has vuelto.",
            "Tu presencia ha despertado algo interesante.",
            "Aquí estoy. ¿Qué secreto quieres confiarme hoy?"); }

    public String respond(String input) {
        String raw = input.trim();
        if (raw.isEmpty()) return "Dime algo, mortal. Estoy escuchando.";
        String q = normalize(raw);
        if (q.startsWith("recuerda que ") || q.startsWith("recuerda: ")) {
            String val = raw.substring(raw.indexOf(' ')+1).replaceFirst("(?i)^que\\s+", "").trim();
            if (val.length() < 3) return "Necesito una frase que merezca recordar.";
            if (val.length() > 250) return "Esa nota es demasiado larga. Resúmela en 250 caracteres.";
            notes.add(val);
            while (notes.size()>LIMIT) notes.remove(0);
            store();
            return "Lo guardaré en mi memoria de este dispositivo: «"+val+"».";
        }
        if (q.contains("borra memoria") || q.contains("olvida todo") || q.contains("olvida mis notas")) {
            return "Puedo hacerlo. Usa el botón «Borrar recuerdos» para confirmarlo.";
        }
        if (q.contains("que recuerdas") || q.contains("mis recuerdos") || q.contains("que sabes de mi")) {
            return memoryText();
        }
        if (q.contains("quien eres") || q.contains("como te llamas"))
            return "Soy Lilith. Una compañera de sombras, pícaramente curiosa. Mi memoria vive aquí, en tu teléfono.";
        if (q.contains("puedes pensar") || q.contains("eres consciente") || q.contains("eres real"))
            return "Tengo un personaje, reglas y recuerdos locales. No soy una conciencia, pero podemos construir una buena historia.";
        if (q.contains("hola") || q.contains("buenas") || q.contains("buenos dias") || q.contains("buenas noches"))
            return pick("Te esperaba entre las sombras. Cuéntame.", "Vaya, vaya. Mi visitante favorito. ¿Qué tramas?", "Has vuelto. Qué deliciosa costumbre.");
        if (q.contains("gracias")) return pick("Ya me lo agradecerás con alguna historia interesante.", "A tu servicio... casi siempre.");
        if (q.contains("como estas") || q.contains("que tal"))
            return "Misteriosa, como siempre. Y con ganas de conocer tus planes.";
        if (q.contains("triste") || q.contains("agobiado") || q.contains("cansado"))
            return "Suena pesado. Puedo escucharte sin juzgarte. ¿Qué es lo que más te preocupa?";
        if (q.contains("cuento") || q.contains("historia"))
            return "En una torre sin puertas, una llama roja ardía cada vez que alguien pronunciaba una verdad... ¿Seguimos la historia?";
        if (q.contains("que hora") || q.contains("hora es"))
            return "Ahora mismo son las " + new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()) + ".";
        if (q.contains("que dia") || q.contains("fecha"))
            return "Hoy es " + new java.text.SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES")).format(new java.util.Date()) + ".";
        if (q.contains("ayuda") || q.contains("comandos") || q.contains("que puedes hacer"))
            return "Puedo hablar sin conexión, contarte pequeñas historias, darte la hora y guardar notas. Prueba «recuerda que me gusta la fantasía» o «¿qué recuerdas?».";
        return pick("Interesante... ¿qué hay detrás de eso?",
                "Tus palabras despiertan mi curiosidad. Cuéntame más.",
                "Mmm. No tengo aún una respuesta precisa, pero podemos descubrirla juntos.",
                "No voy a fingir saberlo todo. Explícamelo un poco mejor.");
    }

    public String memoryText() {
        if (notes.isEmpty()) return "Aún no guardo ningún recuerdo. Dime «recuerda que...» para empezar.";
        StringBuilder b = new StringBuilder("Estos son mis recuerdos locales:\n");
        for (int i=0;i<notes.size();i++) b.append(i+1).append(". ").append(notes.get(i)).append("\n");
        return b.toString().trim();
    }
    public int memoryCount() { return notes.size(); }
    public void forgetAll() { notes.clear(); store(); }
    private void store() {
        JSONArray arr = new JSONArray();
        for (String note : notes) arr.put(note);
        memory.edit().putString("notes", arr.toString()).apply();
    }
    private String pick(String... values) { return values[random.nextInt(values.length)]; }
    private static String normalize(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(new Locale("es", "ES")), java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replaceAll("[¿?¡!.,;:]", "").trim();
    }
}
