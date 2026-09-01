package com.rentify.report.dto;

import com.rentify.report.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
    @NotNull(message = "Reported user ID is required")
    Long reportedUserId,

    @NotNull(message = "Rental ID is required")
    Long rentalId,

    @NotNull(message = "Report reason is required")
    ReportReason reason,

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,

    String evidenceImage
) {}
