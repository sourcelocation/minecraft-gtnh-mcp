package dev.gtnhmcp.core;

import net.minecraft.entity.player.EntityPlayer;

/** What tools may read from the running client. */
public interface Game {

    /** The local player; throws {@link ToolException} outside a world. */
    EntityPlayer player();

    Items items();
}
