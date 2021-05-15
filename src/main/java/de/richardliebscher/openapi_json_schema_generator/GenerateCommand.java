package de.richardliebscher.openapi_json_schema_generator;

import com.fasterxml.jackson.databind.ObjectWriter;
import de.richardliebscher.openapi_json_schema_generator.jsonschema.JsonSchema;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class GenerateCommand {
    private final String input;
    private final String mainSchema;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Converter converter;
    private final Consumer<Message> warningsListener;

    public int run() {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveCombinators(false);
        parseOptions.setFlatten(false);

        SwaggerParseResult result;
        if (input.equals("-")) {
            try {
                result = new OpenAPIParser()
                        .readContents(IOUtils.toString(inputStream, StandardCharsets.UTF_8), null, parseOptions);
            } catch (IOException exception) {
                warningsListener.accept(
                        Message.error("Failed to read OpenAPI spec"));
                return 2;
            }
        } else {
            result = new OpenAPIParser()
                    .readLocation(input, null, parseOptions);
        }

        if (result.getMessages() != null) {
            result.getMessages().forEach(
                    w -> warningsListener.accept(Message.warning(w)));
        }

        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            warningsListener.accept(
                    Message.error("Failed to parse OpenAPI spec"));
            return 2;
        }

        JsonSchema jsonSchema = converter.convert(openAPI.getComponents(), mainSchema);

        // print
        ObjectWriter objectWriter = ObjectMapperFactory.createJson()
                .writerWithDefaultPrettyPrinter();
        try {
            objectWriter.writeValue(outputStream, jsonSchema);
        } catch (IOException e) {
            warningsListener.accept(
                    Message.error("Failed to generate json: " + e.getMessage()));
            return 3;
        }

        return 0;
    }
}