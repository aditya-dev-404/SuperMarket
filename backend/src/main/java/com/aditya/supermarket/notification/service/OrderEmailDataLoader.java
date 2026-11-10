package com.aditya.supermarket.notification.service;

import com.aditya.supermarket.notification.service.OrderEmailData.Address;
import com.aditya.supermarket.notification.service.OrderEmailData.Line;
import com.aditya.supermarket.notification.service.OrderEmailData.Person;
import com.aditya.supermarket.notification.service.OrderEmailData.SubOrderData;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.ShippingAddress;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads an order (with customer and vendor contact details) for the email listener. The events only
 * carry ids, so the email always reflects the committed state of the order.
 */
@Service
@RequiredArgsConstructor
public class OrderEmailDataLoader {

    private final OrderRepository orderRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Optional<OrderEmailData> load(Long orderId) {
        return orderRepository.findAllDetailedByIdIn(List.of(orderId)).stream()
            .findFirst()
            .flatMap(order -> userService.findById(order.getUserId())
                .map(customer -> toData(order, customer)));
    }

    private static OrderEmailData toData(Order order, User customer) {
        return new OrderEmailData(
            order.getId(),
            order.getOrderNumber(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getSubtotal(),
            order.getShippingFee(),
            order.getTotal(),
            new Person(customer.getEmail(), customer.getFirstName()),
            address(order.getShippingAddress()),
            order.getSubOrders().stream().map(OrderEmailDataLoader::subOrder).toList());
    }

    private static SubOrderData subOrder(SubOrder subOrder) {
        User vendorUser = subOrder.getVendor().getUser();
        return new SubOrderData(
            subOrder.getId(),
            subOrder.getStatus(),
            subOrder.getVendor().getStoreName(),
            new Person(vendorUser.getEmail(), vendorUser.getFirstName()),
            subOrder.getSubtotal(),
            subOrder.getItems().stream().map(OrderEmailDataLoader::line).toList());
    }

    private static Line line(OrderItem item) {
        String variant = java.util.stream.Stream.of(item.getSize(), item.getColour())
            .filter(part -> part != null && !part.isBlank())
            .collect(java.util.stream.Collectors.joining(", "));
        return new Line(item.getProductName(), variant.isEmpty() ? null : variant,
            item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
    }

    private static Address address(ShippingAddress address) {
        return new Address(address.getFullName(), address.getPhone(), address.getLine1(), address.getLine2(),
            address.getCity(), address.getState(), address.getPincode());
    }
}
