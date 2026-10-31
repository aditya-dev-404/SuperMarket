package com.aditya.supermarket.order.service;

import static com.aditya.supermarket.order.OrderTestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.dto.PagedResponse;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.dto.OrderSummaryResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderInventory orderInventory;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderInventory, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenLockedOrder(Order order) {
        when(orderRepository.findForUpdateByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
    }

    @Test
    void cancel_cancelsTheOrder_andReturnsItsStock() {
        Order order = order(ORDER_ID, PaymentMethod.COD);
        givenLockedOrder(order);

        OrderResponse response = orderService.cancel(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(SubOrderStatus.CANCELLED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.cancellable()).isFalse();
        assertThat(response.subOrders().getFirst().cancelledAt()).isEqualTo(NOW);
        verify(orderInventory).restore(order);
    }

    @Test
    void cancel_marksARefundPending_forAPaidOrder() {
        Order order = order(ORDER_ID, PaymentMethod.ONLINE);
        order.markPaid();
        givenLockedOrder(order);

        OrderResponse response = orderService.cancel(USER_ID, ORDER_ID);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        verify(orderInventory).restore(order);
    }

    @Test
    void cancel_rejects_andKeepsTheStock_onceTheOrderHasShipped() {
        Order order = order(ORDER_ID, PaymentMethod.COD);
        order.getSubOrders().getFirst().advanceTo(SubOrderStatus.PROCESSING, NOW);
        order.getSubOrders().getFirst().advanceTo(SubOrderStatus.SHIPPED, NOW);
        givenLockedOrder(order);

        assertThatThrownBy(() -> orderService.cancel(USER_ID, ORDER_ID))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("can no longer be cancelled");
        verifyNoInteractions(orderInventory);
    }

    @Test
    void cancel_throwsNotFound_forSomeoneElsesOrMissingOrder() {
        when(orderRepository.findForUpdateByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(USER_ID, ORDER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(orderInventory);
    }

    @Test
    void get_returnsTheOrder() {
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID))
            .thenReturn(Optional.of(order(ORDER_ID, PaymentMethod.COD)));

        OrderResponse response = orderService.get(USER_ID, ORDER_ID);

        assertThat(response.id()).isEqualTo(ORDER_ID);
        assertThat(response.total()).isEqualByComparingTo("240.00");
    }

    @Test
    void get_throwsNotFound_forSomeoneElsesOrMissingOrder() {
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(USER_ID, ORDER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsSummariesOfThePage() {
        Order order = order(ORDER_ID, PaymentMethod.COD);
        when(orderRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findAllDetailedByIdIn(List.of(ORDER_ID))).thenReturn(List.of(order));

        PagedResponse<OrderSummaryResponse> page = orderService.list(USER_ID, 0, 10);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
        OrderSummaryResponse summary = page.content().getFirst();
        assertThat(summary.orderNumber()).isEqualTo("SM-TEST");
        assertThat(summary.itemCount()).isEqualTo(2);
        assertThat(summary.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(summary.total()).isEqualByComparingTo("240.00");
    }

    @Test
    void list_clampsPageAndSize_andSkipsTheDetailQueryForAnEmptyPage() {
        when(orderRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

        PagedResponse<OrderSummaryResponse> page = orderService.list(USER_ID, -3, 1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(USER_ID), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(page.content()).isEmpty();
        verify(orderRepository, never()).findAllDetailedByIdIn(any());
    }
}
