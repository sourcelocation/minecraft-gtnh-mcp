package dev.gtnhmcp.server;

import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import dev.gtnhmcp.core.Extension;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolModule;
import dev.gtnhmcp.item.NeiItems;
import dev.gtnhmcp.output.OutputFormat;

/** Composition root: discovers every {@link ToolModule} and builds the (not yet started) MCP service. */
public final class McpBridge {

    private McpBridge() {}

    public static McpService create(String version, int port, ClientThread clientThread, Logger log) {
        Game game = new MinecraftGame(new NeiItems());
        List<Tool<?, ?>> tools = Extension.load(ToolModule.class)
            .stream()
            .flatMap(
                module -> module.tools(game)
                    .stream())
            .sorted(Comparator.comparing(Tool::name))
            .toList();
        return new McpService(new ToolCatalog(tools, new OutputFormat(), clientThread, log), version, port, log);
    }
}
