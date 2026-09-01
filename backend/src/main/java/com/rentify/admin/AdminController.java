package com.rentify.admin;

import com.rentify.admin.dto.AdminStatsResponse;
import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.common.PaginatedResponse;
import com.rentify.item.ItemCategory;
import com.rentify.item.dto.ItemResponse;
import com.rentify.rental.RentalStatus;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.report.ReportReason;
import com.rentify.report.ReportService;
import com.rentify.report.ReportStatus;
import com.rentify.report.dto.ReportResponse;
import com.rentify.report.dto.ResolveReportRequest;
import com.rentify.user.UserRole;
import com.rentify.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    public AdminController(AdminService adminService, ReportService reportService) {
        this.adminService = adminService;
        this.reportService = reportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, AdminStatsResponse>>> getStats() {
        AdminStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched", Map.of("stats", stats)));
    }

    @GetMapping("/users")
    public ResponseEntity<PaginatedResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean suspended
    ) {
        PaginatedResponse<UserResponse> response = adminService.getUsers(page, limit, search, role, suspended);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> toggleUserSuspension(
            @PathVariable Long id
    ) {
        UserResponse response = adminService.toggleUserSuspension(id);
        String actionMessage = response.isSuspended() ? "User suspended successfully" : "User unsuspended successfully";
        return ResponseEntity.ok(ApiResponse.success(actionMessage, Map.of("user", response)));
    }

    @GetMapping("/items")
    public ResponseEntity<PaginatedResponse<ItemResponse>> getItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) Boolean available
    ) {
        PaginatedResponse<ItemResponse> response = adminService.getItems(page, limit, search, category, available);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long id
    ) {
        adminService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success("Item deleted by admin"));
    }

    @GetMapping("/rentals")
    public ResponseEntity<PaginatedResponse<RentalResponse>> getRentals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) RentalStatus status
    ) {
        PaginatedResponse<RentalResponse> response = adminService.getRentals(page, limit, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports")
    public ResponseEntity<PaginatedResponse<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        PaginatedResponse<ReportResponse> response = reportService.getAdminReports(status, reason, page, limit);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Map<String, ReportResponse>>> resolveReport(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        ReportResponse response = reportService.resolveReport(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", Map.of("report", response)));
    }
}
