-- ============================================================
-- Domus Pacis Platform — PAYE Tax Brackets (configurable)
-- ============================================================
-- Replaces the hardcoded Rwanda PAYE bands in PayrollService
-- with a database-backed, versioned, auditable table.
-- ============================================================

CREATE TABLE paye_tax_brackets (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    lower_bound     DECIMAL(12,2)   NOT NULL COMMENT 'Inclusive lower bound of the bracket (e.g. 0, 360001, 720001)',
    upper_bound     DECIMAL(12,2)   NULL     COMMENT 'Inclusive upper bound of the bracket (NULL = no upper limit)',
    rate            DECIMAL(5,4)    NOT NULL COMMENT 'Tax rate as decimal (e.g. 0.20 for 20%)',
    description     VARCHAR(255)    NULL     COMMENT 'Human-readable label, e.g. "Band 2: 360,001–720,000"',
    effective_from  DATE            NOT NULL COMMENT 'First date this bracket is active',
    effective_to    DATE            NULL     COMMENT 'Last date this bracket is active (NULL = still active)',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Index for efficient lookup by date range
CREATE INDEX idx_paye_bracket_active
    ON paye_tax_brackets (is_active, effective_from, effective_to);

-- Seed the current Rwanda PAYE brackets (2024/2025 tax year)
INSERT INTO paye_tax_brackets (id, lower_bound, upper_bound, rate, description, effective_from, is_active) VALUES
    (UUID(),    0, 360000, 0.0000, 'Band 1: 0 – 360,000 RWF (0%)',          '2024-07-01', TRUE),
    (UUID(), 360001, 720000, 0.2000, 'Band 2: 360,001 – 720,000 RWF (20%)', '2024-07-01', TRUE),
    (UUID(), 720001,   NULL, 0.3000, 'Band 3: > 720,000 RWF (30%)',          '2024-07-01', TRUE);