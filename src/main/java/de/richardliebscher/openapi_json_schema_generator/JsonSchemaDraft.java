package de.richardliebscher.openapi_json_schema_generator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public enum JsonSchemaDraft {
    v4("4", "http://json-schema.org/draft-04/schema#"),
    v6("6", "http://json-schema.org/draft-06/schema#"),
    v7("7", "http://json-schema.org/draft-07/schema#"),
    v2019_09("2019-09", "https://json-schema.org/draft/2019-09/schema");

    public final String name;
    public final String id;

    JsonSchemaDraft(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public static List<String> names() {
        return Arrays.stream(JsonSchemaDraft.values())
                .map(e -> e.name)
                .collect(Collectors.toList());
    }

    public static JsonSchemaDraft fromName(String name) {
        return Arrays.stream(JsonSchemaDraft.values())
                .filter(e -> e.name.equals(name))
                .findFirst()
                .orElseThrow(() -> {
                    throw new IllegalArgumentException(
                            format("expected one of %s but was '%s'", JsonSchemaDraft.names(), name));
                });
    }
}
