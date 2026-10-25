package dev.gtnhmcp.world;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * The six adjacent blocks by name, so the model sees what touches what without rebuilding the grid; air is left out.
 */
final class Neighbours implements BlockDetail {

    private final LoadedBlocks blocks;

    Neighbours(LoadedBlocks blocks) {
        this.blocks = blocks;
    }

    public void describe(BlockAt block, Map<String, Object> details) {
        Map<String, String> around = new LinkedHashMap<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            BlockAt next = blocks
                .at(block.world(), block.x() + side.offsetX, block.y() + side.offsetY, block.z() + side.offsetZ);
            if (next != null) around.put(
                side.name()
                    .toLowerCase(Locale.ROOT),
                blocks.name(next));
        }
        details.put("neighbours", around);
    }
}
