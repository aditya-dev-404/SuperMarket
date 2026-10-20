package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.ProductImage;

public record ImageResponse(Long id, String url, String publicId, int position) {

    public static ImageResponse from(ProductImage i) {
        return new ImageResponse(i.getId(), i.getUrl(), i.getPublicId(), i.getPosition());
    }
}
