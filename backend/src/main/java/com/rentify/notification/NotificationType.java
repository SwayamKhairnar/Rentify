package com.rentify.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum NotificationType {
    RENTAL_REQUEST("rental_request"),
    RENTAL_STATUS("rental_status"),
    REVIEW_RECEIVED("review_received"),
    MESSAGE("message"),
    SYSTEM("system");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationType fromValue(String value) {
        if (value == null) return null;
        for (NotificationType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + value);
    }

    @Converter(autoApply = true)
    public static class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {
        @Override
        public String convertToDatabaseColumn(NotificationType attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public NotificationType convertToEntityAttribute(String dbData) {
            return dbData != null ? NotificationType.fromValue(dbData) : null;
        }
    }
}
