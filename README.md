# LUKKA Televisión — Android TV y Smartphone

Proyecto Android (Kotlin) con el **sistema de suscripción, renovación y bloqueo por vencimiento** definido en
[`DOCUMENTO_MAESTRO.md`](DOCUMENTO_MAESTRO.md) (v1.2), más un reproductor IPTV base y el servidor de licencias.
Organización / licencia: GRM CONEXIÓN SAS.

## ⚠️ Seguridad — leer primero
- **El banco de códigos real (`LUKKA_Banco_Codigos_Activacion_TEMPORAL.json`) NO está en este ZIP y NO debe subirse a GitHub ni ir dentro del APK.** Contiene 18.000 códigos en texto plano. Se importa al servidor con `server/tools/import_bank.php`.
- Usa un repositorio **PRIVADO** si tus listas M3U contienen usuario/clave o tokens.
- No subas claves privadas (`*.pem`), `server/config.php`, `.jks`/`.keystore` (ya están en `.gitignore`).

## Qué incluye
| Carpeta | Contenido |
|---|---|
| `app/` | App Android: arranque, bloqueo, activación, reproductor Media3, pruebas unitarias |
| `server/` | Servidor de licencias PHP + MySQL (`activate.php`, `status.php`), esquema SQL, importador del banco, generador de claves |
| `docs/` | Arquitectura, reglas, formato de licencias, plan de pruebas, banco de ejemplo con datos falsos |
| `.github/workflows/android-build.yml` | Compila el APK de depuración y lo publica como artefacto |

## Estado (Fase 1 del plan: proyecto compilable + módulo de suscripción)
Implementado: estados de suscripción, corte día 5 12:00 (Bogotá, sin gracia), pantalla *Servicio suspendido*, activación con ID + código de 16 dígitos, vínculo de dispositivo, licencia **firmada** (ECDSA P-256), reloj anti-retroceso, modo sin Internet limitado a la expiración firmada (reintento cada 60 s), reproductor con zapping (CH+/CH-, números, deslizar), OSD, diagnóstico de "audio sin imagen", servidor de licencias.

**No verificado todavía:** la compilación del APK y la ejecución en dispositivos. Este ZIP se armó sin Android SDK; la primera compilación real ocurrirá en GitHub Actions. Si falla, el log dirá exactamente qué ajustar.

## Compilar en GitHub (resumen — detalle en `docs/GUIA_GITHUB.md`)
1. Sube el contenido de la carpeta a un repositorio **privado**.
2. Define las variables `LUKKA_LICENSE_API_URL` y `LUKKA_LICENSE_PUBLIC_KEY` (Settings → Secrets and variables → Actions → **Variables**).
3. Actions → *Android Build* → *Run workflow*. Descarga el APK desde *Artifacts*.

Sin esas variables el APK compila igual, pero al activar mostrará "servicio de licencias aún no configurado".

## Pendientes antes de producción (del Documento Maestro §17 y §21.7)
1. **Logotipo oficial** y recursos gráficos (hay un icono y banner provisionales genéricos en `res/drawable`).
2. **Contacto de soporte** (teléfono/WhatsApp): `res/values/strings.xml` → `support_contact` (vacío; no se inventa).
3. **Listas M3U** por plan en `app/src/main/assets/` (ver `LEEME_LISTAS.txt`).
4. Confirmar **sin período de gracia**.
5. **Primera activación a mitad de mes:** hoy dura hasta el siguiente día 5 (puede ser pocos días). Decidir si el primer código debe cubrir un mes completo.
6. Política de **reasignación de plan/dispositivo** y reposición de códigos (existe `server/tools/reassign_device.php`).
7. Hosting HTTPS para `server/api/` y credenciales del servidor (fuera del repositorio).
8. Modelos de TV Box / celulares de prueba.
9. Firma de producción (keystore en GitHub Secrets) — la fase actual solo genera APK de depuración.


## Actualización visual y de reproducción (v1.1)

Esta revisión incorpora una interfaz de reproducción con estética de decodificador, OSD inferior, barra superior de marca y ayudas de control remoto. También actualiza Media3 a 1.4.1 y ajusta el búfer para televisión en vivo.

**Antes de compilar para producción:** agrega las listas autorizadas `m3u_plan_50.m3u`, `m3u_plan_80.m3u` y `m3u_plan_120.m3u` en `app/src/main/assets/`. El ZIP recibido no incluía esos archivos; sin ellos no aparecerán canales. Configura además la URL y clave pública reales del servicio de licencias. Consulta `REVISION_TECNICA.md`.
