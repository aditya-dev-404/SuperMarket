package com.aditya.supermarket.wishlist.repository;

import com.aditya.supermarket.wishlist.entity.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /** Only products still on sale are listed; the product is fetched in the same query (no N+1). */
    @Query(
        value = """
            select w from WishlistItem w
              join fetch w.product p
            where w.userId = :userId
              and p.status = com.aditya.supermarket.catalog.entity.ProductStatus.ACTIVE
            """,
        countQuery = """
            select count(w) from WishlistItem w
            where w.userId = :userId
              and w.product.status = com.aditya.supermarket.catalog.entity.ProductStatus.ACTIVE
            """)
    Page<WishlistItem> findActiveByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    @Query("select w.product.id from WishlistItem w where w.userId = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);
}
