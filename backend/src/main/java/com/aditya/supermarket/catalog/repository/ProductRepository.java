package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByVendorId(Long vendorId, Pageable pageable);
    Page<Product> findByVendorIdAndStatus(Long vendorId, ProductStatus status, Pageable pageable);
    Optional<Product> findByIdAndVendorId(Long id, Long vendorId);
}
