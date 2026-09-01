package com.rentify.rental.dto;

import com.rentify.rental.RentalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRentalStatusRequest(
    @NotNull(message = "Status is required")
    RentalStatus status,

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    String message
) {}
