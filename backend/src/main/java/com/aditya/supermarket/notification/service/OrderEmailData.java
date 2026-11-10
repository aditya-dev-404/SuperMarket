package com.aditya.supermarket.notification.service;

import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Everything an order email needs, copied out of the entities inside one short transaction so the
 * email code never touches lazy JPA relations.
 */
public record OrderEmailData(
    Long orderId,
    String orderNumber,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal total,
    Person customer,
    Address shipTo,
    List<SubOrderData> subOrders) {

    public record Person(String email, String firstName) {
    }

    public record Address(String fullName, String phone, String line1, String line2,
                          String city, String state, String pincode) {

        /** The address as display lines, without blank ones. */
        public List<String> lines() {
            return java.util.stream.Stream.of(
                    fullName,
                    line1,
                    line2,
                    city + ", " + state + " " + pincode,
                    "Phone: " + phone)
                .filter(line -> line != null && !line.isBlank())
                .toList();
        }
    }

    public record Line(String name, String variant, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record SubOrderData(Long id, SubOrderStatus status, String storeName, Person vendor,
                               BigDecimal subtotal, List<Line> items) {

        public int itemCount() {
            return items.stream().mapToInt(Line::quantity).sum();
        }
    }

    public Optional<SubOrderData> subOrder(Long subOrderId) {
        return subOrders.stream().filter(candidate -> candidate.id().equals(subOrderId)).findFirst();
    }

    public List<Line> allItems() {
        return subOrders.stream().flatMap(subOrder -> subOrder.items().stream()).toList();
    }
}
