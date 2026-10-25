package dev.gtnhmcp.client.command;

/** Where a subcommand reports to the player. */
public interface Chat {

    /** A translated line with the mod's prefix. */
    void say(String langKey, Object... args);

    /** A row of commands the player can click to run. */
    void links(String... commands);

    void copyToClipboard(String text);
}
