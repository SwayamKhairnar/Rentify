package com.rentify.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
    @NotNull(message = "Rental ID is required")
    Long rentalId,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    Integer rating,

    @Min(value = 1, message = "Item rating must be at least 1")
    @Max(value = 5, message = "Item rating cannot exceed 5")
    Integer itemRating,

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    String comment
) {}
