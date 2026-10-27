package com.aditya.supermarket.wishlist.entity;

import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A product a user saved for later. Product-level: the size/colour is chosen when moving to cart. */
@Getter
@Entity
@Table(name = "wishlist_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistItem extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    public WishlistItem(Long userId, Product product) {
        this.userId = userId;
        this.product = product;
    }
}
