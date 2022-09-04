package de.richardliebscher.openapi_json_schema_generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchema;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchemaDataType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class Converter {
    private final boolean includeReadOnly;
    private final boolean includeWriteOnly;
    private final JsonSchemaDraft jsonSchemaDraft;
    private final Consumer<Message> warningsListener;

    public JsonSchema convert(Components components, String mainSchema) {
        var path = new JsonPath("components").push("schemas");

        var jsonSchema = new JsonSchema();
        jsonSchema.$schema = jsonSchemaDraft.id;
        jsonSchema.$ref = mapReference(mainSchema, null);

        var definitions = components.getSchemas()
                .entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> convert(e.getValue(), path.push(e.getKey()))));
        if (isOrNewerThan2019_09()) {
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
        jsonSchema.description = schema.getDescription();

        // schema conversions

        jsonSchema.$ref = mapReference(schema.get$ref(), path);
        jsonSchema.not = convert(schema.getNot(), path.push("not"));

        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            jsonSchema.anyOf = convertSchemaList(composedSchema.getAnyOf(), path);
            jsonSchema.oneOf = convertSchemaList(composedSchema.getOneOf(), path);
            jsonSchema.allOf = convertSchemaList(composedSchema.getAllOf(), path);
        }

        if (schema instanceof ArraySchema) {
            jsonSchema.items = convert(((ArraySchema) schema).getItems(), path);
        }

        if (schema.getProperties() != null) {
            jsonSchema.properties = schema.getProperties()
                    .entrySet()
                    .stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> convert(e.getValue(), path.push(e.getKey()))));
        }

        if (schema.getAdditionalProperties() instanceof Boolean) {
            jsonSchema.additionalProperties = schema.getAdditionalProperties();
        } else if (schema.getAdditionalProperties() instanceof Schema) {
            jsonSchema.additionalProperties = convert(
                    (Schema<?>) schema.getAdditionalProperties(), path.push("additionalProperties"));
        }

        // v4 -> v6

        if (jsonSchemaDraft.compareTo(JsonSchemaDraft.v6) >= 0) {
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

        if (jsonSchemaDraft.compareTo(JsonSchemaDraft.v7) >= 0) {
            jsonSchema.readOnly = schema.getReadOnly();
            jsonSchema.writeOnly = schema.getWriteOnly();
        }

        if (isOrNewerThan2019_09()) {
            jsonSchema.deprecated = schema.getDeprecated();
        }

        jsonSchema.examples = convertExample(schema, path);
        jsonSchema.enum_ = convertEnum(schema, path);
        jsonSchema.default_ = schema.getDefault() != null
                ? toJsonNode(schema.getDefault(), path.push("default"))
                : null;

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

        return jsonSchema;
    }

    private String mapReference(String $ref, JsonPath path) {
        if ($ref == null) {
            return null;
        }

        // handle short ref
        if (!$ref.contains(".") && !$ref.contains("/")) {
            $ref = COMPONENTS_SCHEMAS_REF + $ref;
        }

        if ($ref.startsWith(COMPONENTS_SCHEMAS_REF)) {
            String definitionsPath = isOrNewerThan2019_09() ? "#/$defs/" : "#/definitions/";
            return definitionsPath + $ref.substring(COMPONENTS_SCHEMAS_REF.length());
        } else {
            if (path != null) {
                throw new IllegalArgumentException(
                        "At " + path + " reference outside of " + COMPONENTS_SCHEMAS_REF + "not supported: " + $ref);
            } else {
                throw new IllegalArgumentException(
                        "Reference outside of " + COMPONENTS_SCHEMAS_REF + "not supported: " + $ref);
            }
        }
    }

    private boolean isOrNewerThan2019_09() {
        return jsonSchemaDraft.compareTo(JsonSchemaDraft.v2019_09) >= 0;
    }

    private List<JsonNode> convertEnum(Schema<?> schema, JsonPath path) {
        if (schema.getEnum() == null) {
            return null;
        }

        JsonPath enumPath = path.push("enum");
        List<JsonNode> values = IntStream
                .range(0, schema.getEnum().size())
                .mapToObj(i -> toJsonNode(schema.getEnum().get(i), enumPath.push(String.valueOf(i))))
                .collect(toList());
        if (Boolean.TRUE.equals(schema.getNullable())) {
            values.add(NullNode.getInstance());
        }
        return values;
    }

    private List<JsonNode> convertExample(Schema<?> schema, JsonPath path) {
        if (!schema.getExampleSetFlag()) {
            return null;
        }

        JsonNode exampleNode = toJsonNode(schema.getExample(), path.push("example"));
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
                .collect(toList());
    }

    private void warn(JsonPath path, String message) {
        warningsListener.accept(Message.warning(path, message));
    }

    private void setFormat(String format, JsonSchema schema, JsonPath path) {
        if (format == null) {
            return;
        }

        schema.format = format;

        switch (format) {
            case "int32": {
                if (schema.minimum == null) {
                    schema.minimum = BigDecimal.valueOf(Integer.MIN_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = BigDecimal.valueOf(Integer.MAX_VALUE);
                }
                break;
            }
            case "int64": {
                if (schema.minimum == null) {
                    schema.minimum = BigDecimal.valueOf(Long.MIN_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = BigDecimal.valueOf(Long.MAX_VALUE);
                }
                break;
            }
            case "float": {
                if (schema.minimum == null) {
                    schema.minimum = BigDecimal.valueOf(-Float.MAX_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = BigDecimal.valueOf(Float.MAX_VALUE);
                }
                break;
            }
            case "double": {
                if (schema.minimum == null) {
                    schema.minimum = BigDecimal.valueOf(-Double.MAX_VALUE);
                }
                if (schema.maximum == null) {
                    schema.maximum = BigDecimal.valueOf(Double.MAX_VALUE);
                }
                break;
            }
            case "byte": {
                if (jsonSchemaDraft.compareTo(JsonSchemaDraft.v7) >= 0) {
                    schema.contentEncoding = "base64";
                }
                schema.pattern = "^[a-zA-Z0-9+\\/]*=*$";
                break;
            }
            default: {
                break;
            }
        }
    }

}
