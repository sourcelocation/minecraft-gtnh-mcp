package dev.gtnhmcp.server;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.ToolException;

final class MinecraftGame implements Game {

    private final Items items;

    MinecraftGame(Items items) {
        this.items = items;
    }

    public EntityPlayer player() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) throw new ToolException("Not in a world; join one first");
        return player;
    }

    public Items items() {
        return items;
    }
}
