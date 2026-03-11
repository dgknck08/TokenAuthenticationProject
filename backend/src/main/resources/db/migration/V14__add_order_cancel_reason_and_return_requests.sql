ALTER TABLE IF EXISTS orders_table
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(250);

CREATE TABLE IF NOT EXISTS return_requests (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_note VARCHAR(500),
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_return_requests_order
        FOREIGN KEY (order_id) REFERENCES orders_table(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_return_requests_order_id
    ON return_requests (order_id);

CREATE INDEX IF NOT EXISTS idx_return_requests_username_created_at
    ON return_requests (username, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_return_requests_status_created_at
    ON return_requests (status, created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_return_requests_status'
    ) THEN
        ALTER TABLE return_requests
            ADD CONSTRAINT chk_return_requests_status
            CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'));
    END IF;
END$$;
