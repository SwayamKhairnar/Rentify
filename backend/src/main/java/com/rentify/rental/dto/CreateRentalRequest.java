package com.rentify.rental.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRentalRequest(
    @NotNull(message = "Item ID is required")
    Long itemId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    String message
) {}
