package com.aditya.supermarket.cart.repository;

import com.aditya.supermarket.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Whole cart in one query (variant, product, vendor) so building the response never hits N+1. */
    @Query("""
        select i from CartItem i
          join fetch i.variant v
          join fetch v.product p
          join fetch p.vendor
        where i.cart.userId = :userId
        order by i.id
        """)
    List<CartItem> findDetailedByUserId(@Param("userId") Long userId);

    /** Scoped to the owner so one user can never touch another user's line. */
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CartItem i where i.cart.id in (select c.id from Cart c where c.userId = :userId)")
    void deleteAllByUserId(@Param("userId") Long userId);
}
