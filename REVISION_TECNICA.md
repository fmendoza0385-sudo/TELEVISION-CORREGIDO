# Revisión técnica — LUKKA Televisión

## Cambios incluidos
- Renovación visual del reproductor con barra superior de marca, indicador de señal en vivo, OSD inferior de canal y ayudas para control remoto.
- Pantalla de inicio con identidad visual oscura azul, manteniendo intacta la navegación de suscripción.
- Ajuste del búfer de Media3/ExoPlayer para reproducción en vivo y variaciones breves de red. Se conserva la selección HLS/MPEG-TS, los reintentos, el fallback de decodificador y la comprobación de suscripción.
- Incremento de versión Android a 1.1 (versionCode 2).

## Hallazgos que requieren información del operador
1. El ZIP recibido no contiene `m3u_plan_50.m3u`, `m3u_plan_80.m3u` ni `m3u_plan_120.m3u`. `PlaylistRepository` busca exactamente esos tres archivos en `app/src/main/assets/`. Sin las listas reales, la aplicación mostrará el aviso de que no hay lista de canales para el plan. No se inventaron canales ni URLs.
2. `LICENSE_API_URL` apunta a `https://api.grmconexion.com/` y `LICENSE_PUBLIC_KEY` está vacío en `app/build.gradle`. La validación real de suscripciones depende de que el servidor y la clave pública estén configurados correctamente.
3. El JSON `LUKKA_Banco_Codigos_Activacion_TEMPORAL.json` está incluido dentro del ZIP. Debe tratarse como dato sensible de operación: no publicarlo en repositorios públicos ni incluirlo en APK de producción. No se modificó ni se replicó.
4. No se encontró un entorno Android SDK/Gradle configurado en este espacio para certificar una compilación local. El flujo de GitHub Actions existente usa JDK 17, Gradle Wrapper y `assembleDebug`; debe ejecutarse en GitHub para confirmar el resultado de compilación.

## Controles de funcionamiento preservados
- Puerta de acceso y evaluación de estado de suscripción.
- Activación, bloqueo y validación de licencias.
- Selección de lista según plan.
- Cambio de canal con CH+/CH−, deslizamiento y teclado numérico.
- Reproducción HLS/M3U8 y flujo progresivo MPEG-TS.
- Reintentos de reproducción, manejo de ventana en vivo y bloqueo si la suscripción deja de estar autorizada.


## Corrección de compilación — v1.3 (15-11-2026)

- Se conserva una sola definición de los modelos LUKKA en `LukkaConfig.kt`.
- Se excluye el archivo obsoleto oculto `.LukkaConfig.kt`, que duplicaba las clases `LukkaConfig`, `OfflinePolicy`, `LukkaPlan`, `LukkaUser` y `ActivationCode`.
- `LukkaAuthManager.kt` queda alineado con el JSON vigente (`monthly_codes`, `activation_code`, `status`, `plan_id` y `plan_name`) y comprueba de forma segura los campos opcionales.
- No se modifican el reproductor IPTV, la reproducción M3U8/HLS/MPEG-TS, el cambio de canales ni las reglas de suscripción.
- La corrección se verificó revisando la estructura del ZIP y la correspondencia de los modelos con el JSON. La compilación Android debe confirmarse ejecutando el workflow de GitHub Actions.
