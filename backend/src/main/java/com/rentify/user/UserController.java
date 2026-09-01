package com.rentify.user;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.user.dto.UpdateProfileRequest;
import com.rentify.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched", Map.of("user", user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> updateProfile(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse updatedUser = userService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", Map.of("user", updatedUser)));
    }
}
