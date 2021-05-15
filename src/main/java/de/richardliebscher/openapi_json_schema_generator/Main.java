package de.richardliebscher.openapi_json_schema_generator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Iterator;
import java.util.function.Consumer;

@Command(
        name = "generate",
        description = "Generate JSON schema from Open API specification",
        mixinStandardHelpOptions = true,
        versionProvider = Main.VersionProvider.class
)
public class Main implements Runnable {

    @Parameters(
            arity = "1",
            paramLabel = "INPUT",
            description = "Reference to OpenAPI specification in JSON or YAML format")
    private String input;

    @Parameters(
            arity = "0..1",
            paramLabel = "MAIN_SCHEMA",
            description = "Name of schema or reference to schema: 'MySchema' or '#/components/schemas/MySchema'")
    private String mainSchema;

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

    static class VersionProvider implements CommandLine.IVersionProvider {

        public static final String VERSION_PROPERTIES =
                "/de/richardliebscher/openapi_json_schema_generator/version.properties";

        @Override
        public String[] getVersion() {
            Properties prop = new Properties();
            try (InputStream inputStream = Main.class.getResourceAsStream(VERSION_PROPERTIES)) {
                prop.load(inputStream);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

            return new String[] { prop.getProperty("name") + " " + prop.getProperty("version") };
        }
    }

    public static void main(String[] args) {
        try {
            System.exit(new CommandLine(new Main())
                    .registerConverter(JsonSchemaVersion.class, JsonSchemaVersion::fromName)
                    .execute(args));
        } catch (RuntimeException exp) {
            System.err.println("Error: " + exp.getMessage());
            exp.printStackTrace();
            System.exit(5);
        }
    }

    @Override
    public void run() {
        Consumer<Message> warningConsumer = w -> {
            if (w.path != null) {
                System.err.printf("%s: %s%n", w.path, w.message);
            } else {
                System.err.printf("%s%n", w.message);
            }
        };

        Converter converter = new Converter(
                !excludeReadOnly, !excludeWriteOnly, jsonSchemaVersion, warningConsumer);
        GenerateCommand command = new GenerateCommand(
                input, mainSchema, System.in, System.out, converter, warningConsumer);
        System.exit(command.run());
    }
}
