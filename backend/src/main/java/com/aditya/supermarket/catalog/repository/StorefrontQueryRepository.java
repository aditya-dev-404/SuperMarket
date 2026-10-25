package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.dto.ProductCardResponse;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse.CategoryCrumb;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse.ImageInfo;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse.VariantInfo;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse.VendorInfo;
import com.aditya.supermarket.catalog.dto.ProductSearchCriteria;
import com.aditya.supermarket.catalog.dto.ProductSort;
import com.aditya.supermarket.catalog.dto.StorefrontFiltersResponse;
import com.aditya.supermarket.catalog.dto.StorefrontFiltersResponse.CategoryNode;
import com.aditya.supermarket.common.dto.PagedResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-only queries for the public storefront.
 *
 * <p>Why SQL instead of JPA here: the storefront needs MySQL FULLTEXT, a recursive category
 * tree, and per-product aggregates over variants (min price, total stock) combined with
 * filters. That is one set-based query in SQL, instead of loading entities and filtering in
 * Java. All writes still go through the JPA entities in the vendor/admin flows.
 *
 * <p>Visibility rule (applied by EVERY query below): a product is public only when
 * product.status = ACTIVE, its category is active, and its vendor is APPROVED; only
 * ACTIVE variants are ever considered.
 */
@Repository
public class StorefrontQueryRepository {

    /** Tables + visibility rules shared by every public query. Alias: p, c, v. */
    private static final String VISIBLE_PRODUCTS = """
            FROM products p
            JOIN categories c ON c.id = p.category_id AND c.active = TRUE
            JOIN vendors v ON v.id = p.vendor_id AND v.status = 'APPROVED'
            """;

    /** Category and all of its active descendants, bound to :categorySlug. */
    private static final String CATEGORY_TREE_CTE = """
            WITH RECURSIVE cat_tree (id) AS (
                SELECT id FROM categories WHERE slug = :categorySlug AND active = TRUE
                UNION ALL
                SELECT ch.id FROM categories ch
                JOIN cat_tree ct ON ch.parent_id = ct.id
                WHERE ch.active = TRUE
            )
            """;

    private final JdbcClient jdbc;

    public StorefrontQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- list / search

