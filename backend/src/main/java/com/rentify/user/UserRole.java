package com.rentify.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum UserRole {
    STUDENT("student"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserRole fromValue(String value) {
        if (value == null) return null;
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + value);
    }

    @Converter(autoApply = true)
    public static class UserRoleConverter implements AttributeConverter<UserRole, String> {
        @Override
        public String convertToDatabaseColumn(UserRole attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public UserRole convertToEntityAttribute(String dbData) {
            return dbData != null ? UserRole.fromValue(dbData) : null;
        }
    }
}
