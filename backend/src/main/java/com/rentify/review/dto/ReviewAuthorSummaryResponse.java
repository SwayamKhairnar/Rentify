package com.rentify.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.user.User;

public record ReviewAuthorSummaryResponse(
    Long id,
    String name,
    String avatar,
    String campus
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static ReviewAuthorSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        return new ReviewAuthorSummaryResponse(
            user.getId(),
            user.getName(),
            user.getAvatar(),
            user.getCampus()
        );
    }
}
