package de.richardliebscher.openapi_json_schema_generator;

import java.util.function.Function;
import java.util.function.Supplier;

public class Nulls {
    public static <T, R> R mapNullable(T t, Function<T, R> mapper) {
        return t == null ? null : mapper.apply(t);
    }
}
