package de.richardliebscher.openapi_json_schema_generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchema;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchemaDataType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class Converter {
    private final boolean includeReadOnly;
    private final boolean includeWriteOnly;
    private final JsonSchemaVersion jsonSchemaVersion;
    private final Consumer<Message> warningsListener;

    JsonSchema convert(Components components) {
        var path = new JsonPath("components").push("schemas");
        var jsonSchema = new JsonSchema();
        var definitions = components.getSchemas()
                .entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> convert(e.getValue(), path.push(e.getKey()))));
        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v2019_09) >= 0) {
            jsonSchema.$defs = definitions;
        } else {
            jsonSchema.definitions = definitions;
        }
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

        JsonSchema jsonSchema = new JsonSchema();
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

        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            jsonSchema.anyOf = convertSchemaList(composedSchema.getAnyOf(), path);
            jsonSchema.oneOf = convertSchemaList(composedSchema.getOneOf(), path);
            jsonSchema.allOf = convertSchemaList(composedSchema.getAllOf(), path);
        }

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
            if (Boolean.TRUE.equals(schema.getExclusiveMinimum())) {
                jsonSchema.exclusiveMinimum = schema.getMinimum();
            } else {
                jsonSchema.minimum = schema.getMinimum();
            }
        } else {
            jsonSchema.maximum = schema.getMaximum();
            jsonSchema.exclusiveMaximum = schema.getMaximum();
            jsonSchema.minimum = schema.getMinimum();
            jsonSchema.exclusiveMinimum = schema.getMinimum();
        }

        // OpenAPI additions

        if (schema.getType() != null) {
            JsonSchemaDataType type = JsonSchemaDataType.fromValue(schema.getType());
            if (Boolean.TRUE.equals(schema.getNullable())) {
                jsonSchema.type = List.of(type, JsonSchemaDataType.NULL);
            } else {
                jsonSchema.type = List.of(type);
            }
        }
        setFormat(schema.getFormat(), jsonSchema, path);

        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v7) >= 0) {
            jsonSchema.readOnly = schema.getReadOnly();
            jsonSchema.writeOnly = schema.getWriteOnly();
        }

        if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v2019_09) >= 0) {
            jsonSchema.deprecated = schema.getDeprecated();
        }

        jsonSchema.examples = convertExample(schema, path);

        // Warnings
        if (schema.getExternalDocs() != null) {
            warn(path, "'externalDocs' property at ignored: " + schema.getExternalDocs());
        }
        if (schema.getXml() != null) {
            warn(path, "'xml' property ignored");
        }
        if (schema.getExtensions() != null) {
            warn(path, "'extensions' property ignored");
        }
        if (schema.getDiscriminator() != null) {
            // TODO: can we do better?
            warn(path, "'discriminator' property ignored");
        }

        // TODO: jsonSchema.enum_ = schema.getEnum();
        // TODO: jsonSchema.default_ = schema.getDefault();
        return jsonSchema;
    }

    private List<JsonNode> convertExample(Schema<?> schema, JsonPath path) {
        if (!schema.getExampleSetFlag()) {
            return null;
        }

        JsonNode exampleNode = toJsonNode(schema.getExample(), path);
        return exampleNode == null ? null : List.of(exampleNode);
    }

    private JsonNode toJsonNode(Object example, JsonPath path) {
        if (example == null) {
            return NullNode.getInstance();
        } else if (example instanceof JsonNode) {
            return (JsonNode) example;
        } else if (example instanceof BigDecimal) {
            return DecimalNode.valueOf((BigDecimal) example);
        } else if (example instanceof Number) {
            return DecimalNode.valueOf(new BigDecimal(example.toString()));
        } else if (example instanceof String) {
            return TextNode.valueOf((String) example);
        } else if (example instanceof Boolean) {
            return BooleanNode.valueOf((Boolean) example);
        } else {
            warn(path, "value ignored because of internal error: unsupported example type: "
                    + example.getClass().getName());
            return null;
        }
    }

    private List<JsonSchema> convertSchemaList(@SuppressWarnings("rawtypes") List<Schema> schemaList, JsonPath path) {
        if (schemaList == null) {
            return null;
        }

        return IntStream
                .range(0, schemaList.size())
                .mapToObj(i -> convert(schemaList.get(i), path.push(String.valueOf(i))))
                .collect(Collectors.toList());
    }

    private void warn(JsonPath path, String message) {
        warningsListener.accept(Message.warning(path, message));
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
                if (schema.minimum == null) {
                    schema.minimum = new BigDecimal(Integer.MIN_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = new BigDecimal(Integer.MAX_VALUE);
                }
                break;
            }
            case "int64": {
                if (schema.minimum == null) {
                    schema.minimum = new BigDecimal(Long.MIN_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = new BigDecimal(Long.MAX_VALUE);
                }
                break;
            }
            case "byte": {
                if (jsonSchemaVersion.compareTo(JsonSchemaVersion.v7) >= 0) {
                    schema.contentMediaType = "base64";
                }
                break;
            }
            default: {
                warn(path, "'" + format + "' format ignored");
                break;
            }
        }
    }

}
