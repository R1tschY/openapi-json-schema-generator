package de.richardliebscher.openapi_json_schema_generator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public enum JsonSchemaVersion {
    v4("draft-4"), v6("draft-6"), v7("draft-7"), v2019_09("2019-09");

    public final String name;

    JsonSchemaVersion(String name) {
        this.name = name;
    }

    public static List<String> names() {
        return Arrays.stream(JsonSchemaVersion.values())
                .map(e -> e.name)
                .collect(Collectors.toList());
    }

    public static JsonSchemaVersion fromName(String name) {
        return Arrays.stream(JsonSchemaVersion.values())
                .filter(e -> e.name.equals(name))
                .findFirst()
                .orElseThrow(() -> {
                    throw new IllegalArgumentException(
                            format("expected one of %s but was '%s'", JsonSchemaVersion.names(), name));
                });
    }
}
