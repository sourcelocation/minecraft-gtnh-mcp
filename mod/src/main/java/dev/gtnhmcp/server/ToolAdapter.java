package dev.gtnhmcp.server;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;

import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolException;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Exposes a {@link Tool} as an MCP tool: generated schemas, structured output, errors as {@code isError}. */
final class ToolAdapter {

    private static final McpSchema.ToolAnnotations READ_ONLY = new McpSchema.ToolAnnotations(
        null,
        true,
        false,
        true,
        false,
        null);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JsonMapper json;
    private final ClientThread clientThread;
    private final Logger log;
    private final SchemaGenerator schemas;

    ToolAdapter(JsonMapper json, ClientThread clientThread, Logger log) {
        this.json = json;
        this.clientThread = clientThread;
        this.log = log;
        this.schemas = new SchemaGenerator(
            new SchemaGeneratorConfigBuilder(json, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
                .build());
    }

    SyncToolSpecification adapt(Tool<?, ?> tool) {
        McpSchema.Tool spec = McpSchema.Tool.builder(tool.name())
            .description(tool.description())
            .inputSchema(schema(tool.input()))
            .outputSchema(schema(tool.output()))
            .annotations(READ_ONLY)
            .build();
        return new SyncToolSpecification(spec, (context, request) -> call(tool, request.arguments()));
    }

    private Map<String, Object> schema(Type type) {
        return json.convertValue(schemas.generateSchema(type), MAP);
    }

    private <I, O> McpSchema.CallToolResult call(Tool<I, O> tool, Map<String, Object> arguments) {
        I input;
        try {
            input = json.convertValue(arguments == null ? Map.of() : arguments, json.constructType(tool.input()));
        } catch (JacksonException e) {
            return error("Invalid arguments: " + e.getOriginalMessage());
        }
        try {
            O output = clientThread.call(() -> tool.call(input));
            Map<String, Object> structured = json.convertValue(output, MAP);
            return McpSchema.CallToolResult.builder()
                .structuredContent(structured)
                .addTextContent(json.writeValueAsString(structured))
                .build();
        } catch (ToolException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            log.warn("Tool {} failed", tool.name(), e);
            return error(
                e.getClass()
                    .getSimpleName() + ": "
                    + e.getMessage());
        }
    }

    private static McpSchema.CallToolResult error(String message) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(message)
            .isError(true)
            .build();
    }
}
