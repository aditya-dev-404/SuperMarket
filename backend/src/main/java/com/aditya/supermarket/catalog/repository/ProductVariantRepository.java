package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    boolean existsBySkuIgnoreCase(String sku);
}
