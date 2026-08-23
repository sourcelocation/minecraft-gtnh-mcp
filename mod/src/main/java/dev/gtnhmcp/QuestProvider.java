package dev.gtnhmcp;

import net.minecraft.client.Minecraft;

import com.google.gson.JsonObject;

final class QuestProvider implements QueryProvider {

    private final BetterQuestingProvider quests = new BetterQuestingProvider();

    public JsonObject query(String method, JsonObject params) {
        if (Minecraft.getMinecraft().thePlayer == null) return BridgeResponses.unsupported("player unavailable");
        return BridgeResponses.response(
            quests.query(params, Minecraft.getMinecraft().thePlayer),
            "client",
            Minecraft.getMinecraft()
                .isSingleplayer() ? "authoritative" : "observed");
    }
}
