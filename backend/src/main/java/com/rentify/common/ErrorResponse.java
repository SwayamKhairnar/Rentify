package com.rentify.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    boolean success,
    String message
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message);
    }
}
