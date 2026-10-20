package com.aditya.supermarket.catalog.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Column(nullable = false)
    private int position;
}
