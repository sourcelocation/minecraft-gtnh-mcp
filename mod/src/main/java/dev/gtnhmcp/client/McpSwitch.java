package dev.gtnhmcp.client;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import dev.gtnhmcp.server.McpService;

/** Turns the MCP endpoint on or off now, and remembers the choice in the config file. */
public final class McpSwitch {

    private final McpService service;

    public McpSwitch(McpService service) {
        this.service = service;
    }

    /** Applies the saved setting at startup. */
    public void restore() {
        if (McpConfig.enabled) service.start();
    }

    /** Returns false when turning on failed, e.g. because the port is taken; the setting is then left unchanged. */
    public boolean set(boolean on) {
        if (on && !service.start()) return false;
        if (!on) service.stop();
        McpConfig.enabled = on;
        ConfigurationManager.save(McpConfig.class);
        return true;
    }
}
