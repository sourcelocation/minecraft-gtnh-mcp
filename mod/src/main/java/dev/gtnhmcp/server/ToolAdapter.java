package dev.gtnhmcp.server;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import dev.gtnhmcp.core.Tool;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/** Exposes catalog entries as MCP tools and counts the calls MCP clients make. */
final class ToolAdapter {

    /** Every call is idempotent and closed-world; only what it touches differs. */
    private static final Map<Tool.Access, McpSchema.ToolAnnotations> ANNOTATIONS = Map.of(
        Tool.Access.READ,
        new McpSchema.ToolAnnotations(null, true, false, true, false, null),
        Tool.Access.WRITE,
        new McpSchema.ToolAnnotations(null, false, false, true, false, null),
        Tool.Access.DELETE,
        new McpSchema.ToolAnnotations(null, false, true, true, false, null));

    private final ToolCatalog catalog;
    private final AtomicLong calls = new AtomicLong();

    ToolAdapter(ToolCatalog catalog) {
        this.catalog = catalog;
    }

    long calls() {
        return calls.get();
    }

    SyncToolSpecification adapt(ToolCatalog.Entry entry) {
        Tool<?, ?> tool = entry.tool();
        McpSchema.Tool spec = McpSchema.Tool.builder(tool.name())
            .title(tool.title())
            .description(tool.description())
            .inputSchema(entry.input())
            .outputSchema(entry.output())
            .annotations(ANNOTATIONS.get(tool.access()))
            .build();
        return new SyncToolSpecification(spec, (context, request) -> {
            calls.incrementAndGet();
            return catalog.call(entry, request.arguments())
                .fold(ToolAdapter::success, ToolAdapter::failure);
        });
    }

    private static CallToolResult success(ToolResult.Success result) {
        return CallToolResult.builder()
            .structuredContent(result.structured())
            .addTextContent(result.json())
            .build();
    }

    private static CallToolResult failure(ToolResult.Failure result) {
        return CallToolResult.builder()
            .addTextContent(result.message())
            .isError(true)
            .build();
    }
}
