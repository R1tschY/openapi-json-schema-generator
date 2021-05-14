package de.richardliebscher.openapi_json_schema_generator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCommandTest {

    private final MessageCollector messageCollector = new MessageCollector();
    private final Converter defaultConverter = new Converter(true, true, JsonSchemaVersion.v2019_09, messageCollector);

    @Test
    void checkMinimal() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object());

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        assertEquals(0, jsonValue.asObject().get("$defs").asObject().size());
    }

    @Test
    void checkInt32() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object().add("type", "integer").add("format", "int32")));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals("integer", type.get("type").asArray().get(0).asString());
        assertEquals(Integer.MAX_VALUE, type.get("maximum").asInt());
        assertEquals(Integer.MIN_VALUE, type.get("minimum").asInt());
    }

    @Test
    void checkInt64() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object().add("type", "integer").add("format", "int64")));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals("integer", type.get("type").asArray().get(0).asString());
        assertEquals(Long.MAX_VALUE, type.get("maximum").asLong());
        assertEquals(Long.MIN_VALUE, type.get("minimum").asLong());
    }

    @Test
    void checkNullable() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object().add("type", "integer").add("nullable", true)));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals("integer", type.get("type").asArray().get(0).asString());
        assertEquals("null", type.get("type").asArray().get(1).asString());
    }

    @Test
    void checkUpgradeExclusiveMaximum() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()
                        .add("type", "integer")
                        .add("maximum", 42)
                        .add("exclusiveMaximum", true)));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertNull(type.get("maximum"));
        assertEquals(42, type.get("exclusiveMaximum").asInt());
    }

    @Test
    void checkUpgradeExclusiveMinimum() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()
                        .add("type", "integer")
                        .add("minimum", 42)
                        .add("exclusiveMinimum", true)));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertNull(type.get("minimum"));
        assertEquals(42, type.get("exclusiveMinimum").asInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"anyOf", "oneOf", "allOf"})
    void checkAnyOf(String composer) {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object().add(composer, Json.array()
                        .add(Json.object().add("type", "integer"))
                        .add(Json.object().add("type", "string")))));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertNotNull(type.get(composer));
        assertEquals(2, type.get(composer).asArray().size());
    }

    private void assertNoMessages() {
        assertEquals(Collections.emptyList(), messageCollector.getMessages());
    }

    private JsonObject openApiWithSchemas(JsonObject schemas) {
        return Json.object()
                .add("openapi", "3.0.2")
                .add("info", Json.object()
                        .add("version", "1")
                        .add("title", "test"))
                .add("paths", Json.object())
                .add("components", Json.object()
                        .add("schemas", schemas));
    }

    private JsonValue convert(JsonValue input, MessageCollector messageCollector, Converter converter) {
        var inputStream = new ByteArrayInputStream(input.toString().getBytes(StandardCharsets.UTF_8));
        var outputStream = new ByteArrayOutputStream();
        var command = new GenerateCommand("-", inputStream, outputStream, converter, messageCollector);

        int code = command.run();
        if (code != 0) {
            return null;
        }
        return Json.parse(outputStream.toString(StandardCharsets.UTF_8));
    }

    private static class MessageCollector implements Consumer<Message> {
        private final List<Message> messages = new ArrayList<>();

        @Override
        public void accept(Message message) {
            System.err.printf("%s: %s: %s%n", message.severity, message.path, message.message);
            messages.add(message);
        }

        public List<Message> getMessages() {
            return messages;
        }
    }

}