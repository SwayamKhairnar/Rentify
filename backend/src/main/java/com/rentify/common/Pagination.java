package com.rentify.common;

public record Pagination(
    int page,
    int limit,
    long total,
    int pages
) {
    public static Pagination of(int page, int limit, long total) {
        int pages = limit > 0 ? (int) Math.ceil((double) total / limit) : 0;
        return new Pagination(page, limit, total, pages);
    }
}
