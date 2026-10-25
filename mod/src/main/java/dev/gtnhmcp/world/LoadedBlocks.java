package dev.gtnhmcp.world;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Walks the loaded, non-air blocks of a box once, naming each and handing it to every sink. */
final class LoadedBlocks {

    private final BlockNames names;

    LoadedBlocks(BlockNames names) {
        this.names = names;
    }

    /** Returns true when part of the box is in unloaded chunks and was skipped. */
    boolean walk(World world, Box box, List<BlockSink> sinks) {
        boolean partial = false;
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            if (!world.blockExists(x, 0, z)) {
                partial = true;
                continue;
            }
            for (int y = box.minY(); y <= box.maxY(); y++) {
                Block block = world.getBlock(x, y, z);
                if (block.isAir(world, x, y, z)) continue;
                int meta = world.getBlockMetadata(x, y, z);
                BlockAt at = new BlockAt(
                    x,
                    y,
                    z,
                    block,
                    meta,
                    block.hasTileEntity(meta) ? world.getTileEntity(x, y, z) : null);
                String name = names.name(world, at);
                for (BlockSink sink : sinks) sink.accept(at, name);
            }
        }
        return partial;
    }
}
