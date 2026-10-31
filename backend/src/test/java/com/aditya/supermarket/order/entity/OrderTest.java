package com.aditya.supermarket.order.entity;

import static com.aditya.supermarket.order.OrderTestData.COD_FEE;
import static com.aditya.supermarket.order.OrderTestData.addSubOrder;
import static com.aditya.supermarket.order.OrderTestData.emptyOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    /** Two vendors: 2 x 100.00 and 1 x 100.00, so the subtotal is 300.00. */
    private static Order orderWithTwoVendors(PaymentMethod method) {
        Order order = emptyOrder(method);
        addSubOrder(order, 30, 10, 2);
        addSubOrder(order, 31, 11, 1);
        order.applyPricing(method == PaymentMethod.COD ? COD_FEE : BigDecimal.ZERO);
        return order;
    }

    private static void deliver(SubOrder subOrder) {
        subOrder.advanceTo(SubOrderStatus.PROCESSING, NOW);
        subOrder.advanceTo(SubOrderStatus.SHIPPED, NOW);
        subOrder.advanceTo(SubOrderStatus.DELIVERED, NOW);
    }

    @Test
    void codOrder_startsPlaced_withCodPaymentStatus() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COD);
        assertThat(order.getSubOrders()).extracting(SubOrder::getStatus).containsOnly(SubOrderStatus.PLACED);
        assertThat(order.isPayable()).isFalse();
        assertThat(order.isCancellable()).isTrue();
    }

    @Test
    void onlineOrder_startsPendingPayment() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING_PAYMENT);
        assertThat(order.getSubOrders()).extracting(SubOrder::getStatus).containsOnly(SubOrderStatus.PENDING_PAYMENT);
        assertThat(order.isPayable()).isTrue();
    }

    @Test
    void applyPricing_addsUpSubOrdersAndShipping() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        assertThat(order.getSubOrders().get(0).getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(order.getSubOrders().get(1).getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(order.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(order.getShippingFee()).isEqualByComparingTo("40.00");
        assertThat(order.getTotal()).isEqualByComparingTo("340.00");
    }

    @Test
    void onlineOrder_shipsFree() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);

        assertThat(order.getShippingFee()).isEqualByComparingTo("0.00");
        assertThat(order.getTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void markPaid_confirmsEverySubOrder() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);

        order.markPaid();

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getSubOrders()).extracting(SubOrder::getStatus).containsOnly(SubOrderStatus.PLACED);
        assertThat(order.isPayable()).isFalse();
    }

    @Test
    void markPaid_rejects_cashOnDeliveryOrder() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        assertThatThrownBy(order::markPaid)
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("not awaiting payment");
    }

    @Test
    void markPaid_rejects_whenAlreadyPaid() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);
        order.markPaid();

        assertThatThrownBy(order::markPaid).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markPaymentFailed_cancelsEverySubOrder() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);

        order.markPaymentFailed(NOW);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
        assertThat(order.getSubOrders()).extracting(SubOrder::getStatus).containsOnly(SubOrderStatus.CANCELLED);
        assertThat(order.getSubOrders()).extracting(SubOrder::getCancelledAt).containsOnly(NOW);
        assertThat(order.overallStatus()).isEqualTo(SubOrderStatus.CANCELLED);
        assertThat(order.isPayable()).isFalse();
    }

    @Test
    void cancel_unpaidOrder_marksPaymentCancelled() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        order.cancel(NOW);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getSubOrders()).extracting(SubOrder::getStatus).containsOnly(SubOrderStatus.CANCELLED);
        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    void cancel_paidOrder_marksRefundPending() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);
        order.markPaid();

        order.cancel(NOW);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void cancel_isAllowedWhileProcessing() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);
        order.getSubOrders().get(0).advanceTo(SubOrderStatus.PROCESSING, NOW);

        order.cancel(NOW);

        assertThat(order.overallStatus()).isEqualTo(SubOrderStatus.CANCELLED);
    }

    @Test
    void cancel_rejects_onceAnySubOrderHasShipped() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);
        SubOrder first = order.getSubOrders().get(0);
        first.advanceTo(SubOrderStatus.PROCESSING, NOW);
        first.advanceTo(SubOrderStatus.SHIPPED, NOW);

        assertThat(order.isCancellable()).isFalse();
        assertThatThrownBy(() -> order.cancel(NOW))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("can no longer be cancelled");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COD);
        assertThat(order.getSubOrders().get(1).getStatus()).isEqualTo(SubOrderStatus.PLACED);
    }

    @Test
    void cancel_rejects_anAlreadyCancelledOrder() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);
        order.cancel(NOW);

        assertThatThrownBy(() -> order.cancel(NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void advanceTo_movesOneStepAtATime_andStampsTheDates() {
        SubOrder subOrder = orderWithTwoVendors(PaymentMethod.COD).getSubOrders().getFirst();

        subOrder.advanceTo(SubOrderStatus.PROCESSING, NOW);
        assertThat(subOrder.getStatus()).isEqualTo(SubOrderStatus.PROCESSING);

        subOrder.advanceTo(SubOrderStatus.SHIPPED, NOW);
        assertThat(subOrder.getShippedAt()).isEqualTo(NOW);

        subOrder.advanceTo(SubOrderStatus.DELIVERED, NOW);
        assertThat(subOrder.getDeliveredAt()).isEqualTo(NOW);
        assertThat(subOrder.nextStatus()).isEmpty();
    }

    @Test
    void advanceTo_rejects_skippingAStep() {
        SubOrder subOrder = orderWithTwoVendors(PaymentMethod.COD).getSubOrders().getFirst();

        assertThatThrownBy(() -> subOrder.advanceTo(SubOrderStatus.SHIPPED, NOW))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("from PLACED to SHIPPED");
        assertThat(subOrder.getStatus()).isEqualTo(SubOrderStatus.PLACED);
    }

    @Test
    void advanceTo_rejects_goingBackwards() {
        SubOrder subOrder = orderWithTwoVendors(PaymentMethod.COD).getSubOrders().getFirst();
        subOrder.advanceTo(SubOrderStatus.PROCESSING, NOW);

        assertThatThrownBy(() -> subOrder.advanceTo(SubOrderStatus.PLACED, NOW))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void advanceTo_rejects_unpaidAndCancelledSubOrders() {
        Order unpaid = orderWithTwoVendors(PaymentMethod.ONLINE);
        Order cancelled = orderWithTwoVendors(PaymentMethod.COD);
        cancelled.cancel(NOW);

        assertThatThrownBy(() -> unpaid.getSubOrders().getFirst().advanceTo(SubOrderStatus.PROCESSING, NOW))
            .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> cancelled.getSubOrders().getFirst().advanceTo(SubOrderStatus.PROCESSING, NOW))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void codOrder_becomesPaid_onlyOnceEverySubOrderIsDelivered() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        deliver(order.getSubOrders().get(0));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COD);

        deliver(order.getSubOrders().get(1));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void onlineOrder_staysPaid_afterDelivery() {
        Order order = orderWithTwoVendors(PaymentMethod.ONLINE);
        order.markPaid();

        order.getSubOrders().forEach(OrderTest::deliver);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.overallStatus()).isEqualTo(SubOrderStatus.DELIVERED);
    }

    @Test
    void overallStatus_isTheLeastAdvancedActiveSubOrder() {
        Order order = orderWithTwoVendors(PaymentMethod.COD);

        deliver(order.getSubOrders().get(0));
        assertThat(order.overallStatus()).isEqualTo(SubOrderStatus.PLACED);

        order.getSubOrders().get(1).advanceTo(SubOrderStatus.PROCESSING, NOW);
        assertThat(order.overallStatus()).isEqualTo(SubOrderStatus.PROCESSING);
    }
}
