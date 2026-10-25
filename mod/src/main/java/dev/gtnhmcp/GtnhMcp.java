package dev.gtnhmcp;

import java.util.List;

import net.minecraftforge.client.ClientCommandHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import dev.gtnhmcp.client.GuiFactory;
import dev.gtnhmcp.client.McpConfig;
import dev.gtnhmcp.client.McpSwitch;
import dev.gtnhmcp.client.command.DebugCommand;
import dev.gtnhmcp.client.command.McpCommand;
import dev.gtnhmcp.client.command.SetupCommand;
import dev.gtnhmcp.client.command.StatusCommand;
import dev.gtnhmcp.client.command.SwitchCommand;
import dev.gtnhmcp.client.debug.ConsoleLauncher;
import dev.gtnhmcp.client.debug.form.ArgumentForms;
import dev.gtnhmcp.client.debug.view.ArgumentViews;
import dev.gtnhmcp.server.ClientThread;
import dev.gtnhmcp.server.McpBridge;
import dev.gtnhmcp.server.McpService;

/** Wires the mod together on the client; a dedicated server has no NEI data or player to serve. */
@Mod(
    modid = "gtnhmcp",
    name = "GTNH MCP",
    version = Tags.VERSION,
    acceptableRemoteVersions = "*",
    guiFactory = "dev.gtnhmcp.client.GuiFactory",
    dependencies = "required-after:gtnhlib;required-after:modularui2;required-after:NotEnoughItems;required-after:gregtech;after:betterquesting")
public final class GtnhMcp {

    private static final Logger LOG = LogManager.getLogger("gtnhmcp");

    private static boolean client() {
        return FMLCommonHandler.instance()
            .getSide() == Side.CLIENT;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws ConfigException {
        if (client()) ConfigurationManager.registerConfig(McpConfig.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!client()) return;
        ClientThread clientThread = ClientThread.register();
        McpService service = McpBridge.create(Tags.VERSION, McpConfig.port, clientThread, LOG);
        McpSwitch mcpSwitch = new McpSwitch(service);
        mcpSwitch.restore();
        ConsoleLauncher console = new ConsoleLauncher(
            service.catalog(),
            ArgumentForms.standard(),
            ArgumentViews.standard());
        GuiFactory.install(console);
        StatusCommand status = new StatusCommand(service);
        ClientCommandHandler.instance.registerCommand(
            new McpCommand(
                List.of(
                    status,
                    new SetupCommand(service),
                    new SwitchCommand(true, mcpSwitch, status),
                    new SwitchCommand(false, mcpSwitch, status),
                    new DebugCommand(console, clientThread))));
        Runtime.getRuntime()
            .addShutdownHook(new Thread(service::stop, "gtnh-mcp-stop"));
    }
}
