# Lilith Companion v0.4 · Local para Android

**Independiente de Lilith Voice.** Aplicación nativa Android en Java, sin permisos de Internet, sin Azure y sin backend.

## Funcional ahora

- Lilith animada, reacciona al tocarla.
- Chat por texto con personaje «Misteriosa» y respuestas **basadas en reglas**, no en un modelo de lenguaje neuronal.
- Memoria de notas voluntarias: `recuerda que me gusta la fantasía`, `¿qué recuerdas?`.
- Revisión de notas y borrado total con confirmación. Hasta 30 notas de 250 caracteres, almacenadas localmente.
- Respuestas habladas con **las voces españolas locales que haya instaladas en Android**. Se excluyen las voces que el motor declara como dependientes de la red.
- Tono y velocidad configurables y persistentes. Valores por defecto en instalación limpia: **1,00x y 1,10x**. Si hubo una versión anterior, se respetan sus ajustes; botón para restablecer.
- Muestra femenina de Suno reproducible sin conexión como **audio de referencia**. No es una voz clonada y no genera frases nuevas con el timbre de Suno.

## No está implementado

- **Clonación neuronal local de la voz Suno**, modelo XTTS/Piper empaquetado o un LLM local. Requieren elegir modelos, compatibilidad, licencias y benchmarks específicos del teléfono. No confundas TTS de Android con voz neuronal personalizada.
- Micrófono, activación permanente, mascota flotante sobre otras apps y control del móvil.

## Compilación

Repositorio GitHub con `.github/workflows/android-apk.yml`: después de subir estos archivos a `main`, entra en **Actions → Compilar Lilith Companion APK**, descarga el artefacto `LilithCompanion-debug-apk`, descomprímelo e instala `app-debug.apk`.

La compilación está configurada para `compileSdk 35`, Java 17, Gradle 8.9 y AGP definido en `build.gradle`. No se ha compilado una APK en este entorno; la ejecución en GitHub Actions determina si se compila sin errores.

## Privacidad

Esta versión carece de permiso `INTERNET`; no envía notas a servicios web. La voz local se apoya en el motor TTS instalado en el dispositivo, por lo que conviene descargar una voz sin conexión y probar con el modo avión. Los datos locales se borran desde la aplicación (recuerdos) o al eliminar sus datos desde Android (ajustes).

## Actualización desde v0.3

Subir el **contenido** de la carpeta `LilithCompanion/`, incluida `.github`, y sustituir el código anterior. El `applicationId` se mantiene, de modo que puede instalarse encima si el paquete se firma con una clave compatible. Las APK debug de GitHub Actions pueden usar claves distintas entre ejecuciones; si Android rechaza la actualización, tendrás que desinstalar la anterior (perderás los datos locales) e instalar la nueva.
