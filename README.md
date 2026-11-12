# Lilith Companion v0.2 — motor neuronal

App Android independiente de Lilith Voice, con mascota animada y voz configurable.

## Novedades v0.2
- **Azure Speech Neural TTS real** vía API REST, con SSML para tono y velocidad.
- 4 opciones de voz española: `es-ES-ElviraNeural`, `es-ES-AbrilNeural`, `es-ES-ArabellaMultilingualNeural`, `es-ES-IsidoraMultilingualNeural` (la disponibilidad puede variar según región o cuenta).
- Selector de motor: Azure neuronal con conexión o TTS Android, que puede funcionar sin conexión si el móvil tiene voces locales.
- Prueba de audio, detener, respuesta al tocar la mascota.
- Clave privada introducida por el usuario y cifrada en Android Keystore (AES-GCM). No se incluye ninguna credencial en el código. Botón para borrarla.
- Solo HTTPS y dominio de Azure generado a partir de identificador de región validado.
- Los ajustes de tono, velocidad, personalidad, voz y región se conservan localmente.

**Predeterminados:** tono 1.00x, velocidad 1.10x, personalidad Misteriosa, motor Android. El modo neuronal se activa voluntariamente tras configurar la cuenta para no provocar facturación involuntaria. Los valores que se guardaron en una instalación anterior prevalecen.

## Activar el motor neuronal
1. Crea un recurso Speech de Microsoft Azure que admita text-to-speech. Consulta la [documentación oficial](https://learn.microsoft.com/es-es/azure/ai-services/speech-service/rest-text-to-speech).
2. Copia la región del recurso (por ejemplo `westeurope`) y **su clave** en Lilith Companion; no publiques ni subas la clave a GitHub.
3. Pulsa **Guardar conexión neuronal**.
4. Escoge **Azure Speech (neuronal)** y una de las cuatro voces.
5. Ajusta tono y velocidad y pulsa **Probar voz de Lilith**.
6. Si quieres evitar consumo de Azure, vuelve al motor Android. Para borrar la clave local, pulsa **Borrar clave neuronal**.

**Costes y privacidad:** Azure puede cobrar por caracteres de voz procesados. El texto de prueba o el que Lilith pronuncie en modo neuronal se transmite a Microsoft Azure; no se envían contactos ni archivos. Se necesita Internet. La clave se almacena cifrada en este dispositivo, pero una app móvil nunca ofrece la misma protección frente a extracción/abuso que un backend propio; antes de distribuir la app a terceros, sustituir la clave del usuario en cliente por un servidor autenticado con cuotas. No habilitar claves compartidas dentro de la APK.

## Compilación
1. Abre la carpeta `LilithCompanion` en Android Studio (JDK 17, Android SDK 35, plugin Android Gradle 8.7.x).
2. Sincroniza el proyecto con Gradle. Instala lo que Android Studio solicite si es necesario.
3. Conecta tu Xiaomi por USB con depuración USB y pulsa Run, o usa **Build > Build APK(s)**.
4. APK de depuración esperada en `app/build/outputs/apk/debug/`.

## Límites actuales
- No se incluye APK compilada. No se ha ejecutado una compilación Android completa en este entorno.
- No hay aún mascota flotante, transcripción de voz, conversación con un modelo de IA, emociones acústicas reales ni generación de voces nuevas desde cero. **Personalidad** cambia el texto de la muestra, no la acústica del modelo.
- El control de pitch/rate puede ser interpretado de forma ligeramente distinta por Azure y Android TTS.
- Si hay cambio de dispositivo o restauración, la clave podría necesitar volver a introducirse; Android Keystore no permite exportarla.
- Servicio remoto opcional, no un modelo ejecutándose localmente en el móvil.

## Compilar desde GitHub Actions (sin ordenador)
El proyecto incluye `.github/workflows/android-apk.yml`. Crea un repositorio privado nuevo y sube el **contenido** de la carpeta `LilithCompanion` a la raíz (la carpeta `app`, `build.gradle`, `settings.gradle` y `.github` deben quedar en la raíz).

En GitHub > Actions > «Compilar Lilith Companion APK» > Run workflow, ejecuta la compilación. Si el workflow está habilitado y finaliza en verde, abre la ejecución y descarga el artefacto `LilithCompanion-debug-apk`. Dentro viene `app-debug.apk` para instalar en tu Android. Puede ser necesario autorizar la instalación de APKs desde tu gestor de archivos.

Esta compilación es de **depuración**, no para publicar en Play Store. No introduzcas claves Azure en GitHub, archivos del repositorio ni variables del workflow: configura tu clave exclusivamente en la app instalada. No está garantizada la compilación hasta ejecutarla y comprobar el resultado.
