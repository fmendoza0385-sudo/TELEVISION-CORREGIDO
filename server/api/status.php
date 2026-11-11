<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';

// Devuelve la licencia vigente RE-FIRMADA con hora actual del servidor. NO extiende la vigencia.
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    fail('METODO_NO_PERMITIDO', 405);
}

$in = input();
$userId = (string) ($in['user_id'] ?? '');
$device = (string) ($in['device_id'] ?? '');
if (!preg_match('/^\d{4,20}$/', $userId) || !preg_match('/^[a-f0-9]{32}$/', $device)) {
    fail('FORMATO_INVALIDO');
}

try {
    $pdo = db();
    if (tooManyFailures($pdo, $userId)) {
        fail('DEMASIADOS_INTENTOS', 429);
    }
    $st = $pdo->prepare('SELECT * FROM lukka_licenses WHERE user_id = ?');
    $st->execute([$userId]);
    $lic = $st->fetch();

    if (!$lic) {
        audit($pdo, 'FAIL', 'NO_ACTIVADA', $userId, $device);
        fail('NO_ACTIVADA', 404);
    }
    if ((int) $lic['revoked'] === 1) {
        audit($pdo, 'FAIL', 'LICENCIA_REVOCADA', $userId, $device);
        fail('LICENCIA_REVOCADA', 403);
    }
    if ($lic['device_id'] === null || $lic['device_id'] !== $device) {
        audit($pdo, 'FAIL', 'DISPOSITIVO_NO_AUTORIZADO', $userId, $device);
        fail('DISPOSITIVO_NO_AUTORIZADO', 403);
    }
    respond(signedEnvelope(licensePayload($lic, $userId)));
} catch (Throwable $e) {
    error_log('[lukka] status: ' . $e->getMessage());
    fail('ERROR_INTERNO', 500);
}
