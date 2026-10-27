package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only helper so list-style screens (cart, wishlist) can load thumbnails in one query. */
public interface ProductImageRepository extends Repository<ProductImage, Long> {

    /** The main (lowest position) image of each given product. */
    @Query("""
        select i.product.id as productId, i.url as url
        from ProductImage i
        where i.product.id in :productIds
          and i.position = (select min(i2.position) from ProductImage i2 where i2.product.id = i.product.id)
        """)
    List<MainImage> findMainImages(@Param("productIds") Collection<Long> productIds);

    interface MainImage {

        Long getProductId();

        String getUrl();
    }
}
