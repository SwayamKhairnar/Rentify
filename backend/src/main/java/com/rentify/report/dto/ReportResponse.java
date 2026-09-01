package com.rentify.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.report.AdminAction;
import com.rentify.report.Report;
import com.rentify.report.ReportReason;
import com.rentify.report.ReportStatus;
import com.rentify.review.dto.ReviewAuthorSummaryResponse;

import java.time.Instant;

public record ReportResponse(
    Long id,
    ReviewAuthorSummaryResponse reporter,
    ReviewAuthorSummaryResponse reportedUser,
    RentalResponse rental,
    ReportReason reason,
    String description,
    String evidenceImage,
    ReportStatus status,
    AdminAction adminAction,
    String adminNotes,
    Instant createdAt,
    Instant updatedAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static ReportResponse fromEntity(Report report) {
        if (report == null) return null;
        return new ReportResponse(
            report.getId(),
            ReviewAuthorSummaryResponse.fromEntity(report.getReporter()),
            ReviewAuthorSummaryResponse.fromEntity(report.getReportedUser()),
            report.getRental() != null ? RentalResponse.fromEntity(report.getRental()) : null,
            report.getReason(),
            report.getDescription(),
            report.getEvidenceImage(),
            report.getStatus(),
            report.getAdminAction(),
            report.getAdminNotes(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}
