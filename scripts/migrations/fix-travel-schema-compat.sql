-- Compatibility migration for travel-service JPA model
-- Safe to run multiple times.

BEGIN;

-- destinations table compatibility
ALTER TABLE travel_schema.destinations
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- activities table compatibility
ALTER TABLE travel_schema.activities
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS price_estimate DECIMAL(10,2);

-- join table expected by Activity<->Destination many-to-many mapping
CREATE TABLE IF NOT EXISTS travel_schema.activity_destinations (
    activity_id BIGINT NOT NULL REFERENCES travel_schema.activities(id) ON DELETE CASCADE,
    destination_id BIGINT NOT NULL REFERENCES travel_schema.destinations(id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, destination_id)
);

-- backfill join table from legacy destination_id relation if present
INSERT INTO travel_schema.activity_destinations (activity_id, destination_id)
SELECT a.id, a.destination_id
FROM travel_schema.activities a
WHERE a.destination_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- travel_destinations table compatibility
ALTER TABLE travel_schema.travel_destinations
    ADD COLUMN IF NOT EXISTS visit_order INT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- backfill visit_order from legacy order_index if present
UPDATE travel_schema.travel_destinations
SET visit_order = order_index
WHERE visit_order IS NULL;

-- travel_activities table compatibility
ALTER TABLE travel_schema.travel_activities
    ADD COLUMN IF NOT EXISTS planned_date DATE,
    ADD COLUMN IF NOT EXISTS planned_time VARCHAR(20),
    ADD COLUMN IF NOT EXISTS actual_cost DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- backfill from legacy scheduled_* columns if present
UPDATE travel_schema.travel_activities
SET planned_date = scheduled_date
WHERE planned_date IS NULL;

UPDATE travel_schema.travel_activities
SET planned_time = CAST(scheduled_time AS VARCHAR)
WHERE planned_time IS NULL AND scheduled_time IS NOT NULL;

-- travel_accommodations table compatibility
ALTER TABLE travel_schema.travel_accommodations
    ADD COLUMN IF NOT EXISTS name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS price_per_night DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS confirmation_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'EUR',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- transportations table compatibility
ALTER TABLE travel_schema.transportations
    ADD COLUMN IF NOT EXISTS carrier VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- backfill carrier from legacy provider if present
UPDATE travel_schema.transportations
SET carrier = provider
WHERE carrier IS NULL AND provider IS NOT NULL;

COMMIT;
