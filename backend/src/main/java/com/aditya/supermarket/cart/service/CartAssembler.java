package com.aditya.supermarket.cart.service;

import com.aditya.supermarket.cart.dto.CartItemResponse;
import com.aditya.supermarket.cart.dto.CartItemStatus;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.dto.CartVendorGroup;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductImageRepository;
import com.aditya.supermarket.catalog.repository.ProductImageRepository.MainImage;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Turns loaded cart lines into the API response: vendor groups, live prices, line statuses, totals. */
@Component
@RequiredArgsConstructor
public class CartAssembler {

    private final ProductImageRepository productImageRepository;

    public CartResponse assemble(List<CartItem> items) {
        if (items.isEmpty()) {
            return CartResponse.empty();
        }

        Set<Long> productIds = items.stream()
            .map(item -> item.getVariant().getProduct().getId())
            .collect(Collectors.toSet());
        Map<Long, String> imageByProduct = productImageRepository.findMainImages(productIds).stream()
            .collect(Collectors.toMap(MainImage::getProductId, MainImage::getUrl, (first, second) -> first));

        Map<Long, List<CartItem>> itemsByVendor = items.stream()
            .collect(Collectors.groupingBy(
                item -> item.getVariant().getProduct().getVendor().getId(),
                LinkedHashMap::new,
                Collectors.toList()));

        List<CartVendorGroup> groups = itemsByVendor.values().stream()
            .map(vendorItems -> toGroup(vendorItems, imageByProduct))
            .toList();

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = groups.stream().map(CartVendorGroup::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean checkoutReady = groups.stream()
            .flatMap(group -> group.items().stream())
            .allMatch(line -> line.status() == CartItemStatus.AVAILABLE);

        return new CartResponse(groups, totalQuantity, subtotal, checkoutReady);
    }

    private CartVendorGroup toGroup(List<CartItem> vendorItems, Map<Long, String> imageByProduct) {
        Vendor vendor = vendorItems.getFirst().getVariant().getProduct().getVendor();
        List<CartItemResponse> lines = vendorItems.stream()
            .map(item -> toLine(item, imageByProduct))
            .toList();
        // Lines that cannot be bought right now must not inflate what the customer would pay.
        BigDecimal subtotal = lines.stream()
            .filter(line -> line.status() == CartItemStatus.AVAILABLE)
            .map(CartItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartVendorGroup(vendor.getId(), vendor.getStoreName(), lines, subtotal);
    }

    private CartItemResponse toLine(CartItem item, Map<Long, String> imageByProduct) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        BigDecimal unitPrice = CartItemRules.unitPrice(variant);
        return new CartItemResponse(
            item.getId(),
            variant.getId(),
            product.getId(),
            product.getName(),
            product.getSlug(),
            variant.getSku(),
            variant.getSize(),
            variant.getColour(),
            imageByProduct.get(product.getId()),
            unitPrice,
            item.getQuantity(),
            unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
            variant.getStockQuantity(),
            CartItemRules.statusOf(item));
    }
}
