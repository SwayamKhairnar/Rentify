package com.rentify.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentalResponse(
    Long id,
    RentalItemSummaryResponse item,
    RentalUserSummaryResponse renter,
    RentalUserSummaryResponse owner,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal totalPrice,
    RentalStatus status,
    String message,
    Instant createdAt,
    Instant updatedAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static RentalResponse fromEntity(Rental rental) {
        if (rental == null) return null;
        return new RentalResponse(
            rental.getId(),
            RentalItemSummaryResponse.fromEntity(rental.getItem()),
            RentalUserSummaryResponse.fromEntity(rental.getRenter()),
            RentalUserSummaryResponse.fromEntity(rental.getOwner()),
            rental.getStartDate(),
            rental.getEndDate(),
            rental.getTotalPrice(),
            rental.getStatus(),
            rental.getMessage(),
            rental.getCreatedAt(),
            rental.getUpdatedAt()
        );
    }
}
