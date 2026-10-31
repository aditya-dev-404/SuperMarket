package com.aditya.supermarket.order.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One purchased line. Name, price, size and so on are copied from the catalog at checkout; the
 * variant and product ids are only references (they become null if the vendor deletes them later).
 */
@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id")
    private SubOrder subOrder;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_slug", nullable = false, length = 260)
    private String productSlug;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 30)
    private String size;

    @Column(length = 30)
    private String colour;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public OrderItem(Long variantId, Long productId, String productName, String productSlug, String sku,
                     String size, String colour, String imageUrl, BigDecimal unitPrice, int quantity) {
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.sku = sku;
        this.size = size;
        this.colour = colour;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Called by {@link SubOrder#addItem(OrderItem)}, which owns the relationship. */
    void attachTo(SubOrder subOrder) {
        this.subOrder = subOrder;
    }
}
