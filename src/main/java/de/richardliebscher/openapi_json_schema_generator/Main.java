package de.richardliebscher.openapi_json_schema_generator;

import com.fasterxml.jackson.databind.ObjectWriter;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchema;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Command(
        name = "generate",
        description = "Generate JSON schema from Open API specification",
        mixinStandardHelpOptions = true,
        version = "openapi-json-schema-generator 0.1"
)
public class Main implements Runnable {

    @Parameters(
            arity = "1",
            paramLabel = "INPUT",
            description = "Reference to OpenAPI specification in JSON or YAML format")
    private String input;

    @Option(
            names = {"--exclude-read-only"},
            description = "Exclude read only properties.")
    private boolean excludeReadOnly = false;

    @Option(
            names = {"--exclude-write-only"},
            description = "Exclude write only properties.")
    private boolean excludeWriteOnly = false;

    @Option(
            names = {"--json-schema-version"},
            description = "Version of JSON schema for output. Choices: ${COMPLETION-CANDIDATES}",
            completionCandidates = JsonSchemaVersionCandidates.class)
    private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.v2019_09;

    private static class JsonSchemaVersionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return JsonSchemaVersion.names().iterator();
        }
    }

    public static void main(String[] args) {
        CommandLine.ITypeConverter<JsonSchemaVersion> iTypeConverter = value -> {
            for (JsonSchemaVersion v : JsonSchemaVersion.values()) {
                if (v.name.equals(value)) {
                    return v;
                }
            }

            throw new CommandLine.TypeConversionException(
                    format("expected one of %s but was '%s'", JsonSchemaVersion.names(), value));
        };


        try {
            System.exit(new CommandLine(new Main())
                    .registerConverter(JsonSchemaVersion.class, iTypeConverter)
                    .execute(args));
        } catch (RuntimeException exp) {
            System.err.println("Error: " + exp.getMessage());
            exp.printStackTrace();
            System.exit(5);
        }
    }

    public void run() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveCombinators(false);
        parseOptions.setFlatten(false);
        SwaggerParseResult result = new OpenAPIParser()
                .readLocation(input, null, parseOptions);

        if (result.getMessages() != null) {
            result.getMessages().forEach(System.err::println);
        }

        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            System.err.println("Failed to parse OpenAPI spec");
            System.exit(2);
            return;
        }

        Converter converter = new Converter(
                !excludeReadOnly, !excludeWriteOnly, jsonSchemaVersion);
        JsonSchema jsonSchema = converter.convert(openAPI.getComponents());

        // print
        ObjectWriter objectWriter = ObjectMapperFactory.createJson()
                .writerWithDefaultPrettyPrinter();
        try {
            objectWriter.writeValue(System.out, jsonSchema);
        } catch (IOException e) {
            System.err.println("Failed to generate json: " + e.getMessage());
            System.exit(3);
        }
    }
}
