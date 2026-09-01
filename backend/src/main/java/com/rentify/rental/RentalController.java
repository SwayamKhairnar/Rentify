package com.rentify.rental;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.rental.dto.CreateRentalRequest;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.rental.dto.UpdateRentalStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, RentalResponse>>> createRental(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody CreateRentalRequest request
    ) {
        RentalResponse rental = rentalService.createRental(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Rental request submitted", Map.of("rental", rental)));
    }

    @GetMapping("/my-rentals")
    public ResponseEntity<ApiResponse<Map<String, List<RentalResponse>>>> getMyRentals(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<RentalResponse> rentals = rentalService.getMyRentals(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("My rentals fetched", Map.of("rentals", rentals)));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<Map<String, List<RentalResponse>>>> getReceivedRentals(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<RentalResponse> rentals = rentalService.getReceivedRentals(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Received rental requests fetched", Map.of("rentals", rentals)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, RentalResponse>>> getRentalById(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        RentalResponse rental = rentalService.getRentalById(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Rental details fetched", Map.of("rental", rental)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, RentalResponse>>> updateRentalStatus(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRentalStatusRequest request
    ) {
        RentalResponse updatedRental = rentalService.updateRentalStatus(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Rental status updated to " + updatedRental.status().getValue(),
                Map.of("rental", updatedRental)
        ));
    }
}
