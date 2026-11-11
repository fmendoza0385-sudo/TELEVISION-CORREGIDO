# Arquitectura

Kotlin · AndroidX · Media3 (ExoPlayer) · minSdk 21 · targetSdk 34 · sin dependencias de pago.

```
LauncherActivity ──► evalúa estado ──► ACTIVA/POR_VENCER ─► PlayerActivity
                                  ├─► NO_ACTIVADA ───────► ActivationActivity
                                  └─► VENCIDA/otros ─────► BlockingActivity ─► ActivationActivity
```

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `SubscriptionManager` | `subscription/` | **Único criterio** `isPlaybackAllowed()`; activación y refresco online |
| `SubscriptionValidator` | `subscription/` | Verifica firma y evalúa estado (función pura, probada) |
| `LicenseVerifier` | `subscription/` | ECDSA P-256 / SHA-256 con clave pública |
| `SubscriptionPolicy` | `subscription/` | Corte día 5 12:00 America/Bogota, sin gracia |
| `DeviceBindingManager` | `subscription/` | Huella del dispositivo (SHA-256 de ANDROID_ID) |
| `ActivationRepository` | `data/` | Licencia firmada + reloj confiable anti-retroceso |
| `LicenseApi` | `data/` | HTTPS al servidor; solo respuestas firmadas conceden acceso |
| `BlockingActivity` | `blocking/` | Servicio suspendido; Atrás deshabilitado |
| `ActivationActivity` | `activation/` | Captura ID y código (control remoto y táctil) |
| `PlayerActivity` | `player/` | Reproductor; no se crea sin licencia; revalida cada 15 s |
| `PlaybackDiagnostics` | `player/` | Logs técnicos con URL redactada |

## Garantías de bloqueo
1. El reproductor se crea en `onStart` **solo** si `isPlaybackAllowed()`.
2. `PlayerActivity` revalida cada 15 s y al vencer pasa a `BlockingActivity` (borra la pila).
3. `BlockingActivity` ignora Atrás y re-evalúa en `onResume`.
4. Sin conexión: se usa la licencia firmada guardada **hasta su `valid_until`**; reintento cada 60 s; nunca se extiende.
5. Reloj: hora del servidor + tiempo monotónico; tras reinicio, `max(reloj, última hora vista)` → retroceder la fecha no sirve.

## Compatibilidad de video (sección 20 del Documento Maestro)
- HLS y MPEG-TS con banderas `FLAG_ALLOW_NON_IDR_KEYFRAMES` y `FLAG_DETECT_ACCESS_UNITS`.
- `setEnableDecoderFallback(true)`; sin FFmpeg ni decodificación por software forzada.
- Detección: audio sin video, video no decodificable, error de red; reintentos limitados (3); alternancia HLS↔TS una vez.
- Logs: códec, resolución, perfil, soporte del decodificador, modelo/Android; URL solo host.
