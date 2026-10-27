CREATE TABLE carts (
                       id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       user_id    BIGINT      NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       CONSTRAINT uk_carts_user UNIQUE (user_id),
                       CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
                            id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            cart_id    BIGINT      NOT NULL,
                            variant_id BIGINT      NOT NULL,
                            quantity   INT         NOT NULL,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            CONSTRAINT uk_cart_items_cart_variant UNIQUE (cart_id, variant_id),
                            CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
                            CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    -- a vendor deleting a variant silently removes it from every cart
                            CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE TABLE wishlist_items (
                                id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                user_id    BIGINT      NOT NULL,
                                product_id BIGINT      NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                updated_at DATETIME(6) NOT NULL,
                                CONSTRAINT uk_wishlist_items_user_product UNIQUE (user_id, product_id),
                                CONSTRAINT fk_wishlist_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);
CREATE INDEX idx_wishlist_items_user_created ON wishlist_items (user_id, created_at);
