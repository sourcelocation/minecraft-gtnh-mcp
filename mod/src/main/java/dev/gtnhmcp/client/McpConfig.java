package dev.gtnhmcp.client;

import com.gtnewhorizon.gtnhlib.config.Config;

/** Saved to config/gtnhmcp.cfg by GTNHLib, which also builds the in-game config screen from these fields. */
@Config(modid = "gtnhmcp")
@Config.LangKey("gtnhmcp.config")
public final class McpConfig {

    @Config.Comment("Serve MCP when the game starts. /mcp on and /mcp off change this immediately.")
    @Config.LangKey("gtnhmcp.config.enabled")
    @Config.DefaultBoolean(true)
    public static boolean enabled = true;

    @Config.Comment("Port on 127.0.0.1 that MCP clients connect to.")
    @Config.LangKey("gtnhmcp.config.port")
    @Config.DefaultInt(8765)
    @Config.RangeInt(min = 1024, max = 65535)
    @Config.RequiresMcRestart
    public static int port = 8765;
}
