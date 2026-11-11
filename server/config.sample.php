<?php
// Copia este archivo como server/config.php (está en .gitignore) y colócalo FUERA de la carpeta pública
// si tu hosting lo permite. NUNCA lo subas a GitHub.
return [
    'db_dsn'  => 'mysql:host=localhost;dbname=TU_BASE;charset=utf8mb4',
    'db_user' => 'TU_USUARIO',
    'db_pass' => 'TU_CLAVE',

    // Cadena larga y aleatoria (>= 32 caracteres). Se usa para guardar los códigos como HMAC.
    // Si la cambias después de importar el banco, los códigos dejan de funcionar.
    'code_pepper' => 'CAMBIAR_POR_UNA_CADENA_ALEATORIA_LARGA',

    // Clave PRIVADA de firma (la genera tools/generate_keys.php). Nunca va en el APK.
    'private_key_path' => __DIR__ . '/keys/lukka_license_private.pem',
];
