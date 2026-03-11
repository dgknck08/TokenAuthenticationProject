ALTER TABLE IF EXISTS orders_table
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS payment_provider_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN IF NOT EXISTS payment_conversation_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS payment_reference_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS payment_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_error_message VARCHAR(500),
    ADD COLUMN IF NOT EXISTS payment_initialized_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS payment_failed_at TIMESTAMP;

UPDATE orders_table
SET payment_provider_status = 'NOT_STARTED'
WHERE payment_provider_status IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_payment_provider'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_payment_provider
            CHECK (payment_provider IS NULL OR payment_provider IN ('IYZICO'));
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_payment_provider_status'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_payment_provider_status
            CHECK (payment_provider_status IN ('NOT_STARTED', 'PENDING', 'SUCCESS', 'FAILED'));
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_orders_table_payment_conversation
    ON orders_table (payment_conversation_id);

CREATE INDEX IF NOT EXISTS idx_orders_table_payment_token
    ON orders_table (payment_token);
