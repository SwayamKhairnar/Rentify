package com.rentify.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.user.User;

import java.math.BigDecimal;

public record RentalUserSummaryResponse(
    Long id,
    String name,
    String email,
    String avatar,
    String campus,
    String phone,
    BigDecimal rating
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static RentalUserSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        return new RentalUserSummaryResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAvatar(),
            user.getCampus(),
            user.getPhone(),
            user.getRating()
        );
    }
}
