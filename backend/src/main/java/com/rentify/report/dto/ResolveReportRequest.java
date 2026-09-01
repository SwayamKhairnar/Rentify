package com.rentify.report.dto;

import com.rentify.report.AdminAction;
import com.rentify.report.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveReportRequest(
    @NotNull(message = "Status is required")
    ReportStatus status,

    AdminAction adminAction,

    String adminNotes
) {}
