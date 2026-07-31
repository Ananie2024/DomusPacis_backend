-- ============================================================
-- Domus Pacis Platform — Add login lockout columns to users
-- Managed by Flyway. Do not edit manually.
-- ============================================================
-- Restores columns lost when V5__login_rate_limit.sql was deleted
-- and replaced with V5__add_password_changed_at.sql.
-- failed_login_attempts and locked_until are required by User.java
-- but were missing from the migration chain, causing:
--   - Fresh DB: Hibernate validate mode fails at startup
--   - Existing DB: Flyway checksum mismatch on version 5
--
-- This migration is idempotent: IF NOT EXISTS prevents errors on
-- databases where the old V5 already created these columns.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until DATETIME(6) NULL;

-- Existing rows default to 0 failed attempts and no lockout.
-- The application sets these values at runtime.
