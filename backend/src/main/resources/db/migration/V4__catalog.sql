CREATE TABLE categories (
                            id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            name       VARCHAR(100) NOT NULL,
                            slug       VARCHAR(120) NOT NULL,
                            parent_id  BIGINT       NULL,
                            active     BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at DATETIME(6)  NOT NULL,
                            updated_at DATETIME(6)  NOT NULL,
                            CONSTRAINT uk_categories_slug UNIQUE (slug),
                            CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE TABLE products (
                          id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          vendor_id   BIGINT         NOT NULL,
                          category_id BIGINT         NOT NULL,
                          name        VARCHAR(200)   NOT NULL,
                          slug        VARCHAR(260)   NOT NULL,
                          description TEXT           NULL,
                          brand       VARCHAR(100)   NULL,
                          base_price  DECIMAL(12, 2) NOT NULL,
                          status      VARCHAR(20)    NOT NULL,
                          created_at  DATETIME(6)    NOT NULL,
                          updated_at  DATETIME(6)    NOT NULL,
                          CONSTRAINT uk_products_slug UNIQUE (slug),
                          CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
                          CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);
CREATE INDEX idx_products_vendor_status ON products (vendor_id, status);
CREATE INDEX idx_products_category_status ON products (category_id, status);

CREATE TABLE product_variants (
                                  id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                  product_id     BIGINT         NOT NULL,
                                  sku            VARCHAR(64)    NOT NULL,
                                  size           VARCHAR(30)    NULL,
                                  colour         VARCHAR(30)    NULL,
                                  price          DECIMAL(12, 2) NULL,
                                  stock_quantity INT            NOT NULL DEFAULT 0,
                                  active         BOOLEAN        NOT NULL DEFAULT TRUE,
                                  created_at     DATETIME(6)    NOT NULL,
                                  updated_at     DATETIME(6)    NOT NULL,
                                  CONSTRAINT uk_variants_sku UNIQUE (sku),
                                  CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE product_images (
                                id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                product_id BIGINT       NOT NULL,
                                url        VARCHAR(500) NOT NULL,
                                public_id  VARCHAR(255) NOT NULL,
                                position   INT          NOT NULL DEFAULT 0,
                                created_at DATETIME(6)  NOT NULL,
                                updated_at DATETIME(6)  NOT NULL,
                                CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);
