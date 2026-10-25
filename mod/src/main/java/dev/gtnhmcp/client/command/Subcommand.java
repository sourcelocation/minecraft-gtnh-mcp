package dev.gtnhmcp.client.command;

/** One {@code /mcp <name>} action; add one by passing it to {@link McpCommand}. */
public interface Subcommand {

    String name();

    void run(Chat chat);
}
