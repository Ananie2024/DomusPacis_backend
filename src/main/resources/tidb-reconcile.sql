-- ============================================================
-- Domus Pacis Platform — TiDB Schema Reconciliation
-- ============================================================
-- Reconciles the TiDB Cloud deployment database (schema `test`)
-- which has a partial schema (V1 tables present, V2–V9 missing).
-- Flyway is disabled on TiDB because its history-table creation
-- uses 'CREATE TABLE ... SELECT', which TiDB does not support.
--
-- This script is referenced only by the default profile via
-- spring.sql.init.schema-locations. It is idempotent and safe
-- to run on every startup. Local/test profiles explicitly set
-- spring.sql.init.mode: never and are unaffected.
-- ============================================================

-- ── V2: password_reset_tokens ────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
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

-- ── V4: token_blacklist ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS token_blacklist (
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

-- ── V9: paye_tax_brackets ────────────────────────────────────
CREATE TABLE IF NOT EXISTS paye_tax_brackets (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    lower_bound     DECIMAL(12,2)   NOT NULL,
    upper_bound     DECIMAL(12,2)   NULL,
    rate            DECIMAL(5,4)    NOT NULL,
    description     VARCHAR(255)    NULL,
    effective_from  DATE            NOT NULL,
    effective_to    DATE            NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_paye_bracket_active ON paye_tax_brackets (is_active, effective_from, effective_to)',
    'SELECT 1'
) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'paye_tax_brackets' AND index_name = 'idx_paye_bracket_active');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V3: Seed service assets (only if table is empty) ────────
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Standard Single Room', 'A cosy, well-appointed room perfect for solo travellers and pilgrims seeking peaceful rest. Features a comfortable single bed, work desk, and en-suite bathroom.', 1, 35000, 'PER_NIGHT', true, NOW(), NOW(), '101', 'SINGLE', 1
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Standard Double Room', 'Spacious and comfortable for couples or business travellers, with garden or courtyard views. Includes a double bed, seating area, and en-suite bathroom.', 2, 50000, 'PER_NIGHT', true, NOW(), NOW(), '201', 'DOUBLE', 2
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Twin Room', 'Ideal for colleagues or friends travelling together, offering two comfortable single beds, shared bathroom, and a small sitting area.', 2, 45000, 'PER_NIGHT', true, NOW(), NOW(), '202', 'DOUBLE', 2
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Deluxe Double Room', 'Premium accommodation with a king-size bed, panoramic views of the gardens, mini-fridge, and an upgraded en-suite with bathtub.', 2, 75000, 'PER_NIGHT', true, NOW(), NOW(), '301', 'DOUBLE', 3
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Family Suite', 'A spacious two-room suite with a king bed in the master bedroom and two single beds in the adjoining room. Perfect for families of up to 4.', 4, 120000, 'PER_NIGHT', true, NOW(), NOW(), '302', 'FAMILY', 3
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
SELECT UUID(), 'ROOM', 'Executive Suite', 'Our most luxurious offering — a separate living area, premium furnishings, kitchenette, balcony with city views, and a spacious bathroom with jacuzzi.', 3, 150000, 'PER_NIGHT', true, NOW(), NOW(), '401', 'SUITE', 4
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, hall_code, projector_available, audio_system_available, max_seating_layout)
SELECT UUID(), 'CONFERENCE_HALL', 'Boardroom', 'Intimate boardroom setting ideal for executive meetings, board sessions, and small workshops. Features a 70" Smart TV, video conferencing, whiteboard, and coffee service.', 20, 200000, 'PER_DAY', true, NOW(), NOW(), 'BR-01', true, true, 'BOARDROOM'
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, hall_code, projector_available, audio_system_available, max_seating_layout)
SELECT UUID(), 'CONFERENCE_HALL', 'Seminar Hall', 'Versatile hall suitable for seminars, training sessions, and mid-sized conferences with flexible seating. Equipped with projector, PA system, podium, and air conditioning.', 80, 400000, 'PER_DAY', true, NOW(), NOW(), 'SH-01', true, true, 'CLASSROOM'
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, hall_code, projector_available, audio_system_available, max_seating_layout)
SELECT UUID(), 'CONFERENCE_HALL', 'Main Conference Hall', 'Our flagship conference facility for large plenary sessions, AGMs, and multi-track conferences. Features dual projectors, professional PA system, simultaneous translation booth, stage, and breakout rooms.', 200, 800000, 'PER_DAY', true, NOW(), NOW(), 'CH-01', true, true, 'THEATRE'
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, hall_code, projector_available, audio_system_available, max_seating_layout)
SELECT UUID(), 'CONFERENCE_HALL', 'Banquet Hall', 'Elegant hall designed for gala dinners, award ceremonies, and formal banquets. Includes a dance floor, stage, catering kitchen, and premium lighting system.', 150, 600000, 'PER_EVENT', true, NOW(), NOW(), 'BH-01', true, true, 'BANQUET'
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, is_indoor, has_stage, includes_catering)
SELECT UUID(), 'WEDDING_GARDEN', 'Rose Garden', 'An intimate garden adorned with roses and hedgerows — perfect for elegant, smaller ceremonies. Includes a gazebo, seating for guests, and a dedicated bridal preparation room.', 150, 800000, 'PER_EVENT', true, NOW(), NOW(), false, true, false
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, is_indoor, has_stage, includes_catering)
SELECT UUID(), 'WEDDING_GARDEN', 'Main Wedding Garden', 'Our flagship outdoor venue with manicured lawns, water features, a permanent stage, and a dedicated bridal suite. Can accommodate large wedding parties with catering included.', 350, 1500000, 'PER_EVENT', true, NOW(), NOW(), false, true, true
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, is_indoor, has_stage, includes_catering)
SELECT UUID(), 'WEDDING_GARDEN', 'Indoor Chapel Garden', 'A beautiful indoor-outdoor hybrid venue featuring a glass-roofed chapel surrounded by tropical plants. Ideal for rainy season weddings with the feel of an outdoor ceremony.', 200, 1200000, 'PER_EVENT', true, NOW(), NOW(), true, true, true
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, number_of_beds, includes_chapel, includes_catering)
SELECT UUID(), 'RETREAT_CENTER', 'Pax Retreat House', 'The largest retreat facility, ideal for religious communities, youth programmes, and extended spiritual exercises. Features dormitory-style accommodation, a chapel, dining hall, and meditation garden.', 80, 25000, 'PER_NIGHT', true, NOW(), NOW(), 80, true, true
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, number_of_beds, includes_chapel, includes_catering)
SELECT UUID(), 'RETREAT_CENTER', 'Silentium Lodge', 'A quiet, contemplative retreat centre for individual or small-group silent retreats. Each guest has a private room with desk, access to the prayer garden, and simple meals provided.', 12, 45000, 'PER_NIGHT', true, NOW(), NOW(), 12, true, true
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, number_of_beds, includes_chapel, includes_catering)
SELECT UUID(), 'RETREAT_CENTER', 'Sanctuary Cabin', 'A private self-contained cabin nestled in the wooded area of the property. Perfect for personal retreats, spiritual direction, or sabbatical rest. Includes kitchenette and private prayer corner.', 2, 60000, 'PER_NIGHT', true, NOW(), NOW(), 2, false, false
WHERE NOT EXISTS (SELECT 1 FROM service_assets);

