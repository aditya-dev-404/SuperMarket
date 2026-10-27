package com.aditya.supermarket.wishlist.service;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.product;
import static com.aditya.supermarket.cart.CartTestData.user;
import static com.aditya.supermarket.cart.CartTestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.aditya.supermarket.wishlist.entity.WishlistItem;
import com.aditya.supermarket.wishlist.repository.WishlistItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    private static final long USER_ID = 1L;
    private static final long PRODUCT_ID = 20L;
    private static final long VARIANT_ID = 10L;

    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private CartService cartService;
    @Mock
    private WishlistAssembler wishlistAssembler;

    private WishlistService wishlistService;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistService(
            wishlistItemRepository, productRepository, variantRepository, cartService, wishlistAssembler);
        activeProduct = product(PRODUCT_ID, approvedVendor(30, user(99)), ProductStatus.ACTIVE);
    }

    @Test
    void add_savesNewItem() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);

        wishlistService.add(USER_ID, PRODUCT_ID);

        ArgumentCaptor<WishlistItem> saved = ArgumentCaptor.forClass(WishlistItem.class);
        verify(wishlistItemRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getProduct()).isSameAs(activeProduct);
    }

    @Test
    void add_isIdempotent_whenAlreadySaved() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        wishlistService.add(USER_ID, PRODUCT_ID);

        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void add_throwsNotFound_whenProductIsNotActive() {
        Product draft = product(PRODUCT_ID, approvedVendor(30, user(99)), ProductStatus.DRAFT);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> wishlistService.add(USER_ID, PRODUCT_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(wishlistItemRepository);
    }

    @Test
    void remove_deletesTheItem() {
        WishlistItem item = new WishlistItem(USER_ID, activeProduct);
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(item));

        wishlistService.remove(USER_ID, PRODUCT_ID);

        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void moveToCart_addsOneUnit_andRemovesFromWishlist() {
        WishlistItem item = new WishlistItem(USER_ID, activeProduct);
        ProductVariant variant = variant(VARIANT_ID, activeProduct, 5, null, true);
        CartResponse cart = CartResponse.empty();
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(item));
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1))).thenReturn(cart);

        CartResponse result = wishlistService.moveToCart(USER_ID, PRODUCT_ID, new MoveToCartRequest(VARIANT_ID));

        assertThat(result).isSameAs(cart);
        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void moveToCart_rejectsVariantOfAnotherProduct() {
        WishlistItem item = new WishlistItem(USER_ID, activeProduct);
        Product other = product(21, approvedVendor(30, user(99)), ProductStatus.ACTIVE);
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(item));
        when(variantRepository.findById(VARIANT_ID))
            .thenReturn(Optional.of(variant(VARIANT_ID, other, 5, null, true)));

        assertThatThrownBy(() -> wishlistService.moveToCart(USER_ID, PRODUCT_ID, new MoveToCartRequest(VARIANT_ID)))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(cartService);
        verify(wishlistItemRepository, never()).delete(any(WishlistItem.class));
    }

    @Test
    void moveToCart_throwsNotFound_whenProductIsNotWishlisted() {
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.moveToCart(USER_ID, PRODUCT_ID, new MoveToCartRequest(VARIANT_ID)))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(cartService);
    }
}
