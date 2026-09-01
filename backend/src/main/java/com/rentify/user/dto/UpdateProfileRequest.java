package com.rentify.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    String name,

    @Size(max = 300, message = "Bio cannot exceed 300 characters")
    String bio,

    @Size(max = 100, message = "Campus cannot exceed 100 characters")
    String campus,

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    String phone,

    @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    String avatar
) {}
