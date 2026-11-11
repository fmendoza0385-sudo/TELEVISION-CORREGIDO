<?php
declare(strict_types=1);

date_default_timezone_set('America/Bogota');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

const CUTOFF_DAY = 5;
const CUTOFF_HOUR = 12;

/** Rechazo controlado: nunca concede acceso. */
final class Reject extends Exception {}

function cfg(): array
{
    static $c = null;
    if ($c === null) {
        $path = is_file(__DIR__ . '/../config.php') ? __DIR__ . '/../config.php' : __DIR__ . '/config.php';
        $c = require $path;
    }
    return $c;
}

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        $c = cfg();
        $pdo = new PDO($c['db_dsn'], $c['db_user'], $c['db_pass'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
    }
    return $pdo;
}

function input(): array
{
    $d = json_decode((string) file_get_contents('php://input'), true);
    return is_array($d) ? $d : [];
}

function respond(array $data, int $http = 200): void
{
    http_response_code($http);
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function fail(string $error, int $http = 400): void
{
    respond(['error' => $error], $http);
}

function nowMs(): int
{
    return (int) round(microtime(true) * 1000);
}

/** Primer corte (día 5, 12:00, America/Bogota) estrictamente posterior a $tsMs. Misma regla que la app. */
function nextCutoffAfter(int $tsMs): int
{
    $tz = new DateTimeZone('America/Bogota');
    $dt = (new DateTimeImmutable('@' . intdiv($tsMs, 1000)))->setTimezone($tz);
    $y = (int) $dt->format('Y');
    $m = (int) $dt->format('n');
    $cut = $dt->setDate($y, $m, CUTOFF_DAY)->setTime(CUTOFF_HOUR, 0, 0);
    if ($dt >= $cut) {
        $cut = $dt->setDate($y, $m + 1, CUTOFF_DAY)->setTime(CUTOFF_HOUR, 0, 0);
    }
    return $cut->getTimestamp() * 1000;
}

function codeHash(string $code): string
{
    return hash_hmac('sha256', $code, cfg()['code_pepper']);
}

/** Firma ECDSA P-256/SHA-256 (DER). La app verifica con la clave pública incluida en el APK. */
function signedEnvelope(array $payload): array
{
    $json = json_encode($payload, JSON_UNESCAPED_SLASHES);
    $key = openssl_pkey_get_private('file://' . cfg()['private_key_path']);
    if ($key === false || !openssl_sign($json, $sig, $key, OPENSSL_ALGO_SHA256)) {
        throw new RuntimeException('No se pudo firmar la licencia');
    }
    return ['payload' => base64_encode($json), 'signature' => base64_encode($sig)];
}

function licensePayload(array $lic, string $userId): array
{
    return [
        'schema_version' => 1,
        'result' => 'VALID',
        'user_id' => $userId,
        'license_id' => $lic['license_id'],
        'plan_id' => $lic['plan_id'],
        'renewal_number' => (int) $lic['last_renewal'],
        'valid_from_ms' => (int) $lic['valid_from_ms'],
        'valid_until_ms' => (int) $lic['valid_until_ms'],
        'device_id' => $lic['device_id'],
        'server_time_ms' => nowMs(),
    ];
}

function clientIp(): string
{
    return substr((string) ($_SERVER['REMOTE_ADDR'] ?? ''), 0, 45);
}

function audit(PDO $pdo, string $event, ?string $detail, ?string $userId, ?string $deviceId, ?string $ref = null): void
{
    $st = $pdo->prepare('INSERT INTO lukka_audit (event, detail, user_id, device_id, ip, ref) VALUES (?,?,?,?,?,?)');
    $st->execute([$event, $detail, $userId, $deviceId, clientIp(), $ref]);
}

/** Limita intentos fallidos: >15 en 10 minutos por IP o por usuario. */
function tooManyFailures(PDO $pdo, string $userId): bool
{
    $st = $pdo->prepare(
        "SELECT COUNT(*) FROM lukka_audit WHERE event='FAIL' AND created_at > (NOW() - INTERVAL 10 MINUTE)
         AND (ip = ? OR user_id = ?)"
    );
    $st->execute([clientIp(), $userId]);
    return (int) $st->fetchColumn() > 15;
}
