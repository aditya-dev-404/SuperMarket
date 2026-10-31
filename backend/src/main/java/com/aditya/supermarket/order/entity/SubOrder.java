package com.aditya.supermarket.order.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.vendor.entity.Vendor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The part of an order that one vendor fulfils. Only that vendor moves its status forward, one step
 * at a time (PLACED, PROCESSING, SHIPPED, DELIVERED).
 */
@Getter
@Entity
@Table(name = "sub_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubOrder extends BaseEntity {

    // Named customerOrder (not order) because ORDER is a reserved word in JPQL paths.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order customerOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SubOrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Batch loading turns "one query per sub-order" into one query per page of orders.
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    /** Created through {@link Order#addSubOrder(Vendor)} so an order always owns its sub-orders. */
    SubOrder(Order customerOrder, Vendor vendor, SubOrderStatus status) {
        this.customerOrder = customerOrder;
        this.vendor = vendor;
        this.status = status;
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
        subtotal = subtotal.add(item.getLineTotal());
    }

    /** The only status the vendor may set next, if any. */
    public Optional<SubOrderStatus> nextStatus() {
        return switch (status) {
            case PLACED -> Optional.of(SubOrderStatus.PROCESSING);
            case PROCESSING -> Optional.of(SubOrderStatus.SHIPPED);
            case SHIPPED -> Optional.of(SubOrderStatus.DELIVERED);
            default -> Optional.empty();
        };
    }

    /** Moves exactly one step forward; skipping a step or going back is a business-rule violation. */
    public void advanceTo(SubOrderStatus target, Instant now) {
        if (nextStatus().filter(next -> next == target).isEmpty()) {
            throw new BusinessRuleException("Cannot change the status from " + status + " to " + target);
        }
        status = target;
        if (target == SubOrderStatus.SHIPPED) {
            shippedAt = now;
        } else if (target == SubOrderStatus.DELIVERED) {
            deliveredAt = now;
            customerOrder.completeCodPaymentIfAllDelivered();
        }
    }

    /** The customer may still cancel until the vendor ships. */
    public boolean isCancellable() {
        return status == SubOrderStatus.PENDING_PAYMENT
            || status == SubOrderStatus.PLACED
            || status == SubOrderStatus.PROCESSING;
    }

    void confirmPayment() {
        if (status == SubOrderStatus.PENDING_PAYMENT) {
            status = SubOrderStatus.PLACED;
        }
    }

    void cancel(Instant now) {
        status = SubOrderStatus.CANCELLED;
        cancelledAt = now;
    }
}
