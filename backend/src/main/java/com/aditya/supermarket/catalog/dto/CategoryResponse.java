package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.Category;

public record CategoryResponse(Long id, String name, String slug, Long parentId, boolean active) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(),
            c.getParent() == null ? null : c.getParent().getId(), c.isActive());
    }
}
