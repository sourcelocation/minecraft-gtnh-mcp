package dev.gtnhmcp.world;

import net.minecraft.tileentity.TileEntity;

import dev.gtnhmcp.core.Extension;
import dev.gtnhmcp.core.Items;

/**
 * Names a block with a tile entity the way a player would, e.g. a GregTech machine by its machine or a drawer by its
 * contents; register in {@code META-INF/services}.
 */
public interface BlockDescriber extends Extension {

    /** The block's name, or null when this describer does not know it. */
    String describe(TileEntity tile, Items items);
}
