package dev.gtnhmcp.client.command;

import dev.gtnhmcp.server.McpService;

public final class StatusCommand implements Subcommand {

    private final McpService service;

    public StatusCommand(McpService service) {
        this.service = service;
    }

    public String name() {
        return "status";
    }

    public void run(Chat chat) {
        if (!service.running()) chat.say("gtnhmcp.command.stopped");
        else chat.say("gtnhmcp.command.running", service.url(), service.tools(), service.calls());
        chat.links("/mcp setup", "/mcp debug", service.running() ? "/mcp off" : "/mcp on");
    }
}
