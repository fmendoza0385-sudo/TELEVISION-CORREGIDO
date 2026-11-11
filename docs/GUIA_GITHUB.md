# Guía por fases — del ZIP al APK

## Fase A — Servidor de licencias (en tu hosting con PHP 7.4+/MySQL y HTTPS)
1. Crea una base de datos y ejecuta `server/sql/schema.sql`.
2. Copia `server/config.sample.php` a `server/config.php` y completa datos de BD y un `code_pepper` largo y aleatorio.
3. Por consola en el servidor: `php server/tools/generate_keys.php` → guarda la clave privada en `server/keys/` y **imprime la clave pública (base64)**. Cópiala.
4. Sube **solo `server/api/`** a una ruta pública (ej. `https://tu-dominio.com/lukka/api`). `config.php` y `keys/` deben quedar fuera de lo público o protegidos.
5. Sube el JSON del banco **por un canal seguro** (no por GitHub) y ejecuta:
   `php server/tools/import_bank.php /ruta/LUKKA_Banco_Codigos_Activacion_TEMPORAL.json --dry-run`
   y luego sin `--dry-run`. Después respalda el JSON cifrado y bórralo de forma segura.

## Fase B — GitHub
1. Crea un repositorio **privado** y sube el contenido de esta carpeta (el `.gitignore` ya excluye secretos).
2. Settings → Secrets and variables → Actions → pestaña **Variables** → New repository variable:
   - `LUKKA_LICENSE_API_URL` = la URL de la fase A (https, sin barra final)
   - `LUKKA_LICENSE_PUBLIC_KEY` = la clave pública base64
3. Actions → **Android Build (LUKKA Televisión)** → Run workflow.
4. Al terminar, descarga **LUKKA-Television-debug-apk** desde *Artifacts*.

## Fase C — Pruebas en equipos
Instala el APK en un Android TV y en un smartphone y completa `docs/PRUEBAS.md`.
Para ver diagnóstico de video: `adb logcat -s LukkaPlayer`.

## Prueba rápida sin riesgo
Importa primero `docs/ejemplo_banco_codigos.json` (usuario `99999999`, códigos `0000000000000001`/`…02`) en una base de pruebas.
