package com.rentify.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ItemCondition {
    NEW("new"),
    LIKE_NEW("like-new"),
    GOOD("good"),
    FAIR("fair"),
    POOR("poor");

    private final String value;

    ItemCondition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ItemCondition fromValue(String value) {
        if (value == null) return null;
        for (ItemCondition condition : values()) {
            if (condition.value.equalsIgnoreCase(value) || condition.name().equalsIgnoreCase(value)) {
                return condition;
            }
        }
        throw new IllegalArgumentException("Unknown item condition: " + value);
    }

    @Converter(autoApply = true)
    public static class ItemConditionConverter implements AttributeConverter<ItemCondition, String> {
        @Override
        public String convertToDatabaseColumn(ItemCondition attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ItemCondition convertToEntityAttribute(String dbData) {
            return dbData != null ? ItemCondition.fromValue(dbData) : null;
        }
    }
}
