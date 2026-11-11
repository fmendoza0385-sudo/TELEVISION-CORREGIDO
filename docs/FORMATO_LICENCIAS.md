# Formato de licencias y banco de códigos

## 1. Banco JSON temporal (solo importación)

`LUKKA_Banco_Codigos_Activacion_TEMPORAL.json` — esquema `lukka.subscription-code-bank` v`1.0.0`.

| Campo | Contenido |
|---|---|
| `plans[]` | `PLAN-50`, `PLAN-80`, `PLAN-120` con nº de canales |
| `summary` | 500 usuarios (`80105001`–`80105500`), 36 códigos por usuario, 18.000 códigos únicos |
| `users[].user_id / plan_id` | ID de usuario y plan asignado |
| `users[].monthly_codes[]` | `activation_code` (16 dígitos), `renewal_number` (1–36), `type`, `status` |

**Este archivo contiene secretos en texto plano y NO se incluye en este ZIP.** Se importa al servidor con
`server/tools/import_bank.php`, que guarda cada código como HMAC-SHA256 (no reversible sin el *pepper*).
`docs/ejemplo_banco_codigos.json` muestra la forma con datos falsos.

## 2. Respuesta firmada del servidor (activación y estado)

```json
{ "payload": "<base64 del JSON de abajo>", "signature": "<base64 firma ECDSA P-256 / SHA-256, DER>" }
```

Contenido de `payload` (esquema v1):

| Campo | Significado |
|---|---|
| `schema_version` | `1` |
| `result` | `VALID` (cualquier otro valor nunca habilita) |
| `user_id` | ID de usuario / subcuenta |
| `license_id` | Identificador de licencia |
| `plan_id` | Plan autorizado → lista M3U |
| `renewal_number` | Última renovación aplicada (1–36) |
| `valid_from_ms` / `valid_until_ms` | Vigencia (epoch ms). `valid_until_ms` = siguiente corte (día 5, 12:00, America/Bogota) |
| `device_id` | Huella del dispositivo autorizado (32 hex) |
| `server_time_ms` | Hora del servidor al firmar (base del reloj confiable) |

La app verifica la firma con la **clave pública** (variable `LUKKA_LICENSE_PUBLIC_KEY`). La **clave privada** vive solo en el servidor.

## 3. Errores del servidor (nunca conceden acceso)

`FORMATO_INVALIDO`, `USUARIO_NO_EXISTE`, `CODIGO_INVALIDO`, `CODIGO_USADO`, `CICLO_ANTERIOR`,
`CODIGO_FUERA_DE_ORDEN`, `DISPOSITIVO_NO_AUTORIZADO`, `LICENCIA_REVOCADA`, `NO_ACTIVADA`,
`DEMASIADOS_INTENTOS`, `ERROR_INTERNO`.

## 4. Endpoints

| Método | Ruta | Cuerpo | Efecto |
|---|---|---|---|
| POST | `/activate.php` | `user_id`, `code`, `device_id`, `device_model` | Consume el código (atómico, un solo uso), vincula el equipo, devuelve licencia firmada |
| POST | `/status.php` | `user_id`, `device_id` | Re-firma la licencia vigente con hora actual; **no extiende vigencia** |

## 5. Regla de vigencia implementada en el servidor

- Los códigos de un usuario se canjean **en orden** (renovación 1, 2, 3…).
- `valid_until` = primer corte posterior a *ahora*; si se renueva antes de vencer, posterior al vencimiento actual (se acumulan).
- Una primera activación a mitad de mes dura solo hasta el siguiente día 5 → **ver decisión pendiente en el README**.
