package dev.gtnhmcp.core;

import net.minecraft.util.EnumChatFormatting;

public final class Text {

    private Text() {}

    /** Strips Minecraft formatting codes and collapses whitespace. */
    public static String clean(String text) {
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text == null ? "" : text);
        return plain == null ? ""
            : plain.trim()
                .replaceAll("\\s+", " ");
    }
}
