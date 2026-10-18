package com.aditya.supermarket.vendor.repository;

import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByUserId(Long userId);

    Optional<Vendor> findByUserId(Long userId);

    Optional<Vendor> findByStoreNameIgnoreCase(String storeName);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Vendor> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);
}
