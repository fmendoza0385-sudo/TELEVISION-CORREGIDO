<?php
declare(strict_types=1);
/**
 * Libera la vinculación de dispositivo de un usuario (cambio de TV/celular).
 *   php reassign_device.php 80105001
 * El siguiente código válido que se active desde el equipo nuevo lo vinculará.
 * (La política comercial de reasignación sigue pendiente de aprobación — ver README.)
 */
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require __DIR__ . '/../api/_bootstrap.php';
$uid = $argv[1] ?? '';
if (!preg_match('/^\d{4,20}$/', $uid)) {
    fwrite(STDERR, "Uso: php reassign_device.php ID_USUARIO\n");
    exit(1);
}
$st = db()->prepare('UPDATE lukka_licenses SET device_id = NULL WHERE user_id = ?');
$st->execute([$uid]);
audit(db(), 'REASSIGN', null, $uid, null);
echo $st->rowCount() ? "Dispositivo liberado para $uid.\n" : "El usuario $uid aún no tiene licencia activada.\n";
