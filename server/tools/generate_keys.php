<?php
declare(strict_types=1);
/**
 * Genera el par de claves de firma (ECDSA P-256).
 *   php generate_keys.php
 * - Guarda la clave PRIVADA en server/keys/lukka_license_private.pem (no se sube a GitHub).
 * - Imprime la clave PÚBLICA en base64 (DER) para la variable LUKKA_LICENSE_PUBLIC_KEY de GitHub.
 */
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
$dir = __DIR__ . '/../keys';
$priv = $dir . '/lukka_license_private.pem';
if (is_file($priv)) {
    fwrite(STDERR, "Ya existe una clave privada. No se sobrescribe (cambiarla invalida todas las licencias emitidas).\n");
    exit(1);
}
@mkdir($dir, 0700, true);

$key = openssl_pkey_new(['private_key_type' => OPENSSL_KEYTYPE_EC, 'curve_name' => 'prime256v1']);
if ($key === false) {
    fwrite(STDERR, "OpenSSL no pudo generar la clave.\n");
    exit(1);
}
openssl_pkey_export($key, $pem);
file_put_contents($priv, $pem);
chmod($priv, 0600);

$pubPem = openssl_pkey_get_details($key)['key'];
$b64 = preg_replace('/-----(BEGIN|END) PUBLIC KEY-----|\s+/', '', $pubPem);

echo "Clave privada guardada en: $priv  (RESPÁLDALA en un lugar seguro y NO la subas a GitHub)\n\n";
echo "Clave pública para el APK (variable LUKKA_LICENSE_PUBLIC_KEY):\n$b64\n";
