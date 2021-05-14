package de.richardliebscher.openapi_json_schema_generator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Nested;
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
import java.util.stream.StreamSupport;

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
    void checkObject() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()
                        .add("type", "object")
                        .add("properties", Json.object()
                                .add("id", Json.object().add("type", "integer"))
                                .add("name", Json.object().add("type", "string")))));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals(2, type.get("properties").asObject().size());
    }

    @Nested
    class Example {
        @Test
        void checkObjectExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "object")
                            .add("properties", Json.object()
                                    .add("id", Json.object().add("type", "integer"))
                                    .add("name", Json.object().add("type", "string")))
                            .add("example", Json.object().add("id", 42).add("name", "TEST"))));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals(1, type.get("examples").asArray().size());
        }

        @Test
        void checkNumberExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "number")
                            .add("example", 42.5)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals(42.5, type.get("examples").asArray().get(0).asDouble());
        }

        @Test
        void checkIntegerExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "integer")
                            .add("example", 42)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals(42, type.get("examples").asArray().get(0).asInt());
        }

        @Test
        void checkStringExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "string")
                            .add("example", "TEST")));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals("TEST", type.get("examples").asArray().get(0).asString());
        }

        @Test
        void checkNullExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "string")
                            .add("nullable", true)
                            .add("example", Json.NULL)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals(Json.NULL, type.get("examples").asArray().get(0));
        }

        @Test
        void checkNoExample() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "string")
                            .add("nullable", true)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertNull(type.get("examples"));
        }
    }

    @Test
    void checkAnyEnum() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()
                        .add("enum", Json.array()
                                .add(Json.value(42))
                                .add(Json.value("42"))
                                .add(Json.object())
                                .add(Json.array()))));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals(4, type.get("enum").asArray().size());
    }

    @Test
    void checkNoDefault() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertNull(type.get("default"));
    }

    @Test
    void checkDefault() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object()
                        .add("type", "integer")
                        .add("default", Json.value(42))));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals(42, type.get("default").asInt());
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

    @Nested
    class Nullable {
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
        void checkNotNullable() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object().add("type", "integer").add("nullable", false)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertEquals(1, type.get("type").asArray().size());
        }

        @Test
        void checkNullableEnum() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "integer")
                            .add("enum", Json.array(1, 2))
                            .add("nullable", true)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertTrue(StreamSupport.stream(type.get("enum").asArray().spliterator(), false)
                    .anyMatch(e -> e.equals(Json.NULL)));
        }

        @Test
        void checkNotNullableEnum() {
            // ARRANGE
            JsonObject input = openApiWithSchemas(Json.object()
                    .add("Test", Json.object()
                            .add("type", "integer")
                            .add("enum", Json.array(1, 2))
                            .add("nullable", false)));

            // ACT
            JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

            // ASSERT
            assertNotNull(jsonValue);
            assertNoMessages();

            JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
            assertTrue(StreamSupport.stream(type.get("enum").asArray().spliterator(), false)
                    .noneMatch(e -> e.equals(Json.NULL)));
        }
    }

    @Test
    void checkArray() {
        // ARRANGE
        JsonObject input = openApiWithSchemas(Json.object()
                .add("Test", Json.object().add("type", "array").add("items", Json.object().add("type", "integer"))));

        // ACT
        JsonValue jsonValue = convert(input, messageCollector, defaultConverter);

        // ASSERT
        assertNotNull(jsonValue);
        assertNoMessages();

        JsonObject type = jsonValue.asObject().get("$defs").asObject().get("Test").asObject();
        assertEquals("integer", type.get("items").asObject().get("type").asArray().get(0).asString());
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