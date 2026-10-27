package com.aditya.supermarket.wishlist.service;

import com.aditya.supermarket.catalog.dto.ProductCardResponse;
import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.repository.ProductAvailabilityRepository;
import com.aditya.supermarket.catalog.repository.ProductAvailabilityRepository.ProductAvailability;
import com.aditya.supermarket.catalog.repository.ProductImageRepository;
import com.aditya.supermarket.catalog.repository.ProductImageRepository.MainImage;
import com.aditya.supermarket.wishlist.dto.WishlistItemResponse;
import com.aditya.supermarket.wishlist.entity.WishlistItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Builds storefront-style cards for a page of wishlist items with two batched queries. */
@Component
@RequiredArgsConstructor
public class WishlistAssembler {

    private final ProductImageRepository productImageRepository;
    private final ProductAvailabilityRepository productAvailabilityRepository;

    public List<WishlistItemResponse> assemble(List<WishlistItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = items.stream()
            .map(item -> item.getProduct().getId())
            .collect(Collectors.toSet());
        Map<Long, String> imageByProduct = productImageRepository.findMainImages(productIds).stream()
            .collect(Collectors.toMap(MainImage::getProductId, MainImage::getUrl, (first, second) -> first));
        Map<Long, ProductAvailability> availabilityByProduct =
            productAvailabilityRepository.summarizeActiveVariants(productIds).stream()
                .collect(Collectors.toMap(ProductAvailability::getProductId, Function.identity()));

        return items.stream()
            .map(item -> new WishlistItemResponse(
                toCard(item.getProduct(), imageByProduct, availabilityByProduct), item.getCreatedAt()))
            .toList();
    }

    private static ProductCardResponse toCard(Product product,
                                              Map<Long, String> imageByProduct,
                                              Map<Long, ProductAvailability> availabilityByProduct) {
        String imageUrl = imageByProduct.get(product.getId());
        ProductAvailability availability = availabilityByProduct.get(product.getId());
        if (availability == null) {
            // No active variant at all: show the base price, greyed out as out of stock.
            return new ProductCardResponse(
                product.getId(), product.getName(), product.getSlug(), product.getBasePrice(), false, imageUrl, false);
        }
        boolean priceVaries = availability.getMinPrice().compareTo(availability.getMaxPrice()) != 0;
        return new ProductCardResponse(
            product.getId(), product.getName(), product.getSlug(), availability.getMinPrice(),
            priceVaries, imageUrl, availability.getTotalStock() > 0);
    }
}
