-- Compatibility migration for payment-service JPA model
-- Safe to run multiple times.

BEGIN;

-- payment_methods compatibility
ALTER TABLE payment_schema.payment_methods
    ADD COLUMN IF NOT EXISTS last_four_digits VARCHAR(4),
    ADD COLUMN IF NOT EXISTS expiry_month INT,
    ADD COLUMN IF NOT EXISTS expiry_year INT,
    ADD COLUMN IF NOT EXISTS card_brand VARCHAR(50),
    ADD COLUMN IF NOT EXISTS billing_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS provider_token VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- backfill payment_methods from legacy column names
UPDATE payment_schema.payment_methods
SET last_four_digits = last_four
WHERE last_four_digits IS NULL AND last_four IS NOT NULL;

UPDATE payment_schema.payment_methods
SET expiry_month = exp_month
WHERE expiry_month IS NULL AND exp_month IS NOT NULL;

UPDATE payment_schema.payment_methods
SET expiry_year = exp_year
WHERE expiry_year IS NULL AND exp_year IS NOT NULL;

UPDATE payment_schema.payment_methods
SET card_brand = brand
WHERE card_brand IS NULL AND brand IS NOT NULL;

-- payments compatibility
ALTER TABLE payment_schema.payments
    ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS provider_payment_intent_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- make metadata compatible with entity mapping (TEXT)
ALTER TABLE payment_schema.payments
    ALTER COLUMN metadata TYPE TEXT USING metadata::text;

-- backfill payments from legacy column names
UPDATE payment_schema.payments
SET provider_transaction_id = provider_charge_id
WHERE provider_transaction_id IS NULL AND provider_charge_id IS NOT NULL;

UPDATE payment_schema.payments
SET provider_payment_intent_id = provider_payment_id
WHERE provider_payment_intent_id IS NULL AND provider_payment_id IS NOT NULL;

UPDATE payment_schema.payments
SET paid_at = completed_at
WHERE paid_at IS NULL AND completed_at IS NOT NULL;

-- refunds compatibility
ALTER TABLE payment_schema.refunds
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processed_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- backfill refunds from legacy column names
UPDATE payment_schema.refunds
SET processed_at = completed_at
WHERE processed_at IS NULL AND completed_at IS NOT NULL;

COMMIT;
