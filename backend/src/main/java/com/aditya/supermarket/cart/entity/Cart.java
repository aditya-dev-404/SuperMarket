package com.aditya.supermarket.cart.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user's shopping cart (one per user). Lines live in {@link CartItem}; they are loaded through
 * {@code CartItemRepository} so the cart never needs a bidirectional collection.
 */
@Getter
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    public Cart(Long userId) {
        this.userId = userId;
    }
}
