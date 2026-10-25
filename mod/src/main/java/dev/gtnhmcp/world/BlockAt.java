package dev.gtnhmcp.world;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** One loaded, non-air block; {@code tile} is null for blocks without a tile entity. */
record BlockAt(World world, int x, int y, int z, Block block, int meta, TileEntity tile) {

    /** {@code x,y,z}. */
    String at() {
        return x + "," + y + "," + z;
    }
}
