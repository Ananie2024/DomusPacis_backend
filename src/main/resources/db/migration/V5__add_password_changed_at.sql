-- ============================================================
-- Domus Pacis Platform — Add password_changed_at to users
-- Managed by Flyway. Do not edit manually.
-- ============================================================

ALTER TABLE users
    ADD COLUMN password_changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Existing rows get the default (current timestamp on migration apply).
-- New rows will be set explicitly by the application.