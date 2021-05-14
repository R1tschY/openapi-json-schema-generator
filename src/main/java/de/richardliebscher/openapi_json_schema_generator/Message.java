package de.richardliebscher.openapi_json_schema_generator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Message {
    public final Severity severity;
    public final JsonPath path;
    public final String message;

    public static Message warning(JsonPath path, String message) {
        return new Message(Severity.WARNING, path, message);
    }

    public static Message warning(String message) {
        return new Message(Severity.WARNING, null, message);
    }

    public static Message error(JsonPath path, String message) {
        return new Message(Severity.ERROR, path, message);
    }

    public static Message error(String message) {
        return new Message(Severity.ERROR, null, message);
    }

    public enum Severity {
        WARNING, ERROR
    }

    @Override
    public String toString() {
        return "Message{" +
                "severity=" + severity +
                ", path=" + path +
                ", message='" + message + '\'' +
                '}';
    }
}
