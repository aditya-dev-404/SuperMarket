package com.aditya.supermarket.cart.service;

import com.aditya.supermarket.cart.config.CartProperties;
import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.dto.UpdateCartItemRequest;
import com.aditya.supermarket.cart.entity.Cart;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.cart.repository.CartItemRepository;
import com.aditya.supermarket.cart.repository.CartRepository;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductVariantRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side cart. Stock is checked on every change but never reserved; the final stock check
 * happens at checkout (Phase 6). Prices are always read live from the catalog.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CartAssembler cartAssembler;
    private final CartProperties cartProperties;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return currentCart(userId);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        ProductVariant variant = variantRepository.findById(request.variantId())
            .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        assertPurchasable(variant);
        if (isOwnProduct(variant, userId)) {
            throw new BusinessRuleException("You cannot buy your own product");
        }
        // Checked before merging: rejects early and rules out int overflow when adding to a line.
        assertQuantityAllowed(variant, request.quantity());

        Cart cart = cartRepository.findByUserId(userId)
            .orElseGet(() -> cartRepository.save(new Cart(userId)));
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
        assertQuantityAllowed(variant, newQuantity);

        existing.ifPresentOrElse(
            item -> item.changeQuantity(newQuantity),
            () -> cartItemRepository.save(new CartItem(cart, variant, newQuantity)));
        return currentCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = findOwnedItem(userId, itemId);
        ProductVariant variant = item.getVariant();
        assertPurchasable(variant);
        assertQuantityAllowed(variant, request.quantity());
        item.changeQuantity(request.quantity());
        return currentCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        cartItemRepository.delete(findOwnedItem(userId, itemId));
        return currentCart(userId);
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    private CartResponse currentCart(Long userId) {
        return cartAssembler.assemble(cartItemRepository.findDetailedByUserId(userId));
    }

    private CartItem findOwnedItem(Long userId, Long itemId) {
        return cartItemRepository.findByIdAndCartUserId(itemId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    private static void assertPurchasable(ProductVariant variant) {
        if (!CartItemRules.isPurchasable(variant)) {
            throw new BusinessRuleException("This item is no longer available");
        }
    }

    private static boolean isOwnProduct(ProductVariant variant, Long userId) {
        return variant.getProduct().getVendor().getUser().getId().equals(userId);
    }

    private void assertQuantityAllowed(ProductVariant variant, int quantity) {
        int stock = variant.getStockQuantity();
        if (stock <= 0) {
            throw new BusinessRuleException("This item is out of stock");
        }
        if (quantity > stock) {
            throw new BusinessRuleException("Only " + stock + " left in stock");
        }
        int maxPerLine = cartProperties.maxQuantityPerLine();
        if (quantity > maxPerLine) {
            throw new BusinessRuleException("You can add at most " + maxPerLine + " units of the same item");
        }
    }
}
