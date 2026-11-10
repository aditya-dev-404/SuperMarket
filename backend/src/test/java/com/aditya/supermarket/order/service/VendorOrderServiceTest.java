package com.aditya.supermarket.order.service;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.setId;
import static com.aditya.supermarket.cart.CartTestData.user;
import static com.aditya.supermarket.order.OrderTestData.COD_FEE;
import static com.aditya.supermarket.order.OrderTestData.addSubOrder;
import static com.aditya.supermarket.order.OrderTestData.emptyOrder;
import static com.aditya.supermarket.order.OrderTestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.dto.UpdateSubOrderStatusRequest;
import com.aditya.supermarket.order.dto.VendorSubOrderResponse;
import com.aditya.supermarket.order.dto.VendorSubOrderSummaryResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.order.repository.SubOrderRepository;
import com.aditya.supermarket.vendor.entity.Vendor;
import com.aditya.supermarket.vendor.service.VendorContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class VendorOrderServiceTest {

    private static final long VENDOR_ID = 30L;
    private static final long SUB_ORDER_ID = 70L;
    private static final long OTHER_SUB_ORDER_ID = 71L;
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final List<SubOrderStatus> ALL_VISIBLE = List.of(
        SubOrderStatus.PLACED, SubOrderStatus.PROCESSING, SubOrderStatus.SHIPPED,
        SubOrderStatus.DELIVERED, SubOrderStatus.CANCELLED);

    @Mock
    private VendorContext vendorContext;
    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher events;
    private VendorOrderService service;

    @BeforeEach
    void setUp() {
        Vendor vendor = approvedVendor(VENDOR_ID, user(99));
        when(vendorContext.requireApprovedVendor()).thenReturn(vendor);
        service = new VendorOrderService(
            vendorContext, subOrderRepository, orderRepository, Clock.fixed(NOW, ZoneOffset.UTC), events);
    }

    /** A cash-on-delivery order with one sub-order (id 70) that belongs to the logged-in vendor. */
    private static Order codOrderOfThisVendor() {
        Order order = order(5, PaymentMethod.COD);
        setId(order.getSubOrders().getFirst(), SUB_ORDER_ID);
        return order;
    }

    private void givenLockedOrder(Order order) {
        when(orderRepository.findForUpdateBySubOrderIdAndVendorId(SUB_ORDER_ID, VENDOR_ID))
            .thenReturn(Optional.of(order));
    }

    private static UpdateSubOrderStatusRequest request(SubOrderStatus status) {
        return new UpdateSubOrderStatusRequest(status);
    }

    @Test
    void updateStatus_movesTheSubOrderOneStepForward() {
        givenLockedOrder(codOrderOfThisVendor());

        VendorSubOrderResponse response = service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.PROCESSING));

        assertThat(response.status()).isEqualTo(SubOrderStatus.PROCESSING);
        assertThat(response.nextStatus()).isEqualTo(SubOrderStatus.SHIPPED);
    }

    @Test
    void updateStatus_stampsTheDates_andCompletesCodPaymentOnDelivery() {
        givenLockedOrder(codOrderOfThisVendor());

        service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.PROCESSING));
        VendorSubOrderResponse shipped = service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.SHIPPED));
        VendorSubOrderResponse delivered = service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.DELIVERED));

        assertThat(shipped.shippedAt()).isEqualTo(NOW);
        assertThat(delivered.deliveredAt()).isEqualTo(NOW);
        assertThat(delivered.nextStatus()).isNull();
        assertThat(delivered.paymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void updateStatus_leavesTheOtherVendorsSubOrderAlone() {
        Order order = emptyOrder(PaymentMethod.COD);
        SubOrder mine = addSubOrder(order, VENDOR_ID, 10, 1);
        SubOrder other = addSubOrder(order, 31, 11, 1);
        order.applyPricing(COD_FEE);
        setId(mine, SUB_ORDER_ID);
        setId(other, OTHER_SUB_ORDER_ID);
        givenLockedOrder(order);

        service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.PROCESSING));

        assertThat(mine.getStatus()).isEqualTo(SubOrderStatus.PROCESSING);
        assertThat(other.getStatus()).isEqualTo(SubOrderStatus.PLACED);
    }

    @Test
    void updateStatus_rejects_skippingAStep() {
        givenLockedOrder(codOrderOfThisVendor());

        assertThatThrownBy(() -> service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.SHIPPED)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("from PLACED to SHIPPED");
    }

    @Test
    void updateStatus_rejects_aVendorCancellation() {
        givenLockedOrder(codOrderOfThisVendor());

        assertThatThrownBy(() -> service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.CANCELLED)))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateStatus_rejects_anOnlineOrderThatIsNotPaidYet() {
        Order unpaid = order(5, PaymentMethod.ONLINE);
        setId(unpaid.getSubOrders().getFirst(), SUB_ORDER_ID);
        givenLockedOrder(unpaid);

        assertThatThrownBy(() -> service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.PROCESSING)))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(unpaid.getSubOrders().getFirst().getStatus()).isEqualTo(SubOrderStatus.PENDING_PAYMENT);
    }

    @Test
    void updateStatus_throwsNotFound_forAnotherVendorsOrMissingSubOrder() {
        when(orderRepository.findForUpdateBySubOrderIdAndVendorId(SUB_ORDER_ID, VENDOR_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(SUB_ORDER_ID, request(SubOrderStatus.PROCESSING)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_returnsTheSubOrderWithItsNextStep() {
        Order order = codOrderOfThisVendor();
        when(subOrderRepository.findVisibleByIdAndVendor(SUB_ORDER_ID, VENDOR_ID))
            .thenReturn(Optional.of(order.getSubOrders().getFirst()));

        VendorSubOrderResponse response = service.get(SUB_ORDER_ID);

        assertThat(response.id()).isEqualTo(SUB_ORDER_ID);
        assertThat(response.orderNumber()).isEqualTo("SM-TEST");
        assertThat(response.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(response.nextStatus()).isEqualTo(SubOrderStatus.PROCESSING);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(response.subtotal()).isEqualByComparingTo("200.00");
        assertThat(response.shippingAddress().fullName()).isEqualTo("Aditya Kumar");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void get_throwsNotFound_forAnotherVendorsUnpaidOrMissingSubOrder() {
        when(subOrderRepository.findVisibleByIdAndVendor(SUB_ORDER_ID, VENDOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(SUB_ORDER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_showsEveryVisibleStatus_byDefault() {
        Order order = codOrderOfThisVendor();
        when(subOrderRepository.findVisibleByVendor(eq(VENDOR_ID), eq(ALL_VISIBLE), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(order.getSubOrders().getFirst())));

        PagedResponse<VendorSubOrderSummaryResponse> page = service.list(null, 0, 10);

        assertThat(page.totalElements()).isEqualTo(1);
        VendorSubOrderSummaryResponse row = page.content().getFirst();
        assertThat(row.id()).isEqualTo(SUB_ORDER_ID);
        assertThat(row.orderNumber()).isEqualTo("SM-TEST");
        assertThat(row.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(row.paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(row.subtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(row.itemCount()).isEqualTo(2);
        assertThat(row.recipientName()).isEqualTo("Aditya Kumar");
        assertThat(row.city()).isEqualTo("Greater Noida");
    }

    @Test
    void list_filtersByTheRequestedStatus() {
        when(subOrderRepository.findVisibleByVendor(
            eq(VENDOR_ID), eq(List.of(SubOrderStatus.SHIPPED)), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<VendorSubOrderSummaryResponse> page = service.list(SubOrderStatus.SHIPPED, 0, 10);

        assertThat(page.content()).isEmpty();
    }

    @Test
    void list_neverShowsUnpaidOrders_evenWhenAskedFor() {
        PagedResponse<VendorSubOrderSummaryResponse> page = service.list(SubOrderStatus.PENDING_PAYMENT, 0, 10);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verifyNoInteractions(subOrderRepository);
    }

    @Test
    void list_clampsPageAndSize() {
        when(subOrderRepository.findVisibleByVendor(eq(VENDOR_ID), eq(ALL_VISIBLE), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        service.list(null, -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(subOrderRepository).findVisibleByVendor(eq(VENDOR_ID), eq(ALL_VISIBLE), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }
}
