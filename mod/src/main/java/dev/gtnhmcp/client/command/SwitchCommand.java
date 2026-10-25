package dev.gtnhmcp.client.command;

import dev.gtnhmcp.client.McpSwitch;

/** {@code /mcp on} or {@code /mcp off}, then the resulting status. */
public final class SwitchCommand implements Subcommand {

    private final boolean on;
    private final McpSwitch mcpSwitch;
    private final Subcommand status;

    public SwitchCommand(boolean on, McpSwitch mcpSwitch, Subcommand status) {
        this.on = on;
        this.mcpSwitch = mcpSwitch;
        this.status = status;
    }

    public String name() {
        return on ? "on" : "off";
    }

    public void run(Chat chat) {
        if (mcpSwitch.set(on)) status.run(chat);
        else chat.say("gtnhmcp.command.failed");
    }
}
