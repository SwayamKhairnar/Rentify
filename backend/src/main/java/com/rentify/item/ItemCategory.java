package com.rentify.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ItemCategory {
    TEXTBOOKS("textbooks"),
    ELECTRONICS("electronics"),
    BIKES("bikes"),
    CAMERAS("cameras"),
    FURNITURE("furniture"),
    CLOTHING("clothing"),
    SPORTS("sports"),
    INSTRUMENTS("instruments"),
    OTHER("other");

    private final String value;

    ItemCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ItemCategory fromValue(String value) {
        if (value == null) return null;
        for (ItemCategory category : values()) {
            if (category.value.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown item category: " + value);
    }

    @Converter(autoApply = true)
    public static class ItemCategoryConverter implements AttributeConverter<ItemCategory, String> {
        @Override
        public String convertToDatabaseColumn(ItemCategory attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ItemCategory convertToEntityAttribute(String dbData) {
            return dbData != null ? ItemCategory.fromValue(dbData) : null;
        }
    }
}
