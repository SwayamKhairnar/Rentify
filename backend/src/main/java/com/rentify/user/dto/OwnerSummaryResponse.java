package com.rentify.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.user.User;

import java.math.BigDecimal;

public record OwnerSummaryResponse(
    Long id,
    String name,
    String email,
    String avatar,
    String campus,
    BigDecimal rating,
    Integer totalReviews
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static OwnerSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        return new OwnerSummaryResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAvatar(),
            user.getCampus(),
            user.getRating(),
            user.getTotalReviews()
        );
    }
}
