package de.richardliebscher.openapi_json_schema_generator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
}
