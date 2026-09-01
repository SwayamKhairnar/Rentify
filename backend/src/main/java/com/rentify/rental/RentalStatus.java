package com.rentify.rental;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum RentalStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    RentalStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RentalStatus fromValue(String value) {
        if (value == null) return null;
        for (RentalStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown rental status: " + value);
    }

    @Converter(autoApply = true)
    public static class RentalStatusConverter implements AttributeConverter<RentalStatus, String> {
        @Override
        public String convertToDatabaseColumn(RentalStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public RentalStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? RentalStatus.fromValue(dbData) : null;
        }
    }
}
