CREATE INDEX IF NOT EXISTS idx_orders_table_status_created_at ON orders_table (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_table_user_id_created_at ON orders_table (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_table_paid_at ON orders_table (paid_at);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_status'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_status
            CHECK (status IN ('CREATED', 'PAID', 'REFUNDED', 'CANCELLED'));
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_payment_method'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_payment_method
            CHECK (payment_method IS NULL OR payment_method IN ('CARD', 'COD'));
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_total_amount_non_negative'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_total_amount_non_negative
            CHECK (total_amount >= 0);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_order_items_quantity_positive'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT chk_order_items_quantity_positive
            CHECK (quantity > 0);
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_order_items_unit_price_non_negative'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT chk_order_items_unit_price_non_negative
            CHECK (unit_price >= 0);
    END IF;
END$$;
