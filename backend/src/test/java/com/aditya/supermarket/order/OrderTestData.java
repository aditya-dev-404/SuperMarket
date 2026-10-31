package com.aditya.supermarket.order;

import static com.aditya.supermarket.cart.CartTestData.approvedVendor;
import static com.aditya.supermarket.cart.CartTestData.setId;
import static com.aditya.supermarket.cart.CartTestData.user;

import com.aditya.supermarket.address.entity.Address;
import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.ShippingAddress;
import com.aditya.supermarket.order.entity.SubOrder;
import java.math.BigDecimal;

/** Builders for order graphs used by order, payment and vendor-order tests. */
public final class OrderTestData {

    public static final long CUSTOMER_ID = 1L;
    public static final BigDecimal COD_FEE = new BigDecimal("40");
    public static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");

    private OrderTestData() {
    }

    public static Address address() {
        return new Address(CUSTOMER_ID, "Aditya Kumar", "9876543210", "Flat 12, Main Road", null,
            "Greater Noida", "Uttar Pradesh", "201310");
    }

    /** An order without sub-orders yet; add them with {@link #addSubOrder} and then call applyPricing. */
    public static Order emptyOrder(PaymentMethod method) {
        return new Order("SM-TEST", CUSTOMER_ID, method, ShippingAddress.from(address()));
    }

    /** Adds one vendor's sub-order with a single item of {@code quantity} units at {@link #UNIT_PRICE}. */
    public static SubOrder addSubOrder(Order order, long vendorId, long variantId, int quantity) {
        SubOrder subOrder = order.addSubOrder(approvedVendor(vendorId, user(1_000 + vendorId)));
        subOrder.addItem(new OrderItem(variantId, variantId + 100, "Product " + variantId,
            "product-" + variantId, "SKU-" + variantId, null, null, null, UNIT_PRICE, quantity));
        return subOrder;
    }

    /** One vendor (id 30), one item of 2 units: subtotal 200, plus 40 shipping for COD. */
    public static Order order(long id, PaymentMethod method) {
        Order order = emptyOrder(method);
        addSubOrder(order, 30, 10, 2);
        order.applyPricing(method == PaymentMethod.COD ? COD_FEE : BigDecimal.ZERO);
        setId(order, id);
        return order;
    }
}
