package dev.gtnhmcp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import dev.gtnhmcp.server.McpBridge;

@Mod(modid = "gtnhmcp", name = "GTNH MCP", version = Tags.VERSION, acceptableRemoteVersions = "*")
public final class GtnhMcp {

    private static final Logger LOG = LogManager.getLogger("gtnhmcp");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // NEI and the player inventory are client-side; a dedicated server has nothing to serve.
        if (FMLCommonHandler.instance()
            .getSide() != Side.CLIENT) return;
        try {
            AutoCloseable server = McpBridge.start(Tags.VERSION, LOG);
            Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    try {
                        server.close();
                    } catch (Exception ignored) {
                        // The JVM is exiting.
                    }
                }, "gtnh-mcp-stop"));
        } catch (Exception e) {
            LOG.error("GTNH MCP could not start; MCP access is disabled", e);
        }
    }
}
