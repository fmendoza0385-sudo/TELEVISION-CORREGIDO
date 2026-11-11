<?php
declare(strict_types=1);
/**
 * Importa el banco temporal JSON (lukka.subscription-code-bank) a MySQL.
 *   php import_bank.php /ruta/LUKKA_Banco_Codigos_Activacion_TEMPORAL.json [--dry-run]
 * Los códigos se guardan como HMAC-SHA256; no se imprimen ni se registran.
 * Ejecutar SOLO por línea de comandos, en el servidor. Después: respaldo protegido y borrado seguro del JSON.
 */
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require __DIR__ . '/../api/_bootstrap.php';

$file = $argv[1] ?? '';
$dry = in_array('--dry-run', $argv, true);
if (!is_file($file)) {
    fwrite(STDERR, "Uso: php import_bank.php archivo.json [--dry-run]\n");
    exit(1);
}

$bank = json_decode((string) file_get_contents($file), true);
if (!is_array($bank) || ($bank['schema'] ?? '') !== 'lukka.subscription-code-bank' || ($bank['schema_version'] ?? '') !== '1.0.0') {
    fwrite(STDERR, "Esquema no reconocido (se espera lukka.subscription-code-bank 1.0.0).\n");
    exit(1);
}

$plans = [];
foreach ($bank['plans'] as $p) {
    $plans[$p['plan_id']] = true;
}

$users = 0;
$codes = 0;
$seen = [];
$rows = [];
foreach ($bank['users'] as $u) {
    $uid = (string) $u['user_id'];
    if (!preg_match('/^\d{4,20}$/', $uid) || !isset($plans[$u['plan_id']])) {
        fwrite(STDERR, "Usuario inválido o plan desconocido: $uid\n");
        exit(1);
    }
    $users++;
    foreach ($u['monthly_codes'] as $c) {
        $code = (string) $c['activation_code'];
        $n = (int) $c['renewal_number'];
        if (!preg_match('/^\d{16}$/', $code) || $n < 1 || $n > 36 || isset($seen[$code])) {
            fwrite(STDERR, "Código inválido o duplicado en el usuario $uid (renovación $n)\n");
            exit(1);
        }
        $seen[$code] = true;
        $codes++;
        $rows[] = [$uid, $u['plan_id'], $n, codeHash($code)];
    }
}

$expected = $bank['summary'] ?? [];
if (($expected['users'] ?? $users) !== $users || ($expected['total_codes'] ?? $codes) !== $codes) {
    fwrite(STDERR, "Los totales no coinciden con el resumen del archivo.\n");
    exit(1);
}
echo "Validado: $users usuarios, $codes códigos únicos.\n";
if ($dry) {
    echo "Modo --dry-run: no se escribió nada.\n";
    exit(0);
}

$pdo = db();
$pdo->beginTransaction();
$insUser = $pdo->prepare('INSERT IGNORE INTO lukka_users (user_id, plan_id) VALUES (?,?)');
$insCode = $pdo->prepare('INSERT INTO lukka_codes (user_id, renewal_number, code_hash) VALUES (?,?,?)');
$doneUsers = [];
foreach ($rows as [$uid, $plan, $n, $hash]) {
    if (!isset($doneUsers[$uid])) {
        $insUser->execute([$uid, $plan]);
        $doneUsers[$uid] = true;
    }
    $insCode->execute([$uid, $n, $hash]);
}
$pdo->commit();
echo "Importación completada. Ahora haz un respaldo protegido y elimina de forma segura el JSON.\n";
