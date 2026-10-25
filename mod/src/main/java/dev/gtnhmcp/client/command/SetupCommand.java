package dev.gtnhmcp.client.command;

import dev.gtnhmcp.server.McpService;

/** Copies the Claude Code setup command; other clients just need the URL. */
public final class SetupCommand implements Subcommand {

    private final McpService service;

    public SetupCommand(McpService service) {
        this.service = service;
    }

    public String name() {
        return "setup";
    }

    public void run(Chat chat) {
        String command = "claude mcp add --transport http gtnh " + service.url();
        chat.copyToClipboard(command);
        chat.say("gtnhmcp.command.copied", command);
        chat.say("gtnhmcp.command.other_clients", service.url());
    }
}
