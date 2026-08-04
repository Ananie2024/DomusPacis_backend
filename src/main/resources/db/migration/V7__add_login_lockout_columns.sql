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

-- Idempotent column addition compatible with MySQL 8.x and MariaDB
DROP PROCEDURE IF EXISTS add_login_lockout_columns;
DELIMITER //
CREATE PROCEDURE add_login_lockout_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'failed_login_attempts' AND table_schema = DATABASE()
    ) THEN
        ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'locked_until' AND table_schema = DATABASE()
    ) THEN
        ALTER TABLE users ADD COLUMN locked_until DATETIME(6) NULL;
    END IF;
END //
DELIMITER ;

CALL add_login_lockout_columns();
DROP PROCEDURE IF EXISTS add_login_lockout_columns;