    public PagedResponse<ProductCardResponse> search(ProductSearchCriteria cr) {
        Map<String, Object> params = new HashMap<>();
        boolean hasText = cr.fullTextQuery() != null;
        boolean hasCategory = cr.categorySlug() != null;

        // 1) variants that satisfy the variant-level filters, aggregated per product
        StringBuilder variantWhere = new StringBuilder(" WHERE pv.active = TRUE");
        if (cr.variantSize() != null) {
            variantWhere.append(" AND pv.size = :variantSize");
            params.put("variantSize", cr.variantSize());
        }
        if (cr.variantColour() != null) {
            variantWhere.append(" AND pv.colour = :variantColour");
            params.put("variantColour", cr.variantColour());
        }
        if (cr.minPrice() != null) {
            variantWhere.append(" AND COALESCE(pv.price, pr.base_price) >= :minPrice");
            params.put("minPrice", cr.minPrice());
        }
        if (cr.maxPrice() != null) {
            variantWhere.append(" AND COALESCE(pv.price, pr.base_price) <= :maxPrice");
            params.put("maxPrice", cr.maxPrice());
        }
        if (cr.inStockOnly()) {
            variantWhere.append(" AND pv.stock_quantity > 0");
        }

        String aggregate = """
                JOIN (
                    SELECT pv.product_id,
                           MIN(COALESCE(pv.price, pr.base_price)) AS min_price,
                           MAX(COALESCE(pv.price, pr.base_price)) AS max_price,
                           SUM(pv.stock_quantity) AS total_stock
                    FROM product_variants pv
                    JOIN products pr ON pr.id = pv.product_id
                """ + variantWhere + "\n" + """
                    GROUP BY pv.product_id
                ) agg ON agg.product_id = p.id
                """;

        // 2) product-level filters
        StringBuilder where = new StringBuilder(" WHERE p.status = 'ACTIVE'");
        String with = "";
        if (hasCategory) {
            with = CATEGORY_TREE_CTE;
            where.append(" AND p.category_id IN (SELECT id FROM cat_tree)");
            params.put("categorySlug", cr.categorySlug());
        }
        if (hasText) {
            where.append(" AND MATCH(p.name, p.description) AGAINST (:ft IN BOOLEAN MODE)");
            params.put("ft", cr.fullTextQuery());
        }

        String fromAndWhere = VISIBLE_PRODUCTS + aggregate + where;

        // 3) total count (same filters, no paging)
        long total = jdbc.sql(with + "SELECT COUNT(*) " + fromAndWhere)
            .params(params)
            .query(Long.class)
            .single();

        if (total == 0) {
            return PagedResponse.of(List.of(), cr.page(), cr.pageSize(), 0);
        }

        // 4) one page of cards
        String select = """
                SELECT p.id, p.name, p.slug,
                       agg.min_price, agg.max_price, agg.total_stock,
                       (SELECT pi.url FROM product_images pi
                         WHERE pi.product_id = p.id
                         ORDER BY pi.`position`, pi.id LIMIT 1) AS image_url
                """
            + (hasText ? ", MATCH(p.name, p.description) AGAINST (:ft IN BOOLEAN MODE) AS relevance\n" : "\n");

        Map<String, Object> pageParams = new HashMap<>(params);
        pageParams.put("limit", cr.pageSize());
        pageParams.put("offset", (long) cr.page() * cr.pageSize());

        List<ProductCardResponse> cards = jdbc
            .sql(with + select + fromAndWhere + orderBy(cr.sort(), hasText) + " LIMIT :limit OFFSET :offset")
            .params(pageParams)
            .query((rs, rowNum) -> new ProductCardResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getBigDecimal("min_price"),
                rs.getBigDecimal("min_price").compareTo(rs.getBigDecimal("max_price")) != 0,
                rs.getString("image_url"),
                rs.getLong("total_stock") > 0))
            .list();

