package com.rentify.admin.dto;

public record AdminStatsResponse(
    long totalUsers,
    long activeUsers,
    long suspendedUsers,
    long totalItems,
    long availableItems,
    long totalRentals,
    long activeRentals,
    long completedRentals,
    long pendingReports,
    long resolvedReports,
    long totalReviews
) {}
