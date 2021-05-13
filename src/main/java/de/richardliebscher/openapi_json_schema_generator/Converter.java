package de.richardliebscher.openapi_json_schema_generator;

import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchema;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchemaDataType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class Converter {
    private final boolean includeReadOnly;
    private final boolean includeWriteOnly;
    private final JsonSchemaVersion jsonSchemaVersion;

    public Converter(boolean includeReadOnly, boolean includeWriteOnly, JsonSchemaVersion jsonSchemaVersion) {
        this.includeReadOnly = includeReadOnly;
        this.includeWriteOnly = includeWriteOnly;
        this.jsonSchemaVersion = jsonSchemaVersion;
    }

    JsonSchema convert(Components components) {
        JsonPath path = new JsonPath("components").push("schemas");
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.definitions = components.getSchemas()
                .entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> convert(e.getValue(), path.push(e.getKey()))));
        return jsonSchema;
    }

    public JsonSchema convert(Schema<?> schema, JsonPath path) {
        // filter

        if (schema == null) {
            return null;
        }
        if (!includeReadOnly && Boolean.TRUE.equals(schema.getReadOnly())) {
            return null; // TODO: handle it top level
        }
        if (!includeWriteOnly && Boolean.TRUE.equals(schema.getWriteOnly())) {
            return null; // TODO: handle it top level
        }

        // trivial

        // TODO: schema.getName();
        JsonSchema jsonSchema = new JsonSchema();
        setFormat(schema.getFormat(), jsonSchema, path);

        jsonSchema.title = schema.getTitle();
        jsonSchema.multipleOf = schema.getMultipleOf();
        jsonSchema.maxLength = schema.getMaxLength();
        jsonSchema.minLength = schema.getMinLength();
        jsonSchema.pattern = schema.getPattern();
        jsonSchema.maxItems = schema.getMaxItems();
        jsonSchema.minItems = schema.getMinItems();
        jsonSchema.uniqueItems = schema.getUniqueItems();
        jsonSchema.maxProperties = schema.getMaxProperties();
        jsonSchema.minProperties = schema.getMinProperties();
        jsonSchema.required = schema.getRequired();
        jsonSchema.additionalProperties = (Boolean) schema.getAdditionalProperties(); // TODO: can be schema
        jsonSchema.description = schema.getDescription();
        jsonSchema.$ref = schema.get$ref(); // TODO: map ref

        // schema conversions

        jsonSchema.not = convert(schema.getNot(), path.push("not"));

        if (schema.getProperties() != null) {
            jsonSchema.properties = schema.getProperties()
                    .entrySet()
                    .stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> convert(e.getValue(), path.push(e.getKey()))));
        }

        // v4 -> v6

        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v6) >= 0) {
            if (Boolean.TRUE.equals(schema.getExclusiveMaximum())) {
                jsonSchema.exclusiveMaximum = schema.getMaximum();
            } else {
                jsonSchema.maximum = schema.getMaximum();
            }
            if (Boolean.TRUE.equals(schema.getExclusiveMaximum())) {
                jsonSchema.exclusiveMinimum = schema.getMinimum();
            } else {
                jsonSchema.minimum = schema.getMinimum();
            }
        }

        // OpenAPI additions

        if (schema.getType() != null) {
            JsonSchemaDataType type = JsonSchemaDataType.fromValue(
                    schema.getType());
            if (Boolean.TRUE.equals(schema.getNullable())) {
                jsonSchema.type = List.of(type, JsonSchemaDataType.NULL);
            } else {
                jsonSchema.type = List.of(type);
            }
        }

        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v7) >= 0) {
            jsonSchema.readOnly = schema.getReadOnly();
            jsonSchema.writeOnly = schema.getWriteOnly();
        }

        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v2019_09) >= 0) {
            jsonSchema.deprecated = schema.getDeprecated();
        }

        // Warnings
        if (schema.getExternalDocs() != null) {
            System.err.println(
                    "'externalDocs' property at "
                            + path + " ignored: " + schema.getExternalDocs());
        }
        if (schema.getXml() != null) {
            System.err.println("'xml' property at " + path + " ignored");
        }
        if (schema.getExtensions() != null) {
            System.err.println("'extensions' property at " + path + " ignored");
        }
        if (schema.getDiscriminator() != null) {
            // TODO: can we do better?
            System.err.println("'discriminator' property at " + path + " ignored");
        }

        // TODO: jsonSchema.examples = mapNullable(schema.getExample(), List::of);
        // TODO: jsonSchema.enum_ = schema.getEnum();
        // TODO: jsonSchema.default_ = schema.getDefault();
        return jsonSchema;
    }

    private void setFormat(String format, JsonSchema schema, JsonPath path) {
        if (format == null) {
            return;
        }

        switch (format) {
            case "date": {
                schema.format = "date";
                break;
            }
            case "date-time": {
                schema.format = "date-time";
                break;
            }
            case "int32": {
                schema.minimum = new BigDecimal(Integer.MIN_VALUE);
                schema.maximum = new BigDecimal(Integer.MAX_VALUE);
                break;
            }
            case "int64": {
                schema.minimum = new BigDecimal(Long.MIN_VALUE);
                schema.maximum = new BigDecimal(Long.MAX_VALUE);
                break;
            }
            default: {
                System.err.println(
                        "'" + format + "' format at " + path + " ignored");
                break;
            }
        }
    }
}
