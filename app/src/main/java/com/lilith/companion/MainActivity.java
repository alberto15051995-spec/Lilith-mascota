package com.lilith.companion;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private Spinner voiceSpinner, personalitySpinner, engineSpinner, neuralSpinner;
    private AzureNeuralTts neural;
    private SecretStore secrets;
    private EditText regionInput, keyInput;
    private static final String[] NEURAL_NAMES = {"es-ES-ElviraNeural", "es-ES-AbrilNeural", "es-ES-ArabellaMultilingualNeural", "es-ES-IsidoraMultilingualNeural"};
    private SeekBar pitchBar, speedBar;
    private TextView pitchLabel, speedLabel, status;
    private final List<Voice> voices = new ArrayList<>();
    private boolean ready = false;
    private static final int DEFAULT_PITCH_PROGRESS = 50; // 0.50 + 50 / 100 = 1.00x
    private static final int DEFAULT_SPEED_PROGRESS = 60; // 0.50 + 60 / 100 = 1.10x
    private static final int DEFAULT_PERSONALITY = 0; // Misteriosa
    private static final String[] PERSONALITIES = {"Misteriosa", "Traviesa", "Elegante", "Cariñosa", "Sarcástica"};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("lilith_preferences", MODE_PRIVATE);
        neural = new AzureNeuralTts(this); secrets = new SecretStore(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xff150e18);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,36,28,36);
        scroll.addView(root); setContentView(scroll);
        TextView title = label("LILITH COMPANION", 25, 0xffee536c); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        root.addView(label("v0.2 · motor neuronal opcional · independiente",14,0xffc8a6ae));
        MascotView mascot = new MascotView(this);
        root.addView(mascot,new LinearLayout.LayoutParams(-1,300));
        root.addView(label("Toca a Lilith para que reaccione",14,0xffe4cbd0));
        mascot.setOnClickListener(v -> { mascot.react(); speak("¿Ya me echabas de menos?", false); });
        root.addView(label("Motor de voz",19,Color.WHITE));
        engineSpinner = new Spinner(this);
        engineSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Android (sin clave, permite modo local)","Azure Speech (neuronal, requiere Internet y puede tener coste)"}));
        engineSpinner.setSelection(prefs.getInt("engine",0)); root.addView(engineSpinner);
        engineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                prefs.edit().putInt("engine",position).apply();
                if (position==0) neural.stop(); else if (tts!=null) tts.stop();
            }
        });
        root.addView(label("Voz neuronal española (Azure)",16,Color.WHITE));
        neuralSpinner = new Spinner(this);
        neuralSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Elvira · natural", "Abril · clara", "Arabella · multilingüe", "Isidora · multilingüe"}));
        neuralSpinner.setSelection(Math.max(0,Math.min(3,prefs.getInt("neuralVoice",0)))); root.addView(neuralSpinner);
        neuralSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){prefs.edit().putInt("neuralVoice",position).apply();}
        });
        root.addView(label("Región de tu recurso Azure Speech (ejemplo: westeurope)",14,Color.WHITE));
        regionInput=new EditText(this); regionInput.setSingleLine(true); regionInput.setText(prefs.getString("azureRegion","westeurope")); root.addView(regionInput);
        root.addView(label("Clave Azure Speech (se guarda cifrada en este dispositivo)",14,Color.WHITE));
        keyInput=new EditText(this); keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(keyInput);
        try { if(!secrets.load().isEmpty()) keyInput.setHint("Clave guardada · deja vacío para conservarla"); }
        catch(Exception e) { keyInput.setHint("No se pudo recuperar la clave anterior"); }
        Button saveAzure=new Button(this); saveAzure.setText("Guardar conexión neuronal"); root.addView(saveAzure);
        saveAzure.setOnClickListener(v -> {
            String region=regionInput.getText().toString().trim().toLowerCase(Locale.ROOT);
            if(!region.matches("[a-z0-9]{2,24}")){status.setText("Región incorrecta: escribe solo el identificador de Azure.");return;}
            try {
                String newKey=keyInput.getText().toString().trim();
                if(!newKey.isEmpty()) secrets.save(newKey);
                prefs.edit().putString("azureRegion",region).apply();
                keyInput.setText("");keyInput.setHint("Clave guardada · deja vacío para conservarla");
                status.setText("Configuración de Azure guardada. Pulsa Probar voz.");
            } catch(Exception e){status.setText("No se pudo almacenar la clave de forma segura.");}
        });
        Button clearAzure=new Button(this);clearAzure.setText("Borrar clave neuronal");root.addView(clearAzure);
        clearAzure.setOnClickListener(v -> {neural.stop();secrets.clear();keyInput.setText("");keyInput.setHint("Introduce una nueva clave");status.setText("Clave neuronal eliminada del dispositivo.");});
        root.addView(label("Voz Android (solo para el motor del teléfono)",19,Color.WHITE));
        voiceSpinner = new Spinner(this); root.addView(voiceSpinner);
        root.addView(label("Tono",16,Color.WHITE));
        pitchLabel = label("",14,0xffe6bcc5); root.addView(pitchLabel);
        pitchBar = new SeekBar(this); pitchBar.setMax(150); pitchBar.setProgress(prefs.getInt("pitch",DEFAULT_PITCH_PROGRESS)); root.addView(pitchBar);
        root.addView(label("Velocidad",16,Color.WHITE));
        speedLabel = label("",14,0xffe6bcc5); root.addView(speedLabel);
        speedBar = new SeekBar(this); speedBar.setMax(150); speedBar.setProgress(prefs.getInt("speed",DEFAULT_SPEED_PROGRESS)); root.addView(speedBar);
        pitchLabel.setText(String.format(Locale.getDefault(),"%.2fx", pitch()));
        speedLabel.setText(String.format(Locale.getDefault(),"%.2fx", speed()));
        pitchBar.setOnSeekBarChangeListener(listener("pitch",pitchLabel,true));
        speedBar.setOnSeekBarChangeListener(listener("speed",speedLabel,false));
        root.addView(label("Personalidad",19,Color.WHITE));
        personalitySpinner = new Spinner(this);
        personalitySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PERSONALITIES));
        personalitySpinner.setSelection(prefs.getInt("personality",DEFAULT_PERSONALITY)); root.addView(personalitySpinner);
        personalitySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){prefs.edit().putInt("personality",position).apply();}
        });
        Button test = new Button(this); test.setText("▶ Probar voz de Lilith"); root.addView(test);
        test.setOnClickListener(v -> speak(sample(),true));
        Button stop = new Button(this); stop.setText("■ Detener voz"); root.addView(stop);
        stop.setOnClickListener(v -> { neural.stop(); if(tts!=null)tts.stop(); status.setText("Reproducción detenida."); });
        status=label("Cargando las voces disponibles…",13,0xffe5a3b1); root.addView(status);
        tts = new TextToSpeech(this, result -> {
            if(result == TextToSpeech.SUCCESS) {
                ready = true;
                runOnUiThread(this::loadVoices);
            } else runOnUiThread(() -> status.setText("No se ha podido iniciar el motor de voz del teléfono."));
        });
    }
    private SeekBar.OnSeekBarChangeListener listener(String pref, TextView label, boolean isPitch) {
        return new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                float value=0.5f+progress/100f;
                label.setText(String.format(Locale.getDefault(),"%.2fx",value));
                if(fromUser) prefs.edit().putInt(pref,progress).apply();
            }
        };
    }
    private float pitch(){ return .5f+pitchBar.getProgress()/100f; }
    private float speed(){ return .5f+speedBar.getProgress()/100f; }
    private TextView label(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);v.setPadding(0,10,0,10);return v;}
    private void loadVoices(){
        if(!ready)return;
        voices.clear();
        Set<Voice> available=tts.getVoices();
        if(available!=null){
            for(Voice voice:available) {
                String lang=voice.getLocale().getLanguage();
                if("es".equals(lang)) voices.add(voice);
            }
        }
        voices.sort(Comparator.comparing(v -> v.getLocale().toLanguageTag()+" " + v.getName()));
        List<String> labels=new ArrayList<>(); labels.add("Voz predeterminada del sistema");
        for(Voice voice:voices){
            String network=voice.isNetworkConnectionRequired()?" · requiere red":" · local";
            labels.add(voice.getLocale().toLanguageTag()+" · "+voice.getName()+network);
        }
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        String saved=prefs.getString("voice","");
        for(int i=0;i<voices.size();i++)if(voices.get(i).getName().equals(saved)){voiceSpinner.setSelection(i+1);break;}
        voiceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                prefs.edit().putString("voice", position==0?"":voices.get(position-1).getName()).apply();
            }
        });
        status.setText("Voces en español detectadas: "+voices.size()+". Las opciones dependen de tu motor de voz instalado.");
    }
    private String sample(){switch(personalitySpinner.getSelectedItemPosition()){
        case 1: return "Uy, mira quién ha venido. ¿Vienes a jugar conmigo, travieso?";
        case 2: return "Bienvenido. Tu presencia no ha pasado desapercibida.";
        case 3: return "Hola, cariño. Me alegra volver a escucharte.";
        case 4: return "Vaya, qué sorpresa. ¿Otra vez necesitas que te salve?";
        default: return "He despertado entre las sombras. Dime, ¿qué deseas de mí?";
    }}
    private void speak(String message,boolean flush){
        if (engineSpinner.getSelectedItemPosition() == 1) {
            if(tts!=null) tts.stop();
            String key;
            try { key = secrets.load(); }
            catch(Exception e) { status.setText("No se puede descifrar la clave de Azure; bórrala y guárdala de nuevo."); return; }
            String region = regionInput.getText().toString().trim().toLowerCase(Locale.ROOT);
            String savedRegion = prefs.getString("azureRegion", "westeurope");
            if(!region.equals(savedRegion)) { status.setText("Guarda primero la nueva región de Azure."); return; }
            neural.speak(message, NEURAL_NAMES[neuralSpinner.getSelectedItemPosition()], savedRegion,
                key, pitch(), speed(), text -> status.setText(text));
            return;
        }
        neural.stop();
        if(!ready || tts==null){Toast.makeText(this,"La voz Android aún no está lista",Toast.LENGTH_SHORT).show();return;}
        tts.setLanguage(new Locale("es","ES"));
        int pos=voiceSpinner.getSelectedItemPosition();
        if(pos>0 && pos<=voices.size()) {
            Voice voice=voices.get(pos-1);
            int res=tts.setVoice(voice);
            if(res==TextToSpeech.ERROR){status.setText("No se pudo seleccionar esa voz.");return;}
        }
        tts.setPitch(pitch()); tts.setSpeechRate(speed());
        tts.speak(message,flush?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,null,"lilith_test");
    }
    @Override protected void onDestroy(){neural.shutdown();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
