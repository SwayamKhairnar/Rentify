package com.rentify.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum AdminAction {
    NONE("none"),
    WARNED("warned"),
    LISTING_REMOVED("listing_removed"),
    ACCOUNT_SUSPENDED("account_suspended"),
    RESOLVED("resolved");

    private final String value;

    AdminAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AdminAction fromValue(String value) {
        if (value == null) return null;
        for (AdminAction action : values()) {
            if (action.value.equalsIgnoreCase(value) || action.name().equalsIgnoreCase(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown admin action: " + value);
    }

    @Converter(autoApply = true)
    public static class AdminActionConverter implements AttributeConverter<AdminAction, String> {
        @Override
        public String convertToDatabaseColumn(AdminAction attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public AdminAction convertToEntityAttribute(String dbData) {
            return dbData != null ? AdminAction.fromValue(dbData) : null;
        }
    }
}
