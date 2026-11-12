# Lilith Companion v0.5 — Local, Misteriosa

Aplicación Android **independiente de Lilith Voice** y sin permiso de Internet. Compilable con GitHub Actions.

## Funciones reales

- Mascota reactiva y chat por texto con **motor conversacional basado en reglas**, sin modelo neuronal incluido. Respuestas algo más variadas y referencias al último tema de conversación.
- Memoria local explícita, compatible con la memoria v0.4: «recuerda que…», «¿qué recuerdas de mí?», con hasta 40 notas. Una misma nota no se duplica.
- Historial local de las 24 últimas intervenciones (12 turnos), visible y borrable con confirmación, **independiente** de los recuerdos.
- Voz TTS de Android con voces españolas locales; tono inicial 1.00x y velocidad inicial 1.10x; se mantienen preferencias existentes.
- Audio de Suno para **escuchar la referencia**. No existe todavía clonación neuronal que produzca frases nuevas con esa voz.
- Modo sin red: la aplicación no solicita permiso `INTERNET`; comprobar que el motor de voz instalado también dispone de datos sin conexión.

## Límites reales

La v0.5 **NO** incluye un LLM neuronal, reconocimiento por micrófono, síntesis de voz clonada, mascota sobre otras apps ni acciones automáticas. Es una mejora incremental comprobable del prototipo local; estas características necesitarían dependencias y modelos Android adicionales y pruebas de rendimiento.

## Compilación con GitHub Actions

Copia el contenido **dentro** de `LilithCompanion/` a la raíz de tu repositorio (incluidas las carpetas ocultas) y sube los cambios a `main`. Abre **Actions → Compilar Lilith Companion APK**. Si no hay secretos configurados, se generará `LilithCompanion-debug-apk` y dentro estará `app-debug.apk`.

**Las APK de depuración de GitHub Actions pueden firmarse con claves diferentes entre compilaciones.** Si aparece el error de conflicto de paquete, para pasar a esta versión de prueba quizá tengas que desinstalar la anterior, perdiendo los datos de la aplicación. El objetivo es terminar usando **una firma estable** desde la primera instalación con ella.

## Firma estable, cuando decidas configurarla

El flujo acepta cuatro secretos de GitHub Actions (Settings → Secrets and variables → Actions):

- `LILITH_KEYSTORE_BASE64`: archivo JKS convertido a Base64 (sin saltos de línea).
- `LILITH_STORE_PASSWORD`: contraseña del almacén.
- `LILITH_KEY_ALIAS`: alias de la clave.
- `LILITH_KEY_PASSWORD`: contraseña de la clave.

Ejemplo **en una terminal privada** con `keytool` y `base64` disponibles (no compartas resultados ni claves en el chat):

```bash
keytool -genkeypair -v -keystore lilith-release.jks -alias lilith -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 lilith-release.jks
```

Guarda **una copia privada de respaldo del JKS y de las contraseñas**. No los subas al repositorio ni al historial de Git. Al tener los cuatro secretos correctamente definidos, Actions creará `LilithCompanion-stable-release-apk`, con `app-release.apk`. La primera instalación con esa firma nueva puede requerir desinstalar una APK debug anterior. Una vez que la primera release estable esté instalada, sus próximas actualizaciones deben conservar **el mismo identificador, clave y un versionCode creciente**.

**Seguridad:** si generas el JKS dentro de Codespaces, evita dejarlo allí: cópialo a un almacenamiento privado seguro y elimínalo del workspace. El secreto Base64 es material sensible, no protección por sí solo.

## Verificaciones antes de dar la versión por probada

1. GitHub Actions termina verde.
2. Instala la APK y abre el chat.
3. Prueba «recuerda que me gustan los dragones», «¿qué recuerdas?», «hablemos de fantasía», «¿de qué hablábamos?».
4. Cierra y abre la app; comprueba que recuerdos e historial persisten.
5. Borra el historial y comprueba que los recuerdos permanecen.
6. Prueba el audio local en modo avión; la voz Suno solo reproduce la muestra de referencia.

El código no se ha compilado ni probado en un dispositivo desde este entorno; la ejecución de Actions y la prueba en el Xiaomi determinarán su estado real.
