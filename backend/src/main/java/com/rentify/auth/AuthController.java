package com.rentify.auth;

import com.rentify.auth.dto.AuthResponse;
import com.rentify.auth.dto.LoginRequest;
import com.rentify.auth.dto.RegisterRequest;
import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> me(@CurrentUser CustomUserDetails userDetails) {
        UserResponse user = authService.getCurrentUser(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", Map.of("user", user)));
    }
}
