package com.aditya.supermarket.order.service;

import static com.aditya.supermarket.order.OrderTestData.addSubOrder;
import static com.aditya.supermarket.order.OrderTestData.emptyOrder;
import static com.aditya.supermarket.order.OrderTestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.cart.dto.AddCartItemRequest;
import com.aditya.supermarket.cart.service.CartService;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class OrderCartRestorerTest {

    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 5L;

    @Mock
    private CartService cartService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private OrderCartRestorer restorer;

    @BeforeEach
    void setUp() {
        restorer = new OrderCartRestorer(cartService, orderRepository, transactionManager);
    }

    @Test
    void restore_addsEveryLineBackToTheCart() {
        Order order = emptyOrder(PaymentMethod.ONLINE);
        addSubOrder(order, 30, 10, 2);
        addSubOrder(order, 31, 11, 1);
        order.applyPricing(BigDecimal.ZERO);
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        int restored = restorer.restore(USER_ID, ORDER_ID);

        assertThat(restored).isEqualTo(2);
        verify(cartService).addItem(USER_ID, new AddCartItemRequest(10L, 2));
        verify(cartService).addItem(USER_ID, new AddCartItemRequest(11L, 1));
    }

    @Test
    void restore_skipsALineThatCanNoLongerBeBought_andKeepsTheRest() {
        Order order = emptyOrder(PaymentMethod.ONLINE);
        addSubOrder(order, 30, 10, 2);
        addSubOrder(order, 31, 11, 1);
        order.applyPricing(BigDecimal.ZERO);
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(cartService.addItem(USER_ID, new AddCartItemRequest(10L, 2)))
            .thenThrow(new BusinessRuleException("Out of stock"));

        int restored = restorer.restore(USER_ID, ORDER_ID);

        assertThat(restored).isEqualTo(1);
        verify(cartService).addItem(USER_ID, new AddCartItemRequest(11L, 1));
    }

    @Test
    void restore_doesNothing_whenTheOrderIsNotFound() {
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(restorer.restore(USER_ID, ORDER_ID)).isZero();
        verifyNoInteractions(cartService);
    }

    @Test
    void restore_skipsLinesWhoseVariantWasDeleted() {
        Order order = order(ORDER_ID, PaymentMethod.ONLINE);
        ReflectionTestUtils.setField(order.getSubOrders().getFirst().getItems().getFirst(), "variantId", null);
        when(orderRepository.findDetailedByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThat(restorer.restore(USER_ID, ORDER_ID)).isZero();
        verify(cartService, never()).addItem(any(), any());
    }
}
