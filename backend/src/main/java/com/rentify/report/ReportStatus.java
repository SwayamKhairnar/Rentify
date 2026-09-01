package com.rentify.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ReportStatus {
    PENDING("pending"),
    REVIEWED("reviewed"),
    RESOLVED("resolved"),
    DISMISSED("dismissed");

    private final String value;

    ReportStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReportStatus fromValue(String value) {
        if (value == null) return null;
        for (ReportStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown report status: " + value);
    }

    @Converter(autoApply = true)
    public static class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {
        @Override
        public String convertToDatabaseColumn(ReportStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ReportStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? ReportStatus.fromValue(dbData) : null;
        }
    }
}
