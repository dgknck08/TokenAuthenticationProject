CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE IF EXISTS product
    ADD COLUMN IF NOT EXISTS search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', COALESCE(name, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(description, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(category, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(brand, '')), 'C')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_product_search_vector
    ON product USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_product_name_trgm
    ON product USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_description_trgm
    ON product USING GIN (description gin_trgm_ops);
