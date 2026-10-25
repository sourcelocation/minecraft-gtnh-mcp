package dev.gtnhmcp.server;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolException;
import dev.gtnhmcp.output.OutputFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every tool with its generated schemas. All tool calls go through here, so MCP clients and the in-game debug screen
 * call tools identically and see identical output; {@link OutputFormat} decides how that output reads.
 */
public final class ToolCatalog {

    /** A tool with its generated input and output JSON Schemas. */
    public record Entry(Tool<?, ?> tool, Map<String, Object> input, Map<String, Object> output) {}

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JsonMapper json;
    private final ClientThread clientThread;
    private final Logger log;
    private final List<Entry> entries;

    ToolCatalog(List<Tool<?, ?>> tools, OutputFormat format, ClientThread clientThread, Logger log) {
        this.json = format.mapper();
        this.clientThread = clientThread;
        this.log = log;
        this.entries = tools.stream()
            .map(tool -> new Entry(tool, format.schema(tool.input()), format.schema(tool.output())))
            .toList();
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Arguments as decoded JSON, the way an MCP client sends them. */
    public ToolResult call(Entry entry, Map<String, Object> arguments) {
        return invoke(entry.tool(), arguments == null ? Map.of() : arguments);
    }

    private <I, O> ToolResult invoke(Tool<I, O> tool, Map<String, Object> arguments) {
        I input;
        try {
            input = json.convertValue(arguments, json.constructType(tool.input()));
        } catch (JacksonException e) {
            return new ToolResult.Failure("Invalid arguments: " + e.getOriginalMessage());
        }
        try {
            Map<String, Object> output = json.convertValue(clientThread.call(() -> tool.call(input)), MAP);
            return new ToolResult.Success(
                output,
                json.writeValueAsString(output),
                json.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(output));
        } catch (ToolException e) {
            return new ToolResult.Failure(e.getMessage());
        } catch (Exception e) {
            log.warn("Tool {} failed", tool.name(), e);
            return new ToolResult.Failure(
                e.getClass()
                    .getSimpleName() + ": "
                    + e.getMessage());
        }
    }
}
