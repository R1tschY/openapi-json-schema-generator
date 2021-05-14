package de.richardliebscher.openapi_json_schema_generator.jsonschema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class JsonSchema {
    public String $schema;
    public String $id;
    public Map<String, JsonSchema> definitions;
    public Map<String, JsonSchema> $defs;
    public String $ref;

    // generic
    public String title;
    public String description;
    @JsonProperty("default")
    public JsonNode default_;
    @JsonProperty("examples")
    public JsonNode examples;

    @JsonProperty("enum")
    public List<JsonNode> enum_;
    @JsonProperty("const")
    public JsonNode const_;

    // Needs Draft-7
    public Boolean readOnly;
    // Needs Draft-7
    public Boolean writeOnly;
    // Needs 2019-09
    public Boolean deprecated;

    public List<JsonSchemaDataType> type; // TODO: can also be only DataType element

    // combiners
    public List<JsonSchema> allOf;
    public List<JsonSchema> anyOf;
    public List<JsonSchema> oneOf;
    public JsonSchema not;

    // conditions
    @JsonProperty("if")
    public JsonSchema if_;
    public JsonSchema then;
    @JsonProperty("else")
    public JsonSchema else_;

    // string
    public Integer minLength;
    public Integer maxLength;
    public String pattern;
    public String format;
    public String contentMediaType;
    public String contentEncoding;

    // integer / number
    public BigDecimal multipleOf;
    public BigDecimal minimum;
    public BigDecimal exclusiveMinimum;
    public BigDecimal maximum;
    public BigDecimal exclusiveMaximum;

    // object
    public Map<String, JsonSchema> properties;
    public Boolean additionalProperties;
    public List<String> required;
    public JsonSchema propertyNames;
    public Integer minProperties;
    public Integer maxProperties;
    public Map<String, JsonSchema> patternProperties;

    // array
    public JsonSchema items; // TODO: can be a list
    public JsonSchema contains;
    public Integer minItems;
    public Integer maxItems;
    public Boolean uniqueItems;
}
