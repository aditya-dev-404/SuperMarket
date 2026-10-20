package com.aditya.supermarket.catalog.repository;

import com.aditya.supermarket.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCaseAndParentId(String name, Long parentId);
    List<Category> findByActiveTrueOrderByNameAsc();
}
