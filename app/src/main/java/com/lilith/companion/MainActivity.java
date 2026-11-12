package com.lilith.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lilith Companion 0.5: offline-first, user-initiated character chat and local TTS. */
public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LilithMind mind;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private MediaPlayer samplePlayer;
    private final List<Voice> voices = new ArrayList<>();
    private Spinner voiceSpinner;
    private SeekBar pitchBar, speedBar;
    private TextView pitchValue, speedValue, status, response, memoryCount;
    private EditText messageInput;
    private CheckBox speakAnswers;
    private MascotView mascot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("lilith_preferences", MODE_PRIVATE);
        mind = new LilithMind(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xff150e18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(30));
        scroll.addView(root); setContentView(scroll);
        TextView title=label("LILITH COMPANION", 25, 0xffee536c);
        title.setTypeface(null, Typeface.BOLD); root.addView(title);
        root.addView(label("v0.5 · Sin nube · Personalidad: Misteriosa", 14, 0xffc8a6ae));
        mascot = new MascotView(this);
        root.addView(mascot, new LinearLayout.LayoutParams(-1,dp(240)));
        mascot.setOnClickListener(v -> { mascot.react(); sayAnswer(mind.greet()); });
        root.addView(label("Tócame o escríbeme algo. Te escucho sin enviar datos a Internet.", 14, 0xffd8abb5));

        response=label(mind.greet(),17,Color.WHITE); response.setMinHeight(dp(76));
        response.setBackgroundColor(0xff321d2c); response.setPadding(dp(12),dp(10),dp(12),dp(10)); root.addView(response);
        messageInput = new EditText(this); messageInput.setSingleLine(false); messageInput.setMinLines(1);
        messageInput.setMaxLines(4); messageInput.setHint("Habla con Lilith... (texto)");
        messageInput.setTextColor(Color.WHITE); messageInput.setHintTextColor(0xffb89ca5);
        root.addView(messageInput);
        Button ask = button(root,"✦ Hablar con Lilith");
        ask.setOnClickListener(v -> {
            String text = messageInput.getText().toString();
            if(text.trim().isEmpty()) return;
            String result = mind.respond(text);
            sayAnswer(result); messageInput.setText("");
            memoryCount.setText("Recuerdos guardados: " + mind.memoryCount());
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(messageInput.getWindowToken(),0);
        });
        memoryCount=label("Recuerdos guardados: " + mind.memoryCount(),14,0xffdc9fae); root.addView(memoryCount);
        Button showMemory=button(root,"Ver recuerdos locales");
        showMemory.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Memoria de Lilith")
                .setMessage(mind.memoryText()).setPositiveButton("Cerrar",null).show());
        Button clearMemory=button(root,"Borrar recuerdos");
        clearMemory.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("¿Borrar todos los recuerdos?")
                .setMessage("Se eliminarán las notas guardadas por Lilith en este teléfono.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Borrar",(d,w)->{mind.forgetAll();memoryCount.setText("Recuerdos guardados: 0");sayAnswer("Memoria borrada. Podemos comenzar de nuevo.");}).show());

        Button showConversation=button(root,"Ver conversación reciente");
        showConversation.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Conversación local")
                .setMessage(mind.conversationText()).setPositiveButton("Cerrar",null).show());
        Button clearConversation=button(root,"Borrar conversación");
        clearConversation.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("¿Borrar conversación?")
                .setMessage("El historial reciente y el tema actual se borrarán. Los recuerdos explícitos seguirán guardados.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Borrar",(d,w)->sayAnswerAfterDeletion()).show());
        root.addView(label("Motor conversacional local (reglas y contexto; no es una IA neuronal)",13,0xffd9adb7));
        root.addView(label("Voz local del teléfono",20,Color.WHITE));
        root.addView(label("Solo mostraremos voces de español que no requieran conexión. La voz de Suno es una referencia, no un clon sintetizado.",13,0xffd9adb7));
        voiceSpinner = new Spinner(this); root.addView(voiceSpinner);
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Buscando voces locales..."}));
        root.addView(label("Tono",15,Color.WHITE));
        pitchValue=label("",14,0xffeea9ba);root.addView(pitchValue);
        pitchBar=new SeekBar(this);pitchBar.setMax(150);root.addView(pitchBar);
        root.addView(label("Velocidad",15,Color.WHITE));
        speedValue=label("",14,0xffeea9ba);root.addView(speedValue);
        speedBar=new SeekBar(this);speedBar.setMax(150);root.addView(speedBar);
        // Existing user preferences are preserved; defaults on clean install remain 1.00x / 1.10x.
        pitchBar.setProgress(prefs.getInt("pitch",50));
        speedBar.setProgress(prefs.getInt("speed",60));
        pitchValue.setText(String.format(Locale.getDefault(),"%.2fx", pitch()));
        speedValue.setText(String.format(Locale.getDefault(),"%.2fx", speed()));
        pitchBar.setOnSeekBarChangeListener(seek("pitch",pitchValue,true));
        speedBar.setOnSeekBarChangeListener(seek("speed",speedValue,false));
        Button reset=button(root,"Restablecer tono 1,00x · velocidad 1,10x");
        reset.setOnClickListener(v->{pitchBar.setProgress(50); speedBar.setProgress(60);
            prefs.edit().putInt("pitch",50).putInt("speed",60).apply();});
        speakAnswers = new CheckBox(this); speakAnswers.setText("Leer las respuestas en voz alta");
        speakAnswers.setTextColor(Color.WHITE);speakAnswers.setChecked(prefs.getBoolean("auto_speak",true));root.addView(speakAnswers);
        speakAnswers.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean("auto_speak",checked).apply());
        Button test=button(root,"▶ Probar voz local");
        test.setOnClickListener(v -> sayAnswer("He despertado entre las sombras. Dime, ¿qué deseas de mí?"));
        root.addView(label("Referencia femenina de Suno",19,Color.WHITE));
        Button suno=button(root,"▶ Escuchar muestra original");
        suno.setOnClickListener(v -> playReference());
        Button stop=button(root,"■ Detener audio");
        stop.setOnClickListener(v->{stopAudio();status.setText("Audio detenido.");});
        status=label("Iniciando voz local...",13,0xffeea9ba);root.addView(status);
        root.addView(label("Privacidad: las notas se guardan en el teléfono. No hay servidores, API ni reconocimiento por micrófono. Las voces deben estar descargadas para funcionar sin conexión.",12,0xffb79da4));
        tts = new TextToSpeech(this, result -> {
            ttsReady = result == TextToSpeech.SUCCESS;
            status.post(() -> {
                if(ttsReady) loadOfflineVoices();
                else status.setText("No se ha iniciado la voz Android. Comprueba tu motor TTS instalado.");
            });
        });
    }

    private void sayAnswerAfterDeletion() {
        mind.clearConversation();
        sayAnswer("He olvidado nuestra conversación reciente. Tus recuerdos siguen a salvo.");
    }
    private void sayAnswer(String text) {
        response.setText(text); mascot.react();
        if(speakAnswers.isChecked()) playSpeech(text);
    }
    private void playSpeech(String text) {
        stopAudio();
        if(!ttsReady) {status.setText("Motor TTS no disponible; puedes seguir leyendo las respuestas.");return;}
        int selected=voiceSpinner.getSelectedItemPosition();
        if(selected<0 || selected>=voices.size()) {status.setText("No hay voz española local. Descarga una voz sin conexión en los ajustes del dispositivo.");return;}
        Voice voice=voices.get(selected);
        if(voice.isNetworkConnectionRequired()){status.setText("Esa voz requiere red; el modo local no la utilizará.");return;}
        if(tts.setVoice(voice)==TextToSpeech.ERROR){status.setText("No se pudo activar la voz seleccionada.");return;}
        tts.setPitch(pitch());tts.setSpeechRate(speed());
        int r=tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"lilith_local_"+System.nanoTime());
        status.setText(r==TextToSpeech.ERROR ? "No se pudo hablar con esa voz local." : "Voz local: "+voice.getName());
    }
    private void loadOfflineVoices() {
        voices.clear();
        Set<Voice> available=tts.getVoices();
        if(available!=null) for(Voice v:available)
            if("es".equals(v.getLocale().getLanguage()) && !v.isNetworkConnectionRequired()) voices.add(v);
        voices.sort(Comparator.comparing(v -> v.getLocale().toLanguageTag()+"/"+v.getName()));
        List<String> names=new ArrayList<>();
        for(Voice v:voices) names.add(v.getLocale().toLanguageTag()+" · "+v.getName());
        if(names.isEmpty()) names.add("Sin voces españolas locales instaladas");
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        String saved=prefs.getString("local_voice","");
        for(int i=0;i<voices.size();i++) if(voices.get(i).getName().equals(saved)) {voiceSpinner.setSelection(i);break;}
        voiceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int pos,long id) {
                if(pos<voices.size()) prefs.edit().putString("local_voice",voices.get(pos).getName()).apply();
            }
        });
        status.setText(voices.isEmpty()?"No hay voces españolas sin conexión instaladas.":"Voces españolas locales disponibles: "+voices.size());
    }
    private void playReference() {
        stopAudio();
        try(AssetFileDescriptor fd=getAssets().openFd("lilith_suno_referencia.mp3")) {
            MediaPlayer p=new MediaPlayer();
            p.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            p.setOnCompletionListener(mp->{mp.release();if(samplePlayer==mp) samplePlayer=null;status.setText("Referencia terminada.");});
            p.prepare(); samplePlayer=p; p.start(); status.setText("Escuchando muestra de Suno (no síntesis).");
        }catch(Exception e){status.setText("No se ha podido reproducir el audio de referencia.");}
    }
    private void stopAudio(){if(tts!=null)tts.stop();if(samplePlayer!=null){samplePlayer.release();samplePlayer=null;}}
    private SeekBar.OnSeekBarChangeListener seek(String key,TextView value,boolean isPitch) {
        return new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
            public void onProgressChanged(SeekBar s,int progress,boolean fromUser){
                value.setText(String.format(Locale.getDefault(),"%.2fx",0.5+progress/100.0));
                if(fromUser)prefs.edit().putInt(key,progress).apply();
            }
        };
    }
    private float pitch(){return .5f+pitchBar.getProgress()/100f;}
    private float speed(){return .5f+speedBar.getProgress()/100f;}
    private Button button(LinearLayout root,String text){Button b=new Button(this);b.setText(text);root.addView(b);return b;}
    private TextView label(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(7),0,dp(7));return v;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onDestroy(){stopAudio();if(tts!=null)tts.shutdown();super.onDestroy();}
}
