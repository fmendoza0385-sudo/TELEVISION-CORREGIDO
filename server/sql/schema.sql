-- LUKKA Televisión — servidor de licencias (MySQL/MariaDB, InnoDB, utf8mb4)
-- Los códigos de activación NO se guardan en claro: solo su HMAC-SHA256 (con "pepper" del servidor).

CREATE TABLE IF NOT EXISTS lukka_users (
  user_id     VARCHAR(20)  NOT NULL PRIMARY KEY,
  plan_id     VARCHAR(16)  NOT NULL,           -- PLAN-50 | PLAN-80 | PLAN-120
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lukka_codes (
  id              INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id         VARCHAR(20)  NOT NULL,
  renewal_number  TINYINT UNSIGNED NOT NULL,   -- 1..36
  code_hash       CHAR(64)     NOT NULL,       -- HMAC-SHA256 hex del código de 16 dígitos
  status          ENUM('DISPONIBLE','CONSUMIDO','REVOCADO') NOT NULL DEFAULT 'DISPONIBLE',
  consumed_at     DATETIME NULL,
  consumed_device CHAR(32) NULL,
  UNIQUE KEY uq_user_renewal (user_id, renewal_number),
  UNIQUE KEY uq_code_hash (code_hash),
  CONSTRAINT fk_codes_user FOREIGN KEY (user_id) REFERENCES lukka_users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lukka_licenses (
  user_id        VARCHAR(20) NOT NULL PRIMARY KEY,
  license_id     CHAR(16)    NOT NULL,
  device_id      CHAR(32)    NULL,             -- NULL = sin dispositivo vinculado (tras reasignación)
  plan_id        VARCHAR(16) NOT NULL,
  last_renewal   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  valid_from_ms  BIGINT      NOT NULL,
  valid_until_ms BIGINT      NOT NULL,
  revoked        TINYINT(1)  NOT NULL DEFAULT 0,
  updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_lic_user FOREIGN KEY (user_id) REFERENCES lukka_users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Auditoría: quién activó, cuándo, desde qué equipo. "ref" = huella corta del código, nunca el código.
CREATE TABLE IF NOT EXISTS lukka_audit (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  event      VARCHAR(24) NOT NULL,             -- ACTIVATE_OK | FAIL | STATUS_OK
  detail     VARCHAR(40) NULL,                 -- código de error
  user_id    VARCHAR(20) NULL,
  device_id  CHAR(32)    NULL,
  ip         VARCHAR(45) NULL,
  ref        CHAR(12)    NULL,
  KEY idx_fail_ip (event, ip, created_at),
  KEY idx_fail_user (event, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
