<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    fail('METODO_NO_PERMITIDO', 405);
}

$in = input();
$userId = (string) ($in['user_id'] ?? '');
$code = (string) ($in['code'] ?? '');
$device = (string) ($in['device_id'] ?? '');

if (!preg_match('/^\d{4,20}$/', $userId) || !preg_match('/^\d{16}$/', $code) || !preg_match('/^[a-f0-9]{32}$/', $device)) {
    fail('FORMATO_INVALIDO');
}

$pdo = db();
if (tooManyFailures($pdo, $userId)) {
    fail('DEMASIADOS_INTENTOS', 429);
}

$ref = substr(codeHash($code), 0, 12); // huella para auditoría (nunca el código)

try {
    $pdo->beginTransaction();

    $st = $pdo->prepare('SELECT plan_id FROM lukka_users WHERE user_id = ?');
    $st->execute([$userId]);
    $user = $st->fetch();
    if (!$user) {
        throw new Reject('USUARIO_NO_EXISTE');
    }

    $st = $pdo->prepare('SELECT * FROM lukka_licenses WHERE user_id = ? FOR UPDATE');
    $st->execute([$userId]);
    $lic = $st->fetch() ?: null;

    if ($lic && (int) $lic['revoked'] === 1) {
        throw new Reject('LICENCIA_REVOCADA');
    }
    if ($lic && $lic['device_id'] !== null && $lic['device_id'] !== $device) {
        throw new Reject('DISPOSITIVO_NO_AUTORIZADO');
    }

    // Consumo atómico y de un solo uso (la fila queda bloqueada hasta el commit).
    $st = $pdo->prepare('SELECT id, renewal_number, status FROM lukka_codes WHERE user_id = ? AND code_hash = ? FOR UPDATE');
    $st->execute([$userId, codeHash($code)]);
    $row = $st->fetch();
    if (!$row) {
        throw new Reject('CODIGO_INVALIDO'); // no existe, o pertenece a otro usuario
    }
    if ($row['status'] !== 'DISPONIBLE') {
        throw new Reject('CODIGO_USADO');
    }

    $expected = $lic ? ((int) $lic['last_renewal'] + 1) : 1;
    $n = (int) $row['renewal_number'];
    if ($n < $expected) {
        throw new Reject('CICLO_ANTERIOR');
    }
    if ($n > $expected) {
        throw new Reject('CODIGO_FUERA_DE_ORDEN');
    }

    // Vigencia: hasta el siguiente corte posterior a (ahora) o, si renueva antes de vencer,
    // posterior al vencimiento actual (las renovaciones adelantadas se acumulan).
    $now = nowMs();
    $base = $lic ? max($now, (int) $lic['valid_until_ms']) : $now;
    $until = nextCutoffAfter($base);
    $from = ($lic && (int) $lic['valid_until_ms'] > $now) ? (int) $lic['valid_from_ms'] : $now;

    $pdo->prepare("UPDATE lukka_codes SET status='CONSUMIDO', consumed_at=NOW(), consumed_device=? WHERE id=?")
        ->execute([$device, $row['id']]);

    if ($lic) {
        $pdo->prepare('UPDATE lukka_licenses SET device_id=?, last_renewal=?, valid_from_ms=?, valid_until_ms=?, plan_id=? WHERE user_id=?')
            ->execute([$device, $n, $from, $until, $user['plan_id'], $userId]);
    } else {
        $pdo->prepare('INSERT INTO lukka_licenses (user_id, license_id, device_id, plan_id, last_renewal, valid_from_ms, valid_until_ms) VALUES (?,?,?,?,?,?,?)')
            ->execute([$userId, bin2hex(random_bytes(8)), $device, $user['plan_id'], $n, $from, $until]);
    }

    $st = $pdo->prepare('SELECT * FROM lukka_licenses WHERE user_id = ?');
    $st->execute([$userId]);
    $lic = $st->fetch();

    audit($pdo, 'ACTIVATE_OK', null, $userId, $device, $ref);
    $pdo->commit();

    respond(signedEnvelope(licensePayload($lic, $userId)));
} catch (Reject $r) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    audit($pdo, 'FAIL', $r->getMessage(), $userId, $device, $ref);
    fail($r->getMessage(), $r->getMessage() === 'DISPOSITIVO_NO_AUTORIZADO' ? 403 : 422);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[lukka] activate: ' . $e->getMessage());
    fail('ERROR_INTERNO', 500);
}
