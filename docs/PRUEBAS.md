# Pruebas

## Automáticas (se ejecutan en GitHub Actions: `testDebugUnitTest`)
| Archivo | Cubre |
|---|---|
| `SubscriptionPolicyTest` | Corte día 5 12:00, cambio de año, sin gracia, ventana "por vencer" |
| `SubscriptionValidatorTest` | Estados, dispositivo, resultado no válido, esquema desconocido, fechas incoherentes, firma ECDSA genuina/alterada |
| `M3uParserTest` | Lectura de M3U (BOM, atributos, líneas vacías) |

## Manuales (Documento Maestro §14) — completar en dispositivo real/emulador
Marca el resultado: ✅ / ❌ y anota modelo + versión de Android.

| N.º | Escenario | Resultado esperado | Resultado |
|---|---|---|---|
| 1 | Primera instalación | Mostrar activación | |
| 2 | Código válido del ciclo actual | Activa el servicio | |
| 3 | Código incorrecto | Rechaza y mantiene bloqueo | |
| 4 | ID inexistente | Error claro | |
| 5 | Código de otro usuario | Rechaza | |
| 6 | Código de un ciclo anterior | Rechaza | |
| 7 | Vigente antes del corte | Reproduce | |
| 8 | Llega el día 5 12:00 sin renovar | Bloqueo | |
| 9 | Abrir app con licencia vencida | Bloqueo antes del video | |
| 10 | CH+/CH- bloqueado | No cambia canal | |
| 11 | Atrás bloqueado | No vuelve al reproductor | |
| 12 | Forzar cierre y reabrir | Sigue bloqueado | |
| 13 | Código válido de renovación | Reactiva y carga el plan | |
| 14 | Código válido de otro dispositivo | Rechaza | |
| 15 | Sin conexión con el servidor | Error/reintento según política | |
| 16 | Cambiar fecha del equipo | No concede acceso | |
| 17 | Android TV con control remoto | Activación operable | |
| 18 | Smartphone vertical | Legible y funcional | |
| 19 | Smartphone horizontal | Adaptable | |
| 20 | Respuesta de licencia alterada | Rechaza | |

## Matriz de reproducción (§20.7)
Registrar por canal autorizado: formato (HLS/TS), códec video/audio (ver `adb logcat -s LukkaPlayer`), dispositivo, resultado.
No declarar compatible ningún formato que no se haya probado.
