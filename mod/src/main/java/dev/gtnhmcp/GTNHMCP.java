package dev.gtnhmcp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = GTNHMCP.MODID, name = "GTNH MCP Bridge", version = Tags.VERSION, acceptableRemoteVersions = "*")
public final class GTNHMCP {

    public static final String MODID = "gtnhmcp";
    private static final Logger LOG = LogManager.getLogger(MODID);
    private BridgeServer bridge;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // This bridge intentionally reads client-only APIs (NEI and VP cache). Do not
        // instantiate its router on a dedicated server, where client classes may not load.
        if (FMLCommonHandler.instance()
            .getSide() != Side.CLIENT) return;
        ClientDispatcher dispatcher = new ClientDispatcher();
        FMLCommonHandler.instance()
            .bus()
            .register(dispatcher);
        bridge = new BridgeServer(dispatcher);
        if (!bridge.start()) {
            LOG.error("GTNH MCP bridge could not bind to 127.0.0.1:{}; MCP access is disabled", BridgeServer.PORT);
            bridge = null;
            return;
        }
        Runtime.getRuntime()
            .addShutdownHook(new Thread("gtnh-mcp-bridge-stop") {

                @Override
                public void run() {
                    if (bridge != null) bridge.stop();
                }
            });
    }

    @Mod.EventHandler
    public void stopping(FMLServerStoppingEvent event) {
        if (bridge != null) bridge.stop();
    }
}
