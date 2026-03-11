ALTER TABLE IF EXISTS orders_table
    ADD COLUMN IF NOT EXISTS subtotal_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shipping_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS shipping_full_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS shipping_email VARCHAR(120),
    ADD COLUMN IF NOT EXISTS shipping_phone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS shipping_address_line VARCHAR(255),
    ADD COLUMN IF NOT EXISTS shipping_city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS shipping_postal_code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS shipping_country VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS packed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

ALTER TABLE IF EXISTS orders_table
    DROP CONSTRAINT IF EXISTS chk_orders_table_status;

ALTER TABLE IF EXISTS orders_table
    ADD CONSTRAINT chk_orders_table_status
    CHECK (status IN ('CREATED', 'PAID', 'PACKED', 'SHIPPED', 'DELIVERED', 'REFUNDED', 'CANCELLED'));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_shipping_method'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_shipping_method
            CHECK (shipping_method IS NULL OR shipping_method IN ('STANDARD', 'EXPRESS'));
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_subtotal_non_negative'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_subtotal_non_negative
            CHECK (subtotal_amount >= 0);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_discount_non_negative'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_discount_non_negative
            CHECK (discount_amount >= 0);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_shipping_fee_non_negative'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_shipping_fee_non_negative
            CHECK (shipping_fee >= 0);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_tax_non_negative'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_tax_non_negative
            CHECK (tax_amount >= 0);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_orders_table_coupon_code
    ON orders_table (coupon_code);

CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(19,2) NOT NULL,
    min_order_amount NUMERIC(19,2) NULL,
    max_discount_amount NUMERIC(19,2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    max_redemptions INTEGER NULL,
    per_user_limit INTEGER NULL,
    starts_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_coupons_discount_type'
    ) THEN
        ALTER TABLE coupons
            ADD CONSTRAINT chk_coupons_discount_type
            CHECK (discount_type IN ('PERCENTAGE', 'FIXED'));
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_coupons_discount_value_positive'
    ) THEN
        ALTER TABLE coupons
            ADD CONSTRAINT chk_coupons_discount_value_positive
            CHECK (discount_value > 0);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_coupons_active_expires
    ON coupons (active, expires_at);

CREATE TABLE IF NOT EXISTS coupon_redemptions (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_redemptions_order
        FOREIGN KEY (order_id) REFERENCES orders_table(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_coupon_user
    ON coupon_redemptions (coupon_id, user_id);
