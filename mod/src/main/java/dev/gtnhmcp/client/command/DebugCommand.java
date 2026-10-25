package dev.gtnhmcp.client.command;

import java.util.concurrent.Executor;

import dev.gtnhmcp.client.debug.ConsoleLauncher;

/** Opens the in-game tool console. */
public final class DebugCommand implements Subcommand {

    private final ConsoleLauncher console;
    private final Executor nextTick;

    /** {@code nextTick} must run after the chat screen has closed, or closing it would close the console too. */
    public DebugCommand(ConsoleLauncher console, Executor nextTick) {
        this.console = console;
        this.nextTick = nextTick;
    }

    public String name() {
        return "debug";
    }

    public void run(Chat chat) {
        nextTick.execute(console::open);
    }
}
