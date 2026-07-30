-- Add missing updated_at column to token_blacklist table
-- Required by BaseEntity which all entities extend

ALTER TABLE token_blacklist
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);