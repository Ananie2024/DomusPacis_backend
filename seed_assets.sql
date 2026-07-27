-- Comprehensive Service Assets Seed Data
-- All asset types with realistic pricing and descriptions

-- ============================================================
-- ROOMS (6 rooms with different types)
-- ============================================================
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, room_number, room_type, floor)
VALUES
(UUID(), 'ROOM', 'Standard Single Room', 'A cosy, well-appointed room perfect for solo travellers and pilgrims seeking peaceful rest. Features a comfortable single bed, work desk, and en-suite bathroom.', 1, 35000, 'PER_NIGHT', true, NOW(), NOW(), '101', 'SINGLE', 1),
(UUID(), 'ROOM', 'Standard Double Room', 'Spacious and comfortable for couples or business travellers, with garden or courtyard views. Includes a double bed, seating area, and en-suite bathroom.', 2, 50000, 'PER_NIGHT', true, NOW(), NOW(), '201', 'DOUBLE', 2),
(UUID(), 'ROOM', 'Twin Room', 'Ideal for colleagues or friends travelling together, offering two comfortable single beds, shared bathroom, and a small sitting area.', 2, 45000, 'PER_NIGHT', true, NOW(), NOW(), '202', 'DOUBLE', 2),
(UUID(), 'ROOM', 'Deluxe Double Room', 'Premium accommodation with a king-size bed, panoramic views of the gardens, mini-fridge, and an upgraded en-suite with bathtub.', 2, 75000, 'PER_NIGHT', true, NOW(), NOW(), '301', 'DOUBLE', 3),
(UUID(), 'ROOM', 'Family Suite', 'A spacious two-room suite with a king bed in the master bedroom and two single beds in the adjoining room. Perfect for families of up to 4.', 4, 120000, 'PER_NIGHT', true, NOW(), NOW(), '302', 'FAMILY', 3),
(UUID(), 'ROOM', 'Executive Suite', 'Our most luxurious offering — a separate living area, premium furnishings, kitchenette, balcony with city views, and a spacious bathroom with jacuzzi.', 3, 150000, 'PER_NIGHT', true, NOW(), NOW(), '401', 'SUITE', 4);

-- ============================================================
-- CONFERENCE HALLS (4 halls)
-- ============================================================
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, hall_code, projector_available, audio_system_available, max_seating_layout)
VALUES
(UUID(), 'CONFERENCE_HALL', 'Boardroom', 'Intimate boardroom setting ideal for executive meetings, board sessions, and small workshops. Features a 70" Smart TV, video conferencing, whiteboard, and coffee service.', 20, 200000, 'PER_DAY', true, NOW(), NOW(), 'BR-01', true, true, 'BOARDROOM'),
(UUID(), 'CONFERENCE_HALL', 'Seminar Hall', 'Versatile hall suitable for seminars, training sessions, and mid-sized conferences with flexible seating. Equipped with projector, PA system, podium, and air conditioning.', 80, 400000, 'PER_DAY', true, NOW(), NOW(), 'SH-01', true, true, 'CLASSROOM'),
(UUID(), 'CONFERENCE_HALL', 'Main Conference Hall', 'Our flagship conference facility for large plenary sessions, AGMs, and multi-track conferences. Features dual projectors, professional PA system, simultaneous translation booth, stage, and breakout rooms.', 200, 800000, 'PER_DAY', true, NOW(), NOW(), 'CH-01', true, true, 'THEATRE'),
(UUID(), 'CONFERENCE_HALL', 'Banquet Hall', 'Elegant hall designed for gala dinners, award ceremonies, and formal banquets. Includes a dance floor, stage, catering kitchen, and premium lighting system.', 150, 600000, 'PER_EVENT', true, NOW(), NOW(), 'BH-01', true, true, 'BANQUET');

-- ============================================================
-- WEDDING GARDENS (3 gardens)
-- ============================================================
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, is_indoor, has_stage, includes_catering)
VALUES
(UUID(), 'WEDDING_GARDEN', 'Rose Garden', 'An intimate garden adorned with roses and hedgerows — perfect for elegant, smaller ceremonies. Includes a gazebo, seating for guests, and a dedicated bridal preparation room.', 150, 800000, 'PER_EVENT', true, NOW(), NOW(), false, true, false),
(UUID(), 'WEDDING_GARDEN', 'Main Wedding Garden', 'Our flagship outdoor venue with manicured lawns, water features, a permanent stage, and a dedicated bridal suite. Can accommodate large wedding parties with catering included.', 350, 1500000, 'PER_EVENT', true, NOW(), NOW(), false, true, true),
(UUID(), 'WEDDING_GARDEN', 'Indoor Chapel Garden', 'A beautiful indoor-outdoor hybrid venue featuring a glass-roofed chapel surrounded by tropical plants. Ideal for rainy season weddings with the feel of an outdoor ceremony.', 200, 1200000, 'PER_EVENT', true, NOW(), NOW(), true, true, true);

-- ============================================================
-- RETREAT CENTERS (3 retreats)
-- ============================================================
INSERT INTO service_assets (id, asset_type, name, description, capacity, price_per_unit, pricing_unit, is_available, created_at, updated_at, number_of_beds, includes_chapel, includes_catering)
VALUES
(UUID(), 'RETREAT_CENTER', 'Pax Retreat House', 'The largest retreat facility, ideal for religious communities, youth programmes, and extended spiritual exercises. Features dormitory-style accommodation, a chapel, dining hall, and meditation garden.', 80, 25000, 'PER_NIGHT', true, NOW(), NOW(), 80, true, true),
(UUID(), 'RETREAT_CENTER', 'Silentium Lodge', 'A quiet, contemplative retreat centre for individual or small-group silent retreats. Each guest has a private room with desk, access to the prayer garden, and simple meals provided.', 12, 45000, 'PER_NIGHT', true, NOW(), NOW(), 12, true, true),
(UUID(), 'RETREAT_CENTER', 'Sanctuary Cabin', 'A private self-contained cabin nestled in the wooded area of the property. Perfect for personal retreats, spiritual direction, or sabbatical rest. Includes kitchenette and private prayer corner.', 2, 60000, 'PER_NIGHT', true, NOW(), NOW(), 2, false, false);