package com.aditya.supermarket.order.service;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.product;
import static com.aditya.supermarket.cart.CartTestData.user;
import static com.aditya.supermarket.cart.CartTestData.variant;
import static com.aditya.supermarket.order.OrderTestData.address;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.address.service.AddressService;
import com.aditya.supermarket.cart.entity.Cart;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.cart.repository.CartItemRepository;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.repository.ProductImageRepository;
import com.aditya.supermarket.catalog.repository.ProductImageRepository.MainImage;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.config.CheckoutProperties;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.dto.PlaceOrderRequest;
import com.aditya.supermarket.order.dto.SubOrderResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final long USER_ID = 1L;
    private static final long ADDRESS_ID = 7L;
    private static final BigDecimal COD_FEE = new BigDecimal("40");

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CartService cartService;
    @Mock
    private AddressService addressService;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private OrderInventory orderInventory;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;
    @Mock
    private ApplicationEventPublisher events;

    private CheckoutService checkoutService;
    private Cart cart;
    private Vendor vendorA;
    private Vendor vendorB;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(cartItemRepository, cartService, addressService,
            productImageRepository, orderInventory, orderRepository, orderNumberGenerator,
            new CheckoutProperties(COD_FEE), events);
        cart = new Cart(USER_ID);
        vendorA = approvedVendor(30, user(99));
        vendorB = approvedVendor(31, user(98));
    }

    private CartItem line(long variantId, long productId, Vendor vendor, int stock, BigDecimal priceOverride, int quantity) {
        return new CartItem(cart,
            variant(variantId, product(productId, vendor, ProductStatus.ACTIVE), stock, priceOverride, true), quantity);
    }

    private static PlaceOrderRequest request(PaymentMethod method) {
        return new PlaceOrderRequest(ADDRESS_ID, method);
    }

    private void givenAddressAndCart(CartItem... items) {
        when(addressService.getOwned(USER_ID, ADDRESS_ID)).thenReturn(address());
        when(cartItemRepository.findDetailedByUserId(USER_ID)).thenReturn(List.of(items));
    }

    private void givenOrderCanBeSaved() {
        when(orderNumberGenerator.next()).thenReturn("SM-TEST");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static MainImage mainImage(long productId, String url) {
        return new MainImage() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public String getUrl() {
                return url;
            }
        };
    }

    @Test
    void placeOrder_cod_splitsByVendor_snapshotsLines_andAddsShipping() {
        givenAddressAndCart(
            line(10, 20, vendorA, 5, null, 2),
            line(11, 21, vendorA, 5, new BigDecimal("80.00"), 1),
            line(12, 22, vendorB, 3, null, 1));
        givenOrderCanBeSaved();

        OrderResponse response = checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD));

        assertThat(response.orderNumber()).isEqualTo("SM-TEST");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COD);
        assertThat(response.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(response.subtotal()).isEqualByComparingTo("380.00");
        assertThat(response.shippingFee()).isEqualByComparingTo("40.00");
        assertThat(response.total()).isEqualByComparingTo("420.00");
        assertThat(response.payable()).isFalse();
        assertThat(response.cancellable()).isTrue();
        assertThat(response.shippingAddress().city()).isEqualTo("Greater Noida");

        assertThat(response.subOrders()).hasSize(2);
        SubOrderResponse first = response.subOrders().get(0);
        SubOrderResponse second = response.subOrders().get(1);
        assertThat(first.vendorId()).isEqualTo(30L);
        assertThat(first.storeName()).isEqualTo("Store 30");
        assertThat(first.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(first.subtotal()).isEqualByComparingTo("280.00");
        assertThat(first.items()).hasSize(2);
        assertThat(first.items().get(0).productName()).isEqualTo("Product 20");
        assertThat(first.items().get(0).sku()).isEqualTo("SKU-10");
        assertThat(first.items().get(0).unitPrice()).isEqualByComparingTo("100.00");
        assertThat(first.items().get(0).quantity()).isEqualTo(2);
        assertThat(first.items().get(0).lineTotal()).isEqualByComparingTo("200.00");
        assertThat(first.items().get(1).unitPrice()).isEqualByComparingTo("80.00");
        assertThat(second.vendorId()).isEqualTo(31L);
        assertThat(second.subtotal()).isEqualByComparingTo("100.00");

        verify(orderInventory).deduct(any(Order.class));
        verify(orderRepository).save(any(Order.class));
        verify(cartService).clear(USER_ID);
    }

    @Test
    void placeOrder_online_shipsFree_andWaitsForPayment() {
        givenAddressAndCart(line(10, 20, vendorA, 5, null, 2));
        givenOrderCanBeSaved();

        OrderResponse response = checkoutService.placeOrder(USER_ID, request(PaymentMethod.ONLINE));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING_PAYMENT);
        assertThat(response.status()).isEqualTo(SubOrderStatus.PENDING_PAYMENT);
        assertThat(response.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(response.total()).isEqualByComparingTo("200.00");
        assertThat(response.payable()).isTrue();
    }

    @Test
    void placeOrder_snapshotsTheMainImage() {
        givenAddressAndCart(line(10, 20, vendorA, 5, null, 1));
        givenOrderCanBeSaved();
        when(productImageRepository.findMainImages(any())).thenReturn(List.of(mainImage(20L, "https://img/20.jpg")));

        OrderResponse response = checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD));

        assertThat(response.subOrders().getFirst().items().getFirst().imageUrl()).isEqualTo("https://img/20.jpg");
    }

    @Test
    void placeOrder_savesTheOrderForTheCallingUser() {
        givenAddressAndCart(line(10, 20, vendorA, 5, null, 1));
        givenOrderCanBeSaved();

        checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void placeOrder_rejects_anEmptyCart() {
        givenAddressAndCart();

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("cart is empty");
        verifyNoInteractions(orderRepository, orderInventory, cartService);
    }

    @Test
    void placeOrder_throwsNotFound_whenTheAddressIsNotTheUsers() {
        when(addressService.getOwned(USER_ID, ADDRESS_ID)).thenThrow(new ResourceNotFoundException("Address not found"));

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(cartItemRepository, orderRepository, orderInventory, cartService);
    }

    @Test
    void placeOrder_rejects_aLineThatIsOutOfStock() {
        givenAddressAndCart(line(10, 20, vendorA, 0, null, 1));

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("'Product 20' is out of stock");
        verifyNoInteractions(orderRepository, orderInventory, cartService);
    }

    @Test
    void placeOrder_rejects_aLineWithLessStockThanInTheCart() {
        givenAddressAndCart(line(10, 20, vendorA, 1, null, 2));

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Only 1 of 'Product 20' left");
        verifyNoInteractions(orderRepository, orderInventory, cartService);
    }

    @Test
    void placeOrder_rejects_aLineWhoseProductIsNoLongerActive() {
        CartItem archived = new CartItem(cart,
            variant(10, product(20, vendorA, ProductStatus.ARCHIVED), 5, null, true), 1);
        givenAddressAndCart(archived);

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no longer available");
        verifyNoInteractions(orderRepository, orderInventory, cartService);
    }

    @Test
    void placeOrder_keepsTheCart_whenStockRunsOutDuringCheckout() {
        givenAddressAndCart(line(10, 20, vendorA, 5, null, 2));
        when(orderNumberGenerator.next()).thenReturn("SM-TEST");
        doThrow(new BusinessRuleException("'Product 20' just went out of stock"))
            .when(orderInventory).deduct(any(Order.class));

        assertThatThrownBy(() -> checkoutService.placeOrder(USER_ID, request(PaymentMethod.COD)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("just went out of stock");
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clear(USER_ID);
    }
}
