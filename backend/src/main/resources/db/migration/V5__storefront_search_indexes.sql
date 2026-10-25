-- Phase 4: public storefront (search, filter, pagination)

-- Full-text search over product name + description (MySQL InnoDB FULLTEXT).
ALTER TABLE products
    ADD FULLTEXT INDEX ft_products_name_description (name, description);

-- Storefront listing always filters on status, usually on category, and sorts by newest.
CREATE INDEX idx_products_status_category_created
    ON products (status, category_id, created_at);

-- Variant aggregation (min price, stock, size/colour filters) is always per product + active.
CREATE INDEX idx_variants_product_active
    ON product_variants (product_id, active);
