-- Phase 6: addresses, checkout and orders.
-- An order keeps its own copy (snapshot) of the shipping address, product details and prices, so later
-- edits or deletions of the live data never change what the customer actually ordered.

CREATE TABLE addresses (
                           id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                           user_id    BIGINT       NOT NULL,
                           full_name  VARCHAR(100) NOT NULL,
                           phone      VARCHAR(15)  NOT NULL,
                           line1      VARCHAR(200) NOT NULL,
                           line2      VARCHAR(200) NULL,
                           city       VARCHAR(100) NOT NULL,
                           state      VARCHAR(100) NOT NULL,
                           pincode    VARCHAR(6)   NOT NULL,
                           is_default BOOLEAN      NOT NULL DEFAULT FALSE,
                           created_at DATETIME(6)  NOT NULL,
                           updated_at DATETIME(6)  NOT NULL,
                           CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE orders (
                        id                 BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        order_number       VARCHAR(30)    NOT NULL,
                        user_id            BIGINT         NOT NULL,
                        payment_method     VARCHAR(20)    NOT NULL,
                        payment_status     VARCHAR(20)    NOT NULL,
                        subtotal           DECIMAL(12, 2) NOT NULL,
                        shipping_fee       DECIMAL(12, 2) NOT NULL,
                        total              DECIMAL(12, 2) NOT NULL,
                        shipping_full_name VARCHAR(100)   NOT NULL,
                        shipping_phone     VARCHAR(15)    NOT NULL,
                        shipping_line1     VARCHAR(200)   NOT NULL,
                        shipping_line2     VARCHAR(200)   NULL,
                        shipping_city      VARCHAR(100)   NOT NULL,
                        shipping_state     VARCHAR(100)   NOT NULL,
                        shipping_pincode   VARCHAR(6)     NOT NULL,
                        created_at         DATETIME(6)    NOT NULL,
                        updated_at         DATETIME(6)    NOT NULL,
                        CONSTRAINT uk_orders_order_number UNIQUE (order_number),
                        CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND shipping_fee >= 0 AND total >= 0),
    -- no ON DELETE action: a user who has orders cannot be deleted by accident
                        CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at);

-- One sub-order per vendor in an order; each vendor fulfils (and updates the status of) only their own.
CREATE TABLE sub_orders (
                            id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            order_id     BIGINT         NOT NULL,
                            vendor_id    BIGINT         NOT NULL,
                            status       VARCHAR(20)    NOT NULL,
                            subtotal     DECIMAL(12, 2) NOT NULL,
                            shipped_at   DATETIME(6)    NULL,
                            delivered_at DATETIME(6)    NULL,
                            cancelled_at DATETIME(6)    NULL,
                            created_at   DATETIME(6)    NOT NULL,
                            updated_at   DATETIME(6)    NOT NULL,
                            CONSTRAINT uk_sub_orders_order_vendor UNIQUE (order_id, vendor_id),
                            CONSTRAINT ck_sub_orders_subtotal CHECK (subtotal >= 0),
                            CONSTRAINT fk_sub_orders_order FOREIGN KEY (order_id) REFERENCES orders (id),
                            CONSTRAINT fk_sub_orders_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id)
);
CREATE INDEX idx_sub_orders_vendor_status_created ON sub_orders (vendor_id, status, created_at);

CREATE TABLE order_items (
                             id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                             sub_order_id BIGINT         NOT NULL,
                             variant_id   BIGINT         NULL,
                             product_id   BIGINT         NULL,
                             product_name VARCHAR(200)   NOT NULL,
                             product_slug VARCHAR(260)   NOT NULL,
                             sku          VARCHAR(64)    NOT NULL,
                             size         VARCHAR(30)    NULL,
                             colour       VARCHAR(30)    NULL,
                             image_url    VARCHAR(500)   NULL,
                             unit_price   DECIMAL(12, 2) NOT NULL,
                             quantity     INT            NOT NULL,
                             line_total   DECIMAL(12, 2) NOT NULL,
                             created_at   DATETIME(6)    NOT NULL,
                             updated_at   DATETIME(6)    NOT NULL,
                             CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
                             CONSTRAINT fk_order_items_sub_order FOREIGN KEY (sub_order_id) REFERENCES sub_orders (id),
    -- a vendor may delete a variant or product later; the snapshot columns above keep the order history intact
                             CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL,
                             CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
);

-- One row per payment attempt (the mock gateway now, a real gateway in Phase 10).
CREATE TABLE payments (
                          id                 BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          order_id           BIGINT         NOT NULL,
                          provider           VARCHAR(30)    NOT NULL,
                          status             VARCHAR(20)    NOT NULL,
                          amount             DECIMAL(12, 2) NOT NULL,
                          provider_reference VARCHAR(64)    NULL,
                          created_at         DATETIME(6)    NOT NULL,
                          updated_at         DATETIME(6)    NOT NULL,
                          CONSTRAINT ck_payments_amount CHECK (amount >= 0),
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
