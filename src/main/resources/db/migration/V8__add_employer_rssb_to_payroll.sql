-- ============================================================
-- Domus Pacis Platform — Add employer RSSB contribution column
-- ============================================================

ALTER TABLE payroll_records
    ADD COLUMN employer_rssb_contribution DECIMAL(12,2) NOT NULL DEFAULT 0
    AFTER tax_withheld;