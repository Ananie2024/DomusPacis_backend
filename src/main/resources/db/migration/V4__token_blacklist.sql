-- ============================================================
-- Domus Pacis Platform — Token Blacklist
-- Managed by Flyway. Do not edit manually.
-- ============================================================

CREATE TABLE token_blacklist (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    token_hash      VARCHAR(64)     NOT NULL UNIQUE,
    user_id         VARCHAR(36)     NOT NULL,
    token_type      VARCHAR(16)     NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    invalidated_at  DATETIME(6)     NOT NULL,
    reason          VARCHAR(64)     NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    INDEX idx_tb_token_hash (token_hash),
    INDEX idx_tb_user (user_id),
    INDEX idx_tb_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;