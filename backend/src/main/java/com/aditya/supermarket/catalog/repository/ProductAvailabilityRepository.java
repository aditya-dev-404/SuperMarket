package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only helper: price range and stock of the active variants of many products at once. */
public interface ProductAvailabilityRepository extends Repository<ProductVariant, Long> {

    @Query("""
        select v.product.id as productId,
               min(coalesce(v.price, p.basePrice)) as minPrice,
               max(coalesce(v.price, p.basePrice)) as maxPrice,
               sum(v.stockQuantity) as totalStock
        from ProductVariant v join v.product p
        where p.id in :productIds and v.active = true
        group by v.product.id
        """)
    List<ProductAvailability> summarizeActiveVariants(@Param("productIds") Collection<Long> productIds);

    interface ProductAvailability {

        Long getProductId();

        BigDecimal getMinPrice();

        BigDecimal getMaxPrice();

        Long getTotalStock();
    }
}
