package com.aditya.supermarket.notification.listener;

import com.aditya.supermarket.notification.config.NotificationConfig;
import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.OrderCancelledEvent;
import com.aditya.supermarket.notification.event.OrderPaymentFailedEvent;
import com.aditya.supermarket.notification.event.OrderPlacedEvent;
import com.aditya.supermarket.notification.event.SubOrderStatusChangedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import com.aditya.supermarket.notification.service.OrderEmailData;
import com.aditya.supermarket.notification.service.OrderEmailData.Line;
import com.aditya.supermarket.notification.service.OrderEmailData.SubOrderData;
import com.aditya.supermarket.notification.service.OrderEmailDataLoader;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails for the order lifecycle: confirmation to the customer, one "new order" email per vendor
 * sub-order, payment failure, cancellation (customer and affected vendors) and shipped / delivered
 * updates. Runs after commit and off the request thread; the data is read fresh from the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private static final String TEMPLATE = "order";

    private final NotificationService notificationService;
    private final OrderEmailDataLoader dataLoader;
    private final NotificationProperties properties;

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        dataLoader.load(event.orderId()).ifPresentOrElse(order -> {
            sendConfirmation(order);
            order.subOrders().forEach(subOrder -> sendNewOrderToVendor(order, subOrder));
        }, () -> logMissing(event.orderId()));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentFailed(OrderPaymentFailedEvent event) {
        dataLoader.load(event.orderId()).ifPresentOrElse(order -> send(
                NotificationType.PAYMENT_FAILED, order.customer().email(), order.orderId(),
                "Payment failed for order " + order.orderNumber(),
                model("Payment failed", order.customer().firstName(),
                    List.of("We could not take the payment of " + money(order.total())
                            + " for order " + order.orderNumber() + ", so the order was cancelled.",
                        "Nothing was charged. The items have been returned to your cart so you can try again."),
                    null, null, null, null, "Go to my cart", properties.link("/cart"), null)),
            () -> logMissing(event.orderId()));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCancelled(OrderCancelledEvent event) {
        dataLoader.load(event.orderId()).ifPresentOrElse(order -> {
            sendCancellationToCustomer(order);
            // Vendors only know about orders that were visible to them: every COD order, and online orders that had been paid.
            if (order.paymentMethod() == PaymentMethod.COD || order.paymentStatus() == PaymentStatus.REFUND_PENDING) {
                order.subOrders().forEach(subOrder -> sendCancellationToVendor(order, subOrder));
            }
        }, () -> logMissing(event.orderId()));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSubOrderStatusChanged(SubOrderStatusChangedEvent event) {
        dataLoader.load(event.orderId()).ifPresentOrElse(order -> order.subOrder(event.subOrderId())
                .ifPresentOrElse(subOrder -> sendProgress(order, subOrder, event),
                    () -> logMissing(event.orderId())),
            () -> logMissing(event.orderId()));
    }

    // ---------- customer ----------

    private void sendConfirmation(OrderEmailData order) {
        String payment = order.paymentMethod() == PaymentMethod.COD
            ? "You chose cash on delivery, so please keep " + money(order.total()) + " ready when it arrives."
            : "We received your payment of " + money(order.total()) + ".";
        send(NotificationType.ORDER_CONFIRMATION, order.customer().email(), order.orderId(),
            "Order confirmed: " + order.orderNumber(),
            model("Thank you for your order", order.customer().firstName(),
                List.of("Your order " + order.orderNumber() + " is confirmed. " + payment,
                    "Each seller prepares and ships their own items, so they may arrive separately."),
                items(order.allItems()), totals(order), "Delivering to", order.shipTo().lines(),
                "View my order", properties.link("/orders/" + order.orderId()), null));
    }

    private void sendCancellationToCustomer(OrderEmailData order) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("Your order " + order.orderNumber() + " has been cancelled as requested.");
        if (order.paymentStatus() == PaymentStatus.REFUND_PENDING) {
            paragraphs.add("Your payment of " + money(order.total()) + " will be refunded to your original payment method.");
        } else {
            paragraphs.add("You have not been charged for this order.");
        }
        send(NotificationType.ORDER_CANCELLED, order.customer().email(), order.orderId(),
            "Order cancelled: " + order.orderNumber(),
            model("Your order was cancelled", order.customer().firstName(), paragraphs,
                items(order.allItems()), null, null, null,
                "View my orders", properties.link("/orders"), null));
    }

    private void sendProgress(OrderEmailData order, SubOrderData subOrder, SubOrderStatusChangedEvent event) {
        boolean shipped = event.status() == com.aditya.supermarket.order.entity.SubOrderStatus.SHIPPED;
        String things = subOrder.itemCount() == 1 ? "1 item" : subOrder.itemCount() + " items";
        List<String> paragraphs = shipped
            ? List.of(subOrder.storeName() + " has shipped " + things + " from your order "
                + order.orderNumber() + ".",
            "It is on its way to you.")
            : List.of(subOrder.storeName() + " has marked " + things + " from your order "
                + order.orderNumber() + " as delivered.",
            "We hope you enjoy your purchase.");
        send(shipped ? NotificationType.ORDER_SHIPPED : NotificationType.ORDER_DELIVERED,
            order.customer().email(), order.orderId(),
            (shipped ? "Shipped: " : "Delivered: ") + "your order " + order.orderNumber(),
            model(shipped ? "Your order is on its way" : "Your order was delivered",
                order.customer().firstName(), paragraphs, items(subOrder.items()), null,
                shipped ? "Delivering to" : null, shipped ? order.shipTo().lines() : null,
                "View my order", properties.link("/orders/" + order.orderId()), null));
    }

    // ---------- vendor ----------

    private void sendNewOrderToVendor(OrderEmailData order, SubOrderData subOrder) {
        String payment = order.paymentMethod() == PaymentMethod.COD
            ? "Payment: cash on delivery (the customer pays the courier)."
            : "Payment: already paid online.";
        send(NotificationType.VENDOR_NEW_ORDER, subOrder.vendor().email(), subOrder.id(),
            "New order " + order.orderNumber() + " for " + subOrder.storeName(),
            model("You have a new order", subOrder.vendor().firstName(),
                List.of("Order " + order.orderNumber() + " contains " + subOrder.itemCount()
                        + (subOrder.itemCount() == 1 ? " item" : " items") + " from your store \""
                        + subOrder.storeName() + "\".",
                    payment),
                items(subOrder.items()), subtotalOnly(subOrder), "Ship to", order.shipTo().lines(),
                "Open order", properties.link("/vendor/orders/" + subOrder.id()), null));
    }

    private void sendCancellationToVendor(OrderEmailData order, SubOrderData subOrder) {
        send(NotificationType.VENDOR_ORDER_CANCELLED, subOrder.vendor().email(), subOrder.id(),
            "Order cancelled: " + order.orderNumber(),
            model("An order was cancelled", subOrder.vendor().firstName(),
                List.of("The customer cancelled order " + order.orderNumber() + ".",
                    "Please do not ship the items below. Their stock has been returned to your listing."),
                items(subOrder.items()), null, null, null,
                "Open orders", properties.link("/vendor/orders"), null));
    }

    // ---------- model building ----------

    private void send(NotificationType type, String to, Long referenceId, String subject, Map<String, Object> model) {
        notificationService.send(new NotificationRequest(type, to, subject, TEMPLATE, model, String.valueOf(referenceId)));
    }

    private static Map<String, Object> model(String heading, String firstName, List<String> paragraphs,
                                             List<Map<String, String>> items, List<Map<String, String>> summary,
                                             String addressTitle, List<String> addressLines,
                                             String actionLabel, String actionUrl, String footnote) {
        Map<String, Object> model = new HashMap<>();
        model.put("heading", heading);
        model.put("greeting", firstName == null || firstName.isBlank() ? "Hi there," : "Hi " + firstName + ",");
        model.put("paragraphs", paragraphs);
        putIfPresent(model, "items", items);
        putIfPresent(model, "summary", summary);
        if (addressLines != null && !addressLines.isEmpty()) {
            model.put("addressTitle", addressTitle);
            model.put("addressLines", addressLines);
        }
        model.put("actionLabel", actionLabel);
        model.put("actionUrl", actionUrl);
        if (footnote != null) {
            model.put("footnote", footnote);
        }
        return model;
    }

    private static void putIfPresent(Map<String, Object> model, String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            model.put(key, value);
        }
    }

    private static List<Map<String, String>> items(List<Line> lines) {
        return lines.stream().map(line -> {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("name", line.name());
            if (line.variant() != null) {
                row.put("variant", line.variant());
            }
            row.put("quantity", String.valueOf(line.quantity()));
            row.put("unitPrice", money(line.unitPrice()));
            row.put("lineTotal", money(line.lineTotal()));
            return row;
        }).toList();
    }

    private static List<Map<String, String>> totals(OrderEmailData order) {
        return List.of(
            row("Subtotal", money(order.subtotal())),
            row("Shipping", order.shippingFee().signum() == 0 ? "Free" : money(order.shippingFee())),
            row("Total", money(order.total())));
    }

    private static List<Map<String, String>> subtotalOnly(SubOrderData subOrder) {
        return List.of(row("Your items total", money(subOrder.subtotal())));
    }

    private static Map<String, String> row(String label, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        return row;
    }

    /** Rupees in Indian digit grouping, e.g. "₹1,299.00". */
    static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.of("en", "IN")).format(amount);
    }

    private void logMissing(Long orderId) {
        log.warn("Order {} was not found when preparing its email; nothing sent", orderId);
    }
}
