package com.aditya.supermarket.wishlist.service;

import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductRepository;
import com.aditya.supermarket.catalog.repository.ProductVariantRepository;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.wishlist.dto.MoveToCartRequest;
import com.aditya.supermarket.wishlist.dto.WishlistItemResponse;
import com.aditya.supermarket.wishlist.entity.WishlistItem;
import com.aditya.supermarket.wishlist.repository.WishlistItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    /** Upper bound so a client cannot ask for an unbounded page. */
    private static final int MAX_PAGE_SIZE = 50;

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;
    private final WishlistAssembler wishlistAssembler;

    @Transactional(readOnly = true)
    public Page<WishlistItemResponse> list(Long userId, int page, int size) {
        // id as tie-breaker keeps paging stable when several items share a timestamp.
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<WishlistItem> items = wishlistItemRepository.findActiveByUserId(userId, pageable);
        return new PageImpl<>(wishlistAssembler.assemble(items.getContent()), pageable, items.getTotalElements());
    }

    /** Ids of all wishlisted products, so list pages can fill the heart icons with one request. */
    @Transactional(readOnly = true)
    public List<Long> productIds(Long userId) {
        return wishlistItemRepository.findProductIdsByUserId(userId);
    }

    /** Idempotent: adding a product that is already saved changes nothing. */
    @Transactional
    public void add(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
            .filter(found -> found.getStatus() == ProductStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        wishlistItemRepository.save(new WishlistItem(userId, product));
    }

    /** Idempotent: removing a product that is not saved changes nothing. */
    @Transactional
    public void remove(Long userId, Long productId) {
        wishlistItemRepository.findByUserIdAndProductId(userId, productId)
            .ifPresent(wishlistItemRepository::delete);
    }

    /** Adds one unit of the chosen variant to the cart and removes the product from the wishlist. */
    @Transactional
    public CartResponse moveToCart(Long userId, Long productId, MoveToCartRequest request) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        ProductVariant variant = variantRepository.findById(request.variantId())
            .filter(found -> found.getProduct().getId().equals(productId))
            .orElseThrow(() -> new ResourceNotFoundException("Variant not found for this product"));

        CartResponse cart = cartService.addItem(userId, new AddCartItemRequest(variant.getId(), 1));
        wishlistItemRepository.delete(item);
        return cart;
    }
}
