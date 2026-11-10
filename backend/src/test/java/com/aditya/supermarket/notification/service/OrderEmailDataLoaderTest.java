package com.aditya.supermarket.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.ShippingAddress;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import com.aditya.supermarket.order.repository.OrderRepository;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import com.aditya.supermarket.vendor.entity.Vendor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEmailDataLoaderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    private OrderEmailDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new OrderEmailDataLoader(orderRepository, userService);
    }

    @Test
    void load_copiesCustomerVendorAddressAndItemsIntoPlainData() {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(100L);
        when(order.getUserId()).thenReturn(5L);
        when(order.getOrderNumber()).thenReturn("SM-1");
        when(order.getPaymentMethod()).thenReturn(PaymentMethod.COD);
        when(order.getPaymentStatus()).thenReturn(PaymentStatus.COD);
        when(order.getSubtotal()).thenReturn(new BigDecimal("199.00"));
        when(order.getShippingFee()).thenReturn(new BigDecimal("40.00"));
        when(order.getTotal()).thenReturn(new BigDecimal("239.00"));
        ShippingAddress address = mock(ShippingAddress.class);
        when(address.getFullName()).thenReturn("Asha Rao");
        when(address.getPhone()).thenReturn("9876543210");
        when(address.getLine1()).thenReturn("12 Market Road");
        when(address.getCity()).thenReturn("Noida");
        when(address.getState()).thenReturn("Uttar Pradesh");
        when(address.getPincode()).thenReturn("201301");
        when(order.getShippingAddress()).thenReturn(address);

        User vendorUser = mock(User.class);
        when(vendorUser.getEmail()).thenReturn("vendor@example.com");
        when(vendorUser.getFirstName()).thenReturn("Vikram");
        Vendor vendor = mock(Vendor.class);
        when(vendor.getUser()).thenReturn(vendorUser);
        when(vendor.getStoreName()).thenReturn("Store One");
        OrderItem item = mock(OrderItem.class);
        when(item.getProductName()).thenReturn("T-Shirt");
        when(item.getSize()).thenReturn("M");
        when(item.getColour()).thenReturn("Black");
        when(item.getQuantity()).thenReturn(1);
        when(item.getUnitPrice()).thenReturn(new BigDecimal("199.00"));
        when(item.getLineTotal()).thenReturn(new BigDecimal("199.00"));
        SubOrder subOrder = mock(SubOrder.class);
        when(subOrder.getId()).thenReturn(11L);
        when(subOrder.getStatus()).thenReturn(SubOrderStatus.PLACED);
        when(subOrder.getVendor()).thenReturn(vendor);
        when(subOrder.getSubtotal()).thenReturn(new BigDecimal("199.00"));
        when(subOrder.getItems()).thenReturn(List.of(item));
        when(order.getSubOrders()).thenReturn(List.of(subOrder));

        User customer = mock(User.class);
        when(customer.getEmail()).thenReturn("customer@example.com");
        when(customer.getFirstName()).thenReturn("Asha");
        when(orderRepository.findAllDetailedByIdIn(List.of(100L))).thenReturn(List.of(order));
        when(userService.findById(5L)).thenReturn(Optional.of(customer));

        OrderEmailData data = loader.load(100L).orElseThrow();

        assertThat(data.orderNumber()).isEqualTo("SM-1");
        assertThat(data.customer()).isEqualTo(new OrderEmailData.Person("customer@example.com", "Asha"));
        assertThat(data.shipTo().lines()).containsExactly(
            "Asha Rao", "12 Market Road", "Noida, Uttar Pradesh 201301", "Phone: 9876543210");
        assertThat(data.subOrders()).hasSize(1);
        OrderEmailData.SubOrderData sub = data.subOrders().getFirst();
        assertThat(sub.storeName()).isEqualTo("Store One");
        assertThat(sub.vendor().email()).isEqualTo("vendor@example.com");
        assertThat(sub.items().getFirst().variant()).isEqualTo("M, Black");
        assertThat(data.subOrder(11L)).isPresent();
        assertThat(data.subOrder(99L)).isEmpty();
    }

    @Test
    void load_whenTheOrderDoesNotExist_isEmpty() {
        when(orderRepository.findAllDetailedByIdIn(List.of(100L))).thenReturn(List.of());

        assertThat(loader.load(100L)).isEmpty();
    }

    @Test
    void load_whenTheCustomerIsGone_isEmpty() {
        Order order = mock(Order.class);
        when(order.getUserId()).thenReturn(5L);
        when(orderRepository.findAllDetailedByIdIn(List.of(100L))).thenReturn(List.of(order));
        when(userService.findById(5L)).thenReturn(Optional.empty());

        assertThat(loader.load(100L)).isEmpty();
    }
}
