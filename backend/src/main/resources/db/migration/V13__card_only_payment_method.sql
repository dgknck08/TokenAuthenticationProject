UPDATE orders_table
SET payment_method = 'CARD'
WHERE payment_method = 'COD';

ALTER TABLE IF EXISTS orders_table
    DROP CONSTRAINT IF EXISTS chk_orders_table_payment_method;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_orders_table_payment_method'
    ) THEN
        ALTER TABLE orders_table
            ADD CONSTRAINT chk_orders_table_payment_method
            CHECK (payment_method IS NULL OR payment_method IN ('CARD'));
    END IF;
END$$;
