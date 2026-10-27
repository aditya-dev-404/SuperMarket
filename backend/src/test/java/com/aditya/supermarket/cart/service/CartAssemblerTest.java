package com.aditya.supermarket.cart.service;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.product;
import static com.aditya.supermarket.cart.CartTestData.setId;
import static com.aditya.supermarket.cart.CartTestData.user;
import static com.aditya.supermarket.cart.CartTestData.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.cart.dto.CartItemResponse;
import com.aditya.supermarket.cart.dto.CartItemStatus;
import com.aditya.supermarket.cart.dto.CartResponse;
import com.aditya.supermarket.cart.entity.Cart;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.catalog.repository.ProductImageRepository;
import com.aditya.supermarket.catalog.repository.ProductImageRepository.MainImage;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartAssemblerTest {

    @Mock
    private ProductImageRepository productImageRepository;
    @InjectMocks
    private CartAssembler cartAssembler;

    private final Cart cart = new Cart(1L);
    private final Vendor vendorA = approvedVendor(30, user(91));
    private final Vendor vendorB = approvedVendor(31, user(92));

    private CartItem line(long itemId, ProductVariant variant, int quantity) {
        CartItem item = new CartItem(cart, variant, quantity);
        setId(item, itemId);
        return item;
    }

    @Test
    void assemble_returnsEmptyCart_forNoItems() {
        CartResponse response = cartAssembler.assemble(List.of());

        assertThat(response).isEqualTo(CartResponse.empty());
        verifyNoInteractions(productImageRepository);
    }

    @Test
    void assemble_groupsByVendor_usesLivePrices_andTotals() {
        // product 20 (vendor A): base price 100, variant override 80; product 21 (vendor B): base price 100
        ProductVariant cheap = variant(10, product(20, vendorA, ProductStatus.ACTIVE), 5, new BigDecimal("80.00"), true);
        ProductVariant regular = variant(11, product(21, vendorB, ProductStatus.ACTIVE), 5, null, true);
        MainImage image = mock(MainImage.class);
        when(image.getProductId()).thenReturn(20L);
        when(image.getUrl()).thenReturn("https://img.example/a.jpg");
        when(productImageRepository.findMainImages(anyCollection())).thenReturn(List.of(image));

        CartResponse response = cartAssembler.assemble(List.of(line(1, cheap, 2), line(2, regular, 1)));

        assertThat(response.vendors()).hasSize(2);
        assertThat(response.vendors().get(0).storeName()).isEqualTo("Store 30");
        CartItemResponse first = response.vendors().get(0).items().getFirst();
        assertThat(first.unitPrice()).isEqualByComparingTo("80.00");
        assertThat(first.lineTotal()).isEqualByComparingTo("160.00");
        assertThat(first.imageUrl()).isEqualTo("https://img.example/a.jpg");
        assertThat(response.vendors().get(1).items().getFirst().imageUrl()).isNull();
        assertThat(response.totalQuantity()).isEqualTo(3);
        assertThat(response.subtotal()).isEqualByComparingTo("260.00");
        assertThat(response.checkoutReady()).isTrue();
    }

    @Test
    void assemble_flagsProblemLines_andExcludesThemFromSubtotal() {
        ProductVariant ok = variant(10, product(20, vendorA, ProductStatus.ACTIVE), 5, null, true);
        ProductVariant soldOut = variant(11, product(21, vendorA, ProductStatus.ACTIVE), 0, null, true);
        ProductVariant lowStock = variant(12, product(22, vendorA, ProductStatus.ACTIVE), 2, null, true);
        ProductVariant archived = variant(13, product(23, vendorA, ProductStatus.ARCHIVED), 5, null, true);
        when(productImageRepository.findMainImages(anyCollection())).thenReturn(List.of());

        CartResponse response = cartAssembler.assemble(List.of(
            line(1, ok, 1), line(2, soldOut, 1), line(3, lowStock, 3), line(4, archived, 1)));

        List<CartItemStatus> statuses = response.vendors().getFirst().items().stream()
            .map(CartItemResponse::status).toList();
        assertThat(statuses).containsExactly(
            CartItemStatus.AVAILABLE,
            CartItemStatus.OUT_OF_STOCK,
            CartItemStatus.INSUFFICIENT_STOCK,
            CartItemStatus.UNAVAILABLE);
        assertThat(response.subtotal()).isEqualByComparingTo("100.00");
        assertThat(response.totalQuantity()).isEqualTo(6);
        assertThat(response.checkoutReady()).isFalse();
    }
}
