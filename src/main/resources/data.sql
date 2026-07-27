-- Service Assets seed data

-- Room
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at)
VALUES (UUID(), 'ROOM', 'Room 101', 'Standard room', 2, 50000, 'PER_NIGHT', true, NOW(), NOW());

-- Conference Hall
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at)
VALUES (UUID(), 'CONFERENCE_HALL', 'Grand Hall', 'Main conference hall', 150, 500000, 'PER_EVENT', true, NOW(), NOW());

-- Wedding Garden
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at)
VALUES (UUID(), 'WEDDING_GARDEN', 'Garden A', 'Outdoor wedding garden', 200, 1200000, 'PER_EVENT', true, NOW(), NOW());

-- Retreat Center
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at)
VALUES (UUID(), 'RETREAT_CENTER', 'Retreat House', 'Quiet retreat center', 20, 350000, 'PER_NIGHT', true, NOW(), NOW());