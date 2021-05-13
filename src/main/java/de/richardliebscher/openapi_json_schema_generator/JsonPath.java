package de.richardliebscher.openapi_json_schema_generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonPath {
    private final String name;
    private final JsonPath parent;

    public JsonPath(String name) {
        this.name = name;
        this.parent = null;
    }

    private JsonPath(String name, JsonPath parent) {
        this.name = name;
        this.parent = parent;
    }

    public JsonPath push(String name) {
        return new JsonPath(name, this);
    }

    @Override
    public String toString() {
        List<String> paths = new ArrayList<>();
        JsonPath path = this;
        while (path != null) {
            paths.add(path.name);
            path = path.parent;
        }
        Collections.reverse(paths);
        return "#/" + String.join("/", paths);
    }
}
