package de.richardliebscher.openapi_json_schema_generator.jsonschema;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonSchemaDataType {
    OBJECT("object"),
    ARRAY("array"),
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    NULL("null");

    @JsonValue
    public final String value;

    JsonSchemaDataType(String value) {
        this.value = value;
    }

    public static JsonSchemaDataType fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (JsonSchemaDataType v : JsonSchemaDataType.values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }
        throw new IllegalStateException("Unknown type " + value);
    }
}
