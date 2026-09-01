package com.rentify.auth.dto;

import com.rentify.user.dto.UserResponse;

public record AuthResponse(
    UserResponse user,
    String token
) {}
