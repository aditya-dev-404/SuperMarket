package com.aditya.supermarket.catalog.controller;

import com.aditya.supermarket.catalog.dto.ProductCardResponse;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse;
import com.aditya.supermarket.catalog.dto.StorefrontFiltersResponse;
import com.aditya.supermarket.catalog.service.StorefrontService;
import com.aditya.supermarket.common.dto.PagedResponse;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public storefront API (no authentication).
 * Must be permitted in SecurityConfig: GET /api/v1/products/**
 */
@RestController
@RequestMapping("/api/v1/products")
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    /** Search + filter + sort + paginate. {@code page} is zero-based, default size 15 (max 50). */
    @GetMapping
    public PagedResponse<ProductCardResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String colour,
        @RequestParam(defaultValue = "false") boolean inStock,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer limit) {
        return storefrontService.search(q, category, minPrice, maxPrice, size, colour, inStock, sort, page, limit);
    }

    /** Data for the filter sidebar (categories, sizes, colours, price range). */
    @GetMapping("/filters")
    public StorefrontFiltersResponse filters(@RequestParam(required = false) String category) {
        return storefrontService.getFilters(category);
    }

    /** Public product page by slug. 404 for drafts, archived, or non-approved-vendor products. */
    @GetMapping("/{slug}")
    public ProductDetailResponse detail(@PathVariable String slug) {
        return storefrontService.getBySlug(slug);
    }
}
