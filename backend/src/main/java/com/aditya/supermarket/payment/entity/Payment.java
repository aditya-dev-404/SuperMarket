package com.aditya.supermarket.payment.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import com.aditya.supermarket.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A record of one payment attempt for an order (never holds card data; only the provider's reference). */
@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order customerOrder;

    @Column(nullable = false, length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_reference", length = 64)
    private String providerReference;

    private Payment(Order customerOrder, String provider, PaymentAttemptStatus status,
                    BigDecimal amount, String providerReference) {
        this.customerOrder = customerOrder;
        this.provider = provider;
        this.status = status;
        this.amount = amount;
        this.providerReference = providerReference;
    }

    public static Payment succeeded(Order order, String provider, BigDecimal amount, String providerReference) {
        return new Payment(order, provider, PaymentAttemptStatus.SUCCEEDED, amount, providerReference);
    }

    public static Payment failed(Order order, String provider, BigDecimal amount, String providerReference) {
        return new Payment(order, provider, PaymentAttemptStatus.FAILED, amount, providerReference);
    }
}