        return PagedResponse.of(cards, cr.page(), cr.pageSize(), total);
    }

    /** Only whitelisted fragments are ever concatenated into SQL. */
    private static String orderBy(ProductSort sort, boolean hasText) {
        ProductSort effective = (sort == ProductSort.RELEVANCE && !hasText) ? ProductSort.NEWEST : sort;
        return switch (effective) {
            case PRICE_ASC -> " ORDER BY agg.min_price ASC, p.id DESC";
            case PRICE_DESC -> " ORDER BY agg.min_price DESC, p.id DESC";
            case RELEVANCE -> " ORDER BY relevance DESC, p.id DESC";
            case NEWEST -> " ORDER BY p.created_at DESC, p.id DESC";
        };
    }

    // ---------------------------------------------------------------- detail

    private record ProductRow(
        Long id, String name, String slug, String description,
        BigDecimal basePrice, Long categoryId, Long vendorId, String vendorName) {
    }

    public Optional<ProductDetailResponse> findDetailBySlug(String slug) {
        Optional<ProductRow> row = jdbc.sql("""
                        SELECT p.id, p.name, p.slug, p.description, p.base_price, p.category_id,
                               v.id AS vendor_id, v.store_name AS vendor_name
                        """ + VISIBLE_PRODUCTS + """
                        WHERE p.slug = :slug AND p.status = 'ACTIVE'
                        """)
            .param("slug", slug)
            .query((rs, i) -> new ProductRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getBigDecimal("base_price"),
                rs.getLong("category_id"),
                rs.getLong("vendor_id"),
                rs.getString("vendor_name")))
            .optional();

        if (row.isEmpty()) {
            return Optional.empty();
        }
        ProductRow p = row.get();

        List<VariantInfo> variants = jdbc.sql("""
                        SELECT id, sku, size, colour,
                               COALESCE(price, :basePrice) AS price, stock_quantity
                        FROM product_variants
                        WHERE product_id = :productId AND active = TRUE
                        ORDER BY id
                        """)
            .param("basePrice", p.basePrice())
            .param("productId", p.id())
            .query((rs, i) -> new VariantInfo(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("size"),
                rs.getString("colour"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_quantity"),
                rs.getInt("stock_quantity") > 0))
            .list();

        List<ImageInfo> images = jdbc.sql("""
                        SELECT url, `position` AS pos
                        FROM product_images
                        WHERE product_id = :productId
                        ORDER BY `position`, id
                        """)
            .param("productId", p.id())
            .query((rs, i) -> new ImageInfo(rs.getString("url"), rs.getInt("pos")))
            .list();

        List<CategoryCrumb> breadcrumb = jdbc.sql("""
                        WITH RECURSIVE chain (id, name, slug, parent_id, depth) AS (
                            SELECT id, name, slug, parent_id, 0 FROM categories WHERE id = :categoryId
                            UNION ALL
                            SELECT c.id, c.name, c.slug, c.parent_id, chain.depth + 1
                            FROM categories c JOIN chain ON c.id = chain.parent_id
                        )
                        SELECT name, slug FROM chain ORDER BY depth DESC
                        """)
            .param("categoryId", p.categoryId())
            .query((rs, i) -> new CategoryCrumb(rs.getString("name"), rs.getString("slug")))
            .list();

        return Optional.of(new ProductDetailResponse(
            p.id(), p.name(), p.slug(), p.description(), p.basePrice(),
            new VendorInfo(p.vendorId(), p.vendorName()),
            breadcrumb, variants, images));
    }

    // ---------------------------------------------------------------- filter sidebar data

    public StorefrontFiltersResponse loadFilters(String categorySlug) {
        boolean hasCategory = categorySlug != null;
        String with = hasCategory ? CATEGORY_TREE_CTE : "";
        String categoryClause = hasCategory ? " AND p.category_id IN (SELECT id FROM cat_tree)" : "";

        // visible, active variants (optionally inside one category tree)
        String scope = """
                FROM product_variants pv
                JOIN products p ON p.id = pv.product_id AND p.status = 'ACTIVE'
                JOIN categories c ON c.id = p.category_id AND c.active = TRUE
                JOIN vendors v ON v.id = p.vendor_id AND v.status = 'APPROVED'
                WHERE pv.active = TRUE
                """ + categoryClause;

        List<CategoryNode> categories = jdbc.sql("""
                        SELECT c.name, c.slug, parent.slug AS parent_slug
                        FROM categories c
                        LEFT JOIN categories parent ON parent.id = c.parent_id
                        WHERE c.active = TRUE
                        ORDER BY c.name
                        """)
            .query((rs, i) -> new CategoryNode(
                rs.getString("name"), rs.getString("slug"), rs.getString("parent_slug")))
            .list();

        List<String> sizes = distinctValues(with, "pv.size", scope, categorySlug);
        List<String> colours = distinctValues(with, "pv.colour", scope, categorySlug);

        var priceStmt = jdbc.sql(with
            + "SELECT MIN(COALESCE(pv.price, p.base_price)) AS min_price,"
            + " MAX(COALESCE(pv.price, p.base_price)) AS max_price "
            + scope);
        if (hasCategory) {
            priceStmt = priceStmt.param("categorySlug", categorySlug);
        }
        BigDecimal[] range = priceStmt
            .query((rs, i) -> new BigDecimal[] {rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price")})
            .single();

        return new StorefrontFiltersResponse(categories, sizes, colours, range[0], range[1]);
    }

    private List<String> distinctValues(String with, String column, String scope, String categorySlug) {
        var stmt = jdbc.sql(with + "SELECT DISTINCT " + column + " AS value " + scope
            + " AND " + column + " IS NOT NULL AND " + column + " <> '' ORDER BY value");
        if (categorySlug != null) {
            stmt = stmt.param("categorySlug", categorySlug);
        }
        return stmt.query((rs, i) -> rs.getString("value")).list();
    }
}
