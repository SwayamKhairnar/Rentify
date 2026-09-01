package com.rentify.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ReportReason {
    LATE_RETURN("Late Return"),
    ITEM_DAMAGE("Item Damage"),
    FAKE_PRODUCT("Fake Product/Description"),
    INAPPROPRIATE_BEHAVIOR("Inappropriate Behavior"),
    PAYMENT_ISSUES("Payment Issues"),
    NO_SHOW("No Show"),
    OTHER("Other");

    private final String value;

    ReportReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReportReason fromValue(String value) {
        if (value == null) return null;
        for (ReportReason reason : values()) {
            if (reason.value.equalsIgnoreCase(value) || reason.name().equalsIgnoreCase(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown report reason: " + value);
    }

    @Converter(autoApply = true)
    public static class ReportReasonConverter implements AttributeConverter<ReportReason, String> {
        @Override
        public String convertToDatabaseColumn(ReportReason attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ReportReason convertToEntityAttribute(String dbData) {
            return dbData != null ? ReportReason.fromValue(dbData) : null;
        }
    }
}
