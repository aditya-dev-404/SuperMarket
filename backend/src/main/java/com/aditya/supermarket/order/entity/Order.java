package com.aditya.supermarket.order.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.vendor.entity.Vendor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * What a customer bought in one checkout, split into one {@link SubOrder} per vendor. All the
 * payment and cancellation rules live here so services only orchestrate.
 *
 * <p>The JPA entity name is {@code CustomerOrder} because ORDER is a reserved word in JPQL/SQL.
 */
@Getter
@Entity(name = "CustomerOrder")
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<SubOrder> subOrders = new ArrayList<>();

    public Order(String orderNumber, Long userId, PaymentMethod paymentMethod, ShippingAddress shippingAddress) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.paymentStatus = paymentMethod == PaymentMethod.COD ? PaymentStatus.COD : PaymentStatus.PENDING_PAYMENT;
    }

    /** Cash-on-delivery orders go straight to the vendor; online orders wait for the payment. */
    public SubOrder addSubOrder(Vendor vendor) {
        SubOrderStatus initial = paymentMethod == PaymentMethod.COD
            ? SubOrderStatus.PLACED
            : SubOrderStatus.PENDING_PAYMENT;
        SubOrder subOrder = new SubOrder(this, vendor, initial);
        subOrders.add(subOrder);
        return subOrder;
    }

    /** Call once all sub-orders and their items have been added. */
    public void applyPricing(BigDecimal shippingFee) {
        this.subtotal = subOrders.stream()
            .map(SubOrder::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        this.shippingFee = shippingFee.setScale(2, RoundingMode.HALF_UP);
        this.total = this.subtotal.add(this.shippingFee);
    }

    /** The least advanced active sub-order decides, so "DELIVERED" really means everything arrived. */
    public SubOrderStatus overallStatus() {
        return subOrders.stream()
            .map(SubOrder::getStatus)
            .filter(status -> status != SubOrderStatus.CANCELLED)
            .min(Comparator.naturalOrder())
            .orElse(SubOrderStatus.CANCELLED);
    }

    public boolean isPayable() {
        return paymentMethod == PaymentMethod.ONLINE && paymentStatus == PaymentStatus.PENDING_PAYMENT;
    }

    public boolean isCancellable() {
        return !subOrders.isEmpty() && subOrders.stream().allMatch(SubOrder::isCancellable);
    }

    public void markPaid() {
        assertPayable();
        paymentStatus = PaymentStatus.PAID;
        subOrders.forEach(SubOrder::confirmPayment);
    }

    /** A failed online payment is final: the order is cancelled (the caller returns the stock). */
    public void markPaymentFailed(Instant now) {
        assertPayable();
        paymentStatus = PaymentStatus.PAYMENT_FAILED;
        subOrders.forEach(subOrder -> subOrder.cancel(now));
    }

    /** Cancels the whole order; allowed only while no vendor has shipped anything. */
    public void cancel(Instant now) {
        if (!isCancellable()) {
            throw new BusinessRuleException("This order can no longer be cancelled");
        }
        paymentStatus = paymentStatus == PaymentStatus.PAID ? PaymentStatus.REFUND_PENDING : PaymentStatus.CANCELLED;
        subOrders.forEach(subOrder -> subOrder.cancel(now));
    }

    /** Cash is collected at the door, so a COD order counts as paid once everything is delivered. */
    void completeCodPaymentIfAllDelivered() {
        boolean allDelivered = subOrders.stream().allMatch(subOrder -> subOrder.getStatus() == SubOrderStatus.DELIVERED);
        if (paymentMethod == PaymentMethod.COD && paymentStatus == PaymentStatus.COD && allDelivered) {
            paymentStatus = PaymentStatus.PAID;
        }
    }

    private void assertPayable() {
        if (!isPayable()) {
            throw new BusinessRuleException("This order is not awaiting payment");
        }
    }
}
