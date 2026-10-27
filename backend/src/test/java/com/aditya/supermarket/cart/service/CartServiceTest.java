package com.aditya.supermarket.cart.service;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.product;
import static com.aditya.supermarket.cart.CartTestData.setId;
import static com.aditya.supermarket.cart.CartTestData.user;
import static com.aditya.supermarket.cart.CartTestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.cart.config.CartProperties;
import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.dto.UpdateCartItemRequest;
import com.aditya.supermarket.cart.entity.Cart;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.cart.repository.CartItemRepository;
import com.aditya.supermarket.cart.repository.CartRepository;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductVariantRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final long USER_ID = 1L;
    private static final long CART_ID = 5L;
    private static final long VARIANT_ID = 10L;
    private static final long ITEM_ID = 77L;
    private static final long VENDOR_OWNER_ID = 99L;
    private static final int MAX_PER_LINE = 3;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private CartAssembler cartAssembler;

    private CartService cartService;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
            cartRepository, cartItemRepository, variantRepository, cartAssembler, new CartProperties(MAX_PER_LINE));
        cart = new Cart(USER_ID);
        setId(cart, CART_ID);
    }

    private ProductVariant buyableVariant(int stock) {
        return variant(VARIANT_ID,
            product(20, approvedVendor(30, user(VENDOR_OWNER_ID)), ProductStatus.ACTIVE), stock, null, true);
    }

    private void givenCartExistsWithoutLine(ProductVariant variant) {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(CART_ID, VARIANT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void addItem_createsNewLine_whenVariantNotInCart() {
        givenCartExistsWithoutLine(buyableVariant(5));

        cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 2));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_createsCart_whenUserHasNone() {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(buyableVariant(5)));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartIdAndVariantId(CART_ID, VARIANT_ID)).thenReturn(Optional.empty());

        cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1));

        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_mergesIntoExistingLine() {
        ProductVariant variant = buyableVariant(5);
        CartItem existing = new CartItem(cart, variant, 1);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(CART_ID, VARIANT_ID)).thenReturn(Optional.of(existing));

        cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 2));

        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_rejects_whenMergedQuantityExceedsLineLimit() {
        ProductVariant variant = buyableVariant(10);
        CartItem existing = new CartItem(cart, variant, 2);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(CART_ID, VARIANT_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 2)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("at most 3");
        assertThat(existing.getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_rejects_whenQuantityExceedsStock() {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(buyableVariant(2)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 3)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Only 2 left");
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_rejects_whenQuantityExceedsLineLimit() {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(buyableVariant(10)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 4)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("at most 3");
    }

    @Test
    void addItem_rejects_whenOutOfStock() {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(buyableVariant(0)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("out of stock");
    }

    @Test
    void addItem_rejects_ownProduct() {
        ProductVariant ownVariant = variant(VARIANT_ID,
            product(20, approvedVendor(30, user(USER_ID)), ProductStatus.ACTIVE), 5, null, true);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(ownVariant));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("own product");
        verifyNoInteractions(cartRepository);
    }

    @Test
    void addItem_rejects_whenProductIsNotActive() {
        ProductVariant archived = variant(VARIANT_ID,
            product(20, approvedVendor(30, user(VENDOR_OWNER_ID)), ProductStatus.ARCHIVED), 5, null, true);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no longer available");
    }

    @Test
    void addItem_rejects_whenVariantIsInactive() {
        ProductVariant inactive = variant(VARIANT_ID,
            product(20, approvedVendor(30, user(VENDOR_OWNER_ID)), ProductStatus.ACTIVE), 5, null, false);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no longer available");
    }

    @Test
    void addItem_throwsNotFound_whenVariantDoesNotExist() {
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(VARIANT_ID, 1)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateQuantity_setsNewQuantity() {
        CartItem item = new CartItem(cart, buyableVariant(5), 1);
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        cartService.updateQuantity(USER_ID, ITEM_ID, new UpdateCartItemRequest(3));

        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void updateQuantity_rejects_whenAboveStock() {
        CartItem item = new CartItem(cart, buyableVariant(2), 1);
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, ITEM_ID, new UpdateCartItemRequest(3)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Only 2 left");
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    void updateQuantity_throwsNotFound_forSomeoneElsesOrMissingLine() {
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, ITEM_ID, new UpdateCartItemRequest(2)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_deletesTheLine() {
        CartItem item = new CartItem(cart, buyableVariant(5), 1);
        when(cartItemRepository.findByIdAndCartUserId(ITEM_ID, USER_ID)).thenReturn(Optional.of(item));

        cartService.removeItem(USER_ID, ITEM_ID);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void clear_deletesAllLinesOfTheUser() {
        cartService.clear(USER_ID);

        verify(cartItemRepository).deleteAllByUserId(USER_ID);
    }
}
