package com.rentify.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.user.User;
import com.rentify.user.UserRole;

import java.math.BigDecimal;
import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    String avatar,
    String campus,
    String bio,
    String phone,
    BigDecimal rating,
    Integer totalReviews,
    BigDecimal lenderRating,
    Integer totalLenderReviews,
    BigDecimal renterRating,
    Integer totalRenterReviews,
    BigDecimal itemQualityAverage,
    Integer totalItemQualityReviews,
    boolean isSuspended,
    Instant createdAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getAvatar(),
            user.getCampus(),
            user.getBio(),
            user.getPhone(),
            user.getRating(),
            user.getTotalReviews(),
            user.getLenderRating(),
            user.getTotalLenderReviews(),
            user.getRenterRating(),
            user.getTotalRenterReviews(),
            user.getItemQualityAverage(),
            user.getTotalItemQualityReviews(),
            user.isSuspended(),
            user.getCreatedAt()
        );
    }
}
