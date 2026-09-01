package com.rentify.report;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.common.PaginatedResponse;
import com.rentify.report.dto.CreateReportRequest;
import com.rentify.report.dto.ReportResponse;
import com.rentify.report.dto.ResolveReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, ReportResponse>>> createReport(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody CreateReportRequest request
    ) {
        ReportResponse response = reportService.createReport(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report submitted successfully", Map.of("report", response)));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<ApiResponse<Map<String, List<ReportResponse>>>> getMyReports(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<ReportResponse> reports = reportService.getMyReports(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Your reports fetched", Map.of("reports", reports)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaginatedResponse<ReportResponse>> getAdminReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        PaginatedResponse<ReportResponse> response = reportService.getAdminReports(status, reason, page, limit);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, ReportResponse>>> resolveReport(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        ReportResponse response = reportService.resolveReport(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", Map.of("report", response)));
    }
}
