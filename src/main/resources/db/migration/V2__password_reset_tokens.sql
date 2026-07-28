-- ============================================================
-- Domus Pacis Platform — Password Reset Tokens
-- Managed by Flyway. Do not edit manually.
-- ============================================================

CREATE TABLE password_reset_tokens (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    token           VARCHAR(255)    NOT NULL UNIQUE,
    user_id         VARCHAR(36)     NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    used            BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at         DATETIME(6),
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    INDEX idx_prt_token (token),
    INDEX idx_prt_user (user_id),
    INDEX idx_prt_expires (expires_at),
    INDEX idx_prt_used (used),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
