package com.aditya.supermarket.inventory.repository;

import com.aditya.supermarket.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Atomic stock changes done in the database itself. Reading the stock, subtracting in Java and saving
 * would let two buyers both take the last unit; one UPDATE with a condition cannot.
 */
public interface StockRepository extends Repository<ProductVariant, Long> {

    /** Subtracts the quantity only if enough is left. Returns the rows changed: 1 = done, 0 = not enough stock. */
    @Modifying
    @Query("""
        update ProductVariant v
        set v.stockQuantity = v.stockQuantity - :quantity
        where v.id = :variantId and v.stockQuantity >= :quantity
        """)
    int decrease(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    @Modifying
    @Query("""
        update ProductVariant v
        set v.stockQuantity = v.stockQuantity + :quantity
        where v.id = :variantId
        """)
    int increase(@Param("variantId") Long variantId, @Param("quantity") int quantity);
}
