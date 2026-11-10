package com.aditya.supermarket.payment.service;

import static com.aditya.supermarket.order.OrderTestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.order.dto.OrderResponse;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.order.service.OrderInventory;
import com.aditya.supermarket.payment.dto.PayOrderRequest;
import com.aditya.supermarket.payment.entity.Payment;
import com.aditya.supermarket.payment.entity.PaymentAttemptStatus;
import com.aditya.supermarket.payment.gateway.PaymentGateway;
import com.aditya.supermarket.payment.gateway.PaymentOutcome;
import com.aditya.supermarket.payment.gateway.PaymentResult;
import com.aditya.supermarket.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 5L;
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private OrderInventory orderInventory;
    @Mock
    private ApplicationEventPublisher events;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            orderRepository, paymentRepository, paymentGateway, orderInventory, Clock.fixed(NOW, ZoneOffset.UTC), events);
    }

    private void givenLockedOrder(Order order) {
        when(orderRepository.findForUpdateByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
    }

    private void givenGatewayReturns(Order order, PaymentOutcome outcome, boolean successful) {
        when(paymentGateway.charge(order.getOrderNumber(), order.getTotal(), outcome))
            .thenReturn(new PaymentResult(successful, "REF-1"));
        when(paymentGateway.providerName()).thenReturn("MOCK");
    }

    @Test
    void pay_success_marksTheOrderPaid_andConfirmsTheSubOrders() {
        Order order = order(ORDER_ID, PaymentMethod.ONLINE);
        givenLockedOrder(order);
        givenGatewayReturns(order, PaymentOutcome.SUCCESS, true);

        OrderResponse response = paymentService.pay(USER_ID, ORDER_ID, new PayOrderRequest(PaymentOutcome.SUCCESS));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.status()).isEqualTo(SubOrderStatus.PLACED);
        assertThat(response.payable()).isFalse();
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(payment.getValue().getProviderReference()).isEqualTo("REF-1");
        assertThat(payment.getValue().getProvider()).isEqualTo("MOCK");
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("200.00");
        verifyNoInteractions(orderInventory);
    }

    @Test
    void pay_failure_cancelsTheOrder_andReturnsTheStock() {
        Order order = order(ORDER_ID, PaymentMethod.ONLINE);
        givenLockedOrder(order);
        givenGatewayReturns(order, PaymentOutcome.FAILURE, false);

        OrderResponse response = paymentService.pay(USER_ID, ORDER_ID, new PayOrderRequest(PaymentOutcome.FAILURE));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
        assertThat(response.status()).isEqualTo(SubOrderStatus.CANCELLED);
        assertThat(response.subOrders().getFirst().cancelledAt()).isEqualTo(NOW);
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        verify(orderInventory).restore(order);
    }

    @Test
    void pay_rejects_aCashOnDeliveryOrder() {
        givenLockedOrder(order(ORDER_ID, PaymentMethod.COD));

        assertThatThrownBy(() -> paymentService.pay(USER_ID, ORDER_ID, new PayOrderRequest(PaymentOutcome.SUCCESS)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("not awaiting payment");
        verifyNoInteractions(paymentGateway, paymentRepository, orderInventory);
    }

    @Test
    void pay_rejects_anOrderThatIsAlreadyPaid() {
        Order order = order(ORDER_ID, PaymentMethod.ONLINE);
        order.markPaid();
        givenLockedOrder(order);

        assertThatThrownBy(() -> paymentService.pay(USER_ID, ORDER_ID, new PayOrderRequest(PaymentOutcome.SUCCESS)))
            .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(paymentGateway, paymentRepository, orderInventory);
    }

    @Test
    void pay_throwsNotFound_forSomeoneElsesOrMissingOrder() {
        when(orderRepository.findForUpdateByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(USER_ID, ORDER_ID, new PayOrderRequest(PaymentOutcome.SUCCESS)))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(paymentGateway, paymentRepository, orderInventory);
    }
}
