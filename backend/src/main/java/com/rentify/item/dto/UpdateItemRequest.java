package com.rentify.item.dto;

import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateItemRequest(
    @Size(min = 1, max = 100, message = "Title cannot exceed 100 characters")
    String title,

    @Size(min = 1, max = 1000, message = "Description cannot exceed 1000 characters")
    String description,

    ItemCategory category,

    @DecimalMin(value = "0.01", message = "Price per day must be positive")
    @DecimalMax(value = "100000.00", message = "Price per day cannot exceed 100,000")
    BigDecimal pricePerDay,

    @Size(max = 5, message = "Cannot upload more than 5 images")
    List<String> images,

    ItemCondition condition,

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    String location,

    Boolean isAvailable
) {}
