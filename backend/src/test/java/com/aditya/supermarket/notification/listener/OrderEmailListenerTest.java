package com.aditya.supermarket.notification.listener;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.OrderCancelledEvent;
import com.aditya.supermarket.notification.event.OrderPaymentFailedEvent;
import com.aditya.supermarket.notification.event.OrderPlacedEvent;
import com.aditya.supermarket.notification.event.SubOrderStatusChangedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import com.aditya.supermarket.notification.service.OrderEmailData;
import com.aditya.supermarket.notification.service.OrderEmailData.Address;
import com.aditya.supermarket.notification.service.OrderEmailData.Line;
import com.aditya.supermarket.notification.service.OrderEmailData.Person;
import com.aditya.supermarket.notification.service.OrderEmailData.SubOrderData;
import com.aditya.supermarket.notification.service.OrderEmailDataLoader;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEmailListenerTest {

    private static final long ORDER_ID = 100L;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderEmailDataLoader dataLoader;

    private OrderEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEmailListener(notificationService, dataLoader, properties());
    }

    @Test
    void orderPlaced_codOrder_sendsConfirmationAndOneEmailPerVendor() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.COD, PaymentStatus.COD, "40.00", "538.00", twoSubOrders())));

        listener.onOrderPlaced(new OrderPlacedEvent(ORDER_ID));

        List<NotificationRequest> sent = sent(3);
        NotificationRequest confirmation = sent.get(0);
        assertThat(confirmation.type()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
        assertThat(confirmation.to()).isEqualTo("customer@example.com");
        assertThat(confirmation.template()).isEqualTo("order");
        assertThat(confirmation.subject()).isEqualTo("Order confirmed: SM-20261110-ABC123");
        assertThat(confirmation.model().get("paragraphs").toString()).contains("cash on delivery").contains("538.00");
        assertThat((List<?>) confirmation.model().get("items")).hasSize(2);
        assertThat(confirmation.model().get("summary").toString()).contains("Subtotal", "Shipping", "40.00", "Total");
        assertThat(confirmation.model().get("addressLines").toString()).contains("Asha Rao", "Noida");
        assertThat(confirmation.model().get("actionUrl")).isEqualTo("http://localhost:5173/orders/100");

        assertThat(sent.subList(1, 3)).extracting(NotificationRequest::to)
            .containsExactly("vendor1@example.com", "vendor2@example.com");
        assertThat(sent.subList(1, 3)).extracting(NotificationRequest::type)
            .containsOnly(NotificationType.VENDOR_NEW_ORDER);
        NotificationRequest vendorEmail = sent.get(1);
        assertThat(vendorEmail.referenceId()).isEqualTo("11");
        assertThat(vendorEmail.model().get("paragraphs").toString()).contains("cash on delivery", "Store One");
        assertThat(vendorEmail.model().get("addressLines").toString()).contains("Phone: 9876543210");
        assertThat(vendorEmail.model().get("actionUrl")).isEqualTo("http://localhost:5173/vendor/orders/11");
        assertThat((List<?>) vendorEmail.model().get("items")).hasSize(1);
    }

    @Test
    void orderPlaced_onlineOrder_saysPaymentWasReceivedAndFreeShipping() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.ONLINE, PaymentStatus.PAID, "0.00", "498.00", twoSubOrders())));

        listener.onOrderPlaced(new OrderPlacedEvent(ORDER_ID));

        NotificationRequest confirmation = sent(3).get(0);
        assertThat(confirmation.model().get("paragraphs").toString()).contains("received your payment");
        assertThat(confirmation.model().get("summary").toString()).contains("Free");
        assertThat(sent(3).get(1).model().get("paragraphs").toString()).contains("paid online");
    }

    @Test
    void paymentFailed_tellsTheCustomerNothingWasChargedAndLinksToTheCart() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.ONLINE, PaymentStatus.PAYMENT_FAILED, "0.00", "498.00", twoSubOrders())));

        listener.onPaymentFailed(new OrderPaymentFailedEvent(ORDER_ID));

        NotificationRequest request = sent(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.PAYMENT_FAILED);
        assertThat(request.to()).isEqualTo("customer@example.com");
        assertThat(request.model().get("paragraphs").toString()).contains("Nothing was charged", "498.00");
        assertThat(request.model().get("actionUrl")).isEqualTo("http://localhost:5173/cart");
        assertThat(request.model()).doesNotContainKey("items");
    }

    @Test
    void cancelled_codOrder_notifiesCustomerAndEveryVendor() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.COD, PaymentStatus.CANCELLED, "40.00", "538.00", twoSubOrders())));

        listener.onOrderCancelled(new OrderCancelledEvent(ORDER_ID));

        List<NotificationRequest> sent = sent(3);
        assertThat(sent.get(0).type()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(sent.get(0).model().get("paragraphs").toString()).contains("not been charged");
        assertThat(sent.subList(1, 3)).extracting(NotificationRequest::type)
            .containsOnly(NotificationType.VENDOR_ORDER_CANCELLED);
        assertThat(sent.get(1).model().get("paragraphs").toString()).contains("do not ship");
    }

    @Test
    void cancelled_paidOnlineOrder_mentionsRefundAndNotifiesVendors() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.ONLINE, PaymentStatus.REFUND_PENDING, "0.00", "498.00", twoSubOrders())));

        listener.onOrderCancelled(new OrderCancelledEvent(ORDER_ID));

        List<NotificationRequest> sent = sent(3);
        assertThat(sent.get(0).model().get("paragraphs").toString()).contains("refunded");
    }

    @Test
    void cancelled_unpaidOnlineOrder_onlyTheCustomerIsEmailed() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.ONLINE, PaymentStatus.CANCELLED, "0.00", "498.00", twoSubOrders())));

        listener.onOrderCancelled(new OrderCancelledEvent(ORDER_ID));

        List<NotificationRequest> sent = sent(1);
        assertThat(sent.get(0).to()).isEqualTo("customer@example.com");
    }

    @Test
    void shipped_emailsTheCustomerAboutThatVendorsItemsOnly() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.COD, PaymentStatus.COD, "40.00", "538.00", twoSubOrders())));

        listener.onSubOrderStatusChanged(new SubOrderStatusChangedEvent(ORDER_ID, 12L, SubOrderStatus.SHIPPED));

        NotificationRequest request = sent(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.ORDER_SHIPPED);
        assertThat(request.to()).isEqualTo("customer@example.com");
        assertThat(request.model().get("paragraphs").toString()).contains("Store Two", "1 item");
        assertThat(request.model().get("items").toString()).contains("Jeans");
        assertThat(request.model().get("items").toString()).doesNotContain("T-Shirt");
        assertThat(request.model().get("addressLines").toString()).contains("Asha Rao");
    }

    @Test
    void delivered_emailsTheCustomerWithoutRepeatingTheAddress() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.COD, PaymentStatus.COD, "40.00", "538.00", twoSubOrders())));

        listener.onSubOrderStatusChanged(new SubOrderStatusChangedEvent(ORDER_ID, 11L, SubOrderStatus.DELIVERED));

        NotificationRequest request = sent(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.ORDER_DELIVERED);
        assertThat(request.model().get("paragraphs").toString()).contains("Store One", "delivered");
        assertThat(request.model()).doesNotContainKey("addressLines");
    }

    @Test
    void statusChange_forAnUnknownSubOrder_sendsNothing() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.of(
            order(PaymentMethod.COD, PaymentStatus.COD, "40.00", "538.00", twoSubOrders())));

        listener.onSubOrderStatusChanged(new SubOrderStatusChangedEvent(ORDER_ID, 999L, SubOrderStatus.SHIPPED));

        verify(notificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingOrder_sendsNothing() {
        when(dataLoader.load(ORDER_ID)).thenReturn(Optional.empty());

        listener.onOrderPlaced(new OrderPlacedEvent(ORDER_ID));

        verify(notificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void money_usesRupeesWithTwoDecimals() {
        assertThat(OrderEmailListener.money(new BigDecimal("499"))).contains("499.00").contains("₹");
    }

    private List<NotificationRequest> sent(int expectedCount) {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(expectedCount)).send(captor.capture());
        return captor.getAllValues();
    }

    private static OrderEmailData order(PaymentMethod method, PaymentStatus status, String shipping, String total,
                                        List<SubOrderData> subOrders) {
        return new OrderEmailData(ORDER_ID, "SM-20261110-ABC123", method, status,
            new BigDecimal("498.00"), new BigDecimal(shipping), new BigDecimal(total),
            new Person("customer@example.com", "Asha"),
            new Address("Asha Rao", "9876543210", "12 Market Road", null, "Noida", "Uttar Pradesh", "201301"),
            subOrders);
    }

    private static List<SubOrderData> twoSubOrders() {
        return List.of(
            new SubOrderData(11L, SubOrderStatus.PLACED, "Store One", new Person("vendor1@example.com", "Vikram"),
                new BigDecimal("199.00"),
                List.of(new Line("T-Shirt", "M, Black", 1, new BigDecimal("199.00"), new BigDecimal("199.00")))),
            new SubOrderData(12L, SubOrderStatus.PLACED, "Store Two", new Person("vendor2@example.com", "Vani"),
                new BigDecimal("299.00"),
                List.of(new Line("Jeans", null, 1, new BigDecimal("299.00"), new BigDecimal("299.00")))));
    }
}
