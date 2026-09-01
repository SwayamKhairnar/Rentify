package com.rentify.common;

import java.util.List;

public record PaginatedResponse<T>(
    boolean success,
    String message,
    List<T> data,
    Pagination pagination
) {
    public static <T> PaginatedResponse<T> of(String message, List<T> data, Pagination pagination) {
        return new PaginatedResponse<>(true, message, data, pagination);
    }

    public static <T> PaginatedResponse<T> of(String message, List<T> data, int page, int limit, long total) {
        return new PaginatedResponse<>(true, message, data, Pagination.of(page, limit, total));
    }
}
