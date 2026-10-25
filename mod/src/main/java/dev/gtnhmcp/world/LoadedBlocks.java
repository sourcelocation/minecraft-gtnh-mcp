package dev.gtnhmcp.world;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Reads loaded, non-air blocks: a box walked once, naming each and handing it to every sink, or one position. */
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
                BlockAt at = read(world, x, y, z);
                if (at == null) continue;
                String name = names.name(at);
                for (BlockSink sink : sinks) sink.accept(at, name);
            }
        }
        return partial;
    }

    /** The block at a position, or null when it is air, unloaded or outside the world. */
    BlockAt at(World world, int x, int y, int z) {
        if (y < 0 || y > 255 || !world.blockExists(x, y, z)) return null;
        return read(world, x, y, z);
    }

    String name(BlockAt at) {
        return names.name(at);
    }

    private static BlockAt read(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.isAir(world, x, y, z)) return null;
        int meta = world.getBlockMetadata(x, y, z);
        return new BlockAt(
            world,
            x,
            y,
            z,
            block,
            meta,
            block.hasTileEntity(meta) ? world.getTileEntity(x, y, z) : null);
    }
}
