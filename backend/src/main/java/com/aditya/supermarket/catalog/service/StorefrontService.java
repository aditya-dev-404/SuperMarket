package com.aditya.supermarket.catalog.service;

import com.aditya.supermarket.catalog.dto.ProductCardResponse;
import com.aditya.supermarket.catalog.dto.ProductDetailResponse;
import com.aditya.supermarket.catalog.dto.ProductSearchCriteria;
import com.aditya.supermarket.catalog.dto.ProductSort;
import com.aditya.supermarket.catalog.dto.StorefrontFiltersResponse;
import com.aditya.supermarket.catalog.repository.StorefrontQueryRepository;
import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public (no login) catalog browsing: search, filter, paginate, product page. */
@Service
@Transactional(readOnly = true)
public class StorefrontService {

    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int MAX_PAGE_SIZE = 50;

    private final StorefrontQueryRepository queries;

    public StorefrontService(StorefrontQueryRepository queries) {
        this.queries = queries;
    }

    public PagedResponse<ProductCardResponse> search(
        String q,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String size,
        String colour,
        boolean inStockOnly,
        String sort,
        Integer page,
        Integer limit) {

        Optional<String> fullText = FullTextQuery.toBooleanMode(q);

        // normalise price range: ignore negatives, swap if reversed
        BigDecimal min = nonNegative(minPrice);
        BigDecimal max = nonNegative(maxPrice);
        if (min != null && max != null && min.compareTo(max) > 0) {
            BigDecimal tmp = min;
            min = max;
            max = tmp;
        }

        ProductSort requested = ProductSort.fromParam(sort);
        ProductSort effectiveSort = requested != null
            ? requested
            : (fullText.isPresent() ? ProductSort.RELEVANCE : ProductSort.NEWEST);

        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeLimit = limit == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);

        return queries.search(new ProductSearchCriteria(
            fullText.orElse(null),
            blankToNull(category),
            min,
            max,
            blankToNull(size),
            blankToNull(colour),
            inStockOnly,
            effectiveSort,
            safePage,
            safeLimit));
    }

    public ProductDetailResponse getBySlug(String slug) {
        return queries.findDetailBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public StorefrontFiltersResponse getFilters(String category) {
        return queries.loadFilters(blankToNull(category));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? null : value;
    }
}
