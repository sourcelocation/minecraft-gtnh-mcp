package dev.gtnhmcp.server;

import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;

import cpw.mods.fml.common.FMLCommonHandler;
import dev.gtnhmcp.core.Extension;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolModule;
import dev.gtnhmcp.item.NeiItems;
import tools.jackson.databind.json.JsonMapper;

/** Composition root: discovers every {@link ToolModule} and serves its tools over MCP. */
public final class McpBridge {

    private McpBridge() {}

    public static AutoCloseable start(String version, Logger log) throws Exception {
        // Tool output omits nulls and empty values to save tokens; the SDK keeps its own, standard mapper.
        JsonMapper output = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_EMPTY)
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY))
            .build();
        ClientThread clientThread = new ClientThread();
        FMLCommonHandler.instance()
            .bus()
            .register(clientThread);
        Game game = new MinecraftGame(new NeiItems());
        ToolAdapter adapter = new ToolAdapter(output, clientThread, log);
        List<Tool<?, ?>> tools = Extension.load(ToolModule.class)
            .stream()
            .flatMap(
                module -> module.tools(game)
                    .stream())
            .sorted(Comparator.comparing(Tool::name))
            .toList();
        log.info("Serving {} MCP tools on http://127.0.0.1:{}{}", tools.size(), McpHttpServer.PORT, McpHttpServer.PATH);
        return new McpHttpServer(
            version,
            tools.stream()
                .map(adapter::adapt)
                .toList());
    }
}
