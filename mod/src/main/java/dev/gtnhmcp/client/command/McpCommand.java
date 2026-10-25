package dev.gtnhmcp.client.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.commands.GTNHClientCommand;

/**
 * {@code /mcp}: dispatches to its subcommands, the first being the default. Client-side, so it works on any server.
 */
public final class McpCommand extends GTNHClientCommand {

    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();
    private final Subcommand fallback;
    private final Chat chat = new PlayerChat();

    public McpCommand(List<Subcommand> subcommands) {
        for (Subcommand subcommand : subcommands) this.subcommands.put(subcommand.name(), subcommand);
        this.fallback = subcommands.get(0);
    }

    public String getCommandName() {
        return "mcp";
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/mcp [" + String.join("|", subcommands.keySet()) + "]";
    }

    public void processCommand(ICommandSender sender, String[] args) {
        Subcommand subcommand = args.length == 0 ? fallback : subcommands.get(args[0]);
        if (subcommand == null) chat.say("gtnhmcp.command.usage", getCommandUsage(sender));
        else subcommand.run(chat);
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(
            args,
            subcommands.keySet()
                .toArray(new String[0]))
            : null;
    }

    /** Chat output through GTNHLib's client-command helpers. */
    private final class PlayerChat implements Chat {

        public void say(String langKey, Object... args) {
            addChatMessage(prefix().appendSibling(new ChatComponentTranslation(langKey, args)));
        }

        public void links(String... commands) {
            IChatComponent row = new ChatComponentText("");
            for (String command : commands) row.appendText("  ")
                .appendSibling(link(command));
            addChatMessage(row);
        }

        public void copyToClipboard(String text) {
            McpCommand.this.copyToClipboard(text);
        }

        private IChatComponent prefix() {
            return new ChatComponentText("[MCP] ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD));
        }

        private IChatComponent link(String command) {
            ChatStyle style = new ChatStyle().setColor(EnumChatFormatting.AQUA)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            return new ChatComponentText("[" + command + "]").setChatStyle(style);
        }
    }
}
