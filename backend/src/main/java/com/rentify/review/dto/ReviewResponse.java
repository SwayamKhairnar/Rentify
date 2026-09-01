package com.rentify.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.rental.dto.RentalItemSummaryResponse;
import com.rentify.review.Review;
import com.rentify.review.ReviewType;

import java.time.Instant;

public record ReviewResponse(
    Long id,
    Long rentalId,
    ReviewAuthorSummaryResponse reviewer,
    ReviewAuthorSummaryResponse targetUser,
    RentalItemSummaryResponse item,
    ReviewType type,
    Integer rating,
    Integer itemRating,
    String comment,
    Instant createdAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) return null;
        RentalItemSummaryResponse itemSummary = null;
        if (review.getRental() != null && review.getRental().getItem() != null && review.getType() == ReviewType.LENDER) {
            itemSummary = RentalItemSummaryResponse.fromEntity(review.getRental().getItem());
        }

        return new ReviewResponse(
            review.getId(),
            review.getRental() != null ? review.getRental().getId() : null,
            ReviewAuthorSummaryResponse.fromEntity(review.getReviewer()),
            ReviewAuthorSummaryResponse.fromEntity(review.getReviewee()),
            itemSummary,
            review.getType(),
            review.getRating(),
            review.getItemRating(),
            review.getComment(),
            review.getCreatedAt()
        );
    }
}
