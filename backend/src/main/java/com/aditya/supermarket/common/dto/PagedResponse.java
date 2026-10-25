package com.aditya.supermarket.common.dto;

import java.util.List;

/**
 * Stable JSON shape for paginated lists. {@code page} is zero-based.
 */
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            page <= 0,
            page >= totalPages - 1);
    }
}
