package com.rentify.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ReviewType {
    LENDER("lender"),
    RENTER("renter");

    private final String value;

    ReviewType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReviewType fromValue(String value) {
        if (value == null) return null;
        for (ReviewType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown review type: " + value);
    }

    @Converter(autoApply = true)
    public static class ReviewTypeConverter implements AttributeConverter<ReviewType, String> {
        @Override
        public String convertToDatabaseColumn(ReviewType attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ReviewType convertToEntityAttribute(String dbData) {
            return dbData != null ? ReviewType.fromValue(dbData) : null;
        }
    }
}