-- ── V5: users.password_changed_at ────────────────────────────
SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN password_changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password_changed_at');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V6: token_blacklist.updated_at ───────────────────────────
SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE token_blacklist ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)',
    'SELECT 1'
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'token_blacklist' AND column_name = 'updated_at');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V7: users.failed_login_attempts ──────────────────────────
SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'failed_login_attempts');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V7: users.locked_until ───────────────────────────────────
SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN locked_until DATETIME(6) NULL',
    'SELECT 1'
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'locked_until');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V8: payroll_records.employer_rssb_contribution ───────────
SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE payroll_records ADD COLUMN employer_rssb_contribution DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER tax_withheld',
    'SELECT 1'
) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'payroll_records' AND column_name = 'employer_rssb_contribution');
PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;

-- ── V9: Seed PAYE tax brackets (only if table is empty) ──────
INSERT INTO paye_tax_brackets (id, lower_bound, upper_bound, rate, description, effective_from, is_active)
SELECT UUID(), 0, 360000, 0.0000, 'Band 1: 0 – 360,000 RWF (0%)',          '2024-07-01', TRUE
WHERE NOT EXISTS (SELECT 1 FROM paye_tax_brackets);

INSERT INTO paye_tax_brackets (id, lower_bound, upper_bound, rate, description, effective_from, is_active)
SELECT UUID(), 360001, 720000, 0.2000, 'Band 2: 360,001 – 720,000 RWF (20%)', '2024-07-01', TRUE
WHERE NOT EXISTS (SELECT 1 FROM paye_tax_brackets);

INSERT INTO paye_tax_brackets (id, lower_bound, upper_bound, rate, description, effective_from, is_active)
SELECT UUID(), 720001, NULL, 0.3000, 'Band 3: > 720,000 RWF (30%)',          '2024-07-01', TRUE
WHERE NOT EXISTS (SELECT 1 FROM paye_tax_brackets);